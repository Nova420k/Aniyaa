package com.nyaa.aniyaa.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nyaa.aniyaa.AniyaaApplication
import com.nyaa.aniyaa.data.model.CatalogSite
import com.nyaa.aniyaa.data.model.Category
import com.nyaa.aniyaa.data.model.FilterOption
import com.nyaa.aniyaa.data.model.SavedSearch
import com.nyaa.aniyaa.data.model.SearchHistoryEntry
import com.nyaa.aniyaa.data.model.SearchParams
import com.nyaa.aniyaa.data.model.SortField
import com.nyaa.aniyaa.data.model.SortOrder
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.data.model.toSavedSearch
import com.nyaa.aniyaa.data.network.SiteConfig
import com.nyaa.aniyaa.data.network.toUserMessage
import com.nyaa.aniyaa.data.repository.mergeSearchPages
import com.nyaa.aniyaa.util.LocalFilter
import com.nyaa.aniyaa.util.passes
import com.nyaa.aniyaa.work.SavedSearchWorker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val torrents: List<Torrent> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = true,
    val error: String? = null,
    val notice: String? = null,
    val searchParams: SearchParams = SearchParams(),
    val localFilter: LocalFilter = LocalFilter(),
    val hiddenCount: Int = 0,
    val hasSearched: Boolean = false
)

class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AniyaaApplication
    private val repository = app.nyaaRepository
    private val prefs = app.prefs
    private val historyRepository = app.historyRepository
    private val savedSearchRepository = app.savedSearchRepository

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private var localFilter: LocalFilter = prefs.defaultLocalFilter()
    private var rawTorrents: List<Torrent> = emptyList()
    private var autoLoadsRemaining: Int = 0

    private val _uiState = MutableStateFlow(
        SearchUiState(
            searchParams = prefs.defaultSearchParams(),
            localFilter = localFilter
        )
    )
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _pendingNsfwParams = MutableStateFlow<SearchParams?>(null)
    val pendingNsfwParams: StateFlow<SearchParams?> = _pendingNsfwParams.asStateFlow()

    private var searchJob: Job? = null
    private var loadMoreJob: Job? = null
    private var requestGeneration: Long = 0L
    private val siteSnapshots = mutableMapOf<CatalogSite, SiteSnapshot>()

    init {
        if (!prefs.sukebeiEnabled && _uiState.value.searchParams.site.nsfw) {
            val nyaa = prefs.defaultSearchParams(CatalogSite.NYAA)
            SiteConfig.currentSite = CatalogSite.NYAA
            _uiState.update { it.copy(searchParams = nyaa, localFilter = localFilter) }
        }
        restoreFromCache()
    }

    private data class SiteSnapshot(
        val query: String,
        val state: SearchUiState,
        val raw: List<Torrent>
    )

    fun updateQuery(query: String) {
        _query.value = query
    }

    fun setCategory(category: Category) {
        val current = _uiState.value.searchParams
        if (current.category == category) return
        applyFilters(category, current.filter, current.sortField, current.sortOrder)
    }

    fun setFilter(filter: FilterOption) {
        val current = _uiState.value.searchParams
        val next = if (current.filter == filter) FilterOption.ALL else filter
        if (current.filter == next) return
        applyFilters(current.category, next, current.sortField, current.sortOrder)
    }

    fun setSort(sortField: SortField, sortOrder: SortOrder) {
        val current = _uiState.value.searchParams
        if (current.sortField == sortField && current.sortOrder == sortOrder) return
        applyFilters(current.category, current.filter, sortField, sortOrder)
    }

    fun setLocalFilter(filter: LocalFilter) {
        if (localFilter == filter) return
        localFilter = filter
        autoLoadsRemaining = AUTO_LOAD_LIMIT
        reapplyFilters()
        maybeAutoLoad()
    }

    fun applyFilters(
        category: Category,
        filter: FilterOption,
        sortField: SortField,
        sortOrder: SortOrder
    ) {
        _uiState.update {
            it.copy(
                searchParams = it.searchParams.copy(
                    category = category,
                    filter = filter,
                    sortField = sortField,
                    sortOrder = sortOrder,
                    page = 1
                )
            )
        }
        search(forceNetwork = true)
    }

    fun switchSite(site: CatalogSite) {
        if (site.nsfw && !prefs.sukebeiEnabled) return
        val current = _uiState.value.searchParams.site
        if (current == site) return
        siteSnapshots[current] = SiteSnapshot(_query.value, _uiState.value, rawTorrents)
        SiteConfig.currentSite = site
        val restored = siteSnapshots[site]
        if (restored != null) {
            _query.value = restored.query
            rawTorrents = restored.raw
            _uiState.value = restored.state.copy(
                searchParams = restored.state.searchParams.copy(site = site).withValidCategory(),
                localFilter = localFilter
            )
            reapplyFilters()
            if (restored.state.torrents.isEmpty() && (prefs.loadLatestOnStart || restored.query.isNotBlank())) {
                search(recordHistory = false)
            }
            return
        }
        val defaults = prefs.defaultSearchParams(site).copy(query = _query.value)
        rawTorrents = emptyList()
        _uiState.update {
            it.copy(
                searchParams = defaults,
                torrents = emptyList(),
                hiddenCount = 0,
                localFilter = localFilter,
                hasSearched = false,
                isLoading = false,
                isRefreshing = false,
                error = null,
                canLoadMore = true
            )
        }
        if (_query.value.isNotBlank()) {
            search()
        } else {
            restoreFromCache()
        }
    }

    fun applyParams(params: SearchParams, recordHistory: Boolean = true) {
        val valid = params.withValidCategory()
        if (valid.site.nsfw && !prefs.sukebeiEnabled) {
            _pendingNsfwParams.value = valid
            return
        }
        applyParamsConfirmed(valid, recordHistory)
    }

    fun confirmPendingNsfw() {
        val pending = _pendingNsfwParams.value ?: return
        _pendingNsfwParams.value = null
        prefs.sukebeiEnabled = true
        applyParamsConfirmed(pending, recordHistory = true)
    }

    fun dismissPendingNsfw() {
        _pendingNsfwParams.value = null
    }

    private fun applyParamsConfirmed(valid: SearchParams, recordHistory: Boolean) {
        val current = _uiState.value.searchParams.site
        if (valid.site != current) {
            siteSnapshots[current] = SiteSnapshot(_query.value, _uiState.value, rawTorrents)
        }
        if (valid.site != SiteConfig.currentSite) {
            SiteConfig.currentSite = valid.site
        }
        _query.value = valid.query
        _uiState.update { it.copy(searchParams = valid.copy(page = 1)) }
        search(recordHistory = recordHistory)
    }

    fun applyHistory(entry: SearchHistoryEntry) {
        applyParams(entry.toSearchParams())
    }

    fun applySavedSearch(search: SavedSearch) {
        applyParams(search.toSearchParams())
    }

    fun resetFilters() {
        val site = _uiState.value.searchParams.site
        val defaults = prefs.defaultSearchParams(site)
        localFilter = prefs.defaultLocalFilter()
        val current = _uiState.value.searchParams
        val sameServer = current.category == defaults.category &&
            current.filter == defaults.filter &&
            current.sortField == defaults.sortField &&
            current.sortOrder == defaults.sortOrder
        if (sameServer) {
            autoLoadsRemaining = AUTO_LOAD_LIMIT
            reapplyFilters()
            maybeAutoLoad()
            return
        }
        applyFilters(
            category = defaults.category,
            filter = defaults.filter,
            sortField = defaults.sortField,
            sortOrder = defaults.sortOrder
        )
    }

    fun reapplyFilters() {
        val current = _uiState.value
        publishRaw(
            raw = rawTorrents,
            params = current.searchParams,
            canLoadMore = current.canLoadMore,
            hasSearched = current.hasSearched,
            isLoading = current.isLoading,
            isRefreshing = current.isRefreshing,
            isLoadingMore = current.isLoadingMore,
            error = current.error,
            notice = current.notice
        )
    }

    private fun restoreFromCache() {
        val params = _uiState.value.searchParams.copy(query = _query.value, page = 1).withValidCategory()
        viewModelScope.launch {
            val cached = runCatching { repository.search(params, fromCache = true) }
                .getOrNull()
                ?.getOrNull()
                .orEmpty()
            if (requestGeneration != 0L) return@launch
            if (cached.isNotEmpty()) {
                acceptTorrents(cached, params, replace = true, notice = null, refreshingAfter = true)
            }
            if (prefs.loadLatestOnStart || cached.isNotEmpty() || params.query.isNotBlank()) {
                search(recordHistory = false)
            }
        }
    }

    fun search(recordHistory: Boolean = true, forceNetwork: Boolean = false) {
        loadMoreJob?.cancel()
        searchJob?.cancel()
        autoLoadsRemaining = AUTO_LOAD_LIMIT

        val params = _uiState.value.searchParams.copy(query = _query.value, page = 1).withValidCategory()
        val generation = ++requestGeneration

        val hadResults = _uiState.value.torrents.isNotEmpty() &&
            _uiState.value.searchParams.site == params.site
        _uiState.update {
            it.copy(
                isLoading = !hadResults,
                isRefreshing = hadResults,
                isLoadingMore = false,
                error = null,
                searchParams = params,
                localFilter = localFilter,
                canLoadMore = true
            )
        }

        if (recordHistory && params.query.isNotBlank()) {
            viewModelScope.launch {
                historyRepository.add(params)
            }
        }

        searchJob = viewModelScope.launch {
            if (!forceNetwork && !hadResults) {
                val cached = runCatching { repository.search(params, fromCache = true) }.getOrNull()
                val cachedList = cached?.getOrNull().orEmpty()
                if (generation != requestGeneration) return@launch
                if (cachedList.isNotEmpty()) {
                    acceptTorrents(cachedList, params, replace = true, notice = null, refreshingAfter = true)
                }
            }

            val result = try {
                repository.search(params, forceNetwork = forceNetwork)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            if (generation != requestGeneration) return@launch
            result.fold(
                onSuccess = { torrents ->
                    val used = SiteConfig.resolvedBaseUrl(params.site)
                    val configured = prefs.baseUrl(params.site)
                    val notice = if (!used.equals(configured, ignoreCase = true)) "Using $used" else null
                    acceptTorrents(torrents, params, replace = true, notice = notice)
                },
                onFailure = { e ->
                    if (e is CancellationException) return@fold
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = e.toUserMessage(),
                            hasSearched = true
                        )
                    }
                }
            )
        }
    }

    fun refresh() = search(recordHistory = false, forceNetwork = true)

    fun loadNextPage() {
        if (loadMoreJob?.isActive == true) return

        val state = _uiState.value
        if (state.isLoading || state.isRefreshing || state.isLoadingMore || !state.canLoadMore) return

        val nextPage = state.searchParams.page + 1
        val params = state.searchParams.copy(page = nextPage)
        val generation = requestGeneration

        _uiState.update { it.copy(isLoadingMore = true, error = null) }

        loadMoreJob = viewModelScope.launch {
            val result = try {
                repository.search(params)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
            if (generation != requestGeneration) return@launch
            result.fold(
                onSuccess = { torrents ->
                    if (generation != requestGeneration) return@launch
                    acceptTorrents(torrents, params, replace = false, notice = _uiState.value.notice)
                },
                onFailure = { e ->
                    if (e is CancellationException) return@fold
                    _uiState.update {
                        it.copy(
                            isLoadingMore = false,
                            error = e.toUserMessage()
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    fun torrentByNavId(navId: String, site: CatalogSite? = null): Torrent? =
        rawTorrents.find {
            it.matchesNavId(navId) && (site == null || it.site == site)
        } ?: _uiState.value.torrents.find {
            it.matchesNavId(navId) && (site == null || it.site == site)
        }

    private fun currentFilter() = localFilter.toResultFilter(prefs.hideRules())

    private fun publishRaw(
        raw: List<Torrent>,
        params: SearchParams,
        canLoadMore: Boolean,
        hasSearched: Boolean,
        isLoading: Boolean,
        isRefreshing: Boolean,
        isLoadingMore: Boolean,
        error: String?,
        notice: String?
    ) {
        rawTorrents = raw
        val filtered = raw.filter { it.passes(currentFilter()) }
        _uiState.update {
            it.copy(
                torrents = filtered,
                hiddenCount = (raw.size - filtered.size).coerceAtLeast(0),
                searchParams = params,
                localFilter = localFilter,
                canLoadMore = canLoadMore,
                hasSearched = hasSearched,
                isLoading = isLoading,
                isRefreshing = isRefreshing,
                isLoadingMore = isLoadingMore,
                error = error,
                notice = notice
            )
        }
    }

    private fun acceptTorrents(
        incoming: List<Torrent>,
        params: SearchParams,
        replace: Boolean,
        notice: String?,
        refreshingAfter: Boolean = false
    ) {
        val (merged, canLoadMore) = mergeSearchPages(
            existing = if (replace) emptyList() else rawTorrents,
            incoming = incoming,
            replace = replace
        )
        publishRaw(
            raw = merged,
            params = params,
            canLoadMore = canLoadMore,
            hasSearched = true,
            isLoading = false,
            isRefreshing = refreshingAfter,
            isLoadingMore = false,
            error = null,
            notice = notice
        )
        if (!refreshingAfter) maybeAutoLoad()
    }

    private fun maybeAutoLoad() {
        val state = _uiState.value
        if (autoLoadsRemaining <= 0) return
        if (state.torrents.size >= MIN_VISIBLE_RESULTS) return
        if (state.hiddenCount <= 0) return
        if (!state.canLoadMore || state.isLoading || state.isRefreshing) return
        autoLoadsRemaining -= 1
        val pending = loadMoreJob
        viewModelScope.launch {
            pending?.join()
            loadNextPage()
        }
    }

    private companion object {
        const val MIN_VISIBLE_RESULTS = 10
        const val AUTO_LOAD_LIMIT = 4
    }

    suspend fun saveCurrentSearch(name: String, notify: Boolean): Long {
        val params = _uiState.value.searchParams.copy(query = _query.value)
        return saveSearch(params, name, notify)
    }

    suspend fun saveSearch(params: SearchParams, name: String, notify: Boolean): Long {
        val id = savedSearchRepository.add(params.toSavedSearch(name, notify))
        SavedSearchWorker.sync(getApplication(), savedSearchRepository.getNotifying().isNotEmpty())
        return id
    }
}

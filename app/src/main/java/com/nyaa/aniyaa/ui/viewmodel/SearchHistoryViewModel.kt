package com.nyaa.aniyaa.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nyaa.aniyaa.AniyaaApplication
import com.nyaa.aniyaa.data.model.SavedSearch
import com.nyaa.aniyaa.data.model.SearchHistoryEntry
import com.nyaa.aniyaa.data.model.SearchParams
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.util.passes
import com.nyaa.aniyaa.work.SavedSearchAlerts
import com.nyaa.aniyaa.work.SavedSearchWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SearchHistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AniyaaApplication
    private val historyRepository = app.historyRepository
    private val savedSearchRepository = app.savedSearchRepository
    private val viewedRepository = app.viewedListingRepository

    val history: StateFlow<List<SearchHistoryEntry>> = historyRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val savedSearches: StateFlow<List<SavedSearch>> = savedSearchRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val viewed: StateFlow<List<Torrent>> = viewedRepository.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun consumeMessage() {
        _message.value = null
    }

    fun addEntry(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            historyRepository.add(SearchParams(query = query))
        }
    }

    fun addEntry(params: SearchParams) {
        viewModelScope.launch { historyRepository.add(params) }
    }

    fun removeEntry(entry: SearchHistoryEntry) {
        viewModelScope.launch { historyRepository.remove(entry) }
    }

    fun clearHistory() {
        viewModelScope.launch { historyRepository.clear() }
    }

    fun deleteSavedSearch(id: Long) {
        viewModelScope.launch {
            savedSearchRepository.delete(id)
            syncAlerts()
        }
    }

    fun renameSavedSearch(search: SavedSearch, name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch { savedSearchRepository.update(search.copy(name = trimmed)) }
    }

    fun toggleNotify(search: SavedSearch) {
        viewModelScope.launch {
            savedSearchRepository.update(search.copy(notify = !search.notify))
            syncAlerts()
        }
    }

    fun checkSavedSearchNow(search: SavedSearch) {
        viewModelScope.launch {
            val result = app.nyaaRepository.search(search.toSearchParams(), forceNetwork = true)
            val torrents = result.getOrNull()
            if (torrents == null) {
                _message.value = result.exceptionOrNull()?.message ?: "Could not check this search"
                return@launch
            }
            val ids = torrents.map { it.id }.filter { it.isNotBlank() }
            val newIds = SavedSearchAlerts.newIds(ids, search.lastSeenIds).toSet()
            val visibleNew = torrents.filter { it.id in newIds && it.passes(app.prefs.defaultResultFilter()) }
            savedSearchRepository.update(
                search.copy(
                    lastSeenIds = SavedSearchAlerts.storeIds(ids),
                    lastCheckedAt = System.currentTimeMillis()
                )
            )
            _message.value = if (visibleNew.isEmpty()) {
                "No new results for ${search.displayName()}"
            } else {
                "${visibleNew.size} new listing${if (visibleNew.size == 1) "" else "s"} for ${search.displayName()}"
            }
        }
    }

    private suspend fun syncAlerts() {
        SavedSearchWorker.sync(getApplication(), savedSearchRepository.getNotifying().isNotEmpty())
    }

    fun recordViewed(torrent: Torrent) {
        viewModelScope.launch { viewedRepository.add(torrent) }
    }

    fun removeViewed(torrent: Torrent) {
        viewModelScope.launch { viewedRepository.remove(torrent) }
    }

    fun clearViewed() {
        viewModelScope.launch { viewedRepository.clear() }
    }

    suspend fun savedSearchById(id: Long): SavedSearch? = savedSearchRepository.getById(id)
}

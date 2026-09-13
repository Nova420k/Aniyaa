package com.nyaa.aniyaa.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nyaa.aniyaa.data.api.resolvedMagnet
import com.nyaa.aniyaa.data.model.CatalogSite
import com.nyaa.aniyaa.data.model.Category
import com.nyaa.aniyaa.data.model.FilterOption
import com.nyaa.aniyaa.data.model.SearchParams
import com.nyaa.aniyaa.data.model.SortField
import com.nyaa.aniyaa.data.model.SortOrder
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.AniyaaApplication
import com.nyaa.aniyaa.data.prefs.AppPreferences
import com.nyaa.aniyaa.util.copyText
import com.nyaa.aniyaa.util.shareText
import com.nyaa.aniyaa.util.torrentShareText
import com.nyaa.aniyaa.ui.theme.NyaaLeecher
import com.nyaa.aniyaa.ui.theme.NyaaRemake
import com.nyaa.aniyaa.ui.theme.NyaaSeeder
import com.nyaa.aniyaa.ui.theme.NyaaTrusted
import com.nyaa.aniyaa.ui.viewmodel.BookmarkViewModel
import com.nyaa.aniyaa.ui.viewmodel.SearchHistoryViewModel
import com.nyaa.aniyaa.ui.viewmodel.SearchUiState
import com.nyaa.aniyaa.ui.viewmodel.SearchViewModel
import com.nyaa.aniyaa.util.PubDateFormatter
import com.nyaa.aniyaa.util.formatCount
import com.nyaa.aniyaa.util.hasNotificationPermission
import com.nyaa.aniyaa.util.openMagnet
import com.nyaa.aniyaa.util.magnetExportText
import com.nyaa.aniyaa.util.parseReleaseTitle
import com.nyaa.aniyaa.util.qualityTags
import com.nyaa.aniyaa.util.prepareSavedSearchAlerts
import kotlinx.coroutines.launch

private const val LOAD_MORE_BUFFER = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onTorrentClick: (Torrent) -> Unit,
    onOpenSettings: () -> Unit = {},
    searchViewModel: SearchViewModel = viewModel(),
    searchHistoryViewModel: SearchHistoryViewModel = viewModel(),
    bookmarkViewModel: BookmarkViewModel = viewModel(),
    bottomPadding: Dp = 0.dp
) {
    val viewModel = searchViewModel
    val query by viewModel.query.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bookmarks by bookmarkViewModel.allBookmarks.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val prefs = AniyaaApplication.instance.prefs
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var followSave by remember { mutableStateOf<Pair<String, SearchParams>?>(null) }
    var pendingSave by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    val notifyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val pending = pendingSave
        pendingSave = null
        if (pending != null) {
            if (granted) prepareSavedSearchAlerts(context)
            scope.launch {
                viewModel.saveCurrentSearch(pending.first, granted && pending.second)
                snackbarHostState.showSnackbar("Search saved")
            }
        }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()
    val interactionSource = remember { MutableInteractionSource() }
    val bookmarkedIds = remember(bookmarks) { bookmarks.map { it.bookmarkKey() }.toSet() }
    val viewedListings by searchHistoryViewModel.viewed.collectAsStateWithLifecycle()
    val viewedKeys = remember(viewedListings) { viewedListings.map { it.bookmarkKey() }.toSet() }
    val filterScrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
    var showSukebeiWarning by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    var selectedKeys by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(uiState.error, uiState.torrents.isNotEmpty()) {
        val error = uiState.error
        if (error != null && uiState.torrents.isNotEmpty()) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }
    LaunchedEffect(uiState.notice) {
        val notice = uiState.notice
        if (notice != null) {
            snackbarHostState.showSnackbar(notice)
            viewModel.clearNotice()
        }
    }

    val searchIdentity = "${uiState.searchParams.site.id}|${uiState.searchParams.query}|${uiState.searchParams.category.value}|${uiState.searchParams.filter.value}|${uiState.searchParams.sortField.value}|${uiState.searchParams.sortOrder.value}"
    LaunchedEffect(searchIdentity) {
        listState.scrollToItem(0)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = viewModel::updateQuery,
                        placeholder = {
                            Text(
                                "Search…",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        interactionSource = interactionSource,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            keyboardController?.hide()
                            viewModel.search()
                        }),
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateQuery("") }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        leadingIcon = {
                            IconButton(onClick = {
                                keyboardController?.hide()
                                viewModel.search()
                            }) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        IconButton(onClick = {
                            followSave = null
                            showSaveDialog = true
                        }) {
                            Icon(
                                Icons.Default.BookmarkBorder,
                                contentDescription = "Save this search",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        IconButton(onClick = {
                            selecting = !selecting
                            if (!selecting) selectedKeys = emptySet()
                        }) {
                            Icon(
                                if (selecting) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = if (selecting) "Done selecting" else "Select listings",
                                tint = if (selecting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        IconButton(onClick = { showFilterSheet = true }) {
                            BadgedBox(
                                badge = {
                                    if (uiState.searchParams.hasActiveFilters()) {
                                        Badge()
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.FilterList,
                                    contentDescription = "Filter",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                if (prefs.sukebeiEnabled) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CatalogSite.entries.forEach { site ->
                            FilterChip(
                                selected = uiState.searchParams.site == site,
                                onClick = {
                                    if (site.nsfw && !prefs.sukebeiAcknowledged) {
                                        showSukebeiWarning = true
                                    } else {
                                        viewModel.switchSite(site)
                                    }
                                },
                                label = { Text(if (site.nsfw) "${site.displayName} 18+" else site.displayName) },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (selecting && selectedKeys.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val selected = uiState.torrents.filter { it.bookmarkKey() in selectedKeys }
                            selected.forEach { torrent ->
                                openMagnet(context, torrent.resolvedMagnet(), prefs.preferredTorrentPackage)
                            }
                            scope.launch { snackbarHostState.showSnackbar("Opened ${selected.size} magnet${if (selected.size == 1) "" else "s"}") }
                            selecting = false
                            selectedKeys = emptySet()
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Open magnets (${selectedKeys.size})") }
                    OutlinedButton(
                        onClick = {
                            val selected = uiState.torrents.filter { it.bookmarkKey() in selectedKeys }
                            val magnets = magnetExportText(selected)
                            if (magnets.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar("No magnet links to copy") }
                            } else {
                                copyText(context, "Magnets", magnets)
                                scope.launch { snackbarHostState.showSnackbar("Copied ${magnets.lines().size} magnet${if (magnets.lines().size == 1) "" else "s"}") }
                            }
                        }
                    ) { Text("Copy magnets") }
                }
            }
            SearchResultsBody(
                uiState = uiState,
                listState = listState,
                bottomPadding = bottomPadding,
                bookmarkedIds = bookmarkedIds,
                viewedKeys = viewedKeys,
                selecting = selecting,
                selectedKeys = selectedKeys,
                compact = prefs.compactCards,
                onToggleSelect = { torrent ->
                    val key = torrent.bookmarkKey()
                    selectedKeys = if (key in selectedKeys) selectedKeys - key else selectedKeys + key
                },
                onTorrentClick = onTorrentClick,
                onRetry = { viewModel.search(forceNetwork = true) },
                onOpenSettings = onOpenSettings,
                onRefresh = viewModel::refresh,
                onLoadMore = viewModel::loadNextPage,
                onMagnet = { torrent ->
                    val error = openMagnet(context, torrent.resolvedMagnet(), prefs.preferredTorrentPackage)
                    if (error != null) scope.launch { snackbarHostState.showSnackbar(error) }
                },
                onToggleBookmark = { torrent -> bookmarkViewModel.toggleBookmark(torrent) },
                onCopyMagnet = { torrent ->
                    copyText(context, "Magnet Link", torrent.resolvedMagnet())
                    scope.launch { snackbarHostState.showSnackbar("Copied magnet") }
                },
                onCopyTitle = { torrent ->
                    copyText(context, "Title", torrent.title)
                    scope.launch { snackbarHostState.showSnackbar("Copied title") }
                },
                onShare = { torrent ->
                    shareText(context, torrentShareText(torrent))?.let { msg ->
                        scope.launch { snackbarHostState.showSnackbar(msg) }
                    }
                },
                onFollow = { name, query ->
                    followSave = name to SearchParams(query = query, site = uiState.searchParams.site)
                    showSaveDialog = true
                },
                onSearchQuery = { text ->
                    viewModel.applyParams(SearchParams(query = text, site = uiState.searchParams.site))
                },
                onOpenUser = { username ->
                    viewModel.applyParams(SearchParams(query = "user:$username", site = uiState.searchParams.site))
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            FilterBottomSheetContent(
                searchParams = uiState.searchParams,
                scrollState = filterScrollState,
                categories = uiState.searchParams.site.categories,
                defaultCategory = prefs.defaultCategory(uiState.searchParams.site),
                defaultSortField = com.nyaa.aniyaa.data.model.sortFieldByValue(prefs.defaultSortFieldValue(uiState.searchParams.site)),
                defaultSortOrder = com.nyaa.aniyaa.data.model.sortOrderByValue(prefs.defaultSortOrderValue(uiState.searchParams.site)),
                onReset = viewModel::resetFilters,
                onSaveSearch = {
                    showFilterSheet = false
                    showSaveDialog = true
                },
                onApply = { category, filter, sortField, sortOrder ->
                    viewModel.applyFilters(category, filter, sortField, sortOrder)
                    scope.launch { sheetState.hide() }
                        .invokeOnCompletion { showFilterSheet = false }
                }
            )
        }
    }

    if (showSukebeiWarning) {
        AlertDialog(
            onDismissRequest = { showSukebeiWarning = false },
            title = { Text("Sukebei is 18+") },
            text = {
                Text(
                    "Sukebei lists adult content. You must be 18 or older to continue. " +
                        "You can switch back to Nyaa at any time."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        prefs.sukebeiEnabled = true
                        showSukebeiWarning = false
                        viewModel.switchSite(CatalogSite.SUKEBEI)
                    }
                ) { Text("I am 18+") }
            },
            dismissButton = {
                TextButton(onClick = { showSukebeiWarning = false }) { Text("Cancel") }
            }
        )
    }

    if (showSaveDialog) {
        val follow = followSave
        SaveSearchDialog(
            defaultName = follow?.first ?: query.ifBlank { "Latest listings" },
            onDismiss = {
                showSaveDialog = false
                followSave = null
            },
            onSave = { name, notify ->
                showSaveDialog = false
                val params = follow?.second
                followSave = null
                if (notify && !hasNotificationPermission(context) && Build.VERSION.SDK_INT >= 33) {
                    pendingSave = name to true
                    notifyLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    if (notify) prepareSavedSearchAlerts(context)
                    scope.launch {
                        if (params != null) viewModel.saveSearch(params, name, notify)
                        else viewModel.saveCurrentSearch(name, notify)
                        snackbarHostState.showSnackbar("Search saved")
                    }
                }
            }
        )
    }
}

@Composable
private fun SaveSearchDialog(
    defaultName: String,
    onDismiss: () -> Unit,
    onSave: (String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(defaultName) }
    var notify by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save this search") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                FilterChip(
                    selected = notify,
                    onClick = { notify = !notify },
                    label = { Text("Notify me of new results") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, notify) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchResultsBody(
    uiState: SearchUiState,
    listState: LazyListState,
    bottomPadding: Dp,
    bookmarkedIds: Set<String>,
    viewedKeys: Set<String> = emptySet(),
    selecting: Boolean = false,
    selectedKeys: Set<String> = emptySet(),
    compact: Boolean = false,
    onToggleSelect: (Torrent) -> Unit = {},
    onTorrentClick: (Torrent) -> Unit,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onMagnet: (Torrent) -> Unit,
    onToggleBookmark: (Torrent) -> Unit,
    onCopyMagnet: (Torrent) -> Unit,
    onCopyTitle: (Torrent) -> Unit,
    onShare: (Torrent) -> Unit,
    onFollow: (String, String) -> Unit,
    onSearchQuery: (String) -> Unit,
    onOpenUser: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize()
    ) {
        when {
            uiState.isLoading && uiState.torrents.isEmpty() -> {
                SearchLoadingPlaceholder(bottomPadding = bottomPadding)
            }
            !uiState.hasSearched && uiState.torrents.isEmpty() -> {
                SearchHomeEmpty(
                    siteName = uiState.searchParams.site.displayName,
                    bottomPadding = bottomPadding,
                    onShowLatest = onRetry
                )
            }
            uiState.error != null && uiState.torrents.isEmpty() && uiState.hasSearched -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = bottomPadding)
                        .padding(horizontal = 32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Search failed",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = uiState.error ?: "Unknown error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRetry) { Text("Retry") }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onOpenSettings) { Text("Try a mirror") }
                }
            }
            uiState.torrents.isEmpty() && uiState.hasSearched -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = bottomPadding)
                        .padding(horizontal = 32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "No results found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Try different search terms or filters",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            else -> {
                val shouldLoadMore by remember {
                    derivedStateOf {
                        val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                        val totalItems = listState.layoutInfo.totalItemsCount
                        totalItems > 0 && lastVisibleItem >= totalItems - LOAD_MORE_BUFFER
                    }
                }
                LaunchedEffect(shouldLoadMore, uiState.isLoadingMore, uiState.canLoadMore, uiState.isLoading) {
                    if (shouldLoadMore && !uiState.isLoading && !uiState.isLoadingMore && uiState.canLoadMore) {
                        onLoadMore()
                    }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 8.dp + bottomPadding
                    )
                ) {
                    itemsIndexed(
                        items = uiState.torrents,
                        key = { index, torrent -> torrent.listKey(index) },
                        contentType = { _, _ -> "torrent" }
                    ) { _, torrent ->
                        TorrentCard(
                            torrent = torrent,
                            onClick = {
                                if (selecting) onToggleSelect(torrent) else onTorrentClick(torrent)
                            },
                            isBookmarked = torrent.bookmarkKey() in bookmarkedIds,
                            selected = torrent.bookmarkKey() in selectedKeys,
                            compact = compact,
                            showSiteBadge = false,
                            onMagnet = { onMagnet(torrent) },
                            onCopyMagnet = { onCopyMagnet(torrent) },
                            onToggleBookmark = { onToggleBookmark(torrent) },
                            onCopyTitle = { onCopyTitle(torrent) },
                            onShare = { onShare(torrent) },
                            onFollow = onFollow,
                            viewed = torrent.bookmarkKey() in viewedKeys,
                            onSearchQuery = onSearchQuery,
                            onOpenUser = onOpenUser
                        )
                    }
                    if (uiState.isLoadingMore) {
                        item(key = "loading-more", contentType = "loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheetContent(
    searchParams: SearchParams,
    scrollState: ScrollState = rememberScrollState(),
    categories: List<Category> = searchParams.site.categories,
    defaultCategory: Category = categories.first(),
    defaultSortField: SortField = SortField.DATE,
    defaultSortOrder: SortOrder = SortOrder.DESC,
    onReset: () -> Unit,
    onSaveSearch: () -> Unit = {},
    onApply: (Category, FilterOption, SortField, SortOrder) -> Unit
) {
    var tempCategory by remember(key1 = searchParams) { mutableStateOf(searchParams.category) }
    var tempFilter by remember(key1 = searchParams) { mutableStateOf(searchParams.filter) }
    var tempSortField by remember(key1 = searchParams) { mutableStateOf(searchParams.sortField) }
    var tempSortOrder by remember(key1 = searchParams) { mutableStateOf(searchParams.sortOrder) }
    var categoryExpanded by remember { mutableStateOf(false) }
    val maxSheetHeight = (LocalConfiguration.current.screenHeightDp - 72).coerceAtLeast(320).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxSheetHeight)
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
        ) {
        Text(
            text = "Search Filters",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Tip: user:Name finds that uploader’s listings",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "CATEGORY",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.filter { it.isPrimary }.forEach { category ->
                FilterChip(
                    selected = category.groups(tempCategory),
                    onClick = { tempCategory = category },
                    label = { Text(category.shortLabel) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
            val moreCategories = categories.filter { !it.isPrimary }
            if (moreCategories.isNotEmpty()) {
                Box {
                    FilterChip(
                        selected = moreCategories.any { it.value == tempCategory.value },
                        onClick = { categoryExpanded = true },
                        label = { Text("More") },
                        shape = RoundedCornerShape(12.dp)
                    )
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        moreCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.displayName) },
                                onClick = {
                                    tempCategory = category
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "FILTER",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterOption.entries.forEach { option ->
                FilterChip(
                    selected = tempFilter == option,
                    onClick = { tempFilter = option },
                    label = { Text(option.displayName) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "SORT BY",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SortField.entries.forEach { field ->
                FilterChip(
                    selected = tempSortField == field,
                    onClick = { tempSortField = field },
                    label = { Text(field.displayName) },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "ORDER",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SortOrder.entries.forEach { order ->
                FilterChip(
                    selected = tempSortOrder == order,
                    onClick = { tempSortOrder = order },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (order == SortOrder.DESC) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(order.displayName)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onSaveSearch) { Text("Save this search") }
        Spacer(Modifier.height(8.dp))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = {
                    tempCategory = defaultCategory
                    tempFilter = FilterOption.ALL
                    tempSortField = defaultSortField
                    tempSortOrder = defaultSortOrder
                    onReset()
                },
                modifier = Modifier.weight(1f)
            ) { Text("Reset", fontWeight = FontWeight.SemiBold) }
            Button(
                onClick = {
                    onApply(tempCategory, tempFilter, tempSortField, tempSortOrder)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("Apply & Search", fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun SearchHomeEmpty(
    siteName: String,
    bottomPadding: Dp,
    onShowLatest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomPadding)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Search $siteName",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Type a name, or show the latest listings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onShowLatest, shape = RoundedCornerShape(12.dp)) {
            Text("Show latest")
        }
    }
}

@Composable
private fun SearchLoadingPlaceholder(bottomPadding: Dp) {
    val pulse = rememberInfiniteTransition(label = "search-skeleton")
    val alpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "search-skeleton-alpha"
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 8.dp,
            bottom = 8.dp + bottomPadding
        ),
        userScrollEnabled = false
    ) {
        items(8) {
            SearchSkeletonCard(alpha = alpha)
        }
    }
}

@Composable
private fun SearchSkeletonCard(alpha: Float) {
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f + 0.06f * alpha)
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.64f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .width(56.dp)
                        .height(22.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color)
                )
                Box(
                    modifier = Modifier
                        .width(88.dp)
                        .height(22.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color)
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(4) {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun TorrentCard(
    torrent: Torrent,
    onClick: (Torrent) -> Unit,
    isBookmarked: Boolean = false,
    selected: Boolean = false,
    compact: Boolean = false,
    showSiteBadge: Boolean = false,
    onMagnet: (() -> Unit)? = null,
    onCopyMagnet: (() -> Unit)? = null,
    onToggleBookmark: (() -> Unit)? = null,
    onCopyTitle: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    onFollow: ((String, String) -> Unit)? = null,
    onSearchQuery: ((String) -> Unit)? = null,
    onOpenUser: ((String) -> Unit)? = null,
    viewed: Boolean = false
) {
    var menu by remember { mutableStateOf(false) }
    val parsed = remember(torrent.title) { parseReleaseTitle(torrent.title) }
    val quality = remember(torrent.title) { qualityTags(torrent.title) }
    val hasMenu = onMagnet != null || onCopyMagnet != null || onToggleBookmark != null ||
        onCopyTitle != null || onShare != null || onFollow != null
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (viewed) 0.62f else 1f)
            .combinedClickable(
                onClick = { onClick(torrent) },
                onLongClick = { if (hasMenu) menu = true }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box {
        Column(modifier = Modifier.fillMaxWidth().padding(if (compact) 10.dp else 16.dp)) {
            Text(
                text = torrent.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface
            )
            val parsed = remember(torrent.title) { parseReleaseTitle(torrent.title) }
            val group = parsed.group
            val show = parsed.show
            if (!compact && (onSearchQuery != null && (group != null || show != null) ||
                (onOpenUser != null && torrent.submitter.isNotBlank()))
            ) {
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (onSearchQuery != null && group != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.clickable { onSearchQuery(group) }
                        ) {
                            Text(
                                text = group,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (onSearchQuery != null && show != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.clickable { onSearchQuery(show) }
                        ) {
                            Text(
                                text = show,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (onOpenUser != null && torrent.submitter.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.clickable { onOpenUser(torrent.submitter) }
                        ) {
                            Text(
                                text = torrent.submitter,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (showSiteBadge) {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                        Text(
                            text = torrent.site.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                if (torrent.category.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(
                            text = torrent.category,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                quality.forEach { tag ->
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                if (torrent.trusted) {
                    Surface(shape = RoundedCornerShape(8.dp), color = NyaaTrusted.copy(alpha = 0.12f)) {
                        Text(
                            text = "✓ Trusted",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = NyaaTrusted,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (torrent.remake) {
                    Surface(shape = RoundedCornerShape(8.dp), color = NyaaRemake.copy(alpha = 0.12f)) {
                        Text(
                            text = "⚠ Remake",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = NyaaRemake,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Text(
                        text = torrent.size,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Seeders", modifier = Modifier.size(13.dp), tint = NyaaSeeder)
                    Text(text = formatCount(torrent.seeders), style = MaterialTheme.typography.labelMedium, color = NyaaSeeder, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(Icons.Default.ArrowDownward, contentDescription = "Leechers", modifier = Modifier.size(13.dp), tint = NyaaLeecher)
                    Text(text = formatCount(torrent.leechers), style = MaterialTheme.typography.labelMedium, color = NyaaLeecher, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(Icons.Default.Download, contentDescription = "Downloads", modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = formatCount(torrent.downloads), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (torrent.comments > 0) {
                    Text(
                        text = "${torrent.comments}c",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = remember(torrent.pubDate) { PubDateFormatter.formatRelative(torrent.pubDate) },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            if (!compact && (onMagnet != null || onCopyMagnet != null || onToggleBookmark != null)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onMagnet != null) {
                        IconButton(onClick = onMagnet, modifier = Modifier.size(40.dp)) {
                            Icon(
                                Icons.Default.Link,
                                contentDescription = "Open magnet",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (onCopyMagnet != null) {
                        IconButton(onClick = onCopyMagnet, modifier = Modifier.size(40.dp)) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy magnet",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (onToggleBookmark != null) {
                        IconButton(onClick = onToggleBookmark, modifier = Modifier.size(40.dp)) {
                            Icon(
                                if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = if (isBookmarked) "Remove bookmark" else "Bookmark",
                                modifier = Modifier.size(20.dp),
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            if (onMagnet != null) {
                DropdownMenuItem(text = { Text("Open magnet") }, onClick = { menu = false; onMagnet() })
            }
            if (onCopyMagnet != null) {
                DropdownMenuItem(text = { Text("Copy magnet") }, onClick = { menu = false; onCopyMagnet() })
            }
            if (onCopyTitle != null) {
                DropdownMenuItem(text = { Text("Copy title") }, onClick = { menu = false; onCopyTitle() })
            }
            if (onToggleBookmark != null) {
                DropdownMenuItem(
                    text = { Text(if (isBookmarked) "Remove bookmark" else "Bookmark") },
                    onClick = { menu = false; onToggleBookmark() }
                )
            }
            if (onShare != null) {
                DropdownMenuItem(text = { Text("Share") }, onClick = { menu = false; onShare() })
            }
            if (onFollow != null && parsed.show != null) {
                DropdownMenuItem(
                    text = { Text("Follow “${parsed.show}”") },
                    onClick = {
                        menu = false
                        onFollow(parsed.show, parsed.show)
                    }
                )
            }
            if (onFollow != null && parsed.group != null) {
                DropdownMenuItem(
                    text = { Text("Follow [${parsed.group}]") },
                    onClick = {
                        menu = false
                        onFollow(parsed.group, parsed.group)
                    }
                )
            }
        }
        }
    }
}

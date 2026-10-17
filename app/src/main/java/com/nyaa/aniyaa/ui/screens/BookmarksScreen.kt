package com.nyaa.aniyaa.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nyaa.aniyaa.AniyaaApplication
import com.nyaa.aniyaa.data.api.resolvedMagnet
import com.nyaa.aniyaa.data.model.BookmarkSort
import com.nyaa.aniyaa.data.model.CatalogSite
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.data.prefs.AppPreferences
import com.nyaa.aniyaa.ui.viewmodel.BookmarkViewModel
import com.nyaa.aniyaa.util.copyText
import com.nyaa.aniyaa.util.launchHandoff
import com.nyaa.aniyaa.util.magnetExportText
import com.nyaa.aniyaa.util.shareText
import com.nyaa.aniyaa.util.torrentShareText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(
    onTorrentClick: (Torrent) -> Unit,
    bookmarkViewModel: BookmarkViewModel = viewModel(),
    bottomPadding: Dp = 0.dp
) {
    val bookmarks by bookmarkViewModel.bookmarks.collectAsStateWithLifecycle()
    val allBookmarks by bookmarkViewModel.allBookmarks.collectAsStateWithLifecycle()
    val query by bookmarkViewModel.query.collectAsStateWithLifecycle()
    val sort by bookmarkViewModel.sort.collectAsStateWithLifecycle()
    val siteFilter by bookmarkViewModel.siteFilter.collectAsStateWithLifecycle()
    val refreshing by bookmarkViewModel.refreshing.collectAsStateWithLifecycle()
    val message by bookmarkViewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    var selecting by remember { mutableStateOf(false) }
    var selectedKeys by remember { mutableStateOf(setOf<String>()) }
    val sentIds by AniyaaApplication.instance.sentRepository.observe().collectAsStateWithLifecycle(emptyList())
    var sortMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val listScrolling by remember { derivedStateOf { listState.isScrollInProgress } }

    LaunchedEffect(message) {
        val text = message
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            bookmarkViewModel.consumeMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text("Bookmarks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                },
                actions = {
                    if (bookmarks.isNotEmpty()) {
                        IconButton(onClick = {
                            selecting = !selecting
                            if (!selecting) selectedKeys = emptySet()
                        }) {
                            Icon(
                                if (selecting) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = if (selecting) "Done selecting" else "Select bookmarks"
                            )
                        }
                    }
                    if (bookmarks.isNotEmpty() && !selecting) {
                        IconButton(onClick = {
                            val magnets = magnetExportText(bookmarks)
                            if (magnets.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar("No magnet links to copy") }
                            } else {
                                copyText(context, "Magnets", magnets)
                                scope.launch { snackbarHostState.showSnackbar("Copied ${magnets.lines().size} magnet${if (magnets.lines().size == 1) "" else "s"}") }
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Export magnets")
                        }
                    }
                    IconButton(onClick = { bookmarkViewModel.refreshStats() }, enabled = !refreshing) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh stats")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = {
                        val selected = bookmarks.filter { it.bookmarkKey() in selectedKeys }
                        launchHandoff(scope, context, prefs, selected, snackbarHostState)
                        selecting = false
                        selectedKeys = emptySet()
                    }) { Text("Open magnets (${selectedKeys.size})") }
                    OutlinedButton(onClick = {
                        val magnets = magnetExportText(bookmarks.filter { it.bookmarkKey() in selectedKeys })
                        if (magnets.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar("No magnet links to copy") }
                        } else {
                            copyText(context, "Magnets", magnets)
                            scope.launch { snackbarHostState.showSnackbar("Copied ${magnets.lines().size} magnets") }
                        }
                    }) { Text("Copy magnets") }
                }
            }
            if (allBookmarks.isNotEmpty() || query.isNotBlank()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = bookmarkViewModel::updateQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = {
                        Text("Search bookmarks", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp)
                )
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = siteFilter == null,
                        onClick = { bookmarkViewModel.updateSiteFilter(null) },
                        label = { Text("All") }
                    )
                    CatalogSite.entries.forEach { site ->
                        FilterChip(
                            selected = siteFilter == site,
                            onClick = { bookmarkViewModel.updateSiteFilter(site) },
                            label = { Text(site.displayName) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        FilterChip(
                            selected = true,
                            onClick = { sortMenu = true },
                            label = { Text(sort.displayName) }
                        )
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            BookmarkSort.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.displayName) },
                                    onClick = {
                                        bookmarkViewModel.updateSort(option)
                                        sortMenu = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (refreshing) {
                        Text("Refreshing…", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (bookmarks.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(88.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.BookmarkBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = if (query.isBlank()) "No bookmarks yet" else "No matching bookmarks",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (query.isBlank()) "Bookmark torrents to find them here" else "Try a different search",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
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
                            items = bookmarks,
                            key = { index, torrent -> torrent.listKey(index) },
                            contentType = { _, _ -> "bookmark" }
                        ) { _, torrent ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                positionalThreshold = { it * 0.45f },
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        bookmarkViewModel.removeBookmark(torrent)
                                        true
                                    } else {
                                        false
                                    }
                                }
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                enableDismissFromStartToEnd = false,
                                gesturesEnabled = !listScrolling,
                                backgroundContent = {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.errorContainer,
                                            modifier = Modifier.padding(end = 16.dp)
                                        ) {
                                            IconButton(onClick = {
                                                bookmarkViewModel.removeBookmark(torrent)
                                            }) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Remove bookmark",
                                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            ) {
                                TorrentCard(
                                    torrent = torrent,
                                    onClick = {
                                        if (selecting) {
                                            val key = torrent.bookmarkKey()
                                            selectedKeys = if (key in selectedKeys) selectedKeys - key else selectedKeys + key
                                        } else {
                                            onTorrentClick(torrent)
                                        }
                                    },
                                    isBookmarked = true,
                                    selected = torrent.bookmarkKey() in selectedKeys,
                                    sent = torrent.bookmarkKey() in sentIds,
                                    showSiteBadge = siteFilter == null,
                                    onMagnet = {
                                        launchHandoff(scope, context, prefs, listOf(torrent), snackbarHostState)
                                    },
                                    onCopyMagnet = {
                                        copyText(context, "Magnet Link", torrent.resolvedMagnet())
                                        scope.launch { snackbarHostState.showSnackbar("Copied magnet") }
                                    },
                                    onShare = {
                                        shareText(context, torrentShareText(torrent))?.let { msg ->
                                            scope.launch { snackbarHostState.showSnackbar(msg) }
                                        }
                                    },
                                    onToggleBookmark = {
                                        bookmarkViewModel.removeBookmark(torrent)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

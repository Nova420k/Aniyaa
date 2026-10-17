package com.nyaa.aniyaa.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
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
import com.nyaa.aniyaa.data.model.SavedSearch
import com.nyaa.aniyaa.data.model.SearchHistoryEntry
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.data.repository.buildSearchUrl
import com.nyaa.aniyaa.ui.viewmodel.SearchHistoryViewModel
import com.nyaa.aniyaa.util.copyText
import com.nyaa.aniyaa.util.hasNotificationPermission
import com.nyaa.aniyaa.util.prepareSavedSearchAlerts
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchHistoryScreen(
    onHistoryItemClick: (SearchHistoryEntry) -> Unit,
    onSavedSearchClick: (SavedSearch) -> Unit,
    onTorrentClick: (Torrent) -> Unit = {},
    searchHistoryViewModel: SearchHistoryViewModel = viewModel(),
    bottomPadding: Dp = 0.dp
) {
    val history by searchHistoryViewModel.history.collectAsStateWithLifecycle()
    val saved by searchHistoryViewModel.savedSearches.collectAsStateWithLifecycle()
    val viewed by searchHistoryViewModel.viewed.collectAsStateWithLifecycle()
    val message by searchHistoryViewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val listScrolling by remember { derivedStateOf { listState.isScrollInProgress } }
    var pendingNotifyId by remember { mutableStateOf<Long?>(null) }
    var confirmClearHistory by remember { mutableStateOf(false) }
    var pendingDeleteSaved by remember { mutableStateOf<SavedSearch?>(null) }
    var renaming by remember { mutableStateOf<SavedSearch?>(null) }
    var alertRules by remember { mutableStateOf<SavedSearch?>(null) }
    var renameValue by remember { mutableStateOf("") }
    LaunchedEffect(message) {
        val text = message
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            searchHistoryViewModel.consumeMessage()
        }
    }
    val notifyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val id = pendingNotifyId
        pendingNotifyId = null
        if (granted && id != null) {
            prepareSavedSearchAlerts(context)
            saved.find { it.id == id }?.let { searchHistoryViewModel.toggleNotify(it) }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Text("Search History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                },
                actions = {
                    if (viewed.isNotEmpty()) {
                        IconButton(onClick = { searchHistoryViewModel.clearViewed() }) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = "Clear viewed listings",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (history.isNotEmpty()) {
                        IconButton(onClick = { confirmClearHistory = true }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Clear all history",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (history.isEmpty() && saved.isEmpty() && viewed.isEmpty()) {
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
                                Icons.Default.History,
                                contentDescription = null,
                                modifier = Modifier.size(44.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "No search history",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Your recent searches will appear here",
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
                    if (viewed.isNotEmpty()) {
                        item {
                            Text(
                                text = "Recently viewed",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(viewed.take(12), key = { "viewed-${it.bookmarkKey()}" }) { torrent ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                positionalThreshold = { it * 0.45f },
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        searchHistoryViewModel.removeViewed(torrent)
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
                                            IconButton(onClick = { searchHistoryViewModel.removeViewed(torrent) }) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Remove viewed listing",
                                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            ) {
                                TorrentCard(
                                    torrent = torrent,
                                    onClick = onTorrentClick,
                                    showSiteBadge = true
                                )
                            }
                        }
                    }
                    if (saved.isNotEmpty()) {
                        item {
                            Text(
                                text = "Watching",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(saved, key = { "saved-${it.id}" }) { search ->
                            SavedSearchCard(
                                search = search,
                                onClick = { onSavedSearchClick(search) },
                                onCopyRss = {
                                    copyText(context, "RSS", buildSearchUrl(search.toSearchParams()))
                                },
                                onCheckNow = { searchHistoryViewModel.checkSavedSearchNow(search) },
                                onRename = {
                                    renaming = search
                                    renameValue = search.displayName()
                                },
                                onToggleNotify = {
                                    if (search.notify) {
                                        searchHistoryViewModel.toggleNotify(search)
                                    } else if (hasNotificationPermission(context)) {
                                        prepareSavedSearchAlerts(context)
                                        searchHistoryViewModel.toggleNotify(search)
                                    } else if (Build.VERSION.SDK_INT >= 33) {
                                        pendingNotifyId = search.id
                                        notifyLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        prepareSavedSearchAlerts(context)
                                        searchHistoryViewModel.toggleNotify(search)
                                    }
                                },
                                onDelete = { pendingDeleteSaved = search },
                                onAlertRules = { alertRules = search }
                            )
                        }
                    }
                    if (history.isNotEmpty()) {
                        item {
                            Text(
                                text = "Recent",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                    items(history, key = { "${it.site.id}|${it.query}|${it.timestamp}" }) { entry ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            positionalThreshold = { it * 0.45f },
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    searchHistoryViewModel.removeEntry(entry)
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
                                        IconButton(onClick = { searchHistoryViewModel.removeEntry(entry) }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Remove from history",
                                                tint = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            }
                        ) {
                            SearchHistoryCard(
                                entry = entry,
                                onClick = { onHistoryItemClick(entry) }
                            )
                        }
                    }
                }
            }
        }
    }
    if (confirmClearHistory) {
        AlertDialog(
            onDismissRequest = { confirmClearHistory = false },
            title = { Text("Clear search history?") },
            text = { Text("This removes recent searches. Saved searches are kept.") },
            confirmButton = {
                TextButton(onClick = {
                    searchHistoryViewModel.clearHistory()
                    confirmClearHistory = false
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClearHistory = false }) { Text("Cancel") } }
        )
    }
    val deleteSaved = pendingDeleteSaved
    if (deleteSaved != null) {
        AlertDialog(
            onDismissRequest = { pendingDeleteSaved = null },
            title = { Text("Delete saved search?") },
            text = { Text("“${deleteSaved.displayName()}” will be removed, including alerts.") },
            confirmButton = {
                TextButton(onClick = {
                    searchHistoryViewModel.deleteSavedSearch(deleteSaved.id)
                    pendingDeleteSaved = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteSaved = null }) { Text("Cancel") } }
        )
    }
    val rules = alertRules
    if (rules != null) {
        AlertDialog(
            onDismissRequest = { alertRules = null },
            title = { Text("Alert rules") },
            text = {
                Column {
                    Text("Notifications for this search skip listings that do not match.")
                    TextButton(onClick = {
                        val trusted = !rules.alertTrustedOnly
                        searchHistoryViewModel.updateAlertRule(rules, trusted, rules.alertResolution)
                        alertRules = rules.copy(alertTrustedOnly = trusted)
                    }) { Text(if (rules.alertTrustedOnly) "Trusted only: on" else "Trusted only: off") }
                    listOf("" to "Any quality", "2160p" to "2160p", "1080p" to "1080p", "720p" to "720p", "480p" to "480p").forEach { (tag, label) ->
                        TextButton(onClick = {
                            searchHistoryViewModel.updateAlertRule(rules, rules.alertTrustedOnly, tag)
                            alertRules = rules.copy(alertResolution = tag)
                        }) { Text(if (rules.alertResolution == tag) "✓ $label" else label) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { alertRules = null }) { Text("Done") } }
        )
    }
    val renameTarget = renaming
    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("Rename saved search") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    singleLine = true,
                    label = { Text("Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    searchHistoryViewModel.renameSavedSearch(renameTarget, renameValue)
                    renaming = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun SavedSearchCard(
    search: SavedSearch,
    onClick: () -> Unit,
    onCopyRss: () -> Unit,
    onCheckNow: () -> Unit,
    onRename: () -> Unit,
    onToggleNotify: () -> Unit,
    onDelete: () -> Unit,
    onAlertRules: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = search.displayName(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val summary = search.toSearchParams().let {
                    buildList {
                        add(search.site.displayName)
                        if (it.query.isNotBlank()) add(it.query)
                        if (it.category.value != "0_0") add(it.category.displayName)
                        if (it.filter.displayName != "No Filter") add(it.filter.displayName)
                    }.joinToString(" · ")
                }
                if (summary.isNotBlank()) {
                    Text(text = summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (search.lastHeadline.isNotBlank() || search.unseenCount > 0) {
                    val unseen = if (search.unseenCount > 0) "${search.unseenCount} new" else ""
                    val line = listOf(search.lastHeadline, unseen).filter { it.isNotBlank() }.joinToString(" · ")
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (search.unseenCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (search.lastCheckedAt > 0L) {
                    Text(
                        text = "Checked ${formatTimestamp(search.lastCheckedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            IconButton(onClick = onToggleNotify) {
                Icon(
                    if (search.notify) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                    contentDescription = if (search.notify) "Disable alerts" else "Enable alerts",
                    tint = if (search.notify) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Copy RSS URL") }, onClick = { menu = false; onCopyRss() })
                    DropdownMenuItem(text = { Text("Check now") }, onClick = { menu = false; onCheckNow() })
                    DropdownMenuItem(text = { Text("Alert rules") }, onClick = { menu = false; onAlertRules() })
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; onRename() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}

@Composable
private fun SearchHistoryCard(
    entry: SearchHistoryEntry,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.query,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val extra = listOfNotNull(formatTimestamp(entry.timestamp), entry.filterSummary().ifBlank { null })
                    .joinToString(" · ")
                Text(text = extra, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.Search, contentDescription = "Search again", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private val historyDateFormatter: SimpleDateFormat by lazy {
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 0 -> historyDateFormatter.format(Date(timestamp))
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 604_800_000 -> "${diff / 86_400_000}d ago"
        else -> historyDateFormatter.format(Date(timestamp))
    }
}

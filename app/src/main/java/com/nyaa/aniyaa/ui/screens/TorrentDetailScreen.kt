package com.nyaa.aniyaa.ui.screens

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.text.method.LinkMovementMethod
import android.view.MotionEvent
import android.widget.TextView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nyaa.aniyaa.data.api.resolvedMagnet
import com.nyaa.aniyaa.data.model.CatalogDeepLink
import com.nyaa.aniyaa.data.model.CatalogDeepLinks
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.data.model.TorrentComment
import com.nyaa.aniyaa.data.model.TorrentFileEntry
import com.nyaa.aniyaa.ui.theme.NyaaLeecher
import com.nyaa.aniyaa.ui.theme.NyaaRemake
import com.nyaa.aniyaa.ui.theme.NyaaSeeder
import com.nyaa.aniyaa.ui.theme.NyaaTrusted
import com.nyaa.aniyaa.data.network.AppHttpClient
import com.nyaa.aniyaa.data.network.SiteConfig
import com.nyaa.aniyaa.data.prefs.AppPreferences
import com.nyaa.aniyaa.ui.viewmodel.BookmarkViewModel
import com.nyaa.aniyaa.ui.viewmodel.CommentsUiState
import com.nyaa.aniyaa.ui.viewmodel.CommentsViewModel
import com.nyaa.aniyaa.util.FileNode
import com.nyaa.aniyaa.util.PubDateFormatter
import com.nyaa.aniyaa.util.buildFileTree
import com.nyaa.aniyaa.util.DescriptionFormatter
import com.nyaa.aniyaa.util.copyText
import com.nyaa.aniyaa.util.downloadTorrentFile
import com.nyaa.aniyaa.util.filterFileEntries
import com.nyaa.aniyaa.util.isSafeHttpUrl
import com.nyaa.aniyaa.util.openHttpUrl
import com.nyaa.aniyaa.util.openMagnet
import com.nyaa.aniyaa.util.parseReleaseTitle
import com.nyaa.aniyaa.util.shareText as sharePlainText
import com.nyaa.aniyaa.util.totalSizeLabel
import com.nyaa.aniyaa.util.torrentShareText
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.image.network.OkHttpNetworkSchemeHandler
import io.noties.markwon.linkify.LinkifyPlugin
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TorrentDetailScreen(
    torrent: Torrent,
    onNavigateBack: () -> Unit,
    onOpenUser: (String, com.nyaa.aniyaa.data.model.CatalogSite) -> Unit = { _, _ -> },
    onOpenCatalogLink: (CatalogDeepLink) -> Unit = {},
    onFollow: (String, String) -> Unit = { _, _ -> },
    onSearch: (String) -> Unit = {},
    bookmarkViewModel: BookmarkViewModel = viewModel(),
    commentsViewModel: CommentsViewModel = viewModel(
        key = torrent.id.ifEmpty { torrent.infoHash }.ifEmpty { torrent.guid }
    )
) {
    val context = LocalContext.current
    val prefs = com.nyaa.aniyaa.AniyaaApplication.instance.prefs
    var fileQuery by remember { mutableStateOf("") }
    var filesExpanded by remember(torrent.id, torrent.site) { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val bookmarks by bookmarkViewModel.allBookmarks.collectAsStateWithLifecycle()
    val isBookmarked = bookmarks.any { it.bookmarkKey() == torrent.bookmarkKey() }
    val commentsState by commentsViewModel.uiState.collectAsStateWithLifecycle()
    val displayTorrent = commentsState.resolvedTorrent?.takeIf {
        it.site == torrent.site && (it.id == torrent.id || it.identity() == torrent.identity())
    }
        ?: torrent
    val formattedDate = remember(displayTorrent.pubDate) { PubDateFormatter.format(displayTorrent.pubDate) }
    val magnetLink = remember(displayTorrent.infoHash, displayTorrent.title, displayTorrent.magnetLink) {
        displayTorrent.resolvedMagnet()
    }
    val submitter = commentsState.submitter.ifBlank { displayTorrent.submitter }
    val parsedTitle = remember(displayTorrent.title) { parseReleaseTitle(displayTorrent.title) }
    var viewerImages by remember { mutableStateOf<List<com.nyaa.aniyaa.util.DescriptionImage>>(emptyList()) }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(torrent.id) {
        if (torrent.id.isNotBlank()) {
            commentsViewModel.fetchComments(torrent.id, torrent, torrent.site)
        }
    }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun openUrl(url: String) {
        openHttpUrl(context, url)?.let { showMessage(it) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        displayTorrent.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (magnetLink.isNotEmpty()) {
                        IconButton(onClick = { openMagnet(context, magnetLink, prefs.preferredTorrentPackage)?.let(::showMessage) }) {
                            Icon(
                                Icons.Default.Link,
                                contentDescription = "Open magnet",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = { bookmarkViewModel.toggleBookmark(torrent) }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = if (isBookmarked) "Remove bookmark" else "Add bookmark",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "title") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = displayTorrent.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (displayTorrent.category.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = displayTorrent.category,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
            }

            if (parsedTitle.show != null || parsedTitle.group != null || submitter.isNotBlank()) {
                item(key = "follow") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        parsedTitle.show?.let { show ->
                            FilledTonalButton(
                                onClick = { onSearch(show) },
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("More from “$show”") }
                            OutlinedButton(
                                onClick = { onFollow(show, show) },
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Follow show") }
                        }
                        parsedTitle.group?.let { group ->
                            FilledTonalButton(
                                onClick = { onSearch(group) },
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("More from [$group]") }
                            OutlinedButton(
                                onClick = { onFollow(group, group) },
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Follow group") }
                        }
                        if (submitter.isNotBlank()) {
                            FilledTonalButton(
                                onClick = { onOpenUser(submitter, displayTorrent.site) },
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("More from $submitter") }
                            OutlinedButton(
                                onClick = { onFollow(submitter, "user:$submitter") },
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Follow uploader") }
                        }
                    }
                }
            }

            if (displayTorrent.trusted || displayTorrent.remake) {
                item(key = "badges") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (displayTorrent.trusted) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NyaaTrusted.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "✓ Trusted",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = NyaaTrusted,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (displayTorrent.remake) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NyaaRemake.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "⚠ Remake",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = NyaaRemake,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                }
            }

            item(key = "actions") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Actions",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 2
                    ) {
                        if (magnetLink.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = { openMagnet(context, magnetLink, prefs.preferredTorrentPackage)?.let(::showMessage) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Magnet", maxLines = 1, fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = {
                                    copyText(context, "Magnet Link", magnetLink)
                                    showMessage("Copied to clipboard")
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Copy Magnet", maxLines = 1)
                            }
                        }
                        if (displayTorrent.link.isNotEmpty()) {
                            FilledTonalButton(
                                onClick = { showMessage(downloadTorrentFile(context, displayTorrent)) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Download", maxLines = 1, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        OutlinedButton(
                            onClick = { sharePlainText(context, torrentShareText(displayTorrent))?.let(::showMessage) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share", maxLines = 1)
                        }
                        if (displayTorrent.guid.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { openUrl(displayTorrent.guid) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(displayTorrent.site.viewOnLabel, maxLines = 1)
                            }
                        }
                    }
                }
            }
            }

            item(key = "stats") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Statistics",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(label = "Size", value = displayTorrent.size)
                        StatItem(label = "Seeders", value = displayTorrent.seeders.toString(), valueColor = NyaaSeeder)
                        StatItem(label = "Leechers", value = displayTorrent.leechers.toString(), valueColor = NyaaLeecher)
                        StatItem(label = "Downloads", value = displayTorrent.downloads.toString())
                    }
                    if (displayTorrent.comments > 0) {
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(Modifier.height(12.dp))
                        StatItem(label = "Comments", value = displayTorrent.comments.toString())
                    }
                }
            }
            }

            item(key = "info") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Info",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(16.dp))
                    InfoRow(label = "Date", value = formattedDate.ifEmpty { displayTorrent.pubDate })
                    if (submitter.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(Modifier.height(10.dp))
                        InfoRow(label = "Uploader", value = submitter, onClick = { onOpenUser(submitter, displayTorrent.site) })
                    }
                    if (displayTorrent.infoHash.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(Modifier.height(10.dp))
                        InfoRow(
                            label = "Info Hash",
                            value = displayTorrent.infoHash,
                            onClick = {
                                copyText(context, "Info Hash", displayTorrent.infoHash)
                                showMessage("Copied info hash")
                            }
                        )
                    }
                }
            }
            }

            if (commentsState.error != null && commentsState.description.isEmpty() && !commentsState.isLoading) {
                item(key = "load-error") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Could not load description, files, or comments",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { commentsViewModel.retry(torrent.id, torrent, torrent.site) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Retry")
                        }
                    }
                }
                }
            }

            if (commentsState.isLoading && commentsState.description.isEmpty() && commentsState.fileList.isEmpty()) {
                item(key = "detail-skeleton") {
                    DetailLoadingSkeleton()
                }
            }

            if (commentsState.description.isNotEmpty()) {
                item(key = "description") {
                    DescriptionCard(
                        markdown = commentsState.description,
                        onCatalogLink = { url ->
                            val parsed = CatalogDeepLinks.parse(url, displayTorrent.site)
                            if (parsed != null && (parsed.viewId != null || parsed.searchParams != null)) {
                                onOpenCatalogLink(parsed)
                                true
                            } else {
                                false
                            }
                        }
                    )
                }
            }

            if (commentsState.fileList.isNotEmpty()) {
                val visibleFiles = filterFileEntries(commentsState.fileList, fileQuery)
                item(key = "files") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            FileListHeader(
                                count = commentsState.fileList.size,
                                totalSize = totalSizeLabel(commentsState.fileList),
                                expanded = filesExpanded,
                                onToggle = { filesExpanded = !filesExpanded }
                            )
                            if (filesExpanded) {
                                if (commentsState.fileList.size > 8) {
                                    Spacer(Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = fileQuery,
                                        onValueChange = { fileQuery = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        placeholder = { Text("Filter files") }
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                                FileTree(
                                    nodes = buildFileTree(visibleFiles),
                                    onCopyPath = { path ->
                                        copyText(context, "File path", path)
                                        showMessage("Copied path")
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item(key = "comments-header") {
                CommentsHeader(
                    commentsState = commentsState,
                    torrent = torrent,
                    onRetry = { commentsViewModel.retry(torrent.id, torrent, torrent.site) },
                    onOpenPage = { if (torrent.guid.isNotEmpty()) openUrl(torrent.guid) }
                )
            }
            if (!commentsState.isLoading && commentsState.error == null && commentsState.comments.isNotEmpty()) {
                itemsIndexed(
                    items = commentsState.comments,
                    key = { index, comment -> "com-${comment.id.ifEmpty { index.toString() }}" },
                    contentType = { _, _ -> "comment" }
                ) { _, comment ->
                    CommentItem(
                        comment = comment,
                        onOpenUser = { if (comment.username.isNotBlank()) onOpenUser(comment.username, displayTorrent.site) },
                        onOpenPermalink = {
                            val base = displayTorrent.guid.ifBlank { "${SiteConfig.baseUrl(displayTorrent.site)}/view/${displayTorrent.id}" }
                            if (comment.id.isNotBlank()) openUrl("$base#com-${comment.id}")
                        },
                        onCatalogLink = { url ->
                            val parsed = CatalogDeepLinks.parse(url, displayTorrent.site)
                            if (parsed != null && (parsed.viewId != null || parsed.searchParams != null)) {
                                onOpenCatalogLink(parsed)
                                true
                            } else {
                                false
                            }
                        },
                        onImageClick = { url ->
                            val images = DescriptionFormatter.allImages(comment.content).ifEmpty {
                                listOf(com.nyaa.aniyaa.util.DescriptionImage(url))
                            }
                            viewerImages = images
                            viewerIndex = images.indexOfFirst { it.url == url }.coerceAtLeast(0)
                        }
                    )
                }
            }

            item(key = "bottom-space") {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
    val startIndex = viewerIndex
    if (startIndex != null && viewerImages.isNotEmpty()) {
        ImageViewerDialog(
            images = viewerImages,
            startIndex = startIndex.coerceIn(viewerImages.indices),
            onDismiss = {
                viewerIndex = null
                viewerImages = emptyList()
            }
        )
    }
}

@Composable
private fun CommentItem(
    comment: TorrentComment,
    onOpenUser: () -> Unit = {},
    onOpenPermalink: () -> Unit = {},
    onCatalogLink: (String) -> Boolean = { false },
    onImageClick: ((String) -> Unit)? = null
) {
    val avatarSize = 36.dp
    val avatarSpacing = 10.dp

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(avatarSpacing)
        ) {
            Box(modifier = Modifier.size(avatarSize)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(avatarSize)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = comment.username.take(1).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                if (comment.avatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(comment.avatarUrl)
                            .size(96)
                            .crossfade(true)
                            .build(),
                        contentDescription = "${comment.username}'s avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(avatarSize)
                            .clip(CircleShape)
                    )
                }
            }
            Column {
                Text(
                    text = comment.username,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onOpenUser)
                )
                Text(
                    text = comment.date.ifBlank { "Permalink" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.clickable(onClick = onOpenPermalink)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        MarkdownContent(
            markdown = comment.content,
            modifier = Modifier.padding(start = avatarSize + avatarSpacing),
            onCatalogLink = onCatalogLink,
            onImageClick = onImageClick
        )
    }
}

@Composable
internal fun MarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier,
    onCatalogLink: (String) -> Boolean = { false },
    compact: Boolean = true,
    renderInlineImages: Boolean = true,
    onImageClick: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val linkColor = MaterialTheme.colorScheme.primary.toArgb()
    val quoteColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f).toArgb()
    val codeBg = MaterialTheme.colorScheme.surfaceContainerLowest.toArgb()
    val outline = MaterialTheme.colorScheme.outlineVariant.toArgb()
    val textSizeSp = if (compact) {
        MaterialTheme.typography.bodySmall.fontSize.value
    } else {
        MaterialTheme.typography.bodyMedium.fontSize.value
    }
    val markwon = remember(
        context,
        onCatalogLink,
        linkColor,
        quoteColor,
        codeBg,
        outline,
        compact,
        renderInlineImages,
        onImageClick
    ) {
        val builder = Markwon.builder(context)
        if (renderInlineImages) {
            builder.usePlugin(ImagesPlugin.create { plugin ->
                plugin.addSchemeHandler(OkHttpNetworkSchemeHandler.create(AppHttpClient.imageClient))
                plugin.errorHandler { _, _ ->
                    ColorDrawable(AndroidColor.TRANSPARENT).apply { setBounds(0, 0, 0, 0) }
                }
            })
        }
        builder
            .usePlugin(TablePlugin.create(context))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(LinkifyPlugin.create(true))
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureTheme(builder: io.noties.markwon.core.MarkwonTheme.Builder) {
                    builder
                        .linkColor(linkColor)
                        .isLinkUnderlined(false)
                        .blockMargin(if (compact) 8 else 14)
                        .blockQuoteColor(quoteColor)
                        .blockQuoteWidth(if (compact) 2 else 4)
                        .codeBackgroundColor(codeBg)
                        .codeBlockBackgroundColor(codeBg)
                        .codeTextColor(textColor)
                        .codeBlockTextColor(textColor)
                        .headingBreakHeight(0)
                        .headingTextSizeMultipliers(
                            if (compact) {
                                floatArrayOf(1.18f, 1.12f, 1.06f, 1.02f, 1f, 1f)
                            } else {
                                floatArrayOf(1.55f, 1.35f, 1.2f, 1.1f, 1.05f, 1f)
                            }
                        )
                        .thematicBreakColor(outline)
                        .thematicBreakHeight(if (compact) 1 else 2)
                        .listItemColor(linkColor)
                        .bulletListItemStrokeWidth(if (compact) 1 else 2)
                }

                override fun configureSpansFactory(builder: io.noties.markwon.MarkwonSpansFactory.Builder) {
                    if (onImageClick != null) {
                        builder.appendFactory(org.commonmark.node.Image::class.java) { _, props ->
                            val dest = io.noties.markwon.image.ImageProps.DESTINATION.require(props)
                            object : android.text.style.ClickableSpan() {
                                override fun onClick(widget: android.view.View) {
                                    if (isSafeHttpUrl(dest)) onImageClick.invoke(dest)
                                }
                                override fun updateDrawState(ds: android.text.TextPaint) = Unit
                            }
                        }
                    }
                }

                override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                    builder.linkResolver { view, link ->
                        if (onCatalogLink(link)) return@linkResolver
                        if (onImageClick != null && isSafeHttpUrl(link) && DescriptionFormatter.isImageUrl(link)) {
                            onImageClick.invoke(link)
                            return@linkResolver
                        }
                        if (!isSafeHttpUrl(link)) return@linkResolver
                        try {
                            view.context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(link)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: Exception) {
                        }
                    }
                }
            })
            .build()
    }
    AndroidView(
        factory = { ctx ->
            TextView(ctx).apply {
                setTextIsSelectable(true)
                movementMethod = LinkMovementMethod.getInstance()
                setOnTouchListener { view, event ->
                    if (event.actionMasked == MotionEvent.ACTION_MOVE) {
                        view.parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    false
                }
            }
        },
        modifier = modifier.fillMaxWidth(),
        update = { textView ->
            textView.setTextColor(textColor)
            textView.setLinkTextColor(linkColor)
            textView.textSize = textSizeSp
            textView.setLineSpacing(if (compact) 2f else 6f, if (compact) 1.15f else 1.25f)
            textView.setPadding(0, 0, 0, 0)
            textView.setTextIsSelectable(true)
            val prepared = DescriptionFormatter.prepare(markdown)
            if (textView.tag != prepared) {
                textView.tag = prepared
                markwon.setMarkdown(textView, prepared)
            }
        }
    )
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FileListHeader(
    count: Int,
    totalSize: String = "",
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "File List",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = if (totalSize.isBlank()) "$count" else "$count · $totalSize",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.weight(1f))
        Icon(
            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "Hide files" else "Show files",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FileTree(nodes: List<FileNode>, onCopyPath: (String) -> Unit, indent: Int = 0) {
    Column(modifier = Modifier.fillMaxWidth()) {
        nodes.forEach { node ->
            FileTreeNode(node = node, onCopyPath = onCopyPath, indent = indent)
        }
    }
}

@Composable
private fun FileTreeNode(node: FileNode, onCopyPath: (String) -> Unit, indent: Int) {
    var expanded by remember(node.path) { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (node.isFolder) expanded = !expanded else onCopyPath(node.path)
            }
            .padding(start = (indent * 16).dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            if (node.isFolder) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Text(
            text = node.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (node.size.isNotEmpty()) {
            Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                Text(
                    text = node.size,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
    if (node.isFolder && expanded) {
        FileTree(nodes = node.children, onCopyPath = onCopyPath, indent = indent + 1)
    }
}

@Composable
private fun DetailLoadingSkeleton(includeTitle: Boolean = true) {
    val pulse = rememberInfiniteTransition(label = "detail-skeleton")
    val alpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "detail-skeleton-alpha"
    )
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f + 0.06f * alpha)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        if (includeTitle) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.fillMaxWidth(0.35f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(color))
                    Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(4.dp)).background(color))
                    Box(Modifier.fillMaxWidth(0.9f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(color))
                    Box(Modifier.fillMaxWidth(0.7f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(color))
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.fillMaxWidth(0.2f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(color))
                    repeat(3) {
                        Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(4.dp)).background(color))
                    }
                }
            }
        } else {
            repeat(3) {
                Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(4.dp)).background(color))
            }
        }
    }
}

@Composable
private fun CommentsHeader(
    commentsState: CommentsUiState,
    torrent: Torrent,
    onRetry: () -> Unit,
    onOpenPage: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Comments",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))
        when {
            commentsState.isLoading -> {
                DetailLoadingSkeleton(includeTitle = false)
            }
            commentsState.error != null -> {
                Text(
                    text = commentsState.error ?: "Could not load details",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Retry")
                }
                if (torrent.guid.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onOpenPage,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("View comments in browser")
                    }
                }
            }
            commentsState.comments.isEmpty() && commentsState.hasFetched -> {
                if (torrent.comments > 0 && torrent.guid.isNotEmpty()) {
                    Text(
                        text = "Comments could not be loaded in-app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onOpenPage,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("View comments in browser")
                    }
                } else {
                    Text(
                        text = "No comments to display",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun FileListItem(file: TorrentFileEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.AutoMirrored.Filled.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Text(
            text = file.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (file.size.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Text(
                    text = file.size,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

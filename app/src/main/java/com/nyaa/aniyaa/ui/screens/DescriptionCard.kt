package com.nyaa.aniyaa.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nyaa.aniyaa.util.DescriptionFormatter
import com.nyaa.aniyaa.util.DescriptionImage
import com.nyaa.aniyaa.util.isSafeHttpUrl

@Composable
fun DescriptionCard(
    markdown: String,
    onCatalogLink: (String) -> Boolean,
    onImageClick: ((String) -> Unit)? = null
) {
    if (markdown.isBlank()) return
    var viewerUrl by remember { mutableStateOf<String?>(null) }
    val images = remember(markdown) { DescriptionFormatter.allImages(markdown) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Text(
                text = "DESCRIPTION",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            MarkdownContent(
                markdown = markdown,
                onCatalogLink = onCatalogLink,
                compact = false,
                renderInlineImages = true,
                onImageClick = { url ->
                    if (onImageClick != null) onImageClick(url) else viewerUrl = url
                }
            )
        }
    }
    val clicked = viewerUrl
    if (clicked != null) {
        val viewerImages = images.ifEmpty { listOf(DescriptionImage(clicked)) }
        val startIndex = viewerImages.indexOfFirst { it.url == clicked }.coerceAtLeast(0)
        ImageViewerDialog(
            images = viewerImages,
            startIndex = startIndex,
            onDismiss = { viewerUrl = null }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ImageViewerDialog(
    images: List<DescriptionImage>,
    startIndex: Int,
    onDismiss: () -> Unit
) {
    val safe = images.filter { isSafeHttpUrl(it.url) }
    if (safe.isEmpty()) return
    val pagerState = rememberPagerState(initialPage = startIndex.coerceIn(safe.indices)) { safe.size }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val image = safe[page]
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(image.url)
                        .crossfade(true)
                        .build(),
                    contentDescription = image.alt.ifBlank { "Image ${page + 1}" },
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(8.dp)
                    .align(Alignment.TopEnd)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

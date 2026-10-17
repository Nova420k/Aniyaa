package com.nyaa.aniyaa.util

import android.content.Context
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import com.nyaa.aniyaa.AniyaaApplication
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.data.api.resolvedMagnet
import com.nyaa.aniyaa.data.network.RemoteClient
import com.nyaa.aniyaa.data.prefs.AppPreferences
import com.nyaa.aniyaa.data.prefs.RemoteKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class HandoffResult {
    data object SentRemote : HandoffResult()
    data object SentLocal : HandoffResult()
    data class Failed(val message: String) : HandoffResult()
}

suspend fun handoffMagnets(
    context: Context,
    prefs: AppPreferences,
    magnets: List<String>
): HandoffResult {
    val links = magnets.map { it.trim() }.filter { it.isNotBlank() }
    if (links.isEmpty()) return HandoffResult.Failed("No magnet link available")
    if (prefs.remoteKind != RemoteKind.NONE && prefs.remoteBaseUrl.isNotBlank()) {
        val remoteError = withContext(Dispatchers.IO) {
            RemoteClient().addMagnets(prefs, links)
        }
        if (remoteError == null) return HandoffResult.SentRemote
        if (links.size > 1) return HandoffResult.Failed(remoteError)
        val local = openMagnet(context, links.first(), prefs.preferredTorrentPackage)
        return if (local == null) HandoffResult.SentLocal else HandoffResult.Failed(local)
    }
    var failure: String? = null
    var opened = 0
    links.forEach { magnet ->
        val error = openMagnet(context, magnet, prefs.preferredTorrentPackage)
        if (error == null) opened += 1 else if (failure == null) failure = error
    }
    return if (opened > 0) HandoffResult.SentLocal else HandoffResult.Failed(failure ?: "Could not open magnet")
}

fun launchHandoff(
    scope: CoroutineScope,
    context: Context,
    prefs: AppPreferences,
    torrents: List<Torrent>,
    snackbar: SnackbarHostState
) {
    scope.launch {
        when (val result = handoffMagnets(context, prefs, torrents.map { it.resolvedMagnet() })) {
            is HandoffResult.Failed -> snackbar.showSnackbar(result.message)
            else -> {
                val sent = AniyaaApplication.instance.sentRepository
                torrents.forEach { sent.add(it.bookmarkKey()) }
                val action = snackbar.showSnackbar("Sent", actionLabel = "Undo")
                if (action == SnackbarResult.ActionPerformed) {
                    torrents.forEach { sent.remove(it.bookmarkKey()) }
                }
            }
        }
    }
}

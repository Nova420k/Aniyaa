package com.nyaa.aniyaa.util

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import com.nyaa.aniyaa.data.api.resolvedMagnet
import com.nyaa.aniyaa.data.model.Torrent

data class TorrentApp(val packageName: String, val label: String)

fun listTorrentApps(context: Context): List<TorrentApp> {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("magnet:?xt=urn:btih:0000000000000000000000000000000000000000"))
    val flags = PackageManager.MATCH_DEFAULT_ONLY
    val resolved = context.packageManager.queryIntentActivities(intent, flags)
    return resolved.map {
        TorrentApp(
            packageName = it.activityInfo.packageName,
            label = it.loadLabel(context.packageManager).toString()
        )
    }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
}

fun openMagnet(context: Context, magnet: String, preferredPackage: String): String? {
    if (magnet.isBlank()) return "No magnet link available"
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(magnet)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (preferredPackage.isNotBlank()) {
        intent.setPackage(preferredPackage)
    }
    return try {
        context.startActivity(intent)
        null
    } catch (_: ActivityNotFoundException) {
        if (preferredPackage.isNotBlank()) {
            intent.setPackage(null)
            return try {
                context.startActivity(intent)
                null
            } catch (_: ActivityNotFoundException) {
                "No torrent app found to open this magnet"
            }
        }
        "No torrent app found to open this magnet"
    } catch (e: Exception) {
        e.message ?: "Could not open magnet"
    }
}

fun openHttpUrl(context: Context, url: String): String? {
    if (url.isBlank()) return "Missing link"
    return try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        null
    } catch (_: ActivityNotFoundException) {
        "No app found to open this link"
    } catch (e: Exception) {
        e.message ?: "Could not open link"
    }
}

fun copyText(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

fun shareText(context: Context, text: String): String? {
    return try {
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                },
                "Share"
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        null
    } catch (e: Exception) {
        e.message ?: "Could not share"
    }
}

fun downloadTorrentFile(context: Context, torrent: Torrent): String {
    val url = torrent.link.ifBlank { return "No torrent file link" }
    val name = buildString {
        val base = torrent.title.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(80).ifBlank { torrent.id.ifBlank { "torrent" } }
        append(base)
        if (!base.endsWith(".torrent", ignoreCase = true)) append(".torrent")
    }
    return try {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(name)
            .setDescription("Aniyaa")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        request.addRequestHeader("User-Agent", com.nyaa.aniyaa.data.network.AppHttpClient.USER_AGENT)
        manager.enqueue(request)
        "Downloading $name"
    } catch (e: Exception) {
        e.message ?: "Could not start download"
    }
}

fun magnetExportText(torrents: List<Torrent>): String =
    torrents.map { it.resolvedMagnet() }.filter { it.isNotBlank() }.joinToString("\n")

fun torrentShareText(torrent: Torrent): String {
    val magnet = torrent.resolvedMagnet()
    return buildString {
        append(torrent.title)
        append("\n\n")
        if (magnet.isNotEmpty()) append("Magnet: ").append(magnet).append('\n')
        if (torrent.guid.isNotEmpty()) append("Page: ").append(torrent.guid)
    }
}

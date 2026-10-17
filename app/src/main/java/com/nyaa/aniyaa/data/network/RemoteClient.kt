package com.nyaa.aniyaa.data.network

import com.nyaa.aniyaa.data.prefs.AppPreferences
import com.nyaa.aniyaa.data.prefs.RemoteKind
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class RemoteClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()
) {
    fun addMagnets(prefs: AppPreferences, magnets: List<String>): String? {
        val base = normalizeRemoteBase(prefs.remoteBaseUrl) ?: return "Enter a remote client address"
        val links = magnets.map { it.trim() }.filter { it.isNotBlank() }
        if (links.isEmpty()) return "No magnet link available"
        return try {
            when (prefs.remoteKind) {
                RemoteKind.NONE -> "Remote client is off"
                RemoteKind.QBITTORRENT -> addQbittorrent(base, prefs.remoteUsername, prefs.remotePassword, links)
                RemoteKind.TRANSMISSION -> addTransmission(base, prefs.remoteUsername, prefs.remotePassword, links)
            }
        } catch (e: Exception) {
            e.message ?: "Could not reach the remote client"
        }
    }

    private fun addQbittorrent(base: String, username: String, password: String, magnets: List<String>): String? {
        val loginBody = FormBody.Builder()
            .add("username", username)
            .add("password", password)
            .build()
        val login = client.newCall(
            Request.Builder().url("$base/api/v2/auth/login").post(loginBody).build()
        ).execute()
        val loginText = login.body.string()
        if (!login.isSuccessful || loginText.contains("Fails", ignoreCase = true)) {
            return "Could not sign in to qBittorrent"
        }
        val cookie = login.headers("Set-Cookie")
            .joinToString("; ") { it.substringBefore(';') }
            .ifBlank { return "qBittorrent did not accept the sign-in" }
        val addBody = FormBody.Builder().add("urls", magnets.joinToString("\n")).build()
        val add = client.newCall(
            Request.Builder()
                .url("$base/api/v2/torrents/add")
                .header("Cookie", cookie)
                .post(addBody)
                .build()
        ).execute()
        add.close()
        return if (add.isSuccessful) null else "qBittorrent rejected the magnet"
    }

    private fun addTransmission(base: String, username: String, password: String, magnets: List<String>): String? {
        var session = ""
        magnets.forEach { magnet ->
            val body = transmissionAddBody(magnet)
            var response = postTransmission(base, username, password, session, body)
            if (response.code == 409) {
                session = response.header("X-Transmission-Session-Id").orEmpty()
                response.close()
                if (session.isBlank()) return "Transmission did not provide a session"
                response = postTransmission(base, username, password, session, body)
            }
            val ok = response.isSuccessful
            response.close()
            if (!ok) return "Transmission rejected the magnet"
        }
        return null
    }

    private fun postTransmission(
        base: String,
        username: String,
        password: String,
        session: String,
        body: String
    ) = client.newCall(
        Request.Builder()
            .url("$base/transmission/rpc")
            .header("X-Transmission-Session-Id", session)
            .apply {
                if (username.isNotBlank()) header("Authorization", Credentials.basic(username, password))
            }
            .post(body.toRequestBody(JSON))
            .build()
    ).execute()

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

internal fun transmissionAddBody(magnet: String): String =
    JSONObject()
        .put("method", "torrent-add")
        .put("arguments", JSONObject().put("filename", magnet))
        .toString()

internal fun normalizeRemoteBase(raw: String): String? {
    val trimmed = raw.trim().trimEnd('/')
    if (trimmed.isEmpty()) return null
    val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "http://$trimmed"
    }
    val scheme = withScheme.substringBefore("://")
    if (scheme != "http" && scheme != "https") return null
    val host = withScheme.substringAfter("://").substringBefore('/').substringBefore('?')
    if (host.isBlank() || host.startsWith('.')) return null
    return withScheme
}

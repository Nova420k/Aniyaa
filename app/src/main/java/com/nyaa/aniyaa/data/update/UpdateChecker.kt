package com.nyaa.aniyaa.data.update

import com.nyaa.aniyaa.BuildConfig
import com.nyaa.aniyaa.data.network.AppHttpClient
import com.nyaa.aniyaa.data.network.await
import org.json.JSONObject

data class AppUpdate(
    val versionName: String,
    val htmlUrl: String,
    val notes: String,
    val apkUrl: String = ""
)

object UpdateChecker {
    internal val latestUrls = listOf(
        "https://api.github.com/repos/Nova420k/Aniyaa/releases/latest",
        "https://api.github.com/repos/Gourab0002/Aniyaa/releases/latest"
    )

    suspend fun check(): Result<AppUpdate?> {
        var lastError: Exception? = null
        for (url in latestUrls) {
            try {
                val request = AppHttpClient.newRequest(url).newBuilder()
                    .header("Accept", "application/vnd.github+json")
                    .build()
                AppHttpClient.instance.newCall(request).await().use { response ->
                    if (!response.isSuccessful) {
                        lastError = Exception("Could not check for updates (HTTP ${response.code})")
                        return@use
                    }
                    val body = response.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val tag = json.optString("tag_name").removePrefix("v")
                    if (tag.isBlank() || !isNewer(tag, BuildConfig.VERSION_NAME)) {
                        return Result.success(null)
                    }
                    return Result.success(
                        AppUpdate(
                            versionName = tag,
                            htmlUrl = json.optString("html_url")
                                .ifBlank { "https://github.com/Nova420k/Aniyaa/releases/latest" },
                            notes = json.optString("body"),
                            apkUrl = apkAssetUrl(json)
                        )
                    )
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        return Result.failure(lastError ?: Exception("Could not check for updates"))
    }

    internal fun isNewer(remote: String, local: String): Boolean {
        val remoteParts = remote.removePrefix("v").split('.', '-', '_')
            .mapNotNull { it.filter(Char::isDigit).toIntOrNull() }
        val localParts = local.removePrefix("v").split('.', '-', '_')
            .mapNotNull { it.filter(Char::isDigit).toIntOrNull() }
        val max = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until max) {
            val r = remoteParts.getOrNull(i) ?: 0
            val l = localParts.getOrNull(i) ?: 0
            if (r != l) return r > l
        }
        return false
    }

    internal fun apkAssetUrl(json: JSONObject): String {
        val assets = json.optJSONArray("assets") ?: return ""
        val pairs = ArrayList<Pair<String, String>>(assets.length())
        for (i in 0 until assets.length()) {
            val asset = assets.optJSONObject(i) ?: continue
            pairs += asset.optString("name") to asset.optString("browser_download_url")
        }
        return apkAssetUrl(pairs)
    }

    internal fun apkAssetUrl(assets: List<Pair<String, String>>): String =
        assets.firstOrNull { it.first.endsWith(".apk", ignoreCase = true) }?.second.orEmpty()
}

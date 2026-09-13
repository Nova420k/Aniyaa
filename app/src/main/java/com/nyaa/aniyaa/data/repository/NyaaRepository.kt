package com.nyaa.aniyaa.data.repository

import com.nyaa.aniyaa.data.api.NyaaCommentParser
import com.nyaa.aniyaa.data.api.NyaaHtmlSearchParser
import com.nyaa.aniyaa.data.api.NyaaRssParser
import com.nyaa.aniyaa.data.model.CatalogSite
import com.nyaa.aniyaa.data.model.SearchParams
import com.nyaa.aniyaa.data.model.SortField
import com.nyaa.aniyaa.data.model.SortOrder
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.util.parseSizeBytes
import com.nyaa.aniyaa.data.model.TorrentPageData
import com.nyaa.aniyaa.data.network.AppHttpClient
import com.nyaa.aniyaa.data.network.CatalogMirrors
import com.nyaa.aniyaa.data.network.HttpException
import com.nyaa.aniyaa.data.network.SiteConfig
import com.nyaa.aniyaa.data.network.await
import com.nyaa.aniyaa.data.network.isFailoverWorthy
import com.nyaa.aniyaa.data.network.toUserMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

internal const val NYAA_PAGE_SIZE = 75

internal fun looksLikeChallengePage(html: String): Boolean {
    val sample = html.take(8_000).lowercase()
    return "cf-browser-verification" in sample ||
        "challenge-platform" in sample ||
        "just a moment" in sample ||
        "checking your browser" in sample ||
        ("cf-ray" in sample && "torrent-list" !in sample)
}

internal fun looksLikeNyaaSearchPage(html: String): Boolean {
    val sample = html.take(20_000).lowercase()
    return "torrent-list" in sample || "class=\"table" in sample || "/view/" in sample
}

internal fun torrentIdentity(torrent: Torrent): String = torrent.bookmarkKey()

internal fun sortTorrents(torrents: List<Torrent>, params: SearchParams): List<Torrent> {
    val comparator = when (params.sortField) {
        SortField.SEEDERS -> compareBy<Torrent> { it.seeders }
        SortField.LEECHERS -> compareBy { it.leechers }
        SortField.DOWNLOADS -> compareBy { it.downloads }
        SortField.COMMENTS -> compareBy { it.comments }
        SortField.SIZE -> compareBy { parseSizeBytes(it.size) ?: 0L }
        SortField.DATE -> return torrents
    }
    return if (params.sortOrder == SortOrder.DESC) {
        torrents.sortedWith(comparator.reversed())
    } else {
        torrents.sortedWith(comparator)
    }
}

internal fun mergeSearchPages(
    existing: List<Torrent>,
    incoming: List<Torrent>,
    replace: Boolean
): Pair<List<Torrent>, Boolean> {
    if (replace) {
        val unique = incoming.distinctBy(::torrentIdentity)
        val canLoadMore = incoming.size >= NYAA_PAGE_SIZE && unique.isNotEmpty()
        return unique to canLoadMore
    }
    if (incoming.isEmpty()) return existing to false
    val seen = existing.mapTo(HashSet(existing.size + incoming.size)) { torrentIdentity(it) }
    val added = ArrayList<Torrent>(incoming.size)
    for (torrent in incoming) {
        if (seen.add(torrentIdentity(torrent))) {
            added.add(torrent)
        }
    }
    val merged = if (added.isEmpty()) existing else existing + added
    val canLoadMore = added.isNotEmpty() && incoming.size >= NYAA_PAGE_SIZE
    return merged to canLoadMore
}

fun buildSearchUrl(
    params: SearchParams,
    baseUrl: String = SiteConfig.baseUrl(params.site),
    rss: Boolean = true
): String {
    val base = baseUrl.trimEnd('/')
    val rawQuery = params.query.trim()
    val userMatch = USER_QUERY_REGEX.find(rawQuery)
    val username = userMatch?.groupValues?.getOrNull(1)?.trim().orEmpty()
    val remainder = if (userMatch != null) rawQuery.removePrefix(userMatch.value).trim() else rawQuery
    val parts = mutableListOf<String>()
    if (rss) parts += "page=rss"
    if (remainder.isNotBlank()) parts += "q=${URLEncoder.encode(remainder, "UTF-8")}"
    if (username.isNotEmpty()) parts += "u=${URLEncoder.encode(username, "UTF-8")}"
    parts += "c=${params.category.value}"
    parts += "f=${params.filter.value}"
    parts += "s=${params.sortField.value}"
    parts += "o=${params.sortOrder.value}"
    if (params.page > 1) parts += "p=${params.page}"
    val query = parts.joinToString("&")
    return if (username.isNotEmpty()) {
        "$base/user/${URLEncoder.encode(username, "UTF-8")}?$query"
    } else {
        "$base/?$query"
    }
}

private val USER_QUERY_REGEX = Regex("^user:([^\\s]+)", RegexOption.IGNORE_CASE)

internal fun looksLikeRss(contentType: String?, body: ByteArray): Boolean {
    val ct = contentType.orEmpty().lowercase()
    if (ct.contains("html")) return false
    val prefix = body
        .take(256)
        .toByteArray()
        .toString(Charsets.UTF_8)
        .trimStart('\uFEFF', ' ', '\n', '\r', '\t')
    val start = prefix.take(80).lowercase()
    if (ct.contains("xml") || ct.contains("rss") || ct.contains("atom")) {
        return start.startsWith("<")
    }
    return start.startsWith("<?xml") || start.startsWith("<rss") || start.startsWith("<feed")
}

internal fun mirrorCandidates(site: CatalogSite, current: String): List<String> {
    val normalizedCurrent = SiteConfig.normalize(current, site)
    val known = CatalogMirrors.forSite(site).map { SiteConfig.normalize(it, site) }
    return (listOf(normalizedCurrent) + known).distinctBy { it.lowercase() }
}

class NyaaRepository(
    private val client: okhttp3.OkHttpClient = AppHttpClient.instance
) {
    suspend fun search(params: SearchParams, fromCache: Boolean = false, forceNetwork: Boolean = false): Result<List<Torrent>> {
        return withContext(Dispatchers.IO) {
            if (fromCache) {
                return@withContext searchAt(params, SiteConfig.resolvedBaseUrl(params.site), fromCache = true, forceNetwork = false)
            }
            var lastFailure: Result<List<Torrent>>? = null
            val bases = mirrorCandidates(params.site, SiteConfig.resolvedBaseUrl(params.site))
            for (base in bases) {
                val result = searchAt(params, base, fromCache = false, forceNetwork = forceNetwork)
                if (result.isSuccess) {
                    if (base != SiteConfig.baseUrl(params.site)) {
                        SiteConfig.setSessionBaseUrl(params.site, base)
                    }
                    return@withContext result
                }
                lastFailure = result
                val error = result.exceptionOrNull()
                if (error?.isFailoverWorthy() != true) {
                    return@withContext result
                }
            }
            lastFailure ?: Result.failure(Exception("Can't reach the site. It may be blocked — try a mirror in Settings."))
        }
    }

    private suspend fun searchAt(
        params: SearchParams,
        baseUrl: String,
        fromCache: Boolean,
        forceNetwork: Boolean
    ): Result<List<Torrent>> {
        if (fromCache) {
            return fetchRss(params, baseUrl, fromCache = true, forceNetwork = false)
        }
        val htmlResult = fetchHtml(params, baseUrl, forceNetwork)
        if (htmlResult.isSuccess) return htmlResult
        val rssResult = fetchRss(params, baseUrl, fromCache = false, forceNetwork = forceNetwork)
        return if (rssResult.isSuccess) {
            rssResult.map { sortTorrents(it, params) }
        } else {
            htmlResult
        }
    }

    suspend fun fetchTorrentPageData(
        torrentId: String,
        site: CatalogSite = SiteConfig.currentSite
    ): Result<TorrentPageData> {
        return withContext(Dispatchers.IO) {
            runCatchingRequest {
                val baseUrl = SiteConfig.resolvedBaseUrl(site)
                val request = requestBuilder("$baseUrl/view/$torrentId").build()
                client.newCall(request).await().use { response ->
                    if (!response.isSuccessful) {
                        throw HttpException(response.code, "HTTP ${response.code}: ${response.message}")
                    }
                    val html = response.body?.string()
                        ?: throw IllegalStateException("Empty response")
                    NyaaCommentParser.parse(html, baseUrl)
                }
            }
        }
    }

    suspend fun probe(baseUrl: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            runCatchingRequest {
                val url = "${baseUrl.trimEnd('/')}/?page=rss"
                val request = requestBuilder(url)
                    .cacheControl(CacheControl.FORCE_NETWORK)
                    .build()
                client.newCall(request).await().use { response ->
                    if (!response.isSuccessful) {
                        throw HttpException(response.code, "HTTP ${response.code}: ${response.message}")
                    }
                    val body = response.body?.string().orEmpty()
                    if (body.isBlank()) {
                        throw IllegalStateException("Empty response")
                    }
                }
            }
        }
    }

    suspend fun fetchTorrent(
        torrentId: String,
        fallback: Torrent? = null,
        site: CatalogSite = fallback?.site ?: SiteConfig.currentSite
    ): Result<Torrent> {
        return fetchTorrentPageData(torrentId, site).map { page ->
            NyaaCommentParser.toTorrent(page, torrentId, fallback, site)
        }
    }

    private suspend fun fetchRss(
        params: SearchParams,
        baseUrl: String,
        fromCache: Boolean,
        forceNetwork: Boolean
    ): Result<List<Torrent>> = runCatchingRequest {
        val request = requestBuilder(buildSearchUrl(params, baseUrl, rss = true))
            .cacheControl(cacheControl(fromCache, forceNetwork))
            .build()
        client.newCall(request).await().use { response ->
            if (!response.isSuccessful) {
                throw HttpException(response.code, "HTTP ${response.code}: ${response.message}")
            }
            val body = response.body ?: throw IllegalStateException("Empty response")
            val bytes = body.bytes()
            val contentType = response.header("Content-Type")
            if (!looksLikeRss(contentType, bytes)) {
                throw HttpException(response.code, "HTTP ${response.code}: catalog returned a web page instead of RSS")
            }
            NyaaRssParser.parse(ByteArrayInputStream(bytes)).map { it.copy(site = params.site) }
        }
    }

    private suspend fun fetchHtml(
        params: SearchParams,
        baseUrl: String,
        forceNetwork: Boolean
    ): Result<List<Torrent>> = runCatchingRequest {
        val request = requestBuilder(buildSearchUrl(params, baseUrl, rss = false))
            .cacheControl(cacheControl(fromCache = false, forceNetwork = forceNetwork))
            .header("Accept", "text/html")
            .build()
        client.newCall(request).await().use { response ->
            if (!response.isSuccessful) {
                throw HttpException(response.code, "HTTP ${response.code}: ${response.message}")
            }
            val html = response.body?.string() ?: throw IllegalStateException("Empty response")
            if (looksLikeChallengePage(html)) {
                throw HttpException(response.code, "Catalog returned a challenge page instead of search results")
            }
            val parsed = NyaaHtmlSearchParser.parse(html, baseUrl).map { it.copy(site = params.site) }
            if (parsed.isEmpty() && !looksLikeNyaaSearchPage(html)) {
                throw HttpException(response.code, "Catalog returned a web page instead of search results")
            }
            parsed
        }
    }

    private fun requestBuilder(url: String): Request.Builder =
        AppHttpClient.newRequest(url).newBuilder()

    private fun cacheControl(fromCache: Boolean, forceNetwork: Boolean): CacheControl {
        return when {
            fromCache -> CacheControl.FORCE_CACHE
            forceNetwork -> CacheControl.Builder().noCache().maxAge(0, TimeUnit.SECONDS).build()
            else -> CacheControl.Builder().maxAge(60, TimeUnit.SECONDS).build()
        }
    }

    private inline fun <T> runCatchingRequest(block: () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception(e.toUserMessage(), e))
        }
    }
}

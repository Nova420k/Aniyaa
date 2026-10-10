package com.nyaa.aniyaa.data.backup

import com.nyaa.aniyaa.data.db.torrentFromBackupJson
import com.nyaa.aniyaa.data.db.torrentToBackupJson
import com.nyaa.aniyaa.data.model.CatalogSite
import com.nyaa.aniyaa.data.model.DarkMode
import com.nyaa.aniyaa.data.model.SavedSearch
import com.nyaa.aniyaa.data.model.SearchHistoryEntry
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.data.prefs.AppPreferences
import com.nyaa.aniyaa.data.repository.BookmarkRepository
import com.nyaa.aniyaa.data.repository.SavedSearchRepository
import com.nyaa.aniyaa.data.repository.SearchHistoryRepository
import com.nyaa.aniyaa.data.repository.ViewedListingRepository
import org.json.JSONArray
import org.json.JSONObject

class BackupManager(
    private val bookmarks: BookmarkRepository,
    private val history: SearchHistoryRepository,
    private val savedSearches: SavedSearchRepository,
    private val viewed: ViewedListingRepository,
    private val prefs: AppPreferences
) {
    suspend fun exportJson(password: String? = null): String {
        val root = JSONObject()
        root.put("version", 3)
        val bookmarkArray = JSONArray()
        bookmarks.getAll().forEach { bookmarkArray.put(torrentToBackupJson(it)) }
        root.put("bookmarks", bookmarkArray)

        val historyArray = JSONArray()
        history.getAll().forEach { entry ->
            historyArray.put(JSONObject().apply {
                put("query", entry.query)
                put("timestamp", entry.timestamp)
                put("site", entry.site.id)
                put("categoryValue", entry.categoryValue)
                put("filterValue", entry.filterValue)
                put("sortFieldValue", entry.sortFieldValue)
                put("sortOrderValue", entry.sortOrderValue)
            })
        }
        root.put("history", historyArray)

        val savedArray = JSONArray()
        savedSearches.getAll().forEach { search ->
            savedArray.put(JSONObject().apply {
                put("name", search.name)
                put("query", search.query)
                put("site", search.site.id)
                put("categoryValue", search.categoryValue)
                put("filterValue", search.filterValue)
                put("sortFieldValue", search.sortFieldValue)
                put("sortOrderValue", search.sortOrderValue)
                put("notify", search.notify)
                put("lastSeenIds", search.lastSeenIds)
                put("lastCheckedAt", search.lastCheckedAt)
            })
        }
        root.put("savedSearches", savedArray)

        val viewedArray = JSONArray()
        viewed.getAll().forEach { viewedArray.put(torrentToBackupJson(it)) }
        root.put("viewed", viewedArray)

        root.put(
            "settings",
            JSONObject().apply {
                put("themeIndex", prefs.themeIndex)
                put("darkMode", prefs.darkMode.value)
                put("currentSite", prefs.currentSite.id)
                put("sukebeiAcknowledged", prefs.sukebeiAcknowledged)
                put("sukebeiEnabled", prefs.sukebeiEnabled)
                put("onboardingComplete", prefs.onboardingComplete)
                put("nyaaBaseUrl", prefs.baseUrl(CatalogSite.NYAA))
                put("sukebeiBaseUrl", prefs.baseUrl(CatalogSite.SUKEBEI))
                put("preferredTorrentPackage", prefs.preferredTorrentPackage)
                put("hideScreenshots", prefs.hideScreenshots)
                put("hideFromRecents", prefs.hideFromRecents)
                put("loadLatestOnStart", prefs.loadLatestOnStart)
                put("savedSearchIntervalHours", prefs.savedSearchIntervalHours)
                put("lockGraceMs", prefs.lockGraceMs)
                put("compactCards", prefs.compactCards)
                put("biometricUnlockEnabled", prefs.biometricUnlockEnabled)
                put("defaultResolution", prefs.defaultResolutionTag)
                put("defaultCodec", prefs.defaultCodecTag)
                put("defaultMinSeeders", prefs.defaultMinSeeders)
                put("defaultHideBatches", prefs.defaultHideBatches)
                put("hideRules", JSONArray(prefs.hideRules()))
            }
        )
        val plaintext = root.toString(2)
        return if (!password.isNullOrBlank()) BackupCrypto.encrypt(plaintext, password) else plaintext
    }

    suspend fun importJson(json: String, merge: Boolean = false, password: String? = null) {
        val decrypted = if (BackupCrypto.isEncrypted(json)) {
            BackupCrypto.decrypt(json, password.orEmpty())
        } else {
            json
        }
        val root = JSONObject(decrypted)
        val settings = root.optJSONObject("settings")
        val fallbackSite = inferFallbackSite(settings)

        val bookmarkItems = mutableListOf<Torrent>()
        val bookmarkArray = root.optJSONArray("bookmarks") ?: JSONArray()
        for (i in 0 until bookmarkArray.length()) {
            bookmarkItems += torrentFromBackupJson(bookmarkArray.getJSONObject(i), fallbackSite)
        }
        if (merge) bookmarks.merge(bookmarkItems) else bookmarks.replaceAll(bookmarkItems)

        val historyItems = mutableListOf<SearchHistoryEntry>()
        val historyArray = root.optJSONArray("history") ?: JSONArray()
        for (i in 0 until historyArray.length()) {
            val obj = historyArray.getJSONObject(i)
            historyItems += SearchHistoryEntry(
                query = obj.optString("query"),
                timestamp = obj.optLong("timestamp"),
                site = if (obj.has("site")) CatalogSite.fromId(obj.optString("site")) else fallbackSite,
                categoryValue = obj.optString("categoryValue", "0_0"),
                filterValue = obj.optInt("filterValue", 0),
                sortFieldValue = obj.optString("sortFieldValue", "id"),
                sortOrderValue = obj.optString("sortOrderValue", "desc")
            )
        }
        if (merge) history.merge(historyItems) else history.replaceAll(historyItems)

        val savedItems = mutableListOf<SavedSearch>()
        val savedArray = root.optJSONArray("savedSearches") ?: JSONArray()
        for (i in 0 until savedArray.length()) {
            val obj = savedArray.getJSONObject(i)
            savedItems += SavedSearch(
                name = obj.optString("name"),
                query = obj.optString("query"),
                site = if (obj.has("site")) CatalogSite.fromId(obj.optString("site")) else fallbackSite,
                categoryValue = obj.optString("categoryValue", "0_0"),
                filterValue = obj.optInt("filterValue", 0),
                sortFieldValue = obj.optString("sortFieldValue", "id"),
                sortOrderValue = obj.optString("sortOrderValue", "desc"),
                notify = obj.optBoolean("notify"),
                lastSeenIds = obj.optString("lastSeenIds"),
                lastCheckedAt = obj.optLong("lastCheckedAt")
            )
        }
        if (merge) savedSearches.merge(savedItems) else savedSearches.replaceAll(savedItems)

        val viewedItems = mutableListOf<Torrent>()
        val viewedArray = root.optJSONArray("viewed") ?: JSONArray()
        for (i in 0 until viewedArray.length()) {
            viewedItems += torrentFromBackupJson(viewedArray.getJSONObject(i), fallbackSite)
        }
        if (merge) viewed.merge(viewedItems) else viewed.replaceAll(viewedItems)

        if (settings != null) {
            if (settings.has("themeIndex")) prefs.themeIndex = settings.optInt("themeIndex")
            if (settings.has("darkMode")) {
                prefs.darkMode = DarkMode.entries.find { it.value == settings.optString("darkMode") } ?: DarkMode.SYSTEM
            }
            if (settings.has("sukebeiAcknowledged")) {
                prefs.sukebeiAcknowledged = settings.optBoolean("sukebeiAcknowledged")
            }
            if (settings.has("sukebeiEnabled")) {
                prefs.sukebeiEnabled = settings.optBoolean("sukebeiEnabled")
            }
            if (settings.has("onboardingComplete")) {
                prefs.onboardingComplete = settings.optBoolean("onboardingComplete")
            }
            if (settings.has("currentSite")) prefs.currentSite = CatalogSite.fromId(settings.optString("currentSite"))
            if (settings.has("nyaaBaseUrl")) prefs.setBaseUrl(CatalogSite.NYAA, settings.optString("nyaaBaseUrl"))
            if (settings.has("sukebeiBaseUrl")) prefs.setBaseUrl(CatalogSite.SUKEBEI, settings.optString("sukebeiBaseUrl"))
            if (settings.has("baseUrl") && !settings.has("sukebeiBaseUrl") && !settings.has("nyaaBaseUrl")) {
                val url = settings.optString("baseUrl")
                val site = CatalogSite.fromUrl(url) ?: fallbackSite
                prefs.setBaseUrl(site, url)
            }
            if (settings.has("preferredTorrentPackage")) {
                prefs.preferredTorrentPackage = settings.optString("preferredTorrentPackage")
            }
            if (settings.has("hideScreenshots")) prefs.hideScreenshots = settings.optBoolean("hideScreenshots")
            if (settings.has("hideFromRecents")) prefs.hideFromRecents = settings.optBoolean("hideFromRecents")
            if (settings.has("loadLatestOnStart")) prefs.loadLatestOnStart = settings.optBoolean("loadLatestOnStart")
            if (settings.has("savedSearchIntervalHours")) {
                prefs.savedSearchIntervalHours = settings.optInt("savedSearchIntervalHours")
            }
            if (settings.has("lockGraceMs")) prefs.lockGraceMs = settings.optLong("lockGraceMs")
            if (settings.has("compactCards")) prefs.compactCards = settings.optBoolean("compactCards")
            if (settings.has("biometricUnlockEnabled")) {
                prefs.biometricUnlockEnabled = settings.optBoolean("biometricUnlockEnabled")
            }
            if (settings.has("defaultResolution")) prefs.defaultResolutionTag = settings.optString("defaultResolution")
            if (settings.has("defaultCodec")) prefs.defaultCodecTag = settings.optString("defaultCodec")
            if (settings.has("defaultMinSeeders")) prefs.defaultMinSeeders = settings.optInt("defaultMinSeeders")
            if (settings.has("defaultHideBatches")) prefs.defaultHideBatches = settings.optBoolean("defaultHideBatches")
            if (settings.has("hideRules")) {
                val rules = settings.optJSONArray("hideRules")
                if (rules != null) {
                    val values = ArrayList<String>(rules.length())
                    for (i in 0 until rules.length()) values += rules.optString(i)
                    prefs.setHideRules(values)
                }
            }
        }
    }

    private fun inferFallbackSite(settings: JSONObject?): CatalogSite {
        val base = settings?.optString("baseUrl").orEmpty()
        return CatalogSite.fromUrl(base) ?: CatalogSite.NYAA
    }
}

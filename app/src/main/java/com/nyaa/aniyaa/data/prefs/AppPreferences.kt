package com.nyaa.aniyaa.data.prefs

import android.content.Context
import com.nyaa.aniyaa.data.model.CatalogSite
import com.nyaa.aniyaa.data.model.Category
import com.nyaa.aniyaa.data.model.DarkMode
import com.nyaa.aniyaa.data.model.FilterOption
import com.nyaa.aniyaa.data.model.SearchParams
import com.nyaa.aniyaa.data.model.SortField
import com.nyaa.aniyaa.data.model.SortOrder
import com.nyaa.aniyaa.data.model.categoryByValue
import com.nyaa.aniyaa.data.model.darkModeByValue
import com.nyaa.aniyaa.data.model.sortFieldByValue
import com.nyaa.aniyaa.data.model.sortOrderByValue
import android.os.SystemClock
import com.nyaa.aniyaa.data.network.SiteConfig
import com.nyaa.aniyaa.util.CodecChoice
import com.nyaa.aniyaa.util.LocalFilter
import com.nyaa.aniyaa.util.ResolutionChoice
import com.nyaa.aniyaa.util.ResultFilter
import com.nyaa.aniyaa.util.SubtitleChoice
import com.nyaa.aniyaa.util.inQuietHours
import java.security.SecureRandom

enum class RemoteKind(val id: String, val label: String) {
    NONE("none", "Off"),
    QBITTORRENT("qbittorrent", "qBittorrent"),
    TRANSMISSION("transmission", "Transmission");

    companion object {
        fun fromId(value: String): RemoteKind =
            entries.find { it.id == value } ?: NONE
    }
}

class AppPreferences(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var themeIndex: Int
        get() = prefs.getInt(KEY_THEME_INDEX, 0)
        set(value) = prefs.edit().putInt(KEY_THEME_INDEX, value).apply()

    var darkMode: DarkMode
        get() = darkModeByValue(prefs.getString(KEY_DARK_MODE, DarkMode.SYSTEM.value).orEmpty())
        set(value) = prefs.edit().putString(KEY_DARK_MODE, value.value).apply()

    var currentSite: CatalogSite
        get() {
            val stored = CatalogSite.fromId(prefs.getString(KEY_CURRENT_SITE, CatalogSite.NYAA.id).orEmpty())
            return if (stored.nsfw && !sukebeiEnabled) CatalogSite.NYAA else stored
        }
        set(value) {
            val resolved = if (value.nsfw && !sukebeiEnabled) CatalogSite.NYAA else value
            prefs.edit().putString(KEY_CURRENT_SITE, resolved.id).apply()
            SiteConfig.currentSite = resolved
        }

    var sukebeiAcknowledged: Boolean
        get() = prefs.getBoolean(KEY_SUKEBEI_ACK, false)
        set(value) = prefs.edit().putBoolean(KEY_SUKEBEI_ACK, value).apply()

    var sukebeiEnabled: Boolean
        get() = prefs.getBoolean(KEY_SUKEBEI_ENABLED, sukebeiAcknowledged)
        set(value) {
            prefs.edit().putBoolean(KEY_SUKEBEI_ENABLED, value).apply()
            if (value) {
                sukebeiAcknowledged = true
            } else if (CatalogSite.fromId(prefs.getString(KEY_CURRENT_SITE, "").orEmpty()).nsfw) {
                currentSite = CatalogSite.NYAA
            }
        }

    var onboardingComplete: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING, value).apply()

    var preferredTorrentPackage: String
        get() = prefs.getString(KEY_TORRENT_PACKAGE, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_TORRENT_PACKAGE, value).apply()

    var lockEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_LOCK_ENABLED, value).apply()

    var hideScreenshots: Boolean
        get() = prefs.getBoolean(KEY_HIDE_SCREENSHOTS, lockEnabled)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_SCREENSHOTS, value).apply()

    var hideFromRecents: Boolean
        get() = prefs.getBoolean(KEY_HIDE_RECENTS, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_RECENTS, value).apply()

    var lastUpdateCheckAt: Long
        get() = prefs.getLong(KEY_UPDATE_CHECK_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_UPDATE_CHECK_AT, value).apply()

    var dismissedUpdateVersion: String
        get() = prefs.getString(KEY_DISMISSED_UPDATE, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_DISMISSED_UPDATE, value).apply()

    var roomMigrated: Boolean
        get() = prefs.getBoolean(KEY_ROOM_MIGRATED, false)
        set(value) = prefs.edit().putBoolean(KEY_ROOM_MIGRATED, value).apply()

    var loadLatestOnStart: Boolean
        get() = prefs.getBoolean(KEY_LOAD_LATEST, true)
        set(value) = prefs.edit().putBoolean(KEY_LOAD_LATEST, value).apply()

    var savedSearchIntervalHours: Int
        get() = prefs.getInt(KEY_ALERT_HOURS, 6).let { hours ->
            ALERT_INTERVAL_HOURS.minByOrNull { kotlin.math.abs(it - hours) } ?: 6
        }
        set(value) = prefs.edit().putInt(KEY_ALERT_HOURS, value.coerceIn(1, 24)).apply()

    var compactCards: Boolean
        get() = prefs.getBoolean(KEY_COMPACT_CARDS, false)
        set(value) = prefs.edit().putBoolean(KEY_COMPACT_CARDS, value).apply()

    var biometricUnlockEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC, true)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC, value).apply()

    var lockGraceMs: Long
        get() {
            val stored = prefs.getLong(KEY_LOCK_GRACE, 15_000L)
            return LOCK_GRACE_OPTIONS.minByOrNull { kotlin.math.abs(it - stored) } ?: 15_000L
        }
        set(value) = prefs.edit().putLong(KEY_LOCK_GRACE, value.coerceAtLeast(0L)).apply()

    val hasPin: Boolean
        get() = !prefs.getString(KEY_PIN_HASH, "").isNullOrBlank()

    fun pinLockRemainingMs(nowElapsed: Long = SystemClock.elapsedRealtime()): Long {
        val untilElapsed = prefs.getLong(KEY_PIN_LOCK_UNTIL_ELAPSED, 0L)
        if (untilElapsed > 0L) {
            return (untilElapsed - nowElapsed).coerceAtLeast(0L)
        }
        val untilWall = prefs.getLong(KEY_PIN_LOCK_UNTIL, 0L)
        return (untilWall - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    fun baseUrl(site: CatalogSite): String {
        val stored = prefs.getString(baseUrlKey(site), "").orEmpty()
        return SiteConfig.normalize(stored, site)
    }

    fun setBaseUrl(site: CatalogSite, url: String) {
        val normalized = SiteConfig.normalize(url, site)
        prefs.edit().putString(baseUrlKey(site), normalized).apply()
        SiteConfig.setBaseUrl(site, normalized)
    }

    fun defaultCategoryValue(site: CatalogSite): String =
        prefs.getString(siteKey(KEY_DEFAULT_CATEGORY, site), site.categories.first().value).orEmpty()

    fun setDefaultCategoryValue(site: CatalogSite, value: String) {
        prefs.edit().putString(siteKey(KEY_DEFAULT_CATEGORY, site), value).apply()
    }

    fun defaultSortFieldValue(site: CatalogSite = currentSite): String =
        prefs.getString(siteKey(KEY_DEFAULT_SORT_FIELD, site), SortField.DATE.value).orEmpty()

    fun setDefaultSortFieldValue(site: CatalogSite, value: String) {
        prefs.edit().putString(siteKey(KEY_DEFAULT_SORT_FIELD, site), value).apply()
    }

    fun defaultSortOrderValue(site: CatalogSite = currentSite): String =
        prefs.getString(siteKey(KEY_DEFAULT_SORT_ORDER, site), SortOrder.DESC.value).orEmpty()

    fun setDefaultSortOrderValue(site: CatalogSite, value: String) {
        prefs.edit().putString(siteKey(KEY_DEFAULT_SORT_ORDER, site), value).apply()
    }

    fun defaultCategory(site: CatalogSite = currentSite): Category =
        categoryByValue(defaultCategoryValue(site), site)

    fun defaultSearchParams(site: CatalogSite = currentSite): SearchParams = SearchParams(
        site = site,
        category = defaultCategory(site),
        filter = FilterOption.ALL,
        sortField = sortFieldByValue(defaultSortFieldValue(site)),
        sortOrder = sortOrderByValue(defaultSortOrderValue(site))
    ).withValidCategory()

    var defaultResolutionTag: String
        get() = ResolutionChoice.fromTag(prefs.getString(KEY_DEFAULT_RESOLUTION, "").orEmpty()).tag
        set(value) = prefs.edit().putString(KEY_DEFAULT_RESOLUTION, ResolutionChoice.fromTag(value).tag).apply()

    var defaultCodecTag: String
        get() = CodecChoice.fromTag(prefs.getString(KEY_DEFAULT_CODEC, "").orEmpty()).tag
        set(value) = prefs.edit().putString(KEY_DEFAULT_CODEC, CodecChoice.fromTag(value).tag).apply()

    var defaultSubtitleTag: String
        get() = SubtitleChoice.fromTag(prefs.getString(KEY_DEFAULT_SUBTITLE, "").orEmpty()).tag
        set(value) = prefs.edit().putString(KEY_DEFAULT_SUBTITLE, SubtitleChoice.fromTag(value).tag).apply()

    var defaultMinSeeders: Int
        get() = LocalFilter.coerceSeeders(prefs.getInt(KEY_DEFAULT_MIN_SEEDERS, 0))
        set(value) = prefs.edit().putInt(KEY_DEFAULT_MIN_SEEDERS, LocalFilter.coerceSeeders(value)).apply()

    var defaultHideBatches: Boolean
        get() = prefs.getBoolean(KEY_DEFAULT_HIDE_BATCHES, false)
        set(value) = prefs.edit().putBoolean(KEY_DEFAULT_HIDE_BATCHES, value).apply()

    fun defaultLocalFilter(): LocalFilter = LocalFilter(
        resolution = ResolutionChoice.fromTag(defaultResolutionTag),
        codec = CodecChoice.fromTag(defaultCodecTag),
        subtitle = SubtitleChoice.fromTag(defaultSubtitleTag),
        minSeeders = defaultMinSeeders,
        hideBatches = defaultHideBatches
    )

    var quietHoursEnabled: Boolean
        get() = prefs.getBoolean(KEY_QUIET_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_QUIET_ENABLED, value).apply()

    var quietStartHour: Int
        get() = prefs.getInt(KEY_QUIET_START, 23).coerceIn(0, 23)
        set(value) = prefs.edit().putInt(KEY_QUIET_START, value.coerceIn(0, 23)).apply()

    var quietEndHour: Int
        get() = prefs.getInt(KEY_QUIET_END, 7).coerceIn(0, 23)
        set(value) = prefs.edit().putInt(KEY_QUIET_END, value.coerceIn(0, 23)).apply()

    fun quietHoursActive(hour: Int): Boolean =
        inQuietHours(hour, quietStartHour, quietEndHour, quietHoursEnabled)

    var remoteKind: RemoteKind
        get() = RemoteKind.fromId(prefs.getString(KEY_REMOTE_KIND, RemoteKind.NONE.id).orEmpty())
        set(value) = prefs.edit().putString(KEY_REMOTE_KIND, value.id).apply()

    var remoteBaseUrl: String
        get() = prefs.getString(KEY_REMOTE_URL, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_REMOTE_URL, value.trim()).apply()

    var remoteUsername: String
        get() = prefs.getString(KEY_REMOTE_USER, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_REMOTE_USER, value).apply()

    var remotePassword: String
        get() = prefs.getString(KEY_REMOTE_PASSWORD, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_REMOTE_PASSWORD, value).apply()

    fun hideRules(): List<String> =
        prefs.getString(KEY_HIDE_RULES, "").orEmpty()
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    fun defaultResultFilter(): ResultFilter = defaultLocalFilter().toResultFilter(hideRules())

    fun addHideRule(raw: String): String? {
        val rule = raw.trim().replace(Regex("\\s+"), " ").take(MAX_HIDE_RULE_LENGTH)
        if (rule.length < 2) return "Use at least 2 characters"
        val current = hideRules()
        if (current.any { it.equals(rule, ignoreCase = true) }) return "Already hidden"
        if (current.size >= MAX_HIDE_RULES) return "You can hide up to $MAX_HIDE_RULES phrases"
        prefs.edit().putString(KEY_HIDE_RULES, (current + rule).joinToString("\n")).apply()
        return null
    }

    fun removeHideRule(rule: String) {
        val next = hideRules().filterNot { it.equals(rule, ignoreCase = true) }
        prefs.edit().putString(KEY_HIDE_RULES, next.joinToString("\n")).apply()
    }

    fun setHideRules(rules: List<String>) {
        val cleaned = rules.map { it.trim().replace(Regex("\\s+"), " ") }
            .filter { it.length >= 2 }
            .distinctBy { it.lowercase() }
            .take(MAX_HIDE_RULES)
        prefs.edit().putString(KEY_HIDE_RULES, cleaned.joinToString("\n")).apply()
    }

    fun applyToSiteConfig() {
        CatalogSite.entries.forEach { site ->
            SiteConfig.setBaseUrl(site, baseUrl(site))
        }
        SiteConfig.currentSite = currentSite
    }

    fun setPin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val algorithm = PinHasher.preferredAlgorithm()
        val hash = PinHasher.hash(pin, salt, algorithm)
        prefs.edit()
            .putString(KEY_PIN_SALT, salt.toHex())
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_ALGO, algorithm)
            .remove(KEY_PIN_FAILURES)
            .remove(KEY_PIN_LOCK_UNTIL)
            .remove(KEY_PIN_LOCK_UNTIL_ELAPSED)
            .apply()
    }

    fun verifyPin(pin: String, nowElapsed: Long = SystemClock.elapsedRealtime()): Boolean {
        if (pinLockRemainingMs(nowElapsed) > 0L) return false
        val saltHex = prefs.getString(KEY_PIN_SALT, "").orEmpty()
        val stored = prefs.getString(KEY_PIN_HASH, "").orEmpty()
        if (saltHex.isBlank() || stored.isBlank()) return false
        val salt = saltHex.fromHex()
        val algorithm = prefs.getString(KEY_PIN_ALGO, "").orEmpty().ifBlank { PinHasher.LEGACY_SHA256 }
        val matches = PinHasher.matches(pin, salt, stored, algorithm) ||
            (algorithm != PinHasher.LEGACY_SHA256 && PinHasher.matches(pin, salt, stored, PinHasher.LEGACY_SHA256))
        if (matches) {
            if (algorithm == PinHasher.LEGACY_SHA256) {
                setPin(pin)
            } else {
                prefs.edit()
                    .remove(KEY_PIN_FAILURES)
                    .remove(KEY_PIN_LOCK_UNTIL)
                    .remove(KEY_PIN_LOCK_UNTIL_ELAPSED)
                    .apply()
            }
            return true
        }
        val failures = prefs.getInt(KEY_PIN_FAILURES, 0) + 1
        val lockMs = PinHasher.lockoutMillis(failures)
        prefs.edit()
            .putInt(KEY_PIN_FAILURES, failures)
            .putLong(KEY_PIN_LOCK_UNTIL_ELAPSED, if (lockMs > 0L) nowElapsed + lockMs else 0L)
            .remove(KEY_PIN_LOCK_UNTIL)
            .apply()
        return false
    }

    fun clearPin() {
        prefs.edit()
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_ALGO)
            .remove(KEY_PIN_FAILURES)
            .remove(KEY_PIN_LOCK_UNTIL)
            .remove(KEY_PIN_LOCK_UNTIL_ELAPSED)
            .putBoolean(KEY_LOCK_ENABLED, false)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_THEME_INDEX = "theme_index"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_CURRENT_SITE = "current_site"
        private const val KEY_SUKEBEI_ACK = "sukebei_ack"
        private const val KEY_SUKEBEI_ENABLED = "sukebei_enabled"
        private const val KEY_ONBOARDING = "onboarding_complete"
        private const val KEY_DEFAULT_CATEGORY = "default_category"
        private const val KEY_DEFAULT_SORT_FIELD = "default_sort_field"
        private const val KEY_DEFAULT_SORT_ORDER = "default_sort_order"
        private const val KEY_DEFAULT_RESOLUTION = "default_resolution"
        private const val KEY_DEFAULT_CODEC = "default_codec"
        private const val KEY_DEFAULT_SUBTITLE = "default_subtitle"
        private const val KEY_QUIET_ENABLED = "quiet_hours_enabled"
        private const val KEY_QUIET_START = "quiet_hours_start"
        private const val KEY_QUIET_END = "quiet_hours_end"
        private const val KEY_REMOTE_KIND = "remote_kind"
        private const val KEY_REMOTE_URL = "remote_url"
        private const val KEY_REMOTE_USER = "remote_user"
        private const val KEY_REMOTE_PASSWORD = "remote_password"
        private const val KEY_DEFAULT_MIN_SEEDERS = "default_min_seeders"
        private const val KEY_DEFAULT_HIDE_BATCHES = "default_hide_batches"
        private const val KEY_HIDE_RULES = "hide_rules"
        const val MAX_HIDE_RULES = 50
        const val MAX_HIDE_RULE_LENGTH = 64
        private const val KEY_TORRENT_PACKAGE = "torrent_package"
        private const val KEY_LOCK_ENABLED = "lock_enabled"
        private const val KEY_HIDE_SCREENSHOTS = "hide_screenshots"
        private const val KEY_HIDE_RECENTS = "hide_recents"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_UPDATE_CHECK_AT = "update_check_at"
        private const val KEY_DISMISSED_UPDATE = "dismissed_update"
        private const val KEY_ROOM_MIGRATED = "room_migrated"
        private const val KEY_LOAD_LATEST = "load_latest_on_start"
        private const val KEY_ALERT_HOURS = "saved_search_interval_hours"
        private const val KEY_LOCK_GRACE = "lock_grace_ms"
        private const val KEY_PIN_ALGO = "pin_algo"
        private const val KEY_PIN_FAILURES = "pin_failures"
        private const val KEY_PIN_LOCK_UNTIL = "pin_lock_until"
        private const val KEY_PIN_LOCK_UNTIL_ELAPSED = "pin_lock_until_elapsed"
        private const val KEY_COMPACT_CARDS = "compact_cards"
        private const val KEY_BIOMETRIC = "biometric_unlock"

        val ALERT_INTERVAL_HOURS = listOf(1, 3, 6, 12, 24)
        val LOCK_GRACE_OPTIONS = listOf(0L, 15_000L, 60_000L, 300_000L)

        fun lockGraceLabel(ms: Long): String = when (ms) {
            0L -> "Immediately"
            15_000L -> "15 seconds"
            60_000L -> "1 minute"
            300_000L -> "5 minutes"
            else -> "${ms / 1000} seconds"
        }

        private fun baseUrlKey(site: CatalogSite) = "base_url_${site.id}"

        private fun siteKey(base: String, site: CatalogSite) = "${base}_${site.id}"
    }
}

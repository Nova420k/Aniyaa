package com.nyaa.aniyaa.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nyaa.aniyaa.AniyaaApplication
import com.nyaa.aniyaa.data.backup.BackupManager
import com.nyaa.aniyaa.data.model.CatalogSite
import com.nyaa.aniyaa.data.model.DarkMode
import com.nyaa.aniyaa.data.network.toUserMessage
import com.nyaa.aniyaa.data.update.AppUpdate
import com.nyaa.aniyaa.data.update.UpdateChecker
import com.nyaa.aniyaa.data.update.UpdateInstaller
import com.nyaa.aniyaa.work.SavedSearchWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AniyaaApplication
    val prefs = app.prefs
    private val backupManager = BackupManager(
        bookmarks = app.bookmarkRepository,
        history = app.historyRepository,
        savedSearches = app.savedSearchRepository,
        viewed = app.viewedListingRepository,
        prefs = prefs
    )

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _update = MutableStateFlow<AppUpdate?>(null)
    val update: StateFlow<AppUpdate?> = _update.asStateFlow()

    private val _checkingUpdate = MutableStateFlow(false)
    val checkingUpdate: StateFlow<Boolean> = _checkingUpdate.asStateFlow()

    private val _installingUpdate = MutableStateFlow(false)
    val installingUpdate: StateFlow<Boolean> = _installingUpdate.asStateFlow()

    private val _downloadedUpdate = MutableStateFlow<java.io.File?>(null)
    val downloadedUpdate: StateFlow<java.io.File?> = _downloadedUpdate.asStateFlow()

    fun setThemeIndex(index: Int) {
        prefs.themeIndex = index
    }

    fun setDarkMode(mode: DarkMode) {
        prefs.darkMode = mode
    }

    fun setBaseUrl(site: CatalogSite, url: String) {
        prefs.setBaseUrl(site, url)
        _message.value = "Using ${prefs.baseUrl(site)}"
    }

    fun testMirror(site: CatalogSite, url: String) {
        viewModelScope.launch {
            val normalized = com.nyaa.aniyaa.data.network.SiteConfig.normalize(url, site)
            val result = withContext(Dispatchers.IO) { app.nyaaRepository.probe(normalized) }
            result.fold(
                onSuccess = { _message.value = "$normalized is reachable" },
                onFailure = { _message.value = it.toUserMessage() }
            )
        }
    }

    fun setLoadLatestOnStart(enabled: Boolean) {
        prefs.loadLatestOnStart = enabled
    }

    fun setSavedSearchIntervalHours(hours: Int) {
        prefs.savedSearchIntervalHours = hours
        viewModelScope.launch {
            SavedSearchWorker.sync(
                getApplication(),
                app.savedSearchRepository.getNotifying().isNotEmpty(),
                hours
            )
        }
        _message.value = "Alerts every ${hours}h"
    }

    fun setLockGraceMs(ms: Long) {
        prefs.lockGraceMs = ms
    }

    fun setCurrentSite(site: CatalogSite) {
        prefs.currentSite = site
        if (site.nsfw) prefs.sukebeiAcknowledged = true
    }

    fun acknowledgeSukebei() {
        prefs.sukebeiAcknowledged = true
        prefs.sukebeiEnabled = true
    }

    fun setSukebeiEnabled(enabled: Boolean) {
        if (enabled && !prefs.sukebeiAcknowledged) return
        prefs.sukebeiEnabled = enabled
        if (!enabled) {
            prefs.currentSite = CatalogSite.NYAA
        }
    }

    fun setDefaultCategory(value: String, site: CatalogSite = prefs.currentSite) {
        prefs.setDefaultCategoryValue(site, value)
    }

    fun setDefaultSortField(value: String, site: CatalogSite = prefs.currentSite) {
        prefs.setDefaultSortFieldValue(site, value)
    }

    fun setDefaultSortOrder(value: String, site: CatalogSite = prefs.currentSite) {
        prefs.setDefaultSortOrderValue(site, value)
    }

    fun setDefaultResolution(tag: String) {
        prefs.defaultResolutionTag = tag
    }

    fun setDefaultCodec(tag: String) {
        prefs.defaultCodecTag = tag
    }

    fun setDefaultMinSeeders(value: Int) {
        prefs.defaultMinSeeders = value
    }

    fun setDefaultHideBatches(enabled: Boolean) {
        prefs.defaultHideBatches = enabled
    }

    fun addHideRule(raw: String) {
        val error = prefs.addHideRule(raw)
        if (error != null) _message.value = error
    }

    fun removeHideRule(rule: String) {
        prefs.removeHideRule(rule)
    }

    fun setPreferredTorrentPackage(value: String) {
        prefs.preferredTorrentPackage = value
    }

    fun setLockEnabled(enabled: Boolean) {
        if (enabled && !prefs.hasPin) {
            _message.value = "Set a PIN first"
            return
        }
        prefs.lockEnabled = enabled
        if (enabled) {
            prefs.hideScreenshots = true
            app.lockController.lock()
            _message.value = "Screenshot blocking is on while the app is locked"
        } else {
            app.lockController.unlock()
        }
    }

    fun setPin(pin: String) {
        prefs.setPin(pin)
        _message.value = "PIN saved"
    }

    fun clearPin() {
        prefs.clearPin()
        app.lockController.unlock()
        _message.value = "PIN removed"
    }

    fun setCompactCards(enabled: Boolean) {
        prefs.compactCards = enabled
    }

    fun setBiometricUnlockEnabled(enabled: Boolean) {
        prefs.biometricUnlockEnabled = enabled
    }

    fun setHideScreenshots(enabled: Boolean) {
        prefs.hideScreenshots = enabled
    }

    fun setHideFromRecents(enabled: Boolean) {
        prefs.hideFromRecents = enabled
    }

    fun consumeMessage() {
        _message.value = null
    }

    fun checkForUpdates(manual: Boolean = true) {
        viewModelScope.launch {
            _checkingUpdate.value = true
            val result = withContext(Dispatchers.IO) { UpdateChecker.check() }
            _checkingUpdate.value = false
            result.fold(
                onSuccess = { update ->
                    prefs.lastUpdateCheckAt = System.currentTimeMillis()
                    if (update == null) {
                        if (manual) _message.value = "You're on the latest version"
                        _update.value = null
                    } else if (update.versionName == prefs.dismissedUpdateVersion && !manual) {
                        _update.value = null
                    } else {
                        _update.value = update
                    }
                },
                onFailure = {
                    if (manual) _message.value = "Could not check for updates"
                }
            )
        }
    }

    fun dismissUpdate() {
        _update.value?.let { prefs.dismissedUpdateVersion = it.versionName }
        _update.value = null
        _downloadedUpdate.value = null
    }

    fun downloadAndInstallUpdate(context: android.content.Context) {
        val update = _update.value ?: return
        val existing = _downloadedUpdate.value
        if (existing != null && existing.exists()) {
            _message.value = UpdateInstaller.install(context, existing) ?: "Opening installer"
            return
        }
        if (update.apkUrl.isBlank()) {
            _message.value = "No APK attached to this release"
            return
        }
        viewModelScope.launch {
            _installingUpdate.value = true
            try {
                val file = withContext(Dispatchers.IO) { UpdateInstaller.download(context, update.apkUrl) }
                _downloadedUpdate.value = file
                _message.value = UpdateInstaller.install(context, file) ?: "Opening installer"
            } catch (e: Exception) {
                _message.value = e.message ?: "Could not download update"
            } finally {
                _installingUpdate.value = false
            }
        }
    }

    suspend fun exportBackup(password: String? = null): String =
        withContext(Dispatchers.IO) { backupManager.exportJson(password) }

    fun clearNetworkCache() {
        com.nyaa.aniyaa.data.network.AppHttpClient.clearCache()
        _message.value = "Network cache cleared"
    }

    fun resetLocalData() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    app.bookmarkRepository.replaceAll(emptyList())
                    app.historyRepository.clear()
                    app.savedSearchRepository.replaceAll(emptyList())
                    app.viewedListingRepository.replaceAll(emptyList())
                    app.sentRepository.clear()
                    com.nyaa.aniyaa.data.network.AppHttpClient.clearCache()
                }
                _message.value = "Local data cleared"
            } catch (e: Exception) {
                _message.value = e.message ?: "Could not clear local data"
            }
        }
    }

    fun importBackup(json: String, merge: Boolean = false, password: String? = null) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { backupManager.importJson(json, merge, password) }
                SavedSearchWorker.sync(getApplication(), app.savedSearchRepository.getNotifying().isNotEmpty())
                _message.value = if (merge) "Backup merged" else "Backup restored"
            } catch (e: Exception) {
                _message.value = e.message ?: "Could not restore backup"
            }
        }
    }
}

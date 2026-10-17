package com.nyaa.aniyaa

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.nyaa.aniyaa.data.db.AppDatabase
import com.nyaa.aniyaa.data.db.migrateFromLegacy
import com.nyaa.aniyaa.data.network.AppHttpClient
import com.nyaa.aniyaa.data.prefs.AppPreferences
import com.nyaa.aniyaa.data.repository.BookmarkRepository
import com.nyaa.aniyaa.data.repository.SavedSearchRepository
import com.nyaa.aniyaa.data.repository.SearchHistoryRepository
import com.nyaa.aniyaa.data.repository.NyaaRepository
import com.nyaa.aniyaa.data.repository.SentRepository
import com.nyaa.aniyaa.data.repository.ViewedListingRepository
import com.nyaa.aniyaa.ui.lock.AppLockController
import com.nyaa.aniyaa.work.SavedSearchWorker
import coil.Coil
import coil.ImageLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AniyaaApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var prefs: AppPreferences
        private set
    lateinit var database: AppDatabase
        private set
    lateinit var bookmarkRepository: BookmarkRepository
        private set
    lateinit var historyRepository: SearchHistoryRepository
        private set
    lateinit var savedSearchRepository: SavedSearchRepository
        private set
    lateinit var nyaaRepository: NyaaRepository
        private set
    lateinit var viewedListingRepository: ViewedListingRepository
        private set
    lateinit var sentRepository: SentRepository
        private set
    val lockController = AppLockController()

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = AppPreferences(this)
        prefs.applyToSiteConfig()
        AppHttpClient.configure(cacheDir)
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .okHttpClient(AppHttpClient.imageClient)
                .crossfade(true)
                .build()
        )
        database = AppDatabase.get(this)
        bookmarkRepository = BookmarkRepository(database)
        historyRepository = SearchHistoryRepository(database)
        savedSearchRepository = SavedSearchRepository(database)
        nyaaRepository = NyaaRepository()
        viewedListingRepository = ViewedListingRepository(database)
        sentRepository = SentRepository(database)
        applicationScope.launch {
            database.migrateFromLegacy(this@AniyaaApplication, prefs)
        }
        applicationScope.launch {
            SavedSearchWorker.sync(this@AniyaaApplication, savedSearchRepository.getNotifying().isNotEmpty())
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                lockController.onForeground(prefs.lockEnabled, prefs.hasPin, prefs.lockGraceMs)
            }

            override fun onStop(owner: LifecycleOwner) {
                if (prefs.lockEnabled) {
                    lockController.onBackground()
                }
            }
        })
    }

    companion object {
        lateinit var instance: AniyaaApplication
            private set
    }
}

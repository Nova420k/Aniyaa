package com.nyaa.aniyaa.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nyaa.aniyaa.AniyaaApplication
import com.nyaa.aniyaa.MainActivity
import com.nyaa.aniyaa.R
import com.nyaa.aniyaa.data.db.AppDatabase
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.data.repository.NyaaRepository
import com.nyaa.aniyaa.data.repository.SavedSearchRepository
import com.nyaa.aniyaa.ui.widget.WatchingWidget
import com.nyaa.aniyaa.util.passes
import com.nyaa.aniyaa.util.watchingHeadline
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

class SavedSearchWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.get(applicationContext)
        val savedRepo = SavedSearchRepository(database)
        val repository = NyaaRepository()
        val notifying = savedRepo.getNotifying()
        if (notifying.isEmpty()) return Result.success()
        val prefs = (applicationContext as AniyaaApplication).prefs
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        if (prefs.quietHoursActive(hour)) return Result.success()
        ensureChannel(applicationContext)

        var anyFailure = false
        notifying.forEach { saved ->
            var torrents: List<Torrent>? = null
            for (attempt in 0 until 3) {
                torrents = repository.search(saved.toSearchParams(), forceNetwork = true).getOrNull()
                if (torrents != null) break
                if (attempt < 2) delay(2_000L * (attempt + 1))
            }
            if (torrents == null) {
                anyFailure = true
                return@forEach
            }
            if (torrents.isEmpty()) return@forEach
            val ids = torrents.map { it.id }.filter { it.isNotBlank() }
            val newIds = SavedSearchAlerts.newIds(ids, saved.lastSeenIds).toSet()
            val filter = prefs.defaultResultFilter()
            val visibleNew = torrents.filter { it.id in newIds && it.passes(filter) && matchesAlert(it, saved) }
            savedRepo.update(
                saved.copy(
                    lastSeenIds = SavedSearchAlerts.storeIds(ids),
                    lastCheckedAt = System.currentTimeMillis(),
                    lastHeadline = watchingHeadline(torrents.first().title),
                    unseenCount = if (visibleNew.isNotEmpty()) visibleNew.size else saved.unseenCount
                )
            )
            if (visibleNew.isNotEmpty()) {
                notifyNewResults(
                    applicationContext,
                    saved.id,
                    saved.displayName(),
                    visibleNew.size,
                    visibleNew.map { it.title }
                )
            }
        }
        WatchingWidget.refresh(applicationContext)
        return if (anyFailure) Result.retry() else Result.success()
    }

    companion object {
        private const val UNIQUE_NAME = "saved-search-alerts"
        private const val CHANNEL_ID = "saved_searches"

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
        }

        fun sync(context: Context, hasNotifying: Boolean, intervalHours: Int? = null) {
            if (hasNotifying) enqueue(context, intervalHours, replace = true) else cancel(context)
        }

        fun enqueue(context: Context, intervalHours: Int? = null, replace: Boolean = false) {
            val hours = (intervalHours
                ?: (context.applicationContext as? AniyaaApplication)?.prefs?.savedSearchIntervalHours
                ?: 6).coerceIn(1, 24)
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<SavedSearchWorker>(hours.toLong(), TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                if (replace) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(NotificationManager::class.java)
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Saved search alerts",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                manager.createNotificationChannel(channel)
            }
        }

        private fun notifyNewResults(
            context: Context,
            savedId: Long,
            name: String,
            count: Int,
            titles: List<String>
        ) {
            val intent = Intent(context, MainActivity::class.java).apply {
                data = Uri.parse("aniyaa://saved/$savedId")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pending = PendingIntent.getActivity(
                context,
                savedId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val style = NotificationCompat.InboxStyle()
                .setBigContentTitle("New results for $name")
            SavedSearchAlerts.inboxLines(titles, count).forEach { style.addLine(it) }
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notify)
                .setContentTitle("New results for $name")
                .setContentText(SavedSearchAlerts.contentText(count))
                .setStyle(style)
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()
            try {
                NotificationManagerCompat.from(context).notify(1000 + savedId.hashCode(), notification)
            } catch (_: SecurityException) {
            }
        }
    }
}

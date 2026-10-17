package com.nyaa.aniyaa.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.nyaa.aniyaa.MainActivity
import com.nyaa.aniyaa.R
import com.nyaa.aniyaa.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WatchingWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                render(context.applicationContext, appWidgetManager, appWidgetIds)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, WatchingWidget::class.java))
            if (ids.isEmpty()) return
            val intent = Intent(context, WatchingWidget::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }

        internal suspend fun render(
            context: Context,
            manager: AppWidgetManager,
            ids: IntArray
        ) {
            val searches = withContext(Dispatchers.IO) { AppDatabase.get(context).savedSearchDao().getAll() }
            val featured = searches.firstOrNull { it.notify && it.lastHeadline.isNotBlank() }
                ?: searches.firstOrNull { it.lastHeadline.isNotBlank() }
                ?: searches.firstOrNull()
            val title = featured?.name?.ifBlank { null } ?: "Aniyaa"
            val subtitle = featured?.lastHeadline?.ifBlank { null } ?: "Open Aniyaa"
            ids.forEach { id ->
                val views = RemoteViews(context.packageName, R.layout.watching_widget)
                views.setTextViewText(R.id.widget_title, title)
                views.setTextViewText(R.id.widget_subtitle, subtitle)
                val open = Intent(context, MainActivity::class.java).apply {
                    if (featured != null) data = Uri.parse("aniyaa://saved/${featured.id}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val click = PendingIntent.getActivity(
                    context,
                    id,
                    open,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, click)
                manager.updateAppWidget(id, views)
            }
        }
    }
}

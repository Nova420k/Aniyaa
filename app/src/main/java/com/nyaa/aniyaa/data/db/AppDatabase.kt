package com.nyaa.aniyaa.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.nyaa.aniyaa.data.model.CatalogSite
import com.nyaa.aniyaa.data.model.FilterOption
import com.nyaa.aniyaa.data.model.SearchHistoryEntry
import com.nyaa.aniyaa.data.model.SortField
import com.nyaa.aniyaa.data.model.SortOrder
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.data.prefs.AppPreferences
import org.json.JSONArray
import org.json.JSONObject

@Database(
    entities = [
        BookmarkEntity::class,
        HistoryEntity::class,
        SavedSearchEntity::class,
        ViewedListingEntity::class,
        SentListingEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun historyDao(): HistoryDao
    abstract fun savedSearchDao(): SavedSearchDao
    abstract fun viewedListingDao(): ViewedListingDao
    abstract fun sentListingDao(): SentListingDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sent_listings (
                        identity TEXT NOT NULL,
                        sentAt INTEGER NOT NULL,
                        PRIMARY KEY(identity)
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE saved_searches ADD COLUMN lastHeadline TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE saved_searches ADD COLUMN unseenCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE saved_searches ADD COLUMN alertTrustedOnly INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE saved_searches ADD COLUMN alertResolution TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS viewed_listings (
                        identity TEXT NOT NULL,
                        site TEXT NOT NULL,
                        torrentId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        link TEXT NOT NULL,
                        guid TEXT NOT NULL,
                        pubDate TEXT NOT NULL,
                        seeders INTEGER NOT NULL,
                        leechers INTEGER NOT NULL,
                        downloads INTEGER NOT NULL,
                        infoHash TEXT NOT NULL,
                        category TEXT NOT NULL,
                        size TEXT NOT NULL,
                        comments INTEGER NOT NULL,
                        trusted INTEGER NOT NULL,
                        remake INTEGER NOT NULL,
                        magnetLink TEXT NOT NULL,
                        submitter TEXT NOT NULL,
                        viewedAt INTEGER NOT NULL,
                        PRIMARY KEY(identity)
                    )
                    """.trimIndent()
                )
            }
        }

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aniyaa.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
        }
    }
}

suspend fun AppDatabase.migrateFromLegacy(context: Context, prefs: AppPreferences) {
    if (prefs.roomMigrated) return

    val bookmarkPrefs = context.getSharedPreferences("bookmarks", Context.MODE_PRIVATE)
    val bookmarkJson = bookmarkPrefs.getString("bookmarks_list", "[]") ?: "[]"
    val existingBookmarks = bookmarkDao().getAll()
    if (existingBookmarks.isEmpty() && bookmarkJson != "[]") {
        val torrents = parseLegacyBookmarks(bookmarkJson)
        if (torrents.isNotEmpty()) {
            bookmarkDao().upsertAll(
                torrents.mapIndexed { index, torrent ->
                    torrent.toBookmarkEntity(addedAt = System.currentTimeMillis() - index)
                }
            )
        }
    }

    val historyPrefs = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)
    val historyJson = historyPrefs.getString("history_list", "[]") ?: "[]"
    val existingHistory = historyDao().getAll()
    if (existingHistory.isEmpty() && historyJson != "[]") {
        parseLegacyHistory(historyJson).forEach { historyDao().insert(it.toEntity()) }
    }

    val themePrefs = context.getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
    if (themePrefs.contains("theme_index") && !context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).contains("theme_index")) {
        prefs.themeIndex = themePrefs.getInt("theme_index", 0)
    }

    prefs.roomMigrated = true
}

private fun parseLegacyBookmarks(json: String): List<Torrent> {
    return try {
        val array = JSONArray(json)
        (0 until array.length()).map { i ->
            torrentFromBackupJson(array.getJSONObject(i), CatalogSite.NYAA)
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun parseLegacyHistory(json: String): List<SearchHistoryEntry> {
    return try {
        val array = JSONArray(json)
        (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            SearchHistoryEntry(
                query = obj.optString("query", ""),
                timestamp = obj.optLong("timestamp", 0L),
                site = CatalogSite.fromId(obj.optString("site", CatalogSite.NYAA.id)),
                categoryValue = obj.optString("categoryValue", "0_0"),
                filterValue = obj.optInt("filterValue", FilterOption.ALL.value),
                sortFieldValue = obj.optString("sortFieldValue", SortField.DATE.value),
                sortOrderValue = obj.optString("sortOrderValue", SortOrder.DESC.value)
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

fun torrentToBackupJson(torrent: Torrent): JSONObject = JSONObject().apply {
    put("id", torrent.id)
    put("title", torrent.title)
    put("link", torrent.link)
    put("guid", torrent.guid)
    put("pubDate", torrent.pubDate)
    put("seeders", torrent.seeders)
    put("leechers", torrent.leechers)
    put("downloads", torrent.downloads)
    put("infoHash", torrent.infoHash)
    put("category", torrent.category)
    put("size", torrent.size)
    put("comments", torrent.comments)
    put("trusted", torrent.trusted)
    put("remake", torrent.remake)
    put("magnetLink", torrent.magnetLink)
    put("submitter", torrent.submitter)
    put("addedAt", torrent.addedAt)
    put("site", torrent.site.id)
}

fun torrentFromBackupJson(obj: JSONObject, fallbackSite: CatalogSite = CatalogSite.NYAA): Torrent = Torrent(
    id = obj.optString("id"),
    title = obj.optString("title"),
    link = obj.optString("link"),
    guid = obj.optString("guid"),
    pubDate = obj.optString("pubDate"),
    seeders = obj.optInt("seeders"),
    leechers = obj.optInt("leechers"),
    downloads = obj.optInt("downloads"),
    infoHash = obj.optString("infoHash"),
    category = obj.optString("category"),
    size = obj.optString("size"),
    comments = obj.optInt("comments"),
    trusted = obj.optBoolean("trusted"),
    remake = obj.optBoolean("remake"),
    magnetLink = obj.optString("magnetLink"),
    submitter = obj.optString("submitter"),
    addedAt = obj.optLong("addedAt", 0L),
    site = CatalogSite.fromUrl(obj.optString("guid").ifBlank { obj.optString("link") })
        ?: if (obj.has("site")) CatalogSite.fromId(obj.optString("site")) else fallbackSite
)

package com.nyaa.aniyaa.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nyaa.aniyaa.data.model.CatalogSite
import com.nyaa.aniyaa.data.model.SavedSearch
import com.nyaa.aniyaa.data.model.SearchHistoryEntry
import com.nyaa.aniyaa.data.model.Torrent

@Entity(
    tableName = "bookmarks",
    indices = [Index(value = ["identity"], unique = true)]
)
data class BookmarkEntity(
    @PrimaryKey val identity: String,
    val site: String,
    val torrentId: String,
    val title: String,
    val link: String,
    val guid: String,
    val pubDate: String,
    val seeders: Int,
    val leechers: Int,
    val downloads: Int,
    val infoHash: String,
    val category: String,
    val size: String,
    val comments: Int,
    val trusted: Boolean,
    val remake: Boolean,
    val magnetLink: String,
    val submitter: String,
    val addedAt: Long
) {
    fun toTorrent(): Torrent = Torrent(
        id = torrentId,
        title = title,
        link = link,
        guid = guid,
        pubDate = pubDate,
        seeders = seeders,
        leechers = leechers,
        downloads = downloads,
        infoHash = infoHash,
        category = category,
        size = size,
        comments = comments,
        trusted = trusted,
        remake = remake,
        magnetLink = magnetLink,
        submitter = submitter,
        addedAt = addedAt,
        site = CatalogSite.fromId(site)
    )
}

fun Torrent.toBookmarkEntity(addedAt: Long = if (this.addedAt > 0L) this.addedAt else System.currentTimeMillis()): BookmarkEntity =
    BookmarkEntity(
        identity = bookmarkKey(),
        site = site.id,
        torrentId = id,
        title = title,
        link = link,
        guid = guid,
        pubDate = pubDate,
        seeders = seeders,
        leechers = leechers,
        downloads = downloads,
        infoHash = infoHash,
        category = category,
        size = size,
        comments = comments,
        trusted = trusted,
        remake = remake,
        magnetLink = magnetLink,
        submitter = submitter,
        addedAt = addedAt
    )

@Entity(tableName = "search_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val query: String,
    val timestamp: Long,
    val site: String,
    val categoryValue: String,
    val filterValue: Int,
    val sortFieldValue: String,
    val sortOrderValue: String
) {
    fun toEntry(): SearchHistoryEntry = SearchHistoryEntry(
        query = query,
        timestamp = timestamp,
        site = CatalogSite.fromId(site),
        categoryValue = categoryValue,
        filterValue = filterValue,
        sortFieldValue = sortFieldValue,
        sortOrderValue = sortOrderValue
    )
}

fun SearchHistoryEntry.toEntity(): HistoryEntity = HistoryEntity(
    query = query,
    timestamp = timestamp,
    site = site.id,
    categoryValue = categoryValue,
    filterValue = filterValue,
    sortFieldValue = sortFieldValue,
    sortOrderValue = sortOrderValue
)

@Entity(tableName = "saved_searches")
data class SavedSearchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val query: String,
    val site: String,
    val categoryValue: String,
    val filterValue: Int,
    val sortFieldValue: String,
    val sortOrderValue: String,
    val notify: Boolean,
    val lastSeenIds: String,
    val lastCheckedAt: Long,
    val lastHeadline: String = "",
    val unseenCount: Int = 0,
    val alertTrustedOnly: Boolean = false,
    val alertResolution: String = ""
) {
    fun toModel(): SavedSearch = SavedSearch(
        id = id,
        name = name,
        query = query,
        site = CatalogSite.fromId(site),
        categoryValue = categoryValue,
        filterValue = filterValue,
        sortFieldValue = sortFieldValue,
        sortOrderValue = sortOrderValue,
        notify = notify,
        lastSeenIds = lastSeenIds,
        lastCheckedAt = lastCheckedAt,
        lastHeadline = lastHeadline,
        unseenCount = unseenCount,
        alertTrustedOnly = alertTrustedOnly,
        alertResolution = alertResolution
    )
}

@Entity(tableName = "sent_listings")
data class SentListingEntity(
    @PrimaryKey val identity: String,
    val sentAt: Long
)

@Entity(tableName = "viewed_listings")
data class ViewedListingEntity(
    @PrimaryKey val identity: String,
    val site: String,
    val torrentId: String,
    val title: String,
    val link: String,
    val guid: String,
    val pubDate: String,
    val seeders: Int,
    val leechers: Int,
    val downloads: Int,
    val infoHash: String,
    val category: String,
    val size: String,
    val comments: Int,
    val trusted: Boolean,
    val remake: Boolean,
    val magnetLink: String,
    val submitter: String,
    val viewedAt: Long
) {
    fun toTorrent(): Torrent = Torrent(
        id = torrentId,
        title = title,
        link = link,
        guid = guid,
        pubDate = pubDate,
        seeders = seeders,
        leechers = leechers,
        downloads = downloads,
        infoHash = infoHash,
        category = category,
        size = size,
        comments = comments,
        trusted = trusted,
        remake = remake,
        magnetLink = magnetLink,
        submitter = submitter,
        addedAt = viewedAt,
        site = CatalogSite.fromId(site)
    )
}

fun Torrent.toViewedListingEntity(viewedAt: Long = System.currentTimeMillis()): ViewedListingEntity =
    ViewedListingEntity(
        identity = bookmarkKey(),
        site = site.id,
        torrentId = id,
        title = title,
        link = link,
        guid = guid,
        pubDate = pubDate,
        seeders = seeders,
        leechers = leechers,
        downloads = downloads,
        infoHash = infoHash,
        category = category,
        size = size,
        comments = comments,
        trusted = trusted,
        remake = remake,
        magnetLink = magnetLink,
        submitter = submitter,
        viewedAt = viewedAt
    )

fun SavedSearch.toEntity(): SavedSearchEntity = SavedSearchEntity(
    id = id,
    name = name,
    query = query,
    site = site.id,
    categoryValue = categoryValue,
    filterValue = filterValue,
    sortFieldValue = sortFieldValue,
    sortOrderValue = sortOrderValue,
    notify = notify,
    lastSeenIds = lastSeenIds,
    lastCheckedAt = lastCheckedAt,
    lastHeadline = lastHeadline,
    unseenCount = unseenCount,
    alertTrustedOnly = alertTrustedOnly,
    alertResolution = alertResolution
)

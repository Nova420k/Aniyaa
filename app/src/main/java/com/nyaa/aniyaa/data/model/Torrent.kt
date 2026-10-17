package com.nyaa.aniyaa.data.model

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

@Immutable
@Parcelize
data class Torrent(
    val id: String,
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
    val submitter: String = "",
    val addedAt: Long = 0L,
    val site: CatalogSite = CatalogSite.NYAA
) : Parcelable {
    fun identity(): String = when {
        id.isNotEmpty() -> id
        infoHash.isNotEmpty() -> infoHash
        guid.isNotEmpty() -> guid
        else -> "$title|$pubDate|$link"
    }

    fun bookmarkKey(): String = "${site.id}:${identity()}"

    fun matchesNavId(navId: String): Boolean =
        navId.isNotEmpty() && (id == navId || infoHash == navId || navId() == navId)

    fun navId(): String = id.ifBlank { infoHash }.ifBlank { "unknown" }

    fun listKey(index: Int): String {
        val local = when {
            id.isNotEmpty() -> "id:$id"
            infoHash.isNotEmpty() -> "ih:$infoHash"
            guid.isNotEmpty() -> "g:$guid"
            else -> "i:$index:${title.hashCode()}:$pubDate"
        }
        return "${site.id}:$local"
    }
}

enum class SortField(val value: String, val displayName: String) {
    DATE("id", "Date"),
    SEEDERS("seeders", "Seeders"),
    LEECHERS("leechers", "Leechers"),
    SIZE("size", "Size"),
    DOWNLOADS("downloads", "Downloads"),
    COMMENTS("comments", "Comments")
}

enum class SortOrder(val value: String, val displayName: String) {
    DESC("desc", "Descending"),
    ASC("asc", "Ascending")
}

enum class FilterOption(val value: Int, val displayName: String) {
    ALL(0, "No Filter"),
    NO_REMAKES(1, "No Remakes"),
    TRUSTED(2, "Trusted Only")
}

enum class DarkMode(val value: String, val displayName: String) {
    SYSTEM("system", "System"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark")
}

enum class BookmarkSort(val displayName: String) {
    DATE_ADDED("Date added"),
    TITLE("Title"),
    SIZE("Size"),
    SEEDERS("Seeders")
}

data class Category(val value: String, val displayName: String) {
    val isPrimary: Boolean
        get() = value == "0_0" || value.endsWith("_0")

    val groupPrefix: String
        get() = value.substringBefore("_", value)

    fun groups(selected: Category): Boolean {
        if (value == "0_0") return selected.value == "0_0"
        if (!isPrimary) return selected.value == value
        return selected.groupPrefix == groupPrefix
    }

    val shortLabel: String
        get() = displayName.substringBefore(" - ").removePrefix("All ").ifBlank { displayName }
}

fun categoryByValue(value: String, site: CatalogSite = CatalogSite.NYAA): Category =
    site.categories.find { it.value == value } ?: site.categories.first()

fun sortFieldByValue(value: String): SortField =
    SortField.entries.find { it.value == value } ?: SortField.DATE

fun sortOrderByValue(value: String): SortOrder =
    SortOrder.entries.find { it.value == value } ?: SortOrder.DESC

fun filterByValue(value: Int): FilterOption =
    FilterOption.entries.find { it.value == value } ?: FilterOption.ALL

fun darkModeByValue(value: String): DarkMode =
    DarkMode.entries.find { it.value == value } ?: DarkMode.SYSTEM

data class SearchParams(
    val query: String = "",
    val site: CatalogSite = CatalogSite.NYAA,
    val category: Category = NYAA_CATEGORIES[0],
    val filter: FilterOption = FilterOption.ALL,
    val sortField: SortField = SortField.DATE,
    val sortOrder: SortOrder = SortOrder.DESC,
    val page: Int = 1
) {
    fun withValidCategory(): SearchParams {
        val valid = categoryByValue(category.value, site)
        return if (valid == category) this else copy(category = valid)
    }

    fun hasActiveFilters(): Boolean =
        category.value != "0_0" ||
            filter != FilterOption.ALL ||
            sortField != SortField.DATE ||
            sortOrder != SortOrder.DESC

    fun activeFilterCaption(): String = buildList {
        if (!category.isPrimary) add(category.displayName.substringAfter(" - ").ifBlank { category.displayName })
        if (filter != FilterOption.ALL) add(filter.displayName)
        if (sortField != SortField.DATE) add(sortField.displayName)
        if (sortOrder != SortOrder.DESC) add(sortOrder.displayName)
    }.joinToString(" · ")
}

@Immutable
data class TorrentComment(
    val id: String,
    val username: String,
    val avatarUrl: String,
    val date: String,
    val content: String
)

@Immutable
data class TorrentFileEntry(
    val name: String,
    val size: String
)

data class TorrentPageData(
    val description: String,
    val fileList: List<TorrentFileEntry>,
    val comments: List<TorrentComment>,
    val submitter: String = "",
    val title: String = "",
    val category: String = "",
    val size: String = "",
    val infoHash: String = "",
    val seeders: Int = 0,
    val leechers: Int = 0,
    val downloads: Int = 0,
    val commentsCount: Int = 0,
    val trusted: Boolean = false,
    val remake: Boolean = false,
    val magnetLink: String = "",
    val downloadUrl: String = "",
    val pubDate: String = ""
)

data class SearchHistoryEntry(
    val query: String,
    val timestamp: Long,
    val site: CatalogSite = CatalogSite.NYAA,
    val categoryValue: String = NYAA_CATEGORIES.first().value,
    val filterValue: Int = FilterOption.ALL.value,
    val sortFieldValue: String = SortField.DATE.value,
    val sortOrderValue: String = SortOrder.DESC.value
) {
    fun toSearchParams(): SearchParams = SearchParams(
        query = query,
        site = site,
        category = categoryByValue(categoryValue, site),
        filter = filterByValue(filterValue),
        sortField = sortFieldByValue(sortFieldValue),
        sortOrder = sortOrderByValue(sortOrderValue),
        page = 1
    )

    fun filterSummary(): String {
        val parts = buildList {
            add(site.displayName)
            val category = categoryByValue(categoryValue, site)
            if (category.value != site.categories.first().value) add(category.displayName)
            val filter = filterByValue(filterValue)
            if (filter != FilterOption.ALL) add(filter.displayName)
            val sort = sortFieldByValue(sortFieldValue)
            if (sort != SortField.DATE) add(sort.displayName)
        }
        return parts.joinToString(" · ")
    }
}

data class SavedSearch(
    val id: Long = 0L,
    val name: String,
    val query: String,
    val site: CatalogSite = CatalogSite.NYAA,
    val categoryValue: String = NYAA_CATEGORIES.first().value,
    val filterValue: Int = FilterOption.ALL.value,
    val sortFieldValue: String = SortField.DATE.value,
    val sortOrderValue: String = SortOrder.DESC.value,
    val notify: Boolean = false,
    val lastSeenIds: String = "",
    val lastCheckedAt: Long = 0L,
    val lastHeadline: String = "",
    val unseenCount: Int = 0,
    val alertTrustedOnly: Boolean = false,
    val alertResolution: String = ""
) {
    fun toSearchParams(): SearchParams = SearchParams(
        query = query,
        site = site,
        category = categoryByValue(categoryValue, site),
        filter = filterByValue(filterValue),
        sortField = sortFieldByValue(sortFieldValue),
        sortOrder = sortOrderByValue(sortOrderValue),
        page = 1
    )

    fun displayName(): String = name.ifBlank { query.ifBlank { "Latest listings" } }
}

fun SearchParams.toHistoryEntry(timestamp: Long = System.currentTimeMillis()): SearchHistoryEntry =
    SearchHistoryEntry(
        query = query.trim(),
        timestamp = timestamp,
        site = site,
        categoryValue = category.value,
        filterValue = filter.value,
        sortFieldValue = sortField.value,
        sortOrderValue = sortOrder.value
    )

fun SearchParams.toSavedSearch(name: String, notify: Boolean = false): SavedSearch =
    SavedSearch(
        name = name.ifBlank { query.ifBlank { "Latest listings" } },
        query = query.trim(),
        site = site,
        categoryValue = category.value,
        filterValue = filter.value,
        sortFieldValue = sortField.value,
        sortOrderValue = sortOrder.value,
        notify = notify
    )

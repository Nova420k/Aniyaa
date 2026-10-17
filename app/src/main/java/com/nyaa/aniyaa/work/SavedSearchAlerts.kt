package com.nyaa.aniyaa.work

import com.nyaa.aniyaa.data.model.SavedSearch
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.util.qualityTags

fun matchesAlert(torrent: Torrent, search: SavedSearch): Boolean {
    if (search.alertTrustedOnly && !torrent.trusted) return false
    if (search.alertResolution.isNotBlank()) {
        val resolution = qualityTags(torrent.title).firstOrNull { it.endsWith("p") }
        if (resolution != search.alertResolution) return false
    }
    return true
}

object SavedSearchAlerts {
    const val STORED_ID_LIMIT = 40

    fun newIds(currentIds: List<String>, previousCsv: String): List<String> {
        val previous = previousCsv.split(',').map { it.trim() }.filter { it.isNotBlank() }.toSet()
        if (previous.isEmpty()) return emptyList()
        return currentIds.map { it.trim() }.filter { it.isNotBlank() && it !in previous }
    }

    fun storeIds(ids: List<String>, limit: Int = STORED_ID_LIMIT): String =
        ids.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(limit).joinToString(",")

    fun contentText(count: Int): String =
        "$count new listing${if (count == 1) "" else "s"}"

    fun inboxLines(titles: List<String>, count: Int): List<String> {
        val lines = titles.map { it.trim() }.filter { it.isNotBlank() }.take(5)
        if (lines.isEmpty()) return listOf(contentText(count))
        return if (count > lines.size) {
            lines + "and ${count - lines.size} more"
        } else {
            lines
        }
    }
}

package com.nyaa.aniyaa.util

import com.nyaa.aniyaa.data.model.Torrent

data class ReleaseGroup(
    val variants: List<Torrent>
) {
    val primary: Torrent get() = variants.first()

    fun label(): String {
        val parsed = parseReleaseTitle(primary.title)
        val show = parsed.show
        val episode = parsed.episode
        return when {
            show != null && episode != null -> "$show — $episode"
            show != null -> show
            else -> primary.title
        }
    }
}

fun groupReleases(torrents: List<Torrent>, enabled: Boolean = true): List<ReleaseGroup> {
    if (!enabled) return torrents.map { ReleaseGroup(listOf(it)) }
    val rows = ArrayList<ReleaseGroup>(torrents.size)
    val indexByKey = HashMap<String, Int>()
    for (torrent in torrents) {
        val parsed = parseReleaseTitle(torrent.title)
        val show = parsed.show
        val episode = parsed.episode
        val key = if (!show.isNullOrBlank() && !episode.isNullOrBlank()) {
            "${torrent.site.id}|${show.lowercase()}|${episode.lowercase()}"
        } else {
            null
        }
        if (key != null) {
            val existing = indexByKey[key]
            if (existing != null) {
                val row = rows[existing]
                rows[existing] = row.copy(variants = row.variants + torrent)
                continue
            }
            indexByKey[key] = rows.size
        }
        rows += ReleaseGroup(listOf(torrent))
    }
    return rows
}

fun variantLabel(torrent: Torrent): String {
    val tags = qualityTags(torrent.title).filter { tag ->
        tag.endsWith("p") || tag == "HEVC" || tag == "AV1" || tag == "AVC" ||
            tag == "Softsub" || tag == "Multi-Subs" || tag == "Dub"
    }
    return tags.joinToString(" ").ifBlank { torrent.size.ifBlank { "Release" } }
}

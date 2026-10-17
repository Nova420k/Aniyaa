package com.nyaa.aniyaa.util

import com.nyaa.aniyaa.data.model.CatalogSite
import com.nyaa.aniyaa.data.model.Torrent
import com.nyaa.aniyaa.data.network.normalizeRemoteBase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImprovementsTest {

    @Test
    fun parse_keepsEpisodeNumber() {
        val parsed = parseReleaseTitle("[SubsPlease] Bocchi the Rock! - 12 (1080p)")
        assertEquals("Bocchi the Rock!", parsed.show)
        assertEquals("12", parsed.episode)
        assertEquals("SubsPlease", parsed.group)
    }

    @Test
    fun qualityTags_readsSubtitles() {
        val tags = qualityTags("[Group] Show - 03 (1080p) [Softsub] [Dub]")
        assertTrue(tags.contains("Softsub"))
        assertTrue(tags.contains("Dub"))
        assertFalse(qualityTags("Show Dual Audio").contains("Dub"))
    }

    @Test
    fun groupReleases_collapsesSameEpisode() {
        val rows = groupReleases(
            listOf(
                torrent("[Group] Show - 12 (1080p) [HEVC]"),
                torrent("[Other] Show - 12 (720p)"),
                torrent("[Group] Other Show - 01 (1080p)")
            )
        )
        assertEquals(2, rows.size)
        assertEquals(2, rows[0].variants.size)
        assertEquals("Show — 12", rows[0].label())
    }

    @Test
    fun quietHours_wrapsPastMidnight() {
        assertTrue(inQuietHours(23, 23, 7, enabled = true))
        assertTrue(inQuietHours(2, 23, 7, enabled = true))
        assertFalse(inQuietHours(12, 23, 7, enabled = true))
        assertFalse(inQuietHours(2, 23, 7, enabled = false))
    }

    @Test
    fun remoteBase_addsHttpAndRejectsBlank() {
        assertEquals("http://192.168.1.5:8080", normalizeRemoteBase("192.168.1.5:8080"))
        assertEquals("https://seed.example", normalizeRemoteBase("https://seed.example/"))
        assertNull(normalizeRemoteBase("  "))
    }

    private fun torrent(title: String) = Torrent(
        id = title,
        title = title,
        link = "",
        guid = "",
        pubDate = "",
        seeders = 1,
        leechers = 0,
        downloads = 0,
        infoHash = "",
        category = "",
        size = "1 GiB",
        comments = 0,
        trusted = true,
        remake = false,
        magnetLink = "magnet:?xt=urn:btih:$title",
        site = CatalogSite.NYAA
    )
}

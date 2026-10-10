package com.nyaa.aniyaa.util

import com.nyaa.aniyaa.data.model.Torrent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultFilterTest {

    @Test
    fun resolutionAndCodec_keepOnlyTheRequestedTags() {
        val hevc = torrent("[SubsPlease] Show - 12 (1080p) [HEVC]")
        val avc = torrent("[SubsPlease] Show - 12 (720p) [x264]")
        val bare = torrent("[SubsPlease] Show - 12")
        val filter = ResultFilter(
            resolution = ResolutionChoice.P1080,
            codec = CodecChoice.HEVC
        )
        assertTrue(hevc.passes(filter))
        assertFalse(avc.passes(filter))
        assertFalse(bare.passes(filter))
    }

    @Test
    fun minSeeders_dropsQuietListings() {
        val filter = ResultFilter(minSeeders = 5)
        assertTrue(torrent("Show", seeders = 5).passes(filter))
        assertFalse(torrent("Show", seeders = 4).passes(filter))
    }

    @Test
    fun hideBatches_dropsPacksAndKeepsEpisodes() {
        assertTrue(isBatchTitle("[Erai-raws] Show - 01~12 [1080p]"))
        assertTrue(isBatchTitle("[Group] Show Batch [1080p]"))
        assertTrue(isBatchTitle("Show Complete (1080p)"))
        assertTrue(isBatchTitle("Manga Vol.01-05"))
        assertFalse(isBatchTitle("[SubsPlease] Show - 12 (1080p)"))
        assertFalse(isBatchTitle("Movie (2019-2024)"))
        assertFalse(isBatchTitle("Encode 720-1080 comparison"))
        assertFalse(isBatchTitle("Incomplete raw"))
        val filter = ResultFilter(hideBatches = true)
        assertFalse(torrent("[Group] Show - 01-12 [1080p]").passes(filter))
        assertTrue(torrent("[SubsPlease] Show - 12 (1080p)").passes(filter))
    }

    @Test
    fun hideRules_matchTitleUploaderAndGroup() {
        val listing = torrent(
            title = "[SubsPlease] Show - 12 (1080p)",
            submitter = "alice"
        )
        assertFalse(torrent("[Mini] Show - 01").passes(ResultFilter(hideRules = listOf("mini"))))
        assertFalse(
            torrent("[Other] Show - 01", submitter = "alice")
                .passes(ResultFilter(hideRules = listOf("Alice")))
        )
        assertFalse(listing.passes(ResultFilter(hideRules = listOf("subsplease"))))
        assertTrue(listing.passes(ResultFilter(hideRules = listOf("nobody"))))
    }

    private fun torrent(
        title: String,
        seeders: Int = 10,
        submitter: String = ""
    ) = Torrent(
        id = title,
        title = title,
        link = "",
        guid = "",
        pubDate = "",
        seeders = seeders,
        leechers = 0,
        downloads = 0,
        infoHash = "",
        category = "",
        size = "",
        comments = 0,
        trusted = false,
        remake = false,
        magnetLink = "",
        submitter = submitter
    )
}

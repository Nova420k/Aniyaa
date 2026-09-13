package com.nyaa.aniyaa.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DescriptionFormatterTest {

    @Test
    fun convertBbcode_turnsCommonTagsIntoMarkdown() {
        val raw = "[b]Bold[/b] [i]Hi[/i] [url=https://nyaa.si]Nyaa[/url] [img]https://cdn.example.com/a.png[/img]"
        val out = DescriptionFormatter.convertBbcode(raw)
        assertTrue(out.contains("**Bold**"))
        assertTrue(out.contains("*Hi*"))
        assertTrue(out.contains("[Nyaa](https://nyaa.si)"))
        assertTrue(out.contains("![](https://cdn.example.com/a.png)"))
    }

    @Test
    fun allImages_collectsUniqueUrls() {
        val images = DescriptionFormatter.allImages(
            "![one](https://cdn.example.com/1.png)\ntext\nhttps://files.catbox.moe/ab.png"
        )
        assertEquals(2, images.size)
    }

    @Test
    fun imagesIn_findsInlineMarkdownAndBareUrls() {
        val line = "Source ![one](https://cdn.example.com/1.png) and https://files.catbox.moe/ab.png extra"
        val images = DescriptionFormatter.imagesIn(line)
        assertEquals(2, images.size)
        assertEquals("https://cdn.example.com/1.png", images[0].url)
        assertEquals("https://files.catbox.moe/ab.png", images[1].url)
    }

    @Test
    fun blocks_keepsSingleImageAsGallery() {
        val blocks = DescriptionFormatter.blocks("Cover\n![art](https://i.imgur.com/abc.jpg)\nDone")
        assertTrue(blocks.any { it is DescriptionBlock.Gallery && it.images.single().url.contains("imgur") })
    }

    @Test
    fun blocks_groupsConsecutiveImages() {
        val raw = """
            Intro
            ![one](https://cdn.example.com/1.png)
            ![two](https://cdn.example.com/2.jpg)
            https://cdn.example.com/3.webp
            Outro
        """.trimIndent()
        val blocks = DescriptionFormatter.blocks(raw)
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is DescriptionBlock.Markdown)
        val gallery = blocks[1] as DescriptionBlock.Gallery
        assertEquals(3, gallery.images.size)
        assertTrue(blocks[2] is DescriptionBlock.Markdown)
    }

    @Test
    fun blocks_parsesMarkdownTable() {
        val raw = """
            | Codec | Size |
            | --- | --- |
            | HEVC | 1.2 GiB |
            | AVC | 2.0 GiB |
        """.trimIndent()
        val blocks = DescriptionFormatter.blocks(raw)
        val table = blocks.single() as DescriptionBlock.Table
        assertEquals(listOf("Codec", "Size"), table.headers)
        assertEquals(2, table.rows.size)
        assertEquals("HEVC", table.rows[0][0])
    }

    @Test
    fun blocks_extractsFencedCode() {
        val raw = "Before\n```\nmediainfo\n```\nAfter"
        val blocks = DescriptionFormatter.blocks(raw)
        assertEquals(3, blocks.size)
        assertEquals("mediainfo", (blocks[1] as DescriptionBlock.Code).body)
    }

    @Test
    fun compactMarkdown_dropsEmptyAndUnsafeLinks() {
        val raw = "See [Nyaa](https://nyaa.si) and [gone]() plus [bad](javascript:alert(1))"
        val out = DescriptionFormatter.compactMarkdown(raw)
        assertTrue(out.contains("[Nyaa](https://nyaa.si)"))
        assertTrue(out.contains("gone"))
        assertTrue(out.contains("bad"))
        assertTrue(!out.contains("]()"))
        assertTrue(!out.contains("javascript:"))
    }

    @Test
    fun prepare_stripsEmptyImagesAndExtraBlankLines() {
        val raw = "Title\n\n\n![]()\n\nBody"
        val out = DescriptionFormatter.prepare(raw)
        assertEquals("Title\n\nBody", out)
    }

    @Test
    fun prepare_stripsEmptyImageArtifacts() {
        val out = DescriptionFormatter.prepare("!( )\n![]()\nKeep")
        assertEquals("Keep", out)
        assertTrue(!out.contains("!("))
    }

    @Test
    fun prepare_keepsMarkdownImagesForGfm() {
        val raw = "![art](https://i.imgur.com/abc.jpg)\n\nHello"
        val out = DescriptionFormatter.prepare(raw)
        assertTrue(out.contains("![art](https://i.imgur.com/abc.jpg)"))
        assertTrue(out.contains("Hello"))
    }

    @Test
    fun isImageArtifact_detectsLeftoverMarkdown() {
        assertTrue(DescriptionFormatter.isImageArtifact("!( )"))
        assertTrue(DescriptionFormatter.isImageArtifact("![]()"))
        assertTrue(!DescriptionFormatter.isImageArtifact("Hello"))
    }
}

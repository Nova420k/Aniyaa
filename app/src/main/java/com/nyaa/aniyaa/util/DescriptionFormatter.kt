package com.nyaa.aniyaa.util

sealed class DescriptionBlock {
    data class Markdown(val text: String) : DescriptionBlock()
    data class Gallery(val images: List<DescriptionImage>) : DescriptionBlock()
    data class Table(val headers: List<String>, val rows: List<List<String>>) : DescriptionBlock()
    data class Code(val body: String) : DescriptionBlock()
}

data class DescriptionImage(
    val url: String,
    val alt: String = ""
)

object DescriptionFormatter {

    fun prepare(raw: String): String {
        return stripEmptyImageMarkup(normalizeWhitespace(convertBbcode(raw.trim())))
    }

    fun allImages(raw: String): List<DescriptionImage> {
        val found = LinkedHashMap<String, DescriptionImage>()
        prepare(raw).lineSequence().forEach { line ->
            imagesIn(line).forEach { image -> found.putIfAbsent(image.url, image) }
        }
        return found.values.toList()
    }

    fun blocks(raw: String): List<DescriptionBlock> {
        val prepared = prepare(raw)
        if (prepared.isBlank()) return emptyList()
        return splitBlocks(prepared)
    }

    internal fun convertBbcode(input: String): String {
        var text = input.replace("\r\n", "\n").replace('\r', '\n')
        text = BB_IMG.replace(text) { match ->
            val url = match.groupValues.drop(1).firstOrNull { it.isNotBlank() }.orEmpty().trim()
            if (url.isEmpty()) match.value else "![]($url)"
        }
        text = BB_URL_LABELED.replace(text) { "[${it.groupValues[2]}](${it.groupValues[1].trim()})" }
        text = BB_URL.replace(text) { "[${it.groupValues[1]}](${it.groupValues[1].trim()})" }
        text = BB_BOLD.replace(text, "**$1**")
        text = BB_ITALIC.replace(text, "*$1*")
        text = BB_STRIKE.replace(text, "~~$1~~")
        text = BB_UNDER.replace(text, "$1")
        text = BB_CODE_BLOCK.replace(text) { "\n```\n${it.groupValues[1].trim()}\n```\n" }
        text = BB_QUOTE.replace(text) { quote ->
            quote.groupValues[1].trim().lines().joinToString("\n") { "> $it" } + "\n"
        }
        text = BB_LIST_ITEM.replace(text, "\n- ")
        text = BB_LIST.replace(text, "$1")
        text = BB_SPOILER.replace(text) { "\n**Spoiler**\n\n${it.groupValues[1].trim()}\n" }
        text = BB_ALIGN.replace(text, "$1")
        text = BB_STYLE.replace(text, "$1")
        text = BB_HR.replace(text, "\n\n---\n\n")
        text = BB_LEFTOVER.replace(text, "")
        return text
    }

    internal fun normalizeWhitespace(input: String): String {
        return input
            .replace(Regex("[ \\t]+\\n"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    internal fun compactMarkdown(input: String): String = stripEmptyImageMarkup(input)

    internal fun stripEmptyImageMarkup(input: String): String {
        var text = EMPTY_IMAGE.replace(input, "")
        text = MARKDOWN_LINK_HREF.replace(text) { match ->
            val label = match.groupValues[1]
            val url = match.groupValues[2].trim()
            if (isSafeHttpUrl(url) && label.isNotBlank()) match.value else label
        }
        return normalizeWhitespace(text)
    }

    private fun splitBlocks(input: String): List<DescriptionBlock> {
        val lines = input.split('\n')
        val blocks = ArrayList<DescriptionBlock>()
        val buffer = StringBuilder()

        fun flushMarkdown() {
            val text = compactMarkdown(buffer.toString()).trim()
            buffer.setLength(0)
            if (text.isNotEmpty() && !isImageArtifact(text)) {
                blocks += DescriptionBlock.Markdown(text)
            }
        }

        var index = 0
        while (index < lines.size) {
            val line = lines[index]
            when {
                line.trim().startsWith("```") -> {
                    flushMarkdown()
                    val body = StringBuilder()
                    index++
                    while (index < lines.size && !lines[index].trim().startsWith("```")) {
                        if (body.isNotEmpty()) body.append('\n')
                        body.append(lines[index])
                        index++
                    }
                    blocks += DescriptionBlock.Code(body.toString())
                    if (index < lines.size) index++
                }
                isTableSeparatorContext(lines, index) -> {
                    flushMarkdown()
                    val tableLines = ArrayList<String>()
                    while (index < lines.size && lines[index].contains('|')) {
                        tableLines += lines[index]
                        index++
                    }
                    parseTable(tableLines)?.let { blocks += it }
                }
                imagesIn(line).isNotEmpty() -> {
                    flushMarkdown()
                    val leftover = stripImages(line)
                    if (leftover.isNotBlank() && !isImageArtifact(leftover)) {
                        blocks += DescriptionBlock.Markdown(leftover)
                    }
                    val images = ArrayList<DescriptionImage>()
                    while (index < lines.size) {
                        val found = imagesIn(lines[index])
                        if (found.isEmpty()) {
                            if (lines[index].isBlank() &&
                                index + 1 < lines.size &&
                                imagesIn(lines[index + 1]).isNotEmpty()
                            ) {
                                index++
                                continue
                            }
                            break
                        }
                        if (index > 0 && images.isNotEmpty()) {
                            val extraText = stripImages(lines[index])
                            if (extraText.isNotBlank()) break
                        }
                        images += found
                        index++
                    }
                    if (images.isNotEmpty()) {
                        blocks += DescriptionBlock.Gallery(images.distinctBy { it.url })
                    }
                }
                else -> {
                    if (buffer.isNotEmpty()) buffer.append('\n')
                    buffer.append(line)
                    index++
                }
            }
        }
        flushMarkdown()
        return blocks.ifEmpty { listOf(DescriptionBlock.Markdown(input)) }
    }

    private fun isTableSeparatorContext(lines: List<String>, index: Int): Boolean {
        if (index + 1 >= lines.size) return false
        if (!lines[index].contains('|')) return false
        return isTableSeparator(lines[index + 1])
    }

    private fun isTableSeparator(line: String): Boolean {
        val cells = splitTableRow(line)
        return cells.isNotEmpty() && cells.all { cell ->
            val trimmed = cell.trim()
            trimmed.isNotEmpty() && trimmed.all { it == '-' || it == ':' } && trimmed.contains('-')
        }
    }

    private fun parseTable(tableLines: List<String>): DescriptionBlock.Table? {
        val rows = tableLines
            .filter { !isTableSeparator(it) }
            .map { splitTableRow(it) }
            .filter { it.isNotEmpty() }
        if (rows.isEmpty()) return null
        val headers = rows.first()
        val body = rows.drop(1).map { row ->
            if (row.size >= headers.size) {
                row.take(headers.size)
            } else {
                row + List(headers.size - row.size) { "" }
            }
        }
        return DescriptionBlock.Table(headers, body)
    }

    private fun splitTableRow(line: String): List<String> {
        val trimmed = line.trim().removePrefix("|").removeSuffix("|")
        return trimmed.split('|').map { it.trim() }
    }

    internal fun imagesIn(line: String): List<DescriptionImage> {
        val found = LinkedHashMap<String, DescriptionImage>()
        MARKDOWN_IMAGE_ANY.findAll(line).forEach { match ->
            val url = match.groupValues[2].trim()
            if (isSafeHttpUrl(url)) {
                found[url] = DescriptionImage(url, match.groupValues[1].trim())
            }
        }
        MARKDOWN_LINK_ANY.findAll(line).forEach { match ->
            val url = match.groupValues[2].trim()
            if (url !in found && isImageUrl(url) && isSafeHttpUrl(url)) {
                found[url] = DescriptionImage(url, match.groupValues[1].trim())
            }
        }
        BARE_IMAGE_ANY.findAll(line).forEach { match ->
            val url = match.value.trim().trimEnd(')', ',', '.', ';')
            if (url !in found && isSafeHttpUrl(url) && isImageUrl(url)) {
                found[url] = DescriptionImage(url)
            }
        }
        return found.values.toList()
    }

    internal fun stripImages(line: String): String {
        var text = WRAPPED_IMAGE.replace(line, " ")
        text = MARKDOWN_IMAGE_ANY.replace(text, " ")
        text = MARKDOWN_LINK_ANY.replace(text) { match ->
            val url = match.groupValues[2].trim()
            if (isImageUrl(url)) " " else match.value
        }
        text = BARE_IMAGE_ANY.replace(text) { match ->
            if (isImageUrl(match.value.trim().trimEnd(')', ',', '.', ';'))) " " else match.value
        }
        text = EMPTY_IMAGE.replace(text, " ")
        return text.replace(Regex("\\s+"), " ").trim()
    }

    internal fun isImageArtifact(text: String): Boolean {
        val stripped = EMPTY_IMAGE.replace(text, "")
            .replace(Regex("""[!\[\]()]+"""), "")
            .trim()
        return stripped.isEmpty()
    }

    internal fun isImageUrl(url: String): Boolean {
        val lower = url.lowercase()
        if (IMAGE_EXT.containsMatchIn(lower)) return true
        return IMAGE_HOSTS.any { lower.contains(it) }
    }

    internal fun imageFromLine(line: String): DescriptionImage? = imagesIn(line).singleOrNull()

    private val BB_IMG = Regex(
        """\[img(?:\s*=\s*"?([^\]"\s]+)"?)?]\s*(.*?)\s*\[/img]|\[img=([^\]]+)]""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val BB_URL_LABELED = Regex(
        """\[url=([^\]]+)](.*?)\[/url]""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val BB_URL = Regex(
        """\[url](.*?)\[/url]""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val BB_BOLD = Regex("""\[b](.*?)\[/b]""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val BB_ITALIC = Regex("""\[i](.*?)\[/i]""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val BB_STRIKE = Regex("""\[s](.*?)\[/s]""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val BB_UNDER = Regex("""\[u](.*?)\[/u]""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val BB_CODE_BLOCK = Regex(
        """\[code](.*?)\[/code]""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val BB_QUOTE = Regex(
        """\[quote(?:=[^\]]+)?](.*?)\[/quote]""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val BB_LIST = Regex(
        """\[list](.*?)\[/list]""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val BB_LIST_ITEM = Regex("""\[\*]|\[li]""", RegexOption.IGNORE_CASE)
    private val BB_SPOILER = Regex(
        """\[spoiler(?:=[^\]]+)?](.*?)\[/spoiler]""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val BB_ALIGN = Regex(
        """\[(?:center|left|right)](.*?)\[/(?:center|left|right)]""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val BB_STYLE = Regex(
        """\[(?:color|size|font)=[^\]]+](.*?)\[/(?:color|size|font)]""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val BB_HR = Regex("""\[hr/?]""", RegexOption.IGNORE_CASE)
    private val BB_LEFTOVER = Regex(
        """\[/?(?:b|i|u|s|img|url|quote|code|list|center|left|right|color|size|font|spoiler|hr|li)(?:=[^\]]*)?]""",
        RegexOption.IGNORE_CASE
    )
    private val WRAPPED_IMAGE = Regex(
        """\[!\[(.*?)]\(\s*(https?://[^)\s]+)(?:\s+"[^"]*")?\s*\)]\(([^)]*)\)"""
    )
    private val EMPTY_IMAGE = Regex("""!\[[^\]]*]\(\s*\)|!\(\s*\)""")
    private val MARKDOWN_LINK_HREF = Regex("""(?<!!)\[((?:\\.|[^\]\\])*)]\(([^)]*)\)""")
    private val MARKDOWN_IMAGE_ANY = Regex(
        """!\[(.*?)]\(\s*(https?://[^)\s]+)(?:\s+"[^"]*")?\s*\)"""
    )
    private val MARKDOWN_LINK_ANY = Regex("""(?<!!)\[((?:\\.|[^\]\\])*)]\((https?://[^)\s]+)\)""")
    private val BARE_IMAGE_ANY = Regex(
        """https?://[^\s)<>"']+""",
        RegexOption.IGNORE_CASE
    )
    private val IMAGE_EXT = Regex("""\.(?:png|jpe?g|gif|webp|avif|bmp)(?:\?|$|#)""", RegexOption.IGNORE_CASE)
    private val IMAGE_HOSTS = listOf(
        "i.imgur.com",
        "imgur.com/",
        "catbox.moe",
        "files.catbox.moe",
        "imgchest.com",
        "cdn.imgchest.com",
        "i.ibb.co",
        "ibb.co/",
        "imgbb.com",
        "postimg.cc",
        "i.postimg.cc",
        "imagebam.com",
        "imgbox.com",
        "freeimage.host",
        "iili.io",
        "kei.gg",
        "slow.pics",
        "slowpics.org",
        "imgpile.com",
        "lensdump.com",
        "p.sda1.dev"
    )
}

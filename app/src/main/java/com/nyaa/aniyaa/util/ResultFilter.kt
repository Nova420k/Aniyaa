package com.nyaa.aniyaa.util

import com.nyaa.aniyaa.data.model.Torrent

enum class ResolutionChoice(val tag: String, val label: String) {
    ANY("", "Any"),
    P2160("2160p", "2160p"),
    P1080("1080p", "1080p"),
    P720("720p", "720p"),
    P480("480p", "480p");

    companion object {
        fun fromTag(value: String): ResolutionChoice =
            entries.find { it.tag.equals(value, ignoreCase = true) } ?: ANY
    }
}

enum class CodecChoice(val tag: String, val label: String) {
    ANY("", "Any"),
    HEVC("HEVC", "HEVC"),
    AV1("AV1", "AV1"),
    AVC("AVC", "AVC");

    companion object {
        fun fromTag(value: String): CodecChoice =
            entries.find { it.tag.equals(value, ignoreCase = true) } ?: ANY
    }
}

data class LocalFilter(
    val resolution: ResolutionChoice = ResolutionChoice.ANY,
    val codec: CodecChoice = CodecChoice.ANY,
    val minSeeders: Int = 0,
    val hideBatches: Boolean = false
) {
    fun toResultFilter(hideRules: List<String>): ResultFilter = ResultFilter(
        resolution = resolution,
        codec = codec,
        minSeeders = minSeeders,
        hideBatches = hideBatches,
        hideRules = hideRules
    )

    companion object {
        val SEED_CHOICES = listOf(0, 1, 5, 10, 50)

        fun coerceSeeders(value: Int): Int =
            SEED_CHOICES.minByOrNull { kotlin.math.abs(it - value) } ?: 0
    }
}

data class ResultFilter(
    val resolution: ResolutionChoice = ResolutionChoice.ANY,
    val codec: CodecChoice = CodecChoice.ANY,
    val minSeeders: Int = 0,
    val hideBatches: Boolean = false,
    val hideRules: List<String> = emptyList()
)

fun Torrent.passes(filter: ResultFilter): Boolean {
    if (filter.minSeeders > 0 && seeders < filter.minSeeders) return false
    if (filter.hideBatches && isBatchTitle(title)) return false
    if (filter.resolution != ResolutionChoice.ANY) {
        val tag = qualityTags(title).firstOrNull { it.endsWith("p") }
        if (tag != filter.resolution.tag) return false
    }
    if (filter.codec != CodecChoice.ANY) {
        if (filter.codec.tag !in qualityTags(title)) return false
    }
    if (filter.hideRules.any { matchesHideRule(this, it) }) return false
    return true
}

fun isBatchTitle(title: String): Boolean {
    if (BATCH_WORD.containsMatchIn(title) || COMPLETE_WORD.containsMatchIn(title)) return true
    return RANGE.findAll(title).any { match ->
        val start = match.groupValues[1].toIntOrNull() ?: return@any false
        val end = match.groupValues[2].toIntOrNull() ?: return@any false
        if (start == end) return@any false
        val low = minOf(start, end)
        val high = maxOf(start, end)
        if (match.groupValues[1].length == 4 && match.groupValues[2].length == 4 &&
            low >= 1900 && high <= 2100
        ) {
            return@any false
        }
        if (start in RESOLUTIONS && end in RESOLUTIONS) return@any false
        high - low in 1..2000
    }
}

fun matchesHideRule(torrent: Torrent, rule: String): Boolean {
    val needle = rule.trim()
    if (needle.isEmpty()) return false
    if (torrent.title.contains(needle, ignoreCase = true)) return true
    if (torrent.submitter.contains(needle, ignoreCase = true)) return true
    val group = parseReleaseTitle(torrent.title).group
    return group != null && group.contains(needle, ignoreCase = true)
}

private val RESOLUTIONS = setOf(480, 720, 1080, 2160)
private val BATCH_WORD = Regex("""\bbatch(?:es)?\b""", RegexOption.IGNORE_CASE)
private val COMPLETE_WORD = Regex("""\bcomplete\b""", RegexOption.IGNORE_CASE)
private val RANGE = Regex("""(\d{1,4})\s*[-–~～]\s*(\d{1,4})""")

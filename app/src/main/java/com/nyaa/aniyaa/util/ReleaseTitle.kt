package com.nyaa.aniyaa.util

data class ParsedReleaseTitle(
    val group: String?,
    val show: String?
)

fun parseReleaseTitle(title: String): ParsedReleaseTitle {
    val trimmed = title.trim()
    if (trimmed.isEmpty()) return ParsedReleaseTitle(null, null)
    val group = GROUP_REGEX.find(trimmed)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
    var rest = if (group != null) trimmed.removePrefix("[$group]").trim() else trimmed
    rest = rest.replace(BRACKET_REGEX, " ").replace(PAREN_REGEX, " ")
    rest = rest.replace(Regex("\\s+"), " ").trim()
    rest = rest.removeSuffix(".mkv").removeSuffix(".mp4").removeSuffix(".avi").trim()
    val episode = EPISODE_REGEX.find(rest)
    val show = if (episode != null) {
        rest.substring(0, episode.range.first).trim().trimEnd('-').trim()
    } else {
        rest
    }.takeIf { it.length >= 2 }
    return ParsedReleaseTitle(
        group = group?.takeIf { it.length in 2..32 && !it.contains("http", ignoreCase = true) },
        show = show?.takeIf { it != group }
    )
}

private val GROUP_REGEX = Regex("^\\[([^\\]]+)]")
private val BRACKET_REGEX = Regex("\\[[^]]+]")
private val PAREN_REGEX = Regex("\\([^)]+\\)")
private val EPISODE_REGEX = Regex(
    "\\s[-–]\\s(?:S\\d+E\\d+|E\\d+|\\d{1,4}(?:\\.\\d+)?)\\b",
    RegexOption.IGNORE_CASE
)

fun qualityTags(title: String): List<String> {
    val tags = ArrayList<String>(4)
    when {
        RESOLUTION_2160.containsMatchIn(title) -> tags += "2160p"
        RESOLUTION_1080.containsMatchIn(title) -> tags += "1080p"
        RESOLUTION_720.containsMatchIn(title) -> tags += "720p"
        RESOLUTION_480.containsMatchIn(title) -> tags += "480p"
    }
    when {
        CODEC_HEVC.containsMatchIn(title) -> tags += "HEVC"
        CODEC_AV1.containsMatchIn(title) -> tags += "AV1"
        CODEC_AVC.containsMatchIn(title) -> tags += "AVC"
    }
    if (DUAL_AUDIO.containsMatchIn(title)) tags += "Dual Audio"
    if (HDR.containsMatchIn(title)) tags += "HDR"
    return tags
}

private val RESOLUTION_2160 = Regex("2160p|\\b4k\\b", RegexOption.IGNORE_CASE)
private val RESOLUTION_1080 = Regex("1080p", RegexOption.IGNORE_CASE)
private val RESOLUTION_720 = Regex("720p", RegexOption.IGNORE_CASE)
private val RESOLUTION_480 = Regex("480p", RegexOption.IGNORE_CASE)
private val CODEC_HEVC = Regex("hevc|x265|h\\.?265", RegexOption.IGNORE_CASE)
private val CODEC_AV1 = Regex("\\bav1\\b", RegexOption.IGNORE_CASE)
private val CODEC_AVC = Regex("x264|h\\.?264|\\bavc\\b", RegexOption.IGNORE_CASE)
private val DUAL_AUDIO = Regex("dual[- ]?audio", RegexOption.IGNORE_CASE)
private val HDR = Regex("\\b(?:HDR10\\+?|Dolby.?Vision|\\bDV\\b)\\b", RegexOption.IGNORE_CASE)

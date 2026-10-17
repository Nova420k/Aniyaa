package com.nyaa.aniyaa.util

fun inQuietHours(hour: Int, start: Int, end: Int, enabled: Boolean): Boolean {
    if (!enabled) return false
    if (start !in 0..23 || end !in 0..23 || start == end) return false
    val current = hour.coerceIn(0, 23)
    return if (start < end) {
        current in start until end
    } else {
        current >= start || current < end
    }
}

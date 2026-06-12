package com.example.fitnesstracker.util

/**
 * Shared formatting utilities for duration and pace values.
 * Fix #28: Moved from TrackScreen.kt so these can be used in any screen
 * without creating a circular dependency.
 */

/**
 * Formats a duration in seconds to "H:MM:SS" or "MM:SS" depending on length.
 */
fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", m, s)
    }
}

/**
 * Formats a pace in seconds-per-km (or seconds-per-mile) to "M:SS".
 * Returns "--:--" for invalid/zero values.
 */
fun formatPace(paceSecPerKm: Double): String {
    if (paceSecPerKm <= 0.0 || paceSecPerKm.isInfinite() || paceSecPerKm.isNaN()) return "--:--"
    val mins = (paceSecPerKm / 60.0).toInt()
    val secs = (paceSecPerKm % 60.0).toInt()
    return String.format(java.util.Locale.US, "%d:%02d", mins, secs)
}

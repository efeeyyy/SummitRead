package com.example.summitread.data

import kotlin.math.roundToInt

/** Pure helpers for page logging (kept free of Android types so they are JVM-testable). */
object PageMath {
    /** Assumed reading speed used for ClimbTime (#35) and the stopwatch page suggestion (#28). */
    const val MINUTES_PER_PAGE = 1.5

    /** Pages that may still be logged: never lets the sum of sessions exceed [totalPages]. */
    fun clampToRemaining(requested: Int, totalPages: Int, alreadyRead: Int): Int {
        val remaining = (totalPages - alreadyRead).coerceAtLeast(0)
        return requested.coerceIn(0, remaining)
    }

    /** Suggested pages for an elapsed reading time (at least 1 page). */
    fun suggestedPages(elapsedMillis: Long): Int =
        (elapsedMillis / 60_000.0 / MINUTES_PER_PAGE).roundToInt().coerceAtLeast(1)
}

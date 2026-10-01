package com.example.summitread.data

import java.util.Locale

/** hh:mm:ss formatting of elapsed reading time (hours are not capped). Pure, JVM-testable. */
object ElapsedTimeFormat {
    fun format(millis: Long): String {
        val totalSeconds = millis.coerceAtLeast(0L) / 1000
        return String.format(
            Locale.US,
            "%02d:%02d:%02d",
            totalSeconds / 3600,
            (totalSeconds / 60) % 60,
            totalSeconds % 60
        )
    }
}

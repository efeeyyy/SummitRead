package com.example.summitread.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** Formats an epoch-millis timestamp in the device time zone and locale. */
interface DateFormatter {
    fun format(millis: Long): String
}

/**
 * Creates a [DateFormatter] for a CLDR/ICU style [pattern] such as "MMM d, yyyy", "EEE" or "EEEE, MMM d", using the
 * device's current locale and time zone. Android: `SimpleDateFormat`; iOS: `NSDateFormatter` (unverified on a Mac).
 */
expect fun createDateFormatter(pattern: String): DateFormatter

@Composable
fun rememberDateFormatter(pattern: String): DateFormatter = remember(pattern) { createDateFormatter(pattern) }

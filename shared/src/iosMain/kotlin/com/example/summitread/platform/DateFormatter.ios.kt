package com.example.summitread.platform

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970

// UNVERIFIED on a Mac (compiled on Windows only).
actual fun createDateFormatter(pattern: String): DateFormatter {
    val formatter = NSDateFormatter().apply {
        dateFormat = pattern
        locale = NSLocale.currentLocale
    }
    return object : DateFormatter {
        override fun format(millis: Long): String =
            formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(millis / 1000.0))
    }
}

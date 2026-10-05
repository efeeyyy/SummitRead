package com.example.summitread.platform

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun createDateFormatter(pattern: String): DateFormatter {
    val format = SimpleDateFormat(pattern, Locale.getDefault())
    return object : DateFormatter {
        override fun format(millis: Long): String = format.format(Date(millis))
    }
}

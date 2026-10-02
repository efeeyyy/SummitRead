package com.example.summitread.data

/** hh:mm:ss formatting of elapsed reading time (hours are not capped). Pure, platform independent. */
object ElapsedTimeFormat {
    fun format(millis: Long): String {
        val totalSeconds = millis.coerceAtLeast(0L) / 1000
        return "${pad2(totalSeconds / 3600)}:${pad2((totalSeconds / 60) % 60)}:${pad2(totalSeconds % 60)}"
    }

    private fun pad2(value: Long): String = value.toString().padStart(2, '0')
}

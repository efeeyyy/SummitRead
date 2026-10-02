package com.example.summitread.ui.home

import androidx.compose.ui.graphics.Color

/** Hue (0..360), saturation (0..1) and value/brightness (0..1) of a picked colour. */
data class Hsv(val hue: Float, val saturation: Float, val value: Float) {
    fun toColor(): Color = Color.hsv(hue.coerceIn(0f, 360f), saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
}

/** Converts a colour to HSV (pure math, no platform graphics types so it is unit-testable). */
fun Color.toHsv(): Hsv {
    val max = maxOf(red, green, blue)
    val minC = minOf(red, green, blue)
    val delta = max - minC
    val hue = when {
        delta == 0f -> 0f
        max == red -> 60f * (((green - blue) / delta) % 6f)
        max == green -> 60f * ((blue - red) / delta + 2f)
        else -> 60f * ((red - green) / delta + 4f)
    }.let { if (it < 0f) it + 360f else it }
    return Hsv(hue, if (max == 0f) 0f else delta / max, max)
}

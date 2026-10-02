package com.example.summitread.ui.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.test.Test
import kotlin.test.assertEquals

class HsvTest {

    @Test
    fun primaryColoursHaveExpectedHue() {
        assertEquals(0f, Color(0xFFFF0000).toHsv().hue, 0.5f)
        assertEquals(120f, Color(0xFF00FF00).toHsv().hue, 0.5f)
        assertEquals(240f, Color(0xFF0000FF).toHsv().hue, 0.5f)
    }

    @Test
    fun greyHasNoSaturationAndWhiteIsBright() {
        val grey = Color(0xFF808080).toHsv()
        assertEquals(0f, grey.saturation, 0.001f)
        assertEquals(1f, Color.White.toHsv().value, 0.001f)
    }

    @Test
    fun roundTripKeepsTheColourAndStoredArgbIsOpaque() {
        val original = Color(0xFF7B1FA2)
        val back = original.toHsv().toColor()
        assertEquals(original.toArgb(), back.toArgb())
        assertEquals(0xFF, (back.toArgb() ushr 24) and 0xFF)
    }
}

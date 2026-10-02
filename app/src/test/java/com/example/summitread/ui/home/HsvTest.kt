package com.example.summitread.ui.home

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

/** The pure Hsv maths is tested in :shared; this keeps the app-side palette lookup covered. */
class HsvTest {

    @Test
    fun savedColourIsResolvedBeforeAccentIndex() {
        val custom = 0xFF123456.toInt()
        assertEquals(Color(custom), baseColorOf(custom, accentColorIndex = 2))
        // Without a custom colour the accent palette entry (and a safe default for a bad index) is used.
        assertEquals(baseColorOf(null, 0), baseColorOf(null, 99))
    }
}

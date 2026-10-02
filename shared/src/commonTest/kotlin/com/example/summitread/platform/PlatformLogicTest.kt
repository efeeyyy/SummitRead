package com.example.summitread.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlatformLogicTest {

    @Test
    fun tiltIsDampedAndClamped() {
        assertEquals(10f to -5f, TiltFilter.filter(20f, -10f))
        // damped 0.5x: 180 deg raw -> 90 -> clamped to the +-45 limit
        assertEquals(45f to -45f, TiltFilter.filter(180f, -180f))
    }

    @Test
    fun nonFiniteTiltIsRejected() {
        assertNull(TiltFilter.filter(Float.NaN, 0f))
        assertNull(TiltFilter.filter(0f, Float.POSITIVE_INFINITY))
    }

    @Test
    fun reminderTextsMentionTheStreakAndTheTimeLeft() {
        val (lastTitle, lastBody) = StreakReminderTexts.forReminder(streak = 1, hoursBefore = 1)
        assertEquals("Last chance to save your streak!", lastTitle)
        assertTrue("1 day streak" in lastBody)

        val (title, body) = StreakReminderTexts.forReminder(streak = 5, hoursBefore = 6)
        assertEquals("Keep the fire alive!", title)
        assertTrue("About 6 hours left" in body)
        assertTrue("5 days streak" in body)
    }
}

package com.example.summitread.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PageMathTest {

    @Test
    fun clampNeverExceedsTotalPages() {
        assertEquals(30, PageMath.clampToRemaining(30, totalPages = 300, alreadyRead = 0))
        assertEquals(50, PageMath.clampToRemaining(1000, totalPages = 300, alreadyRead = 250))
        assertEquals(0, PageMath.clampToRemaining(10, totalPages = 300, alreadyRead = 300))
        assertEquals(0, PageMath.clampToRemaining(10, totalPages = 300, alreadyRead = 400)) // legacy over-read
    }

    @Test
    fun clampRejectsNonPositiveRequests() {
        assertEquals(0, PageMath.clampToRemaining(0, totalPages = 300, alreadyRead = 0))
        assertEquals(0, PageMath.clampToRemaining(-5, totalPages = 300, alreadyRead = 0))
    }

    @Test
    fun suggestedPagesUsesOnePointFiveMinutesPerPageAndAtLeastOne() {
        assertEquals(1, PageMath.suggestedPages(10_000L))
        assertEquals(20, PageMath.suggestedPages(30L * 60 * 1000)) // 30 min -> 20 pages
        assertEquals(40, PageMath.suggestedPages(60L * 60 * 1000))
    }

    @Test
    fun elapsedTimeFormatsAsHoursMinutesSeconds() {
        assertEquals("00:00:00", ElapsedTimeFormat.format(0))
        assertEquals("00:01:05", ElapsedTimeFormat.format(65_999))
        assertEquals("01:00:00", ElapsedTimeFormat.format(3_600_000))
        assertEquals("100:00:01", ElapsedTimeFormat.format(360_001_000))
        assertEquals("00:00:00", ElapsedTimeFormat.format(-5))
    }

    @Test
    fun relicEmojiIsStablePerBook() {
        assertEquals(RelicEmojis.forBook(7), RelicEmojis.forBook(7))
        assertEquals(RelicEmojis.all[0], RelicEmojis.forBook(0))
        assertEquals(RelicEmojis.all[1], RelicEmojis.forBook(7))
    }
}

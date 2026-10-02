package com.example.summitread.ui.mountain

import com.example.summitread.data.db.ReadingSessionEntity
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test

class MountainLogicTest {

    @Test
    fun tiersFollowPageCount() {
        assertEquals(MountainTier.HILL, MountainTier.forPages(0))
        assertEquals(MountainTier.HILL, MountainTier.forPages(199))
        assertEquals(MountainTier.STANDARD, MountainTier.forPages(200))
        assertEquals(MountainTier.STANDARD, MountainTier.forPages(399))
        assertEquals(MountainTier.EVEREST, MountainTier.forPages(400))
        assertEquals(MountainTier.EVEREST, MountainTier.forPages(1200))
        // taller tier = higher peak (smaller y ratio)
        assertTrue(MountainTier.EVEREST.peakYRatio < MountainTier.STANDARD.peakYRatio)
        assertTrue(MountainTier.STANDARD.peakYRatio < MountainTier.HILL.peakYRatio)
    }

    @Test
    fun skyIsNightAtMidnightAndDayAtNoon() {
        assertEquals(1f, SkyModel.skyAt(2f).night, 0.001f)
        assertEquals(0f, SkyModel.skyAt(12f).night, 0.001f)
        assertEquals(1f, SkyModel.skyAt(23f).night, 0.001f)
        val dawn = SkyModel.skyAt(8f)
        val noon = SkyModel.skyAt(12f)
        assertTrue(dawn.top != noon.top)
        // blends smoothly between keys
        val mid = SkyModel.skyAt(5.75f).night
        assertTrue(mid < 1f && mid > 0.25f)
    }

    @Test
    fun sunIsUpOnlyByDay() {
        assertTrue(SkyModel.isSunUp(12f))
        assertFalse(SkyModel.isSunUp(3f))
        assertFalse(SkyModel.isSunUp(22f))
        assertEquals(0f, SkyModel.sunArc(6f), 0.001f)
        assertEquals(1f, SkyModel.sunArc(19f), 0.001f)
    }

    @Test
    fun stormAfterMoreThan36Hours() {
        assertFalse(WeatherRules.isStormy(0))
        assertFalse(WeatherRules.isStormy(36))
        assertTrue(WeatherRules.isStormy(37))
    }

    private fun session(id: Long, date: Long, pages: Int, time: Long = 0L) =
        ReadingSessionEntity(id = id, bookId = 1, date = date, pagesRead = pages, timeSpentMillis = time)

    @Test
    fun milestoneReachedUsesCumulativeSessions() {
        val sessions = listOf(
            session(1, 1000, 30, 60_000),
            session(2, 2000, 30, 120_000),
            session(3, 3000, 50, 0)
        )
        // 400 pages book, 110 read: 25% = 100 pages reached in the 3rd session
        val stats = MilestoneStats.compute(0.25f, 400, 110, sessions)
        assertTrue(stats.reached)
        assertEquals(100, stats.pagesAtMilestone)
        assertEquals(3000L, stats.reachedOnMillis)
        assertEquals(3, stats.sessionsToReach)
        assertEquals(180_000L, stats.timeSpentMillis)
        assertEquals(0, stats.pagesToGo)
    }

    @Test
    fun milestoneNotReachedReportsPagesToGo() {
        val sessions = listOf(session(1, 1000, 40))
        val stats = MilestoneStats.compute(0.5f, 300, 40, sessions)
        assertFalse(stats.reached)
        assertEquals(150, stats.pagesAtMilestone)
        assertEquals(110, stats.pagesToGo)
        assertNull(stats.reachedOnMillis)
        assertEquals(1, stats.sessionsToReach)
    }

    @Test
    fun quotesAreStablePerBookAndCampNamed() {
        assertEquals(MilestoneQuotes.quoteFor(0.25f, 7), MilestoneQuotes.quoteFor(0.25f, 7))
        assertEquals("Camp I", MilestoneQuotes.campName(0.25f))
        assertEquals("Camp II", MilestoneQuotes.campName(0.5f))
        assertEquals("Camp III", MilestoneQuotes.campName(0.75f))
    }
}

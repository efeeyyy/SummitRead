package com.example.summitread.data

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StreakCalculatorTest {

    private val zone = TimeZone.of("Europe/Istanbul")
    private val today = LocalDate(2026, 10, 1)

    private fun LocalDate.plusDays(n: Int): LocalDate = plus(n, DateTimeUnit.DAY)

    private fun LocalDate.minusDays(n: Int): LocalDate = minus(n, DateTimeUnit.DAY)

    private fun days(vararg offsets: Int): Set<LocalDate> = offsets.map { today.plusDays(it) }.toSet()

    private fun millisAt(year: Int, month: Int, day: Int, hour: Int, minute: Int = 0, second: Int = 0, tz: TimeZone = zone): Long =
        LocalDateTime(year, month, day, hour, minute, second).toInstant(tz).toEpochMilliseconds()

    @Test
    fun noLogsMeansZero() {
        assertEquals(0, StreakCalculator.currentStreak(emptySet(), today))
    }

    @Test
    fun onlyTodayIsOne() {
        assertEquals(1, StreakCalculator.currentStreak(days(0), today))
    }

    @Test
    fun yesterdayOnlyKeepsStreakAliveUntilTodayEnds() {
        assertEquals(1, StreakCalculator.currentStreak(days(-1), today))
    }

    @Test
    fun missedFullDayResetsToZero() {
        assertEquals(0, StreakCalculator.currentStreak(days(-2, -3, -4), today))
    }

    @Test
    fun consecutiveChainThroughToday() {
        assertEquals(4, StreakCalculator.currentStreak(days(0, -1, -2, -3), today))
    }

    @Test
    fun chainEndingYesterdayCountsWithoutToday() {
        assertEquals(3, StreakCalculator.currentStreak(days(-1, -2, -3), today))
    }

    @Test
    fun gapBreaksTheChain() {
        // today, yesterday, then a gap at -2, older logs do not count
        assertEquals(2, StreakCalculator.currentStreak(days(0, -1, -3, -4, -5), today))
    }

    @Test
    fun backdatingAMissedDayMergesTwoChains() {
        // before: only yesterday counts; the chain D-5..D-3 is cut off by the gap at D-2
        val before = days(-5, -4, -3, -1)
        assertEquals(1, StreakCalculator.currentStreak(before, today))
        // after logging D-2 retroactively the chains merge into one of five days
        val after = before + today.minusDays(2)
        assertEquals(5, StreakCalculator.currentStreak(after, today))
    }

    @Test
    fun backdatingYesterdayWhenTodayIsLoggedKeepsOldStreak() {
        // streak of 5 ended D-2, D-1 was missed, today logged: today alone, then yesterday fills the gap
        val before = days(0, -2, -3, -4, -5, -6)
        assertEquals(1, StreakCalculator.currentStreak(before, today))
        assertEquals(7, StreakCalculator.currentStreak(before + today.minusDays(1), today))
    }

    @Test
    fun backdatingOlderThanTheGapDoesNotChangeTheCurrentStreak() {
        val before = days(0, -1)
        assertEquals(2, StreakCalculator.currentStreak(before + today.minusDays(10), today))
    }

    @Test
    fun sameDayMultipleLogsCountOnce() {
        val morning = millisAt(2026, 10, 1, 7, 30)
        val evening = millisAt(2026, 10, 1, 22, 5)
        val set = listOf(morning, evening).map { StreakCalculator.toLocalDate(it, zone) }.toSet()
        assertEquals(setOf(today), set)
        assertEquals(1, StreakCalculator.currentStreak(set, today))
    }

    @Test
    fun futureDaysAreIgnored() {
        assertEquals(0, StreakCalculator.currentStreak(days(1, 2), today))
        assertEquals(1, StreakCalculator.currentStreak(days(0, 1, 2), today))
    }

    @Test
    fun midnightBoundaryLogsAreDifferentDays() {
        val lastSecond = millisAt(2026, 9, 30, 23, 59, 59)
        val firstSecond = millisAt(2026, 10, 1, 0, 0, 0)
        assertEquals(LocalDate(2026, 9, 30), StreakCalculator.toLocalDate(lastSecond, zone))
        assertEquals(LocalDate(2026, 10, 1), StreakCalculator.toLocalDate(firstSecond, zone))
        val set = setOf(lastSecond, firstSecond).map { StreakCalculator.toLocalDate(it, zone) }.toSet()
        assertEquals(2, StreakCalculator.currentStreak(set, today))
    }

    @Test
    fun streakReadsZeroOnceMidnightPassesWithoutALog() {
        val logged = days(-1, 0) // read yesterday and today
        assertEquals(2, StreakCalculator.currentStreak(logged, today))
        // next day, nothing logged yet: still alive (yesterday counts)
        assertEquals(2, StreakCalculator.currentStreak(logged, today.plusDays(1)))
        // the day after that with no log: a whole day was missed
        assertEquals(0, StreakCalculator.currentStreak(logged, today.plusDays(2)))
    }

    @Test
    fun startOfDayRoundTripsAcrossDstChange() {
        val ny = TimeZone.of("America/New_York")
        val springForward = LocalDate(2026, 3, 8)
        val start = StreakCalculator.startOfDayMillis(springForward, ny)
        assertEquals(springForward, StreakCalculator.toLocalDate(start, ny))
        val next = StreakCalculator.startOfDayMillis(springForward.plusDays(1), ny)
        assertEquals(23L * 60 * 60 * 1000, next - start) // the short day
    }

    @Test
    fun resetMomentIsMidnightEndingTheDayAfterTheLastRead() {
        val lastReadYesterday = StreakCalculator.resetMoment(days(-1), today, zone)
        assertEquals(LocalDateTime(2026, 10, 2, 0, 0).toInstant(zone), lastReadYesterday)

        val lastReadToday = StreakCalculator.resetMoment(days(0, -1), today, zone)
        assertEquals(LocalDateTime(2026, 10, 3, 0, 0).toInstant(zone), lastReadToday)
    }

    @Test
    fun resetMomentIsNullWithoutALiveStreak() {
        assertNull(StreakCalculator.resetMoment(emptySet(), today, zone))
        assertNull(StreakCalculator.resetMoment(days(-2), today, zone))
        assertNull(StreakCalculator.resetMoment(days(3), today, zone)) // only future rows
    }
}

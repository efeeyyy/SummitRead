package com.example.summitread.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderPlannerTest {

    private val zone = ZoneId.of("Europe/Istanbul")
    private val today = LocalDate.of(2026, 10, 1)

    private fun at(day: Int, hour: Int, minute: Int = 0): ZonedDateTime =
        ZonedDateTime.of(2026, 10, day, hour, minute, 0, 0, zone)

    @Test
    fun lastReadYesterdayRemindsSixAndOneHourBeforeMidnight() {
        val plan = ReminderPlanner.plan(setOf(today.minusDays(1)), at(1, 10))
        assertEquals(listOf(6, 1), plan.map { it.hoursBeforeReset })
        assertEquals(at(1, 18).toInstant().toEpochMilli(), plan[0].triggerAtMillis)
        assertEquals(at(1, 23).toInstant().toEpochMilli(), plan[1].triggerAtMillis)
        assertEquals(at(2, 0).toInstant().toEpochMilli(), plan[0].resetAtMillis)
    }

    @Test
    fun pastRemindersAreSkipped() {
        val plan = ReminderPlanner.plan(setOf(today.minusDays(1)), at(1, 20))
        assertEquals(listOf(1), plan.map { it.hoursBeforeReset })
        assertTrue(ReminderPlanner.plan(setOf(today.minusDays(1)), at(1, 23, 30)).isEmpty())
    }

    @Test
    fun loggedTodayPushesRemindersToTomorrowEvening() {
        val plan = ReminderPlanner.plan(setOf(today, today.minusDays(1)), at(1, 10))
        assertEquals(listOf(6, 1), plan.map { it.hoursBeforeReset })
        assertEquals(at(2, 18).toInstant().toEpochMilli(), plan[0].triggerAtMillis)
        assertEquals(at(2, 23).toInstant().toEpochMilli(), plan[1].triggerAtMillis)
    }

    @Test
    fun noLiveStreakMeansNoReminders() {
        assertTrue(ReminderPlanner.plan(emptySet(), at(1, 10)).isEmpty())
        assertTrue(ReminderPlanner.plan(setOf(today.minusDays(2)), at(1, 10)).isEmpty())
    }
}

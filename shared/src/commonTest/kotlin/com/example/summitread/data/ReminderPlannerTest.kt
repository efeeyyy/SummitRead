package com.example.summitread.data

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class ReminderPlannerTest {

    private val zone = TimeZone.of("Europe/Istanbul")
    private val today = LocalDate(2026, 10, 1)

    private fun at(day: Int, hour: Int, minute: Int = 0): Instant =
        LocalDateTime(2026, 10, day, hour, minute).toInstant(zone)

    private fun yesterday(): LocalDate = today.minus(1, DateTimeUnit.DAY)

    private fun twoDaysAgo(): LocalDate = today.minus(2, DateTimeUnit.DAY)

    @Test
    fun lastReadYesterdayRemindsSixAndOneHourBeforeMidnight() {
        val plan = ReminderPlanner.plan(setOf(yesterday()), at(1, 10), zone)
        assertEquals(listOf(6, 1), plan.map { it.hoursBeforeReset })
        assertEquals(at(1, 18).toEpochMilliseconds(), plan[0].triggerAtMillis)
        assertEquals(at(1, 23).toEpochMilliseconds(), plan[1].triggerAtMillis)
        assertEquals(at(2, 0).toEpochMilliseconds(), plan[0].resetAtMillis)
    }

    @Test
    fun pastRemindersAreSkipped() {
        val plan = ReminderPlanner.plan(setOf(yesterday()), at(1, 20), zone)
        assertEquals(listOf(1), plan.map { it.hoursBeforeReset })
        assertTrue(ReminderPlanner.plan(setOf(yesterday()), at(1, 23, 30), zone).isEmpty())
    }

    @Test
    fun loggedTodayPushesRemindersToTomorrowEvening() {
        val plan = ReminderPlanner.plan(setOf(today, yesterday()), at(1, 10), zone)
        assertEquals(listOf(6, 1), plan.map { it.hoursBeforeReset })
        assertEquals(at(2, 18).toEpochMilliseconds(), plan[0].triggerAtMillis)
        assertEquals(at(2, 23).toEpochMilliseconds(), plan[1].triggerAtMillis)
    }

    @Test
    fun noLiveStreakMeansNoReminders() {
        assertTrue(ReminderPlanner.plan(emptySet(), at(1, 10), zone).isEmpty())
        assertTrue(ReminderPlanner.plan(setOf(twoDaysAgo()), at(1, 10), zone).isEmpty())
    }
}

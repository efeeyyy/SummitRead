package com.example.summitread.data

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/** A planned streak-rescue notification. */
data class StreakReminder(
    val hoursBeforeReset: Int,
    val triggerAtMillis: Long,
    val resetAtMillis: Long
)

/** Pure planner for the "streak is about to reset" notifications (#34). */
object ReminderPlanner {
    val HOURS_BEFORE = listOf(6, 1)

    /**
     * Reminders 6h and 1h before the streak resets, only the ones still in the future.
     * Empty when there is no live streak (nothing to rescue).
     */
    fun plan(loggedDays: Collection<LocalDate>, now: Instant, zone: TimeZone): List<StreakReminder> {
        val today = now.toLocalDateTime(zone).date
        val reset = StreakCalculator.resetMoment(loggedDays, today, zone) ?: return emptyList()
        val resetMillis = reset.toEpochMilliseconds()
        val nowMillis = now.toEpochMilliseconds()
        return HOURS_BEFORE.mapNotNull { hours ->
            val trigger = (reset - hours.hours).toEpochMilliseconds()
            if (trigger > nowMillis) StreakReminder(hours, trigger, resetMillis) else null
        }
    }
}

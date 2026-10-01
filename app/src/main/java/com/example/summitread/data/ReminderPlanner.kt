package com.example.summitread.data

import java.time.LocalDate
import java.time.ZonedDateTime

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
    fun plan(loggedDays: Collection<LocalDate>, now: ZonedDateTime): List<StreakReminder> {
        val reset = StreakCalculator.resetMoment(loggedDays, now.toLocalDate(), now.zone) ?: return emptyList()
        val resetMillis = reset.toInstant().toEpochMilli()
        val nowMillis = now.toInstant().toEpochMilli()
        return HOURS_BEFORE.mapNotNull { hours ->
            val trigger = reset.minusHours(hours.toLong()).toInstant().toEpochMilli()
            if (trigger > nowMillis) StreakReminder(hours, trigger, resetMillis) else null
        }
    }
}

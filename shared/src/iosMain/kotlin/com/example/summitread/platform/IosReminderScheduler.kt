package com.example.summitread.platform

import com.example.summitread.data.ReminderPlanner
import com.example.summitread.data.StreakCalculator
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Streak-rescue reminders as scheduled local notifications (UNVERIFIED on a Mac).
 *
 * Unlike Android (where a worker re-validates when it fires) the notification text is fixed when it is scheduled, so
 * the plan is rebuilt after every log (see `StreakReminders.observe`), which also replaces stale requests.
 * Reminders fire at the planned 6 h / 1 h marks; the OS shows them even when the app is not running.
 */
class IosReminderScheduler : ReminderScheduler {

    override fun reschedule(loggedDays: Set<LocalDate>, notificationsEnabled: Boolean) {
        val allIds = ReminderPlanner.HOURS_BEFORE.map(::identifier)
        IosNotifications.cancel(allIds)
        if (!notificationsEnabled) return

        val zone = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val plan = ReminderPlanner.plan(loggedDays, now, zone)
        if (plan.isEmpty()) return

        val streak = StreakCalculator.currentStreak(loggedDays, now.toLocalDateTime(zone).date)
        val nowMillis = now.toEpochMilliseconds()
        plan.forEach { reminder ->
            val (title, body) = StreakReminderTexts.forReminder(streak, reminder.hoursBeforeReset)
            IosNotifications.schedule(
                identifier = identifier(reminder.hoursBeforeReset),
                title = title,
                body = body,
                delaySeconds = (reminder.triggerAtMillis - nowMillis) / 1000.0
            )
        }
    }

    override suspend fun requestPermission(): Boolean = IosNotifications.requestAuthorization()

    private fun identifier(hoursBefore: Int) = "streak_reminder_${hoursBefore}h"
}

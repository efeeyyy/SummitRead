package com.example.summitread.platform

import com.example.summitread.data.BookRepository
import com.example.summitread.data.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * Schedules the two streak-rescue notifications (6 h and 1 h before the streak resets, see `ReminderPlanner`).
 *
 * - Android: WorkManager unique one-time work per reminder (`WorkManagerReminderScheduler`).
 * - iOS: `UNUserNotificationCenter` scheduled requests (`IosReminderScheduler`, unverified on a Mac).
 */
interface ReminderScheduler {

    /**
     * Replaces everything that was scheduled: plans the reminders for [loggedDays] and cancels the ones that are
     * no longer valid. Nothing stays scheduled when [notificationsEnabled] is false or no streak is alive.
     */
    fun reschedule(loggedDays: Set<LocalDate>, notificationsEnabled: Boolean)

    /**
     * Makes sure the OS lets us show notifications and returns whether it does. iOS asks the user here; on Android
     * the POST_NOTIFICATIONS prompt is bound to an Activity and stays in `MainActivity`, so this only reports.
     */
    suspend fun requestPermission(): Boolean
}

/** Texts of the streak reminders, shared so every platform says the same thing. */
object StreakReminderTexts {

    /** Title and body for the reminder that fires [hoursBefore] hours before the reset of a [streak]-day streak. */
    fun forReminder(streak: Int, hoursBefore: Int): Pair<String, String> {
        val dayWord = if (streak == 1) "day" else "days"
        return if (hoursBefore <= 1) {
            "Last chance to save your streak!" to
                "Only about an hour left. Read a few pages to keep your $streak $dayWord streak alive."
        } else {
            "Keep the fire alive!" to
                "About $hoursBefore hours left to read and keep your $streak $dayWord streak going."
        }
    }
}

/** Keeps the reminders scheduled: rescheduled after every log and whenever the notification preference changes. */
object StreakReminders {

    fun observe(
        scope: CoroutineScope,
        repository: BookRepository,
        preferences: UserPreferences,
        scheduler: ReminderScheduler
    ): Job = scope.launch {
        combine(repository.loggedDays, preferences.notificationsEnabled) { days, enabled -> days to enabled }
            .distinctUntilChanged()
            .collect { (days, enabled) -> scheduler.reschedule(days, enabled) }
    }
}

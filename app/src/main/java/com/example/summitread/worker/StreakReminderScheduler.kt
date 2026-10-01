package com.example.summitread.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.summitread.data.BookRepository
import com.example.summitread.data.ReminderPlanner
import com.example.summitread.data.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * Keeps the two streak-rescue notifications (6h and 1h before the streak resets) scheduled.
 * Unique one-time work per reminder with REPLACE; cancelled when notifications are off or no streak is alive.
 * WorkManager is not exact (Doze may delay a run by minutes); the worker re-validates when it fires.
 */
object StreakReminderScheduler {

    private fun workName(hoursBefore: Int) = "streak_reminder_${hoursBefore}h"

    /** Starts observing logs and the notification preference; reschedules after every change. */
    fun observe(
        scope: CoroutineScope,
        context: Context,
        repository: BookRepository,
        preferences: UserPreferences
    ): Job {
        val appContext = context.applicationContext
        // Remove the legacy daily periodic job (replaced by the exact-time reminders below).
        WorkManager.getInstance(appContext).cancelUniqueWork("StreakNotificationWork")
        return scope.launch {
            combine(repository.loggedDays, preferences.notificationsEnabled) { days, enabled -> days to enabled }
                .distinctUntilChanged()
                .collect { (days, enabled) -> reschedule(appContext, days, enabled) }
        }
    }

    fun reschedule(
        context: Context,
        loggedDays: Set<LocalDate>,
        notificationsEnabled: Boolean,
        now: ZonedDateTime = ZonedDateTime.now()
    ) {
        val workManager = WorkManager.getInstance(context)
        val planned = if (notificationsEnabled) ReminderPlanner.plan(loggedDays, now) else emptyList()
        val nowMillis = now.toInstant().toEpochMilli()

        ReminderPlanner.HOURS_BEFORE.forEach { hours ->
            val reminder = planned.firstOrNull { it.hoursBeforeReset == hours }
            if (reminder == null) {
                workManager.cancelUniqueWork(workName(hours))
            } else {
                val request = OneTimeWorkRequestBuilder<StreakNotificationWorker>()
                    .setInitialDelay((reminder.triggerAtMillis - nowMillis).coerceAtLeast(0L), TimeUnit.MILLISECONDS)
                    .setInputData(
                        Data.Builder()
                            .putLong(StreakNotificationWorker.KEY_RESET_AT_MILLIS, reminder.resetAtMillis)
                            .putInt(StreakNotificationWorker.KEY_HOURS_BEFORE, hours)
                            .build()
                    )
                    .build()
                workManager.enqueueUniqueWork(workName(hours), ExistingWorkPolicy.REPLACE, request)
            }
        }
    }
}

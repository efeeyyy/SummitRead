package com.example.summitread.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.summitread.data.ReminderPlanner
import com.example.summitread.notifications.NotificationChannels
import com.example.summitread.platform.ReminderScheduler
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Android [ReminderScheduler]: keeps the two streak-rescue notifications (6h and 1h before the streak resets)
 * scheduled with unique one-time WorkManager work per reminder (REPLACE); cancelled when notifications are off or
 * no streak is alive. WorkManager is not exact (Doze may delay a run by minutes); the worker re-validates when it
 * fires. Observation of logs and the preference lives in the shared `StreakReminders.observe`.
 */
class WorkManagerReminderScheduler(context: Context) : ReminderScheduler {

    private val appContext = context.applicationContext

    init {
        // Remove the legacy daily periodic job (replaced by the exact-time reminders below).
        WorkManager.getInstance(appContext).cancelUniqueWork("StreakNotificationWork")
    }

    override fun reschedule(loggedDays: Set<LocalDate>, notificationsEnabled: Boolean) {
        reschedule(loggedDays, notificationsEnabled, Clock.System.now())
    }

    fun reschedule(loggedDays: Set<LocalDate>, notificationsEnabled: Boolean, now: Instant) {
        val workManager = WorkManager.getInstance(appContext)
        val planned = if (notificationsEnabled) {
            ReminderPlanner.plan(loggedDays, now, TimeZone.currentSystemDefault())
        } else {
            emptyList()
        }
        val nowMillis = now.toEpochMilliseconds()

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

    /** Android's POST_NOTIFICATIONS prompt is Activity bound (see MainActivity); this only reports the state. */
    override suspend fun requestPermission(): Boolean = NotificationChannels.canPost(appContext)

    private fun workName(hoursBefore: Int) = "streak_reminder_${hoursBefore}h"
}

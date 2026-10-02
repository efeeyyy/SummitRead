package com.example.summitread.worker

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.summitread.MainActivity
import com.example.summitread.R
import com.example.summitread.data.StreakCalculator
import com.example.summitread.notifications.NotificationChannels
import com.example.summitread.platform.AndroidAppContainer
import com.example.summitread.platform.StreakReminderTexts
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.time.Clock

/**
 * Fires one "streak is about to reset" notification (scheduled by [WorkManagerReminderScheduler]).
 * It re-validates everything at fire time, so a stale or late run never shows a wrong reminder.
 */
class StreakNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val resetAtMillis = inputData.getLong(KEY_RESET_AT_MILLIS, 0L)
        val hoursBefore = inputData.getInt(KEY_HOURS_BEFORE, 0)
        // Runs of the legacy daily periodic job carry no input: nothing to do.
        if (resetAtMillis == 0L || hoursBefore == 0) return Result.success()

        val context = applicationContext
        val container = AndroidAppContainer.get(context)
        if (!container.preferences.notificationsEnabled.first()) return Result.success()
        if (!NotificationChannels.canPost(context)) return Result.success()

        val days = container.repository.getLoggedDaysOnce()
        val zone = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val today = now.toLocalDateTime(zone).date

        val reset = StreakCalculator.resetMoment(days, today, zone) ?: return Result.success()
        val resetMillis = reset.toEpochMilliseconds()
        // The streak moved on (pages were logged since scheduling) or the reset has already passed.
        if (abs(resetMillis - resetAtMillis) > STALE_TOLERANCE_MILLIS) return Result.success()
        if (now >= reset) return Result.success()

        val streak = StreakCalculator.currentStreak(days, today)
        if (streak <= 0) return Result.success()

        show(context, streak, hoursBefore)
        return Result.success()
    }

    @SuppressLint("MissingPermission") // permission is verified by NotificationChannels.canPost above
    private fun show(context: Context, streak: Int, hoursBefore: Int) {
        NotificationChannels.ensureCreated(context)

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val (title, text) = StreakReminderTexts.forReminder(streak, hoursBefore)

        val notification = NotificationCompat.Builder(context, NotificationChannels.STREAK_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_summit)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BASE + hoursBefore, notification)
    }

    companion object {
        const val KEY_RESET_AT_MILLIS = "reset_at_millis"
        const val KEY_HOURS_BEFORE = "hours_before"
        private const val NOTIFICATION_ID_BASE = 1000
        private const val STALE_TOLERANCE_MILLIS = 60_000L
    }
}

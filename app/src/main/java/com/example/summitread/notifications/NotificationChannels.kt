package com.example.summitread.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** Central place for notification channels and the POST_NOTIFICATIONS check. */
object NotificationChannels {
    const val STREAK_REMINDERS = "streak_reminders"
    const val READING_TIMER = "reading_timer_channel"

    /** Idempotent: creates every channel (no-op below Android 8 and for existing channels). */
    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                STREAK_REMINDERS,
                "Streak Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders shortly before your reading streak would reset"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                READING_TIMER,
                "Reading Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows the active reading session timer"
            }
        )
    }

    /** True if a notification can actually be shown (permission granted on Android 13+, not blocked). */
    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}

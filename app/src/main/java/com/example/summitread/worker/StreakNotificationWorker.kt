package com.example.summitread.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.summitread.R
import com.example.summitread.data.UserPreferences
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class StreakNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val userPreferences = UserPreferences(context)
        val lastReadMillis = userPreferences.lastReadDate.first()
        val currentStreak = userPreferences.currentStreak.first()
        
        if (currentStreak == 0) return Result.success()

        val today = LocalDate.now()
        
        val lastReadDate = if (lastReadMillis > 0L) {
            Instant.ofEpochMilli(lastReadMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        } else {
            null
        }

        if (lastReadDate != null && lastReadDate.isBefore(today)) {
            // Remind the user to keep the streak alive
            sendNotification(currentStreak)
        }

        return Result.success()
    }

    private fun sendNotification(streak: Int) {
        val channelId = "streak_reminder_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Streak Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders to keep your reading streak alive"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Keep the fire alive! 🔥")
            .setContentText("You have a $streak day streak. Read today to keep it going!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }
}

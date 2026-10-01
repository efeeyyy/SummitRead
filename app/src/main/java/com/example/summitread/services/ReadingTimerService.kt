package com.example.summitread.services

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.summitread.MainActivity
import com.example.summitread.R
import com.example.summitread.data.ElapsedTimeFormat
import com.example.summitread.data.UserPreferences
import com.example.summitread.notifications.NotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class TimerStatus { IDLE, RUNNING, PAUSED }

/** Snapshot of the single, app-wide reading timer. It is always bound to one [bookId]. */
data class TimerState(
    val status: TimerStatus = TimerStatus.IDLE,
    val bookId: Long = -1L,
    val elapsedMillis: Long = 0L
)

/**
 * Foreground reading timer bound to a book. Supports real pause/resume (pause keeps the elapsed time).
 *
 * Time is measured with [SystemClock.elapsedRealtime] (no drift, keeps counting in Doze). When the
 * timer is stopped the elapsed time is persisted as a pending session in DataStore
 * ([UserPreferences.pendingTimerSession]) so it is never lost, even if the UI is not around to read it;
 * the Mountain screen of that book turns it into the suggested pages and stores it with the next log.
 */
class ReadingTimerService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main.immediate + serviceJob)
    private var tickJob: Job? = null

    private var status = TimerStatus.IDLE
    private var bookId = -1L
    private var accumulatedMillis = 0L
    private var runStartedRealtime = 0L
    private var stopping = false

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_BOOK_ID = "EXTRA_BOOK_ID"
        const val NOTIFICATION_CHANNEL_ID = NotificationChannels.READING_TIMER
        const val NOTIFICATION_ID = 1

        private val _state = MutableStateFlow(TimerState())
        /** Full timer snapshot (status, bound book, elapsed). */
        val state: StateFlow<TimerState> = _state.asStateFlow()

        private val _isRunning = MutableStateFlow(false)
        /** True only while actively counting (false when paused or idle). */
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _isPaused = MutableStateFlow(false)
        val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

        private val _activeBookId = MutableStateFlow(-1L)
        /** Book the timer is bound to, or -1 when idle. */
        val activeBookId: StateFlow<Long> = _activeBookId.asStateFlow()

        private val _elapsedTime = MutableStateFlow(0L)
        val elapsedTime: StateFlow<Long> = _elapsedTime.asStateFlow()

        /** Used only when the system destroys a live timer: persists its time outside the dying service. */
        private val persistScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        private fun publish(state: TimerState) {
            _state.value = state
            _isRunning.value = state.status == TimerStatus.RUNNING
            _isPaused.value = state.status == TimerStatus.PAUSED
            _activeBookId.value = if (state.status == TimerStatus.IDLE) -1L else state.bookId
            _elapsedTime.value = state.elapsedMillis
        }

        fun start(context: Context, bookId: Long) = send(context, ACTION_START) { putExtra(EXTRA_BOOK_ID, bookId) }
        fun pause(context: Context) = send(context, ACTION_PAUSE)
        fun resume(context: Context) = send(context, ACTION_RESUME)
        fun stop(context: Context) = send(context, ACTION_STOP)

        private fun send(context: Context, action: String, extras: Intent.() -> Unit = {}) {
            context.startService(
                Intent(context, ReadingTimerService::class.java).setAction(action).apply(extras)
            )
        }

        /** hh:mm:ss, hours are not capped. */
        fun formatElapsed(millis: Long): String = ElapsedTimeFormat.format(millis)
    }

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureCreated(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (stopping) return START_NOT_STICKY // a Stop is already being persisted
        when (intent?.action) {
            ACTION_START -> startTimer(intent.getLongExtra(EXTRA_BOOK_ID, -1L))
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
            ACTION_STOP -> stopTimer()
            else -> if (status == TimerStatus.IDLE) stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun currentElapsed(): Long = when (status) {
        TimerStatus.RUNNING -> accumulatedMillis + (SystemClock.elapsedRealtime() - runStartedRealtime)
        else -> accumulatedMillis
    }

    private fun publishState() {
        publish(TimerState(status, bookId, currentElapsed()))
    }

    private fun startTimer(newBookId: Long) {
        if (status != TimerStatus.IDLE) return // the single timer is already bound to a book
        if (newBookId <= 0L) {
            stopSelf()
            return
        }
        bookId = newBookId
        accumulatedMillis = 0L
        status = TimerStatus.RUNNING
        runStartedRealtime = SystemClock.elapsedRealtime()
        publishState()
        startInForeground(buildNotification())
        startTicking()
    }

    private fun pauseTimer() {
        if (status != TimerStatus.RUNNING) {
            if (status == TimerStatus.IDLE) stopSelf()
            return
        }
        tickJob?.cancel()
        accumulatedMillis = currentElapsed()
        status = TimerStatus.PAUSED
        publishState()
        notifyNow()
    }

    private fun resumeTimer() {
        if (status != TimerStatus.PAUSED) {
            if (status == TimerStatus.IDLE) stopSelf()
            return
        }
        status = TimerStatus.RUNNING
        runStartedRealtime = SystemClock.elapsedRealtime()
        publishState()
        notifyNow()
        startTicking()
    }

    private fun stopTimer() {
        if (status == TimerStatus.IDLE) {
            stopSelf()
            return
        }
        val elapsed = currentElapsed()
        val stoppedBookId = bookId
        stopping = true
        tickJob?.cancel()
        serviceScope.launch {
            // Persist first, so the time cannot be lost between the service dying and the UI reading it.
            if (elapsed >= 1000L) {
                try {
                    UserPreferences(applicationContext).addPendingTimerSession(stoppedBookId, elapsed)
                } catch (_: Exception) {
                    // best effort; nothing else to do
                }
            }
            status = TimerStatus.IDLE
            bookId = -1L
            accumulatedMillis = 0L
            stopping = false
            publish(TimerState())
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    /** Ticks on second boundaries of the measured elapsed time, so the display never drifts. */
    private fun startTicking() {
        tickJob?.cancel()
        tickJob = serviceScope.launch {
            while (isActive) {
                val elapsed = currentElapsed()
                publish(TimerState(status, bookId, elapsed))
                notifyNow()
                delay(1000L - elapsed % 1000L)
            }
        }
    }

    private fun startInForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun notifyNow() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun actionIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            this,
            requestCode,
            Intent(this, ReadingTimerService::class.java).setAction(action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val time = formatElapsed(currentElapsed())
        val paused = status == TimerStatus.PAUSED

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(if (paused) "Reading Timer Paused" else "Reading Timer Active")
            .setContentText(if (paused) "Paused $time" else time)
            .setSmallIcon(R.drawable.ic_stat_summit)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)

        if (paused) {
            builder.addAction(0, "Resume", actionIntent(ACTION_RESUME, 1))
        } else {
            builder.addAction(0, "Pause", actionIntent(ACTION_PAUSE, 2))
        }
        builder.addAction(0, "Stop", actionIntent(ACTION_STOP, 3))
        return builder.build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        // A live timer destroyed by the system (not through Stop): keep its time as a pending session.
        if (status != TimerStatus.IDLE) {
            val elapsed = currentElapsed()
            val destroyedBookId = bookId
            if (elapsed >= 1000L && destroyedBookId > 0L) {
                val prefs = UserPreferences(applicationContext)
                persistScope.launch {
                    try {
                        prefs.addPendingTimerSession(destroyedBookId, elapsed)
                    } catch (_: Exception) {
                        // best effort
                    }
                }
            }
        }
        status = TimerStatus.IDLE
        serviceJob.cancel()
        publish(TimerState())
    }
}

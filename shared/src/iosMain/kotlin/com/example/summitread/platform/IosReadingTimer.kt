package com.example.summitread.platform

import com.example.summitread.data.UserPreferences
import com.example.summitread.data.currentTimeMillis
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

/**
 * Timestamp based in-app reading timer for iOS (UNVERIFIED on a Mac).
 *
 * iOS has no foreground service, so the elapsed time is computed from wall-clock timestamps instead of counting
 * ticks: it stays correct while the app is suspended in the background. A local notification reminds the user if the
 * timer is still running after [FORGOTTEN_TIMER_SECONDS]; it is cancelled on pause/stop. On stop the elapsed time is
 * stored as the pending session of the book, exactly like the Android service does.
 *
 * Not handled yet (phase C): the state is in memory only, so a timer running when the OS kills the app is lost;
 * persisting `{bookId, accumulatedMillis, runStartedAtMillis}` in `UserPreferences` would fix that.
 */
class IosReadingTimer(
    private val preferences: UserPreferences,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
) : ReadingTimer {

    private val _state = MutableStateFlow(TimerState())
    override val state: StateFlow<TimerState> = _state.asStateFlow()

    private var tickJob: Job? = null
    private var accumulatedMillis = 0L
    private var runStartedAtMillis = 0L
    private var stopping = false

    private fun elapsed(current: TimerState = _state.value): Long = when (current.status) {
        TimerStatus.RUNNING -> accumulatedMillis + (currentTimeMillis() - runStartedAtMillis).coerceAtLeast(0L)
        else -> accumulatedMillis
    }

    override fun start(bookId: Long) {
        if (stopping || _state.value.status != TimerStatus.IDLE || bookId <= 0L) return
        accumulatedMillis = 0L
        runStartedAtMillis = currentTimeMillis()
        _state.value = TimerState(TimerStatus.RUNNING, bookId, 0L)
        scheduleForgottenTimerReminder()
        startTicking()
    }

    override fun pause() {
        val current = _state.value
        if (current.status != TimerStatus.RUNNING) return
        tickJob?.cancel()
        accumulatedMillis = elapsed(current)
        _state.value = TimerState(TimerStatus.PAUSED, current.bookId, accumulatedMillis)
        IosNotifications.cancel(listOf(FORGOTTEN_TIMER_ID))
    }

    override fun resume() {
        val current = _state.value
        if (current.status != TimerStatus.PAUSED) return
        runStartedAtMillis = currentTimeMillis()
        _state.value = TimerState(TimerStatus.RUNNING, current.bookId, accumulatedMillis)
        scheduleForgottenTimerReminder()
        startTicking()
    }

    override fun stop() {
        val current = _state.value
        if (current.status == TimerStatus.IDLE || stopping) return
        val total = elapsed(current)
        val bookId = current.bookId
        stopping = true
        tickJob?.cancel()
        IosNotifications.cancel(listOf(FORGOTTEN_TIMER_ID))
        scope.launch {
            // Persist first, so the time cannot be lost between the timer ending and the UI reading it.
            if (total >= 1000L) {
                try {
                    preferences.addPendingTimerSession(bookId, total)
                } catch (_: Exception) {
                    // best effort; nothing else to do
                }
            }
            accumulatedMillis = 0L
            _state.value = TimerState()
            stopping = false
        }
    }

    /** Ticks on second boundaries of the measured elapsed time, so the display never drifts. */
    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                val current = _state.value
                val total = elapsed(current)
                _state.value = TimerState(current.status, current.bookId, total)
                delay(1000L - total % 1000L)
            }
        }
    }

    private fun scheduleForgottenTimerReminder() {
        IosNotifications.schedule(
            identifier = FORGOTTEN_TIMER_ID,
            title = "Reading timer is still running",
            body = "Stop it in SummitRead when you are done so your reading time is saved correctly.",
            delaySeconds = FORGOTTEN_TIMER_SECONDS
        )
    }

    private companion object {
        const val FORGOTTEN_TIMER_ID = "reading_timer_running"
        const val FORGOTTEN_TIMER_SECONDS = 2.0 * 60.0 * 60.0
    }
}

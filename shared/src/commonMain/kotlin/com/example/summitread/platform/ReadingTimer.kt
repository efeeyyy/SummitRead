package com.example.summitread.platform

import kotlinx.coroutines.flow.StateFlow

enum class TimerStatus { IDLE, RUNNING, PAUSED }

/** Snapshot of the single, app-wide reading timer. It is always bound to one [bookId]. */
data class TimerState(
    val status: TimerStatus = TimerStatus.IDLE,
    val bookId: Long = -1L,
    val elapsedMillis: Long = 0L
)

/**
 * The single reading timer, bound to one book at a time, with real pause/resume.
 *
 * When the timer is stopped, the elapsed time is persisted as a pending session in
 * `UserPreferences.pendingTimerSession` (never lost); the Mountain screen of that book turns it into the
 * suggested pages and stores it with the next page log.
 *
 * - Android: foreground service (`ReadingTimerService`, notification with Pause/Resume/Stop), elapsed-realtime based.
 * - iOS: timestamp based in-app timer (wall clock, keeps counting while the app is suspended) plus a local
 *   "still running" notification (`IosReadingTimer`, unverified on a Mac).
 */
interface ReadingTimer {
    val state: StateFlow<TimerState>

    /** Starts the timer for [bookId]; ignored while the timer is already bound to a book. */
    fun start(bookId: Long)

    fun pause()

    fun resume()

    /** Stops the timer and stores the elapsed time as the pending session of its book. */
    fun stop()
}

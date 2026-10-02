package com.example.summitread.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlin.time.Clock

/** Wall-clock flows so "today" and "hours since" follow the real time while a screen stays open. */
object DayClock {
    private const val TICK_MILLIS = 60_000L

    /** Emits the current epoch millis immediately and then every minute. */
    fun minuteTicks(clock: Clock = Clock.System): Flow<Long> = flow {
        while (true) {
            emit(clock.now().toEpochMilliseconds())
            delay(TICK_MILLIS)
        }
    }

    /** Emits today's date immediately and again whenever the calendar day changes (checked each minute). */
    fun today(clock: Clock = Clock.System): Flow<LocalDate> = minuteTicks(clock)
        .map { StreakCalculator.toLocalDate(it) }
        .distinctUntilChanged()
}

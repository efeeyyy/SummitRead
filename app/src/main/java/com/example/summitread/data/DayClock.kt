package com.example.summitread.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Wall-clock flows so "today" and "hours since" follow the real time while a screen stays open. */
object DayClock {
    private const val TICK_MILLIS = 60_000L

    /** Emits the current epoch millis immediately and then every minute. */
    fun minuteTicks(): Flow<Long> = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(TICK_MILLIS)
        }
    }

    /** Emits today's date immediately and again whenever the calendar day changes (checked each minute). */
    fun today(): Flow<LocalDate> = minuteTicks()
        .map { StreakCalculator.toLocalDate(it) }
        .distinctUntilChanged()
}

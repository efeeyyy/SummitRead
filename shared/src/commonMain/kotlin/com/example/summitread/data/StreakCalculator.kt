package com.example.summitread.data

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Pure, calendar-day based streak maths (no platform types).
 *
 * A "logged day" is a calendar day (in [zone]) on which at least one reading session was logged for any book.
 * The streak is derived from the set of logged days, never stored, so a missed day reads as 0 immediately.
 */
object StreakCalculator {

    fun toLocalDate(millis: Long, zone: TimeZone = TimeZone.currentSystemDefault()): LocalDate =
        Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone).date

    fun startOfDayMillis(day: LocalDate, zone: TimeZone = TimeZone.currentSystemDefault()): Long =
        day.atStartOfDayIn(zone).toEpochMilliseconds()

    /** Latest logged day that is not in the future (future rows are ignored). */
    fun lastReadDay(loggedDays: Collection<LocalDate>, today: LocalDate): LocalDate? =
        loggedDays.filter { it <= today }.maxOrNull()

    /**
     * Length of the chain of consecutive logged days that is still alive on [today]:
     * it must contain today, or end yesterday (today can still be saved). Otherwise 0.
     */
    fun currentStreak(loggedDays: Collection<LocalDate>, today: LocalDate): Int {
        val days = loggedDays.toHashSet()
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        val anchor = when {
            today in days -> today
            yesterday in days -> yesterday
            else -> return 0
        }
        var count = 0
        var day = anchor
        while (day in days) {
            count++
            day = day.minus(1, DateTimeUnit.DAY)
        }
        return count
    }

    /**
     * Moment the streak breaks if nothing more is logged: local midnight that ends the day after
     * the last read day. Null when there is no live streak.
     */
    fun resetMoment(loggedDays: Collection<LocalDate>, today: LocalDate, zone: TimeZone): Instant? {
        val last = lastReadDay(loggedDays, today) ?: return null
        if (last < today.minus(1, DateTimeUnit.DAY)) return null
        return last.plus(2, DateTimeUnit.DAY).atStartOfDayIn(zone)
    }
}

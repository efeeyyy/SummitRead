package com.example.summitread.ui.profile

import com.example.summitread.data.PageMath
import com.example.summitread.data.db.NoteEntity
import com.example.summitread.data.db.ReadingSessionEntity
import kotlin.math.abs

/** Avatar evolution by completed books (#38): Tier 1 = 0-1, Tier 2 = 2-5, Tier 3 = 6+. */
enum class AvatarTier(val tierNumber: Int, val displayName: String, val minBooks: Int) {
    NOVICE(1, "Novice", 0),
    EXPEDITION_LEADER(2, "Expedition Leader", 2),
    ELITE_ALPINIST(3, "Elite Alpinist", 6);

    /** The next tier, or null at the top. */
    val next: AvatarTier? get() = entries.getOrNull(ordinal + 1)

    companion object {
        fun forCompletedBooks(completed: Int): AvatarTier =
            entries.last { completed >= it.minBooks }
    }
}

/** Pure helpers for the profile screen (free of Android types so they run in JVM tests). */
object ProfileLogic {

    const val TILT_THRESHOLD_DEGREES = 0.5f

    /** ClimbTime (#35): pages x 1.5 min, "N Days, M Hours Climbed". */
    fun formatClimbTime(pages: Int): String {
        val totalMinutes = (pages.coerceAtLeast(0) * PageMath.MINUTES_PER_PAGE).toInt()
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val mins = totalMinutes % 60
        val parts = buildList {
            if (days > 0) add(if (days == 1) "1 Day" else "$days Days")
            if (hours > 0) add(if (hours == 1) "1 Hour" else "$hours Hours")
            // Minutes only while the total is under a day, so the line stays short.
            if (mins > 0 && days == 0) add("$mins Min")
        }
        return if (parts.isEmpty()) "0 Hours Climbed" else parts.joinToString(", ") + " Climbed"
    }

    /** Real timed reading, compact: "3h 20m", "45m", "0m". Seconds are dropped (rounded down). */
    fun formatTimedDuration(millis: Long): String {
        val totalMinutes = millis.coerceAtLeast(0L) / 60_000L
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    }

    /** Fraction of the yearly goal reached, clamped to 0..1. */
    fun goalProgress(completedThisYear: Int, goal: Int): Float =
        (completedThisYear.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f)

    /** True when the tilt moved at least [thresholdDegrees] on either axis (#45, default 0.5 deg). */
    fun exceedsThreshold(
        oldPitch: Float,
        oldRoll: Float,
        newPitch: Float,
        newRoll: Float,
        thresholdDegrees: Float = TILT_THRESHOLD_DEGREES
    ): Boolean =
        abs(newPitch - oldPitch) >= thresholdDegrees || abs(newRoll - oldRoll) >= thresholdDegrees

    /** Sessions in chronological order (day, then real log time, then id). */
    fun sortedSessions(sessions: List<ReadingSessionEntity>): List<ReadingSessionEntity> =
        sessions.sortedWith(compareBy({ it.date }, { it.createdAtMillis }, { it.id }))

    /**
     * Groups notes under the session they belong to. A note appears under exactly one session;
     * notes without a (known) session end up in the second list ("Other notes").
     */
    fun groupNotes(
        sessions: List<ReadingSessionEntity>,
        notes: List<NoteEntity>
    ): Pair<Map<Long, List<NoteEntity>>, List<NoteEntity>> {
        val ids = sessions.map { it.id }.toSet()
        val bySession = notes
            .filter { it.sessionId != null && it.sessionId in ids }
            .groupBy { it.sessionId!! }
            .mapValues { (_, list) -> list.sortedBy { it.dateCreated } }
        val other = notes
            .filter { it.sessionId == null || it.sessionId !in ids }
            .sortedBy { it.dateCreated }
        return bySession to other
    }
}

package com.example.summitread.ui.mountain

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.example.summitread.data.db.ReadingSessionEntity
import kotlin.math.ceil

/** Mountain size class chosen from the page count (#16). */
enum class MountainTier(
    val peakYRatio: Float,
    val peakXRatio: Float,
    /** Trail control points as fractions of the (start -> peak) box; steeper tiers hug the summit line. */
    val c1x: Float,
    val c1y: Float,
    val c2x: Float,
    val c2y: Float
) {
    /** < 200 pages: a small, rounded hill. */
    HILL(peakYRatio = 0.52f, peakXRatio = 0.62f, c1x = 0.40f, c1y = 0.10f, c2x = 0.65f, c2y = 0.12f),

    /** 200..399 pages: a standard mountain. */
    STANDARD(peakYRatio = 0.34f, peakXRatio = 0.68f, c1x = 0.35f, c1y = 0.12f, c2x = 0.75f, c2y = 0.35f),

    /** 400+ pages: a tall, steep, Everest-like summit. */
    EVEREST(peakYRatio = 0.20f, peakXRatio = 0.72f, c1x = 0.30f, c1y = 0.05f, c2x = 0.90f, c2y = 0.50f);

    companion object {
        fun forPages(totalPages: Int): MountainTier = when {
            totalPages >= 400 -> EVEREST
            totalPages >= 200 -> STANDARD
            else -> HILL
        }
    }
}

/** Sky colours for a time of day; [night] is 0 in full daylight and 1 in deep midnight. */
data class SkyColors(val top: Color, val bottom: Color, val night: Float)

/** Temporal sky (#18): dawn, daylight, twilight and deep-midnight gradients blended smoothly by the hour. */
object SkyModel {
    private class Key(val hour: Float, val top: Color, val bottom: Color, val night: Float)

    // Refined palette: cool zenith, warm horizon (soft and desaturated)
    private val midnightTop = Color(0xFF0A1024)
    private val midnightBottom = Color(0xFF1F2B49)
    private val dawnTop = Color(0xFF7C8FD0)
    private val dawnBottom = Color(0xFFFFD6B8)
    private val dayTop = Color(0xFF4F9BEA)
    private val dayBottom = Color(0xFFCFE8FF)
    private val twilightTop = Color(0xFF3E3F86)
    private val twilightBottom = Color(0xFFF4A58C)

    private val keys = listOf(
        Key(0f, midnightTop, midnightBottom, 1f),
        Key(5f, midnightTop, midnightBottom, 1f),
        Key(6.5f, dawnTop, dawnBottom, 0.25f),
        Key(9f, dawnTop, dawnBottom, 0f),
        Key(10.5f, dayTop, dayBottom, 0f),
        Key(15f, dayTop, dayBottom, 0f),
        Key(16.5f, twilightTop, twilightBottom, 0.1f),
        Key(19f, twilightTop, twilightBottom, 0.35f),
        Key(21f, midnightTop, midnightBottom, 1f),
        Key(24f, midnightTop, midnightBottom, 1f)
    )

    /** [hour] is the fractional hour of day, 0 until 24. */
    fun skyAt(hour: Float): SkyColors {
        val h = hour.coerceIn(0f, 24f)
        val next = keys.indexOfFirst { it.hour >= h }.coerceAtLeast(1)
        val a = keys[next - 1]
        val b = keys[next]
        val span = (b.hour - a.hour).coerceAtLeast(0.0001f)
        val f = ((h - a.hour) / span).coerceIn(0f, 1f)
        return SkyColors(
            top = lerp(a.top, b.top, f),
            bottom = lerp(a.bottom, b.bottom, f),
            night = a.night + (b.night - a.night) * f
        )
    }

    /** The sun is up between 06:00 and 19:00. */
    fun isSunUp(hour: Float): Boolean = hour in 6f..19f

    /** 0 at sunrise, 1 at sunset. */
    fun sunArc(hour: Float): Float = ((hour - 6f) / 13f).coerceIn(0f, 1f)
}

/** Weather rule (#20): a storm sets in after more than 36 hours without a logged session. */
object WeatherRules {
    const val STORM_AFTER_HOURS = 36L
    fun isStormy(hoursSinceLastSession: Long): Boolean = hoursSinceLastSession > STORM_AFTER_HOURS
}

/** Intermediate stats shown when a camp (25 / 50 / 75 %) is tapped (#22). */
data class MilestoneStats(
    val fraction: Float,
    val pagesAtMilestone: Int,
    val reached: Boolean,
    val pagesToGo: Int,
    /** Day (start-of-day millis) on which the camp was reached, null if not reached or unknown. */
    val reachedOnMillis: Long?,
    val sessionsToReach: Int,
    val timeSpentMillis: Long
) {
    companion object {
        fun compute(
            fraction: Float,
            totalPages: Int,
            currentPages: Int,
            sessions: List<ReadingSessionEntity>
        ): MilestoneStats {
            val pagesAt = ceil(totalPages * fraction.toDouble()).toInt().coerceIn(0, totalPages)
            val reached = totalPages > 0 && currentPages >= pagesAt
            var cumulative = 0
            var time = 0L
            var count = 0
            var reachedOn: Long? = null
            if (reached) {
                for (s in sessions.sortedWith(compareBy({ it.date }, { it.id }))) {
                    cumulative += s.pagesRead
                    time += s.timeSpentMillis
                    count++
                    if (cumulative >= pagesAt) {
                        reachedOn = s.date
                        break
                    }
                }
                if (reachedOn == null) { // pages were not backed by sessions (legacy data)
                    count = sessions.size
                    time = sessions.sumOf { it.timeSpentMillis }
                }
            } else {
                count = sessions.size
                time = sessions.sumOf { it.timeSpentMillis }
            }
            return MilestoneStats(
                fraction = fraction,
                pagesAtMilestone = pagesAt,
                reached = reached,
                pagesToGo = (pagesAt - currentPages).coerceAtLeast(0),
                reachedOnMillis = reachedOn,
                sessionsToReach = count,
                timeSpentMillis = time
            )
        }
    }
}

/** Motivational quotes per camp; the pick is stable per book. */
object MilestoneQuotes {
    private val quarter = listOf(
        "Every great ascent begins with a single page. Camp one is behind you.",
        "The first quarter is the hardest to start. You already started.",
        "Steady steps beat sudden sprints. Keep this pace."
    )
    private val half = listOf(
        "Halfway up, the view already changes everything. Do not stop now.",
        "You have climbed as far as you have left to climb. Breathe, then go on.",
        "The ridge is in sight. Pages turn faster than you think."
    )
    private val threeQuarters = listOf(
        "The summit is close enough to feel. One more push.",
        "Thin air, clear mind. The last quarter is yours.",
        "Almost there. The best view is saved for those who finish."
    )

    fun quoteFor(fraction: Float, bookId: Long): String {
        val list = when {
            fraction < 0.4f -> quarter
            fraction < 0.6f -> half
            else -> threeQuarters
        }
        return list[(bookId.mod(list.size.toLong())).toInt()]
    }

    fun campName(fraction: Float): String = when {
        fraction < 0.4f -> "Camp I"
        fraction < 0.6f -> "Camp II"
        else -> "Camp III"
    }
}

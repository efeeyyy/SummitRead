package com.example.summitread.ui.mountain

import android.graphics.Paint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.summitread.data.db.NoteEntity
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

private val MILESTONE_FRACTIONS = floatArrayOf(0.25f, 0.5f, 0.75f)

/**
 * Trail, body, trees and sample points of one mountain for a given canvas size. Built once per size/tier
 * (never per frame); the PathMeasure is only used from the main thread.
 */
class MountainGeometry(
    val width: Float,
    val height: Float,
    val unit: Float,
    val tier: MountainTier
) {
    val groundY = height * 0.88f
    val start = Offset(width * 0.08f, groundY)
    val peak = Offset(width * tier.peakXRatio, height * tier.peakYRatio)
    val end = Offset(width * 0.96f, groundY)

    val trail = Path().apply {
        val dx = peak.x - start.x
        val dy = start.y - peak.y
        moveTo(start.x, start.y)
        cubicTo(
            start.x + dx * tier.c1x, start.y - dy * tier.c1y,
            start.x + dx * tier.c2x, peak.y + dy * tier.c2y,
            peak.x, peak.y
        )
    }

    val body = Path().apply {
        val dx = peak.x - start.x
        val dy = start.y - peak.y
        moveTo(start.x, start.y)
        cubicTo(
            start.x + dx * tier.c1x, start.y - dy * tier.c1y,
            start.x + dx * tier.c2x, peak.y + dy * tier.c2y,
            peak.x, peak.y
        )
        cubicTo(
            peak.x + (end.x - peak.x) * 0.15f, peak.y + dy * 0.30f,
            peak.x + (end.x - peak.x) * 0.60f, groundY - dy * 0.20f,
            end.x, end.y
        )
        lineTo(end.x, groundY + 2f)
        lineTo(start.x, groundY + 2f)
        close()
    }

    /** Distant ridge behind the main mountain, for depth. */
    val ridge = Path().apply {
        moveTo(-width * 0.05f, groundY)
        quadraticTo(width * 0.16f, groundY - (groundY - peak.y) * 0.95f, width * 0.40f, groundY)
        quadraticTo(width * 0.70f, groundY - (groundY - peak.y) * 0.70f, width * 1.05f, groundY)
        close()
    }

    /** Upward-pointing trees along the base line (#24): the apex is above the base. */
    val trees = Path().apply {
        for (i in 0..5) {
            val tx = start.x + (width * 0.80f * (i / 5f))
            moveTo(tx, groundY - 30f * unit)
            lineTo(tx - 10f * unit, groundY - 8f * unit)
            lineTo(tx + 10f * unit, groundY - 8f * unit)
            close()
            moveTo(tx, groundY - 20f * unit)
            lineTo(tx - 13f * unit, groundY)
            lineTo(tx + 13f * unit, groundY)
            close()
        }
    }

    private val measure = PathMeasure().apply { setPath(trail, false) }
    val length: Float = measure.length

    fun positionAt(fraction: Float): Offset = measure.getPosition(length * fraction.coerceIn(0f, 1f))

    fun tangentDegAt(fraction: Float): Float {
        val t = measure.getTangent(length * fraction.coerceIn(0f, 1f))
        return Math.toDegrees(atan2(t.y.toDouble(), t.x.toDouble())).toFloat()
    }

    fun segment(fraction: Float, dest: Path) {
        dest.reset()
        measure.getSegment(0f, length * fraction.coerceIn(0f, 1f), dest, true)
    }

    val milestones: List<Pair<Float, Offset>> = MILESTONE_FRACTIONS.map { it to positionAt(it) }
}

/** Shooting-star state machine (#19): random spawn times and positions, updated from the frame loop. */
private class ShootingStar(private val random: Random) {
    var active = false
    var sx = 0f
    var sy = 0f
    var dx = 0f
    var dy = 0f
    var age = 0f
    private var nextAt = 4f
    private var lastTime = 0f
    val duration = 0.9f

    fun update(time: Float, night: Float, width: Float, height: Float) {
        val dt = (time - lastTime).coerceIn(0f, 0.25f)
        lastTime = time
        if (night < 0.6f) {
            active = false
            return
        }
        if (active) {
            age += dt
            if (age >= duration) {
                active = false
                nextAt = time + 3f + random.nextFloat() * 8f
            }
        } else if (time >= nextAt) {
            active = true
            age = 0f
            sx = width * (0.05f + random.nextFloat() * 0.7f)
            sy = height * (0.02f + random.nextFloat() * 0.25f)
            val angle = (20f + random.nextFloat() * 25f) * (PI.toFloat() / 180f)
            val distance = width * (0.30f + random.nextFloat() * 0.25f)
            dx = cos(angle) * distance
            dy = sin(angle) * distance
        }
    }
}

private class SkyFrameInput(var night: Float = 1f, var width: Float = 1f, var height: Float = 1f)

/** Fractional hour of day, re-read every 30 seconds so the sky follows the clock while the screen is open. */
@Composable
private fun rememberHourOfDay(): State<Float> = produceState(initialValue = currentHour()) {
    while (true) {
        delay(30_000)
        value = currentHour()
    }
}

private fun currentHour(): Float {
    val now = LocalTime.now()
    return now.hour + now.minute / 60f
}

private class EmojiPaints(unit: Float) {
    val flag = emojiPaint(22f * unit)
    val tent = emojiPaint(20f * unit)
    val camp = emojiPaint(32f * unit)

    private fun emojiPaint(size: Float) = Paint().apply {
        textSize = size
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
}

/**
 * The mountain scene: temporal sky, sun/moon, stars, drifting clouds, weather, the mountain with its trail,
 * milestone camps and the climber. All animation runs from one `withFrameNanos` clock that is only read while
 * drawing, so the scene never recomposes per frame; paths, brushes, paints and random data are remembered.
 */
@Composable
fun MountainCanvas(
    progress: Float,
    notes: List<NoteEntity>,
    totalPages: Int,
    isFocusMode: Boolean,
    accentColor: Color,
    groundColor: Color,
    isDark: Boolean,
    hoursSinceLastSession: Long,
    streakActive: Boolean,
    isReadingTimerRunning: Boolean,
    onSignpostClick: (NoteEntity) -> Unit,
    onMilestoneClick: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val target = progress.coerceIn(0f, 1f)
    val unit = LocalDensity.current.density
    val tier = MountainTier.forPages(totalPages)

    // Climb: walk up from the base on entry, then from the old to the new progress after logging pages.
    val climb = remember { Animatable(0f) }
    LaunchedEffect(target) {
        val delta = kotlin.math.abs(target - climb.value)
        if (delta < 0.001f) return@LaunchedEffect
        if (climb.value == 0f) delay(500)
        climb.animateTo(target, tween((700 + 2200 * delta).toInt(), easing = FastOutSlowInEasing))
    }

    // Focus mode: zoom (scale + pivot at the climber) and swap the climber for a tent
    val focusAmount by animateFloatAsState(
        targetValue = if (isFocusMode) 1f else 0f,
        animationSpec = tween(600),
        label = "focus_amount"
    )

    // Weather: storm after > 36 h without a log; when pages are logged it is blown away by the wind.
    val isStorm = WeatherRules.isStormy(hoursSinceLastSession)
    val stormLevel = remember { Animatable(if (isStorm) 1f else 0f) }
    val wind = remember { Animatable(0f) }
    LaunchedEffect(isStorm) {
        if (isStorm) {
            wind.snapTo(0f)
            stormLevel.animateTo(1f, tween(2500))
        } else if (stormLevel.value > 0f) {
            coroutineScope {
                launch { wind.animateTo(1f, tween(2600, easing = FastOutLinearInEasing)) }
                launch { stormLevel.animateTo(0f, tween(2600, delayMillis = 300)) }
            }
            wind.snapTo(0f)
        }
    }

    val hour by rememberHourOfDay()
    val sky = remember(hour) { SkyModel.skyAt(hour) }
    val dim = if (isDark) 0.28f * (1f - sky.night) else 0f
    val skyTop = lerp(sky.top, Color.Black, dim)
    val skyBottom = lerp(sky.bottom, Color.Black, dim)
    val shade = max(sky.night, if (isDark) 0.4f else 0f)
    val snowColor = lerp(Color.White, Color(0xFF9FB3C8), shade)
    val midColor = lerp(Color(0xFFCFD8DC), Color(0xFF4A5568), shade)

    val frameInput = remember { SkyFrameInput() }
    val shootingStar = remember { ShootingStar(Random.Default) }
    val clock = remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        var startNanos = -1L
        while (true) {
            withFrameNanos { now ->
                if (startNanos < 0L) startNanos = now
                clock.longValue = now - startNanos
                shootingStar.update(
                    clock.longValue / 1_000_000_000f,
                    frameInput.night,
                    frameInput.width,
                    frameInput.height
                )
            }
        }
    }

    // Static random data computed once
    val starCount = 60
    val stars = remember {
        val r = Random(42)
        FloatArray(starCount * 5) { i ->
            when (i % 5) {
                0 -> r.nextFloat()               // x fraction
                1 -> r.nextFloat() * 0.62f       // y fraction
                2 -> r.nextFloat() * 6.28f       // twinkle phase
                3 -> 1f + r.nextFloat() * 3f     // twinkle speed
                else -> 0.8f + r.nextFloat() * 1.4f // radius (dp)
            }
        }
    }
    // x fraction, y fraction, scale, speed (dp/s)
    val clouds = remember {
        floatArrayOf(
            0.05f, 0.14f, 1.2f, 7f,
            0.35f, 0.26f, 0.9f, 10f,
            0.62f, 0.10f, 1.5f, 5f,
            0.85f, 0.34f, 0.8f, 12f,
            0.20f, 0.42f, 1.0f, 8f
        )
    }
    val paints = remember(unit) { EmojiPaints(unit) }
    val climbedPath = remember { Path() }
    val trailStroke = remember(unit) {
        Stroke(width = 3f * unit, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f * unit, 12f * unit), 0f))
    }
    val climbedStroke = remember(unit) { Stroke(width = 3.5f * unit, cap = StrokeCap.Round) }

    BoxWithConstraints(modifier = modifier.clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp))) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val geometry = remember(widthPx, heightPx, unit, tier) { MountainGeometry(widthPx, heightPx, unit, tier) }
        SideEffect {
            frameInput.night = sky.night
            frameInput.width = widthPx
            frameInput.height = heightPx
        }

        val skyBrush = remember(skyTop, skyBottom, geometry) {
            Brush.verticalGradient(listOf(skyTop, skyBottom), startY = 0f, endY = geometry.groundY)
        }
        val mountainBrush = remember(snowColor, midColor, accentColor, geometry) {
            Brush.verticalGradient(
                colors = listOf(snowColor, midColor, accentColor),
                startY = geometry.peak.y,
                endY = geometry.groundY
            )
        }
        val ridgeColor = remember(midColor, skyBottom) { lerp(midColor, skyBottom, 0.55f) }

        val notePositions = remember(geometry, notes, totalPages) {
            if (totalPages <= 0) emptyList()
            else notes.map { note ->
                note to geometry.positionAt((note.pageNumber.toFloat() / totalPages).coerceIn(0f, 1f))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val f = focusAmount
                    val s = 1f + 0.7f * f
                    val p = geometry.positionAt(climb.value)
                    transformOrigin = TransformOrigin(p.x / size.width, p.y / size.height)
                    scaleX = s
                    scaleY = s
                    translationX = (size.width * 0.5f - p.x)
                        .coerceIn((1f - s) * (size.width - p.x), (s - 1f) * p.x)
                    translationY = (size.height * 0.55f - p.y)
                        .coerceIn((1f - s) * (size.height - p.y), (s - 1f) * p.y)
                    clip = true
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val t = clock.longValue / 1_000_000_000f
                val w = size.width
                val h = size.height
                val g = geometry
                val night = sky.night
                val storm = stormLevel.value
                val windPush = wind.value

                // Sky
                drawRect(brush = skyBrush)

                // Stars (only at night)
                if (night > 0.02f) {
                    for (i in 0 until starCount) {
                        val o = i * 5
                        val twinkle = 0.45f + 0.55f * sin(t * stars[o + 3] + stars[o + 2])
                        drawCircle(
                            color = Color.White.copy(alpha = (twinkle * night).coerceIn(0f, 1f)),
                            radius = stars[o + 4] * unit,
                            center = Offset(stars[o] * w, stars[o + 1] * g.groundY)
                        )
                    }
                    if (shootingStar.active) {
                        val p = shootingStar.age / shootingStar.duration
                        val hx = shootingStar.sx + shootingStar.dx * p
                        val hy = shootingStar.sy + shootingStar.dy * p
                        val fade = (1f - p) * night
                        val len = 0.22f
                        drawLine(
                            color = Color.White.copy(alpha = fade),
                            start = Offset(hx - shootingStar.dx * len, hy - shootingStar.dy * len),
                            end = Offset(hx, hy),
                            strokeWidth = 2.2f * unit,
                            cap = StrokeCap.Round
                        )
                        drawCircle(Color.White.copy(alpha = fade), 2.4f * unit, Offset(hx, hy))
                    }
                }

                // Sun by day (pulsing), moon by night
                if (SkyModel.isSunUp(hour) && night < 0.8f) {
                    val arc = SkyModel.sunArc(hour)
                    val c = Offset(w * (0.12f + 0.76f * arc), g.groundY * (0.42f - 0.30f * sin(arc * PI.toFloat())))
                    val pulse = 1f + 0.07f * sin(t * 2.2f)
                    val a = 1f - night
                    drawCircle(Color(0xFFFFD54F).copy(alpha = 0.16f * a), 78f * unit * pulse, c)
                    drawCircle(Color(0xFFFFD54F).copy(alpha = 0.26f * a), 56f * unit * pulse, c)
                    drawCircle(Color(0xFFFFCA28).copy(alpha = a), 26f * unit, c)
                } else if (night > 0.5f) {
                    val nightHour = if (hour < 12f) hour + 24f else hour
                    val arc = ((nightHour - 19f) / 11f).coerceIn(0f, 1f)
                    val c = Offset(w * (0.12f + 0.76f * arc), g.groundY * (0.40f - 0.28f * sin(arc * PI.toFloat())))
                    drawCircle(Color(0xFFEEF2F7).copy(alpha = 0.12f * night), 40f * unit, c)
                    drawCircle(Color(0xFFEEF2F7).copy(alpha = night), 17f * unit, c)
                }

                // Storm tint on the sky
                if (storm > 0.01f) {
                    drawRect(Color(0xFF263238).copy(alpha = 0.30f * storm), size = Size(w, g.groundY))
                }

                // Clouds drifting continuously (#21)
                val cloudAlpha = lerp(
                    Color.White.copy(alpha = if (streakActive) 0.55f else 0.75f),
                    Color(0xFFB0BEC5).copy(alpha = 0.16f),
                    night
                )
                for (i in 0 until 5) {
                    val o = i * 4
                    val cs = clouds[o + 2] * unit
                    val span = w + 120f * cs
                    val cx = (clouds[o] * span + t * clouds[o + 3] * unit) % span - 60f * cs
                    drawCloud(cx, clouds[o + 1] * g.groundY, cs, cloudAlpha)
                }

                // Distant ridge + mountain
                drawPath(g.ridge, ridgeColor.copy(alpha = 0.55f))
                drawPath(g.body, mountainBrush)

                // Ground fills the strip below the base, so no empty rectangle remains under the canvas
                drawRect(groundColor, topLeft = Offset(0f, g.groundY), size = Size(w, h - g.groundY))

                // Trail: dashed full route, solid climbed part
                drawPath(g.trail, Color.White.copy(alpha = 0.5f), style = trailStroke)
                val shown = climb.value
                if (shown > 0.001f) {
                    g.segment(shown, climbedPath)
                    drawPath(climbedPath, Color.White.copy(alpha = 0.95f), style = climbedStroke)
                }

                // Trees at the base (apex up)
                drawPath(g.trees, Color(0xFF1B5E20))

                // Summit flag
                drawContext.canvas.nativeCanvas.drawText("🚩", g.peak.x + 5f * unit, g.peak.y + 3f * unit, paints.flag)

                // Milestone tents
                for (i in g.milestones.indices) {
                    val m = g.milestones[i]
                    paints.tent.alpha = if (target >= m.first) 255 else 140
                    drawContext.canvas.nativeCanvas.drawText("⛺", m.second.x, m.second.y + 2f * unit, paints.tent)
                }

                // Storm clouds + fog around the summit (#20); wind blows them away when pages are logged
                if (storm > 0.01f) {
                    val push = windPush * w * 1.4f
                    val sway = sin(t * 0.5f) * 12f * unit
                    val stormColor = Color(0xFF37474F).copy(alpha = 0.88f * storm)
                    drawCloud(g.peak.x - 40f * unit + sway + push, g.peak.y - 8f * unit, 1.9f * unit, stormColor)
                    drawCloud(g.peak.x + 36f * unit - sway + push * 1.15f, g.peak.y - 22f * unit, 1.6f * unit, stormColor)
                    drawCloud(g.peak.x - 110f * unit + sway * 0.5f + push * 0.9f, g.peak.y + 10f * unit, 1.4f * unit,
                        Color(0xFF455A64).copy(alpha = 0.8f * storm))
                    val fog = Color(0xFFCFD8DC).copy(alpha = 0.30f * storm * (1f - 0.6f * windPush))
                    for (i in 0..3) {
                        val fy = g.peak.y + (g.groundY - g.peak.y) * (0.08f + 0.2f * i)
                        val fx = ((t * (6f + 3f * i) * unit) % (w * 0.5f)) - w * 0.25f + push
                        drawOval(fog, Offset(fx + w * 0.1f * i - 30f * unit, fy), Size(w * 0.7f, 22f * unit))
                    }
                    if (windPush > 0.02f) {
                        val streak = Color.White.copy(alpha = 0.5f * sin(windPush * PI.toFloat()))
                        for (i in 0..5) {
                            val sy = g.peak.y - 20f * unit + i * 18f * unit
                            val sx = (windPush * w * 1.6f + i * w * 0.13f) % (w * 1.2f) - w * 0.1f
                            drawLine(streak, Offset(sx, sy), Offset(sx + 46f * unit, sy - 3f * unit), 1.6f * unit, StrokeCap.Round)
                        }
                    }
                }

                // Climber: stick figure on Canvas; becomes a tent in focus mode (#23)
                val camp = focusAmount
                val p = g.positionAt(shown)
                val cx = p.x.coerceIn(10f * unit, w - 10f * unit)
                val cy = p.y.coerceIn(40f * unit, h)
                val climbing = climb.isRunning || isReadingTimerRunning
                if (camp < 0.99f) {
                    drawClimber(
                        x = cx,
                        y = cy,
                        unit = unit,
                        tangentDeg = g.tangentDegAt(shown),
                        time = t,
                        state = if (climbing) ClimbingState.CLIMBING else ClimbingState.IDLE,
                        alpha = 1f - camp
                    )
                }
                if (camp > 0.01f) {
                    paints.camp.alpha = (camp * 255).toInt()
                    drawContext.canvas.nativeCanvas.drawText("⛺", cx, cy + 2f * unit, paints.camp)
                }
            }

            // Touch targets: same px coordinates as the canvas, inside the same zoom layer, dp-sized
            val half = with(LocalDensity.current) { 24.dp.roundToPx() }
            notePositions.forEach { (note, pos) ->
                Box(
                    modifier = Modifier
                        .offset { IntOffset(pos.x.toInt() - half, pos.y.toInt() - 2 * half) }
                        .size(48.dp)
                        .semantics { contentDescription = "Note signpost, page ${note.pageNumber}" }
                        .clickable(role = Role.Button, enabled = !isFocusMode) { onSignpostClick(note) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("🪧", fontSize = 22.sp)
                }
            }
            geometry.milestones.forEach { (fraction, pos) ->
                val percent = (fraction * 100).toInt()
                Box(
                    modifier = Modifier
                        .offset { IntOffset(pos.x.toInt() - half, pos.y.toInt() - half - half / 2) }
                        .size(48.dp)
                        .semantics { contentDescription = "Camp at $percent percent" }
                        .clickable(role = Role.Button, enabled = !isFocusMode) { onMilestoneClick(fraction) }
                )
            }
        }
    }
}

/** Soft cloud: three puffs on a flat base. [s] is the size in px per unit. */
private fun DrawScope.drawCloud(cx: Float, cy: Float, s: Float, color: Color) {
    drawCircle(color, 12f * s, Offset(cx - 18f * s, cy + 2f * s))
    drawCircle(color, 16f * s, Offset(cx, cy - 6f * s))
    drawCircle(color, 13f * s, Offset(cx + 18f * s, cy + 1f * s))
    drawRoundRect(
        color,
        topLeft = Offset(cx - 30f * s, cy + 2f * s),
        size = Size(60f * s, 12f * s),
        cornerRadius = CornerRadius(6f * s, 6f * s)
    )
}

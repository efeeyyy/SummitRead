package com.example.summitread.ui.mountain

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.summitread.data.db.NoteEntity
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
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
        lineTo(end.x, groundY + 12f * unit)
        lineTo(start.x, groundY + 12f * unit)
        close()
    }

    /** Distant ridge behind the main mountain, for depth. */
    val ridge = Path().apply {
        moveTo(-width * 0.05f, groundY)
        quadraticTo(width * 0.16f, groundY - (groundY - peak.y) * 0.95f, width * 0.40f, groundY)
        quadraticTo(width * 0.70f, groundY - (groundY - peak.y) * 0.70f, width * 1.05f, groundY)
        lineTo(width * 1.05f, groundY + 12f * unit)
        lineTo(-width * 0.05f, groundY + 12f * unit)
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

    /** Rolling foothills in front of the base: replaces the old flat ground band. */
    val foreground = Path().apply {
        val top = groundY - 4f * unit
        moveTo(0f, top + 6f * unit)
        cubicTo(width * 0.28f, top - 9f * unit, width * 0.58f, top + 9f * unit, width, top - 3f * unit)
        lineTo(width, height)
        lineTo(0f, height)
        close()
    }

    private val measure = PathMeasure().apply { setPath(trail, false) }
    val length: Float = measure.length

    fun positionAt(fraction: Float): Offset = measure.getPosition(length * fraction.coerceIn(0f, 1f))

    fun tangentDegAt(fraction: Float): Float {
        val t = measure.getTangent(length * fraction.coerceIn(0f, 1f))
        return (atan2(t.y.toDouble(), t.x.toDouble()) * (180.0 / PI)).toFloat()
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
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
    return now.hour + now.minute / 60f
}

private val EmojiBrush = SolidColor(Color.Black)

/** Emoji drawn with text layouts measured once (no `android.graphics.Paint`); sizes are in px, like the old paints. */
private class EmojiLayouts(measurer: TextMeasurer, density: Density, unit: Float) {
    val flag = emoji(measurer, density, "🚩", 22f * unit)
    val tent = emoji(measurer, density, "⛺", 20f * unit)
    val camp = emoji(measurer, density, "⛺", 32f * unit)

    private fun emoji(measurer: TextMeasurer, density: Density, text: String, sizePx: Float): TextLayoutResult =
        measurer.measure(text, TextStyle(fontSize = with(density) { sizePx.toSp() }))
}

/** Draws [layout] horizontally centred on [x] with its baseline at [y] (same anchor as `Paint.Align.CENTER`). */
private fun DrawScope.drawEmoji(layout: TextLayoutResult, x: Float, y: Float, alpha: Float) {
    drawText(layout, EmojiBrush, Offset(x - layout.size.width / 2f, y - layout.firstBaseline), alpha)
}

/**
 * Vertical position (fraction of the canvas height) the climber is moved to in focus mode: above the notes
 * sheet, which covers the lower part of the canvas.
 */
const val FOCUS_FOCAL_Y = 0.40f

/**
 * The mountain scene: temporal sky, sun/moon, stars, drifting clouds, weather, the mountain with its trail,
 * milestone camps and the climber. All animation runs from one `withFrameNanos` clock that is only read while
 * drawing, so the scene never recomposes per frame; paths, brushes, paints and random data are remembered.
 *
 * The canvas is full-bleed: it extends under the status bar and the glass header. [topInset] is the part of
 * the canvas covered by them: the sky, stars and clouds fill it, while the mountain scene itself is laid out
 * below it so the summit never hides behind the header.
 */
@Composable
fun MountainCanvas(
    progress: Float,
    notes: List<NoteEntity>,
    totalPages: Int,
    isFocusMode: Boolean,
    accentColor: Color,
    isDark: Boolean,
    hoursSinceLastSession: Long,
    streakActive: Boolean,
    isReadingTimerRunning: Boolean,
    topInset: Dp,
    onSignpostClick: (NoteEntity) -> Unit,
    onMilestoneClick: (Float) -> Unit,
    onSkyLightnessChange: (Boolean) -> Unit,
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
    val snowColor = lerp(Color(0xFFFFFFFF), Color(0xFF9FB3C8), shade)
    val midColor = lerp(Color(0xFFB9C4D2), Color(0xFF3F4B63), shade)
    val baseColor = lerp(accentColor, Color(0xFF0F1A2A), shade * 0.5f)
    val foreColor = lerp(lerp(accentColor, Color(0xFF14261B), 0.55f), Color.Black, shade * 0.45f)
    val treeColor = lerp(foreColor, Color.Black, 0.3f)

    // The status bar icons must stay readable on the sky: report whether the top of the sky is light
    val skyIsLight = skyTop.luminance() > 0.45f
    LaunchedEffect(skyIsLight) { onSkyLightnessChange(skyIsLight) }

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
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val emojis = remember(unit, textMeasurer) { EmojiLayouts(textMeasurer, density, unit) }
    val climbedPath = remember { Path() }
    val trailStroke = remember(unit) {
        Stroke(width = 2.5f * unit, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f * unit, 10f * unit), 0f))
    }
    val climbedStroke = remember(unit) { Stroke(width = 3.5f * unit, cap = StrokeCap.Round) }
    val climbedGlowStroke = remember(unit) { Stroke(width = 8f * unit, cap = StrokeCap.Round) }

    val progressPercent = (target * 100).toInt()
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .semantics { contentDescription = "Mountain: $progressPercent percent climbed" }
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val insetPx = with(LocalDensity.current) { topInset.toPx() }.coerceIn(0f, heightPx * 0.6f)
        val geometry = remember(widthPx, heightPx, insetPx, unit, tier) {
            MountainGeometry(widthPx, heightPx - insetPx, unit, tier)
        }
        val skyHeight = insetPx + geometry.groundY
        SideEffect {
            frameInput.night = sky.night
            frameInput.width = widthPx
            frameInput.height = skyHeight
        }

        val skyBrush = remember(skyTop, skyBottom, skyHeight) {
            Brush.verticalGradient(listOf(skyTop, skyBottom), startY = 0f, endY = skyHeight)
        }
        val mountainBrush = remember(snowColor, midColor, baseColor, geometry) {
            Brush.verticalGradient(
                colors = listOf(snowColor, midColor, baseColor),
                startY = geometry.peak.y,
                endY = geometry.groundY
            )
        }
        // Light comes from the left: the far side of the mountain falls into soft shadow
        val shadowBrush = remember(geometry) {
            Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.22f)),
                startX = geometry.peak.x - geometry.width * 0.04f,
                endX = geometry.end.x
            )
        }
        val ridgeColor = remember(midColor, skyBottom) { lerp(midColor, skyBottom, 0.55f) }
        // Valley mist between the mountain and the foothills
        val mistBrush = remember(skyBottom, geometry) {
            val mist = lerp(Color.White, skyBottom, 0.5f)
            Brush.verticalGradient(
                colors = listOf(Color.Transparent, mist.copy(alpha = 0.5f), Color.Transparent),
                startY = geometry.groundY - 46f * unit,
                endY = geometry.groundY + 8f * unit
            )
        }

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
                    val py = p.y + insetPx
                    transformOrigin = TransformOrigin(p.x / size.width, py / size.height)
                    scaleX = s
                    scaleY = s
                    translationX = (size.width * 0.5f - p.x)
                        .coerceIn((1f - s) * (size.width - p.x), (s - 1f) * p.x)
                    translationY = (size.height * FOCUS_FOCAL_Y - py)
                        .coerceIn((1f - s) * (size.height - py), (s - 1f) * py)
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

                // Sky (also behind the status bar and the header)
                drawRect(brush = skyBrush)

                // Stars (only at night)
                if (night > 0.02f) {
                    for (i in 0 until starCount) {
                        val o = i * 5
                        val twinkle = 0.45f + 0.55f * sin(t * stars[o + 3] + stars[o + 2])
                        drawCircle(
                            color = Color.White.copy(alpha = (twinkle * night).coerceIn(0f, 1f)),
                            radius = stars[o + 4] * unit,
                            center = Offset(stars[o] * w, stars[o + 1] * skyHeight)
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

                // Sun by day (pulsing, soft halo), moon by night; both travel below the header
                if (SkyModel.isSunUp(hour) && night < 0.8f) {
                    val arc = SkyModel.sunArc(hour)
                    val c = Offset(w * (0.12f + 0.76f * arc), insetPx + g.groundY * (0.46f - 0.32f * sin(arc * PI.toFloat())))
                    val pulse = 1f + 0.07f * sin(t * 2.2f)
                    val a = 1f - night
                    drawCircle(Color(0xFFFFE0A3).copy(alpha = 0.12f * a), 96f * unit * pulse, c)
                    drawCircle(Color(0xFFFFD98A).copy(alpha = 0.18f * a), 64f * unit * pulse, c)
                    drawCircle(Color(0xFFFFE9B0).copy(alpha = 0.30f * a), 42f * unit * pulse, c)
                    drawCircle(Color(0xFFFFF4D6).copy(alpha = a), 22f * unit, c)
                } else if (night > 0.5f) {
                    val nightHour = if (hour < 12f) hour + 24f else hour
                    val arc = ((nightHour - 19f) / 11f).coerceIn(0f, 1f)
                    val c = Offset(w * (0.12f + 0.76f * arc), insetPx + g.groundY * (0.44f - 0.30f * sin(arc * PI.toFloat())))
                    drawCircle(Color(0xFFEEF2F7).copy(alpha = 0.10f * night), 44f * unit, c)
                    drawCircle(Color(0xFFEEF2F7).copy(alpha = night), 16f * unit, c)
                    // Crescent: a sky-coloured disc bites into the moon
                    drawCircle(lerp(skyTop, skyBottom, (c.y / skyHeight).coerceIn(0f, 1f)).copy(alpha = night), 13f * unit, Offset(c.x + 6f * unit, c.y - 3f * unit))
                }

                // Storm tint on the sky
                if (storm > 0.01f) {
                    drawRect(Color(0xFF263238).copy(alpha = 0.30f * storm), size = Size(w, skyHeight))
                }

                // Clouds drifting continuously (#21)
                val cloudAlpha = lerp(
                    Color.White.copy(alpha = if (streakActive) 0.5f else 0.7f),
                    Color(0xFFB0BEC5).copy(alpha = 0.14f),
                    night
                )
                for (i in 0 until 5) {
                    val o = i * 4
                    val cs = clouds[o + 2] * unit
                    val span = w + 120f * cs
                    val cx = (clouds[o] * span + t * clouds[o + 3] * unit) % span - 60f * cs
                    drawCloud(cx, insetPx + clouds[o + 1] * g.groundY, cs, cloudAlpha)
                }

                // The mountain scene is laid out below the header
                val canvas = drawContext.canvas
                canvas.save()
                canvas.translate(0f, insetPx)

                // Distant ridge + mountain with a shaded far side and valley mist
                drawPath(g.ridge, ridgeColor.copy(alpha = 0.55f))
                drawPath(g.body, mountainBrush)
                drawPath(g.body, shadowBrush)
                drawRect(mistBrush, topLeft = Offset(0f, g.groundY - 46f * unit), size = Size(w, 54f * unit))

                // Rolling foothills fill the strip below the base, so no empty band remains under the canvas
                drawPath(g.foreground, foreColor)

                // Trail: dashed full route, solid climbed part with a soft glow
                drawPath(g.trail, Color.White.copy(alpha = 0.55f), style = trailStroke)
                val shown = climb.value
                if (shown > 0.001f) {
                    g.segment(shown, climbedPath)
                    drawPath(climbedPath, Color.White.copy(alpha = 0.22f), style = climbedGlowStroke)
                    drawPath(climbedPath, Color.White.copy(alpha = 0.96f), style = climbedStroke)
                }

                // Trees at the base (apex up)
                drawPath(g.trees, treeColor)

                // Summit flag
                drawEmoji(emojis.flag, g.peak.x + 5f * unit, g.peak.y + 3f * unit, 1f)

                // Milestone tents
                for (i in g.milestones.indices) {
                    val m = g.milestones[i]
                    drawEmoji(emojis.tent, m.second.x, m.second.y + 2f * unit, if (target >= m.first) 1f else 140f / 255f)
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
                val cy = p.y.coerceIn(52f * unit, h - insetPx)
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
                    drawEmoji(emojis.camp, cx, cy + 2f * unit, camp)
                }
                canvas.restore()
            }

            // Touch targets: same px coordinates as the canvas, inside the same zoom layer, dp-sized
            val half = with(LocalDensity.current) { 24.dp.roundToPx() }
            val insetInt = insetPx.toInt()
            notePositions.forEach { (note, pos) ->
                Box(
                    modifier = Modifier
                        .offset { IntOffset(pos.x.toInt() - half, pos.y.toInt() + insetInt - 2 * half) }
                        .size(48.dp)
                        .semantics { contentDescription = "Note signpost, page ${note.pageNumber}" }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            enabled = !isFocusMode
                        ) { onSignpostClick(note) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("🪧", fontSize = 22.sp)
                }
            }
            geometry.milestones.forEach { (fraction, pos) ->
                val percent = (fraction * 100).toInt()
                Box(
                    modifier = Modifier
                        .offset { IntOffset(pos.x.toInt() - half, pos.y.toInt() + insetInt - half - half / 2) }
                        .size(48.dp)
                        .semantics { contentDescription = "Camp at $percent percent" }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Button,
                            enabled = !isFocusMode
                        ) { onMilestoneClick(fraction) }
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

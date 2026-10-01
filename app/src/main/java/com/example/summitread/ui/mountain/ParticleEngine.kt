package com.example.summitread.ui.mountain

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** A point in window coordinates that is written by layout callbacks without triggering recomposition. */
class WindowPoint {
    var x: Float = 0f
    var y: Float = 0f
    var isSet: Boolean = false

    fun set(offset: Offset) {
        x = offset.x
        y = offset.y
        isSet = true
    }
}

private const val PARTICLE_COUNT = 36
private const val FIELDS = 6 // angle, burst distance, start delay, duration, size, colour index
private const val BURST_PORTION = 0.3f
private const val FLIGHT_MILLIS = 1400
private const val IMPACT_AT_MILLIS = 1050L

private val SparkColors = arrayOf(Color(0xFFFFA726), Color(0xFFFF7043), Color(0xFFFFCA28), Color(0xFFFFE082))

/**
 * Spark burst (#31): every [burstId] change, particles pop out of [origin] and then fly to [target] (the
 * streak flame). The motion is a pure function of the animation progress (no per-frame state mutation, so it
 * is frame-rate independent), and [onImpact] fires once when the first sparks land so the flame can beat.
 * Both points are window coordinates; they are converted to this overlay's local space when drawing.
 */
@Composable
fun ParticleBurstOverlay(
    burstId: Int,
    origin: WindowPoint,
    target: WindowPoint,
    onImpact: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    val params = remember { FloatArray(PARTICLE_COUNT * FIELDS) }
    val overlayOrigin = remember { WindowPoint() }

    LaunchedEffect(burstId) {
        if (burstId <= 0) return@LaunchedEffect
        val random = Random.Default
        for (i in 0 until PARTICLE_COUNT) {
            val o = i * FIELDS
            params[o] = random.nextFloat() * (2f * PI.toFloat())
            params[o + 1] = 40f + random.nextFloat() * 110f
            params[o + 2] = random.nextFloat() * 0.15f
            params[o + 3] = 0.70f + random.nextFloat() * 0.15f
            params[o + 4] = 3f + random.nextFloat() * 5f
            params[o + 5] = random.nextInt(SparkColors.size).toFloat()
        }
        progress.snapTo(0.0001f)
        coroutineScope {
            launch {
                delay(IMPACT_AT_MILLIS)
                onImpact()
            }
            progress.animateTo(1f, tween(FLIGHT_MILLIS, easing = LinearEasing))
        }
        progress.snapTo(0f)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { overlayOrigin.set(it.positionInWindow()) }
    ) {
        val p = progress.value
        if (p <= 0f || !origin.isSet || !target.isSet) return@Canvas
        val density = this.density
        val ox = origin.x - overlayOrigin.x
        val oy = origin.y - overlayOrigin.y
        val tx = target.x - overlayOrigin.x
        val ty = target.y - overlayOrigin.y

        for (i in 0 until PARTICLE_COUNT) {
            val o = i * FIELDS
            val lt = ((p - params[o + 2]) / params[o + 3]).coerceIn(0f, 1f)
            if (lt <= 0f || lt >= 1f) continue
            val angle = params[o]
            val dist = params[o + 1] * density
            val bx = ox + cos(angle) * dist
            val by = oy + sin(angle) * dist - 30f * density

            val x: Float
            val y: Float
            if (lt < BURST_PORTION) {
                val u = lt / BURST_PORTION
                val e = 1f - (1f - u) * (1f - u) // ease out
                x = ox + (bx - ox) * e
                y = oy + (by - oy) * e
            } else {
                val u = (lt - BURST_PORTION) / (1f - BURST_PORTION)
                val e = u * u // ease in: speeds up towards the flame
                val arc = sin(u * PI.toFloat()) * 36f * density * (if (i % 2 == 0) 1f else -1f)
                x = bx + (tx - bx) * e + arc
                y = by + (ty - by) * e
            }
            val color = SparkColors[params[o + 5].toInt()]
            val radius = params[o + 4] * density * (1f - 0.55f * lt)
            drawCircle(color.copy(alpha = 0.25f), radius * 2.2f, Offset(x, y))
            drawCircle(color.copy(alpha = (1f - 0.3f * lt)), radius, Offset(x, y))
        }
    }
}

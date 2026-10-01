package com.example.summitread.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Hue (0..360), saturation (0..1) and value/brightness (0..1) of a picked colour. */
data class Hsv(val hue: Float, val saturation: Float, val value: Float) {
    fun toColor(): Color = Color.hsv(hue.coerceIn(0f, 360f), saturation.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
}

/** Converts a colour to HSV (pure math, no android.graphics so it is unit-testable). */
fun Color.toHsv(): Hsv {
    val max = maxOf(red, green, blue)
    val minC = minOf(red, green, blue)
    val delta = max - minC
    val hue = when {
        delta == 0f -> 0f
        max == red -> 60f * (((green - blue) / delta) % 6f)
        max == green -> 60f * ((blue - red) / delta + 2f)
        else -> 60f * ((red - green) / delta + 4f)
    }.let { if (it < 0f) it + 360f else it }
    return Hsv(hue, if (max == 0f) 0f else delta / max, max)
}

/**
 * HSV palette: a hue/saturation colour wheel plus a brightness slider. Touching or dragging on the wheel picks
 * hue (angle) and saturation (distance from the centre). Reports every change through [onChange].
 */
@Composable
fun ColorWheelPicker(
    hsv: Hsv,
    onChange: (Hsv) -> Unit,
    modifier: Modifier = Modifier,
    wheelSize: Dp = 190.dp
) {
    val current by rememberUpdatedState(hsv)
    val change by rememberUpdatedState(onChange)

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .size(wheelSize)
                .semantics {
                    contentDescription = "Colour wheel. Hue ${current.hue.toInt()} degrees, " +
                        "saturation ${(current.saturation * 100).toInt()} percent. Drag to choose a colour."
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val radius = min(size.width, size.height) / 2f
                        val center = Offset(size.width / 2f, size.height / 2f)

                        fun pick(position: Offset) {
                            val dx = position.x - center.x
                            val dy = position.y - center.y
                            // Angle measured clockwise from 3 o'clock, matching the sweep gradient below.
                            val degrees = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
                            val hue = if (degrees < 0f) degrees + 360f else degrees
                            val saturation = min(sqrt(dx * dx + dy * dy) / radius, 1f)
                            change(Hsv(hue, saturation, current.value))
                        }

                        pick(down.position)
                        down.consume()
                        drag(down.id) { pointer ->
                            pick(pointer.position)
                            pointer.consume()
                        }
                    }
                }
                .drawWithCache {
                    val radius = min(size.width, size.height) / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val hues = Brush.sweepGradient(
                        colors = listOf(0f, 60f, 120f, 180f, 240f, 300f, 360f).map { Color.hsv(it % 360f, 1f, 1f) },
                        center = center
                    )
                    val whiteCenter = Brush.radialGradient(
                        colors = listOf(Color.White, Color.Transparent),
                        center = center,
                        radius = radius
                    )
                    onDrawBehind {
                        drawCircle(brush = hues, radius = radius, center = center)
                        drawCircle(brush = whiteCenter, radius = radius, center = center)
                        // Brightness: darken the whole wheel as the value goes down.
                        drawCircle(
                            color = Color.Black.copy(alpha = 1f - current.value),
                            radius = radius,
                            center = center
                        )
                        // Selector ring at the picked hue/saturation.
                        val angle = Math.toRadians(current.hue.toDouble())
                        val selectorCenter = Offset(
                            center.x + (cos(angle) * current.saturation * radius).toFloat(),
                            center.y + (sin(angle) * current.saturation * radius).toFloat()
                        )
                        drawCircle(current.toColor(), radius = 11.dp.toPx(), center = selectorCenter)
                        drawCircle(
                            Color.White,
                            radius = 11.dp.toPx(),
                            center = selectorCenter,
                            style = Stroke(width = 3.dp.toPx())
                        )
                        drawCircle(
                            Color.Black.copy(alpha = 0.4f),
                            radius = 13.dp.toPx(),
                            center = selectorCenter,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }
        ) {}

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Brightness", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Slider(
                value = hsv.value,
                onValueChange = { onChange(hsv.copy(value = it)) },
                valueRange = 0.15f..1f,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Colour brightness" }
            )
        }
    }
}

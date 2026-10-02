package com.example.summitread.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import com.example.summitread.R
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Padding between the wheel disc and its bounds, so the selector ring is never cut off at the rim. */
private val WheelRimPadding = 14.dp

/**
 * HSV palette: a hue/saturation colour wheel plus a brightness slider. Touching or dragging on the wheel picks
 * hue (angle) and saturation (distance from the centre). Reports every change through [onChange].
 *
 * The wheel is always square (width up to [wheelSize], height follows with an aspect ratio), so it can never be
 * squashed or cut flat by a parent; put it in a scrolling column when space is tight.
 */
@Composable
fun ColorWheelPicker(
    hsv: Hsv,
    onChange: (Hsv) -> Unit,
    modifier: Modifier = Modifier,
    wheelSize: Dp = 192.dp
) {
    val colors = AppTheme.colors
    val current by rememberUpdatedState(hsv)
    val change by rememberUpdatedState(onChange)
    val description = stringResource(
        R.string.color_wheel_description,
        hsv.hue.toInt(),
        (hsv.saturation * 100).toInt()
    )
    val brightnessDescription = stringResource(R.string.color_brightness_description)

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .widthIn(max = wheelSize)
                .fillMaxWidth()
                .aspectRatio(1f)
                .semantics { contentDescription = description }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val radius = min(size.width, size.height) / 2f - WheelRimPadding.toPx()
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

                        // Only a press on the disc belongs to the wheel; the corners and the rim are left
                        // unconsumed, so the sheet still scrolls when the finger starts there.
                        if ((down.position - center).getDistance() > radius + WheelRimPadding.toPx() / 2f) {
                            return@awaitEachGesture
                        }
                        // Nothing is consumed until the finger has moved past the touch slop (a deliberate drag)
                        // or lifted (a tap), so a parent scroll can still win a plain swipe that began elsewhere.
                        val dragStart = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
                        if (dragStart == null) {
                            val up = currentEvent.changes.firstOrNull { it.id == down.id }
                            if (up != null && !up.pressed && !up.isConsumed) {
                                pick(up.position) // tap
                                up.consume()
                            }
                            return@awaitEachGesture
                        }
                        pick(dragStart.position)
                        drag(dragStart.id) { pointer ->
                            pick(pointer.position)
                            pointer.consume()
                        }
                    }
                }
                .drawWithCache {
                    val radius = min(size.width, size.height) / 2f - WheelRimPadding.toPx()
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
                        // Hairline rim so the disc stays defined on any background.
                        drawCircle(
                            color = colors.separator,
                            radius = radius,
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
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
            Text(
                stringResource(R.string.color_brightness),
                style = AppType.Footnote,
                color = colors.secondaryLabel
            )
            Spacer(modifier = Modifier.width(8.dp))
            Slider(
                value = hsv.value,
                onValueChange = { onChange(hsv.copy(value = it)) },
                valueRange = 0.15f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = colors.tint,
                    activeTrackColor = colors.tint,
                    inactiveTrackColor = colors.tertiaryLabel.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = brightnessDescription }
            )
        }
    }
}

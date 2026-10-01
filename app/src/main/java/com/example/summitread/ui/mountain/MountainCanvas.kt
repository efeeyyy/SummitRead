package com.example.summitread.ui.mountain

import android.graphics.PathMeasure
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.summitread.data.db.NoteEntity
import java.time.LocalTime
import kotlin.math.atan2
import kotlin.math.sin
import kotlin.math.cos
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.drawscope.withTransform

@Composable
fun MountainCanvas(
    progress: Float,
    notes: List<NoteEntity>,
    totalPages: Int,
    isFocusMode: Boolean,
    accentColor: Color,
    surfaceColor: Color,
    onSurfaceColor: Color,
    hoursSinceLastSession: Long,
    onSignpostClick: (NoteEntity) -> Unit,
    onMilestoneClick: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(1500),
        label = "progress_anim"
    )

    // Focus mode: zoom into the climber area instead of uniform scale
    val focusScale by animateFloatAsState(
        targetValue = if (isFocusMode) 1.3f else 1f,
        animationSpec = tween(600),
        label = "focus_scale"
    )

    BoxWithConstraints(modifier = modifier) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        // Animation time using Choreographer clock (not System.nanoTime)
        var animationTime by remember { mutableStateOf(0f) }
        var startFrameTime by remember { mutableLongStateOf(0L) }
        LaunchedEffect(Unit) {
            while (true) {
                withFrameNanos { frameTime ->
                    if (startFrameTime == 0L) startFrameTime = frameTime
                    animationTime = (frameTime - startFrameTime) / 1_000_000_000f
                }
            }
        }

        // Dynamic peak height
        val peakYRatio = when {
            totalPages >= 400 -> 0.1f // Everest
            totalPages >= 200 -> 0.3f // Standard
            else -> 0.5f // Small hill
        }

        // Define the mountain peak
        val peakX = width * 0.7f
        val peakY = height * peakYRatio
        val startX = width * 0.1f
        val startY = height * 0.9f
        val endX = width * 0.95f
        val endY = height * 0.9f

        val path = Path().apply {
            moveTo(startX, startY)
            cubicTo(
                width * 0.3f, height * 0.8f,
                width * 0.5f, height * 0.4f,
                peakX, peakY
            )
            cubicTo(
                width * 0.8f, height * 0.4f,
                width * 0.85f, height * 0.7f,
                endX, endY
            )
            lineTo(startX, endY)
            close()
        }

        // The climbing trail
        val trailPath = Path().apply {
            moveTo(startX, startY)
            cubicTo(
                width * 0.3f, height * 0.8f,
                width * 0.5f, height * 0.4f,
                peakX, peakY
            )
        }

        // Use Android's PathMeasure to find points along the path
        val nativePath = android.graphics.Path().apply {
            moveTo(startX, startY)
            cubicTo(
                width * 0.3f, height * 0.8f,
                width * 0.5f, height * 0.4f,
                peakX, peakY
            )
        }
        val pathMeasure = PathMeasure(nativePath, false)
        val pathLength = pathMeasure.length

        val notePositions = notes.mapNotNull { note ->
            if (totalPages > 0) {
                val fraction = (note.pageNumber.toFloat() / totalPages).coerceIn(0f, 1f)
                val pos = FloatArray(2)
                pathMeasure.getPosTan(pathLength * fraction, pos, null)
                note to Offset(pos[0], pos[1])
            } else null
        }

        // Calculate milestone positions (25%, 50%, 75%)
        val milestones = listOf(0.25f, 0.5f, 0.75f).map { fraction ->
            val pos = FloatArray(2)
            pathMeasure.getPosTan(pathLength * fraction, pos, null)
            fraction to Offset(pos[0], pos[1])
        }

        // Calculate climber position for focus-mode zoom targeting
        val climberPos = FloatArray(2)
        val climberTan = FloatArray(2)
        pathMeasure.getPosTan(pathLength * animatedProgress, climberPos, climberTan)

        // Focus mode translation: shift the canvas so climber is roughly centered
        val focusTranslateX by animateFloatAsState(
            targetValue = if (isFocusMode) (width / 2f - climberPos[0]) * 0.3f else 0f,
            animationSpec = tween(600),
            label = "focus_tx"
        )
        val focusTranslateY by animateFloatAsState(
            targetValue = if (isFocusMode) (height / 2f - climberPos[1]) * 0.3f else 0f,
            animationSpec = tween(600),
            label = "focus_ty"
        )

        val isSystemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
        val currentHour = LocalTime.now().hour

        val skyGradientColors = when {
            isSystemInDarkTheme || currentHour < 6 || currentHour >= 20 -> listOf(Color(0xFF0B1021), Color(0xFF1B2735)) // Deep Midnight
            currentHour in 6..9 -> listOf(Color(0xFFFFB74D), Color(0xFFFFF3E0)) // Dawn
            currentHour in 10..15 -> listOf(Color(0xFF64B5F6), Color(0xFFE3F2FD)) // Daylight
            else -> listOf(Color(0xFF9575CD), Color(0xFFEDE7F6)) // Twilight
        }

        val mountainPeakColor = if (isSystemInDarkTheme) Color.DarkGray else Color.White
        val mountainMidColor = if (isSystemInDarkTheme) Color.Gray else Color.LightGray

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = focusScale
                    scaleY = focusScale
                    translationX = focusTranslateX
                    translationY = focusTranslateY
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
                }
        ) {
            // Sky gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = skyGradientColors,
                    endY = size.height * 0.6f
                )
            )

            // Day Mode: Sun
            if (!isSystemInDarkTheme && currentHour in 6..19) {
                val sunPulse = 1f + 0.05f * sin(animationTime * 3f)
                drawCircle(
                    color = Color(0xFFFFD54F).copy(alpha = 0.4f * sunPulse),
                    radius = 80f * sunPulse,
                    center = Offset(width * 0.8f, height * 0.15f)
                )
                drawCircle(
                    color = Color(0xFFFFCA28),
                    radius = 50f,
                    center = Offset(width * 0.8f, height * 0.15f)
                )
            }

            // Night Mode: Stars & Shooting Stars
            if (isSystemInDarkTheme || currentHour < 6 || currentHour >= 20) {
                val random = java.util.Random(42)
                for (i in 0..50) {
                    val starX = random.nextFloat() * width
                    val starY = random.nextFloat() * height * 0.6f
                    val twinkle = 0.5f + 0.5f * sin(animationTime * (1f + random.nextFloat() * 3f))
                    drawCircle(color = Color.White.copy(alpha = twinkle), radius = 2f, center = Offset(starX, starY))
                }

                val shootingStarTime = animationTime % 5f
                if (shootingStarTime < 0.5f) {
                    val sp = shootingStarTime / 0.5f
                    val sx = width * 0.2f
                    val sy = height * 0.1f
                    val cx = sx + width * 0.5f * sp
                    val cy = sy + height * 0.3f * sp
                    drawLine(
                        color = Color.White,
                        start = Offset(cx - 50f, cy - 30f),
                        end = Offset(cx, cy),
                        strokeWidth = 3f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }

            // Normal Moving Clouds
            val normalCloudOff1 = (animationTime * 15f) % (width * 1.5f)
            val normalCloudOff2 = (animationTime * 20f + width * 0.5f) % (width * 1.5f)
            val baseCloudColor = if (isSystemInDarkTheme) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.3f)
            drawCircle(color = baseCloudColor, radius = 60f, center = Offset(width * 1.2f - normalCloudOff1, height * 0.25f))
            drawCircle(color = baseCloudColor, radius = 40f, center = Offset(width * 1.2f - normalCloudOff1 + 40f, height * 0.27f))
            drawCircle(color = baseCloudColor, radius = 50f, center = Offset(width * 1.2f - normalCloudOff2, height * 0.15f))

            // Storm clouds if inactive > 36 hours (different offsets, no shadowing)
            if (hoursSinceLastSession > 36) {
                val stormOff1 = (animationTime * 20f) % (width * 1.5f)
                val stormOff2 = (animationTime * 15f + width * 0.5f) % (width * 1.5f)
                val stormColor = if (isSystemInDarkTheme) Color.White.copy(alpha = 0.1f) else Color.DarkGray.copy(alpha = 0.2f)
                drawCircle(color = stormColor, radius = 80f, center = Offset(stormOff1 - 100f, height * 0.2f))
                drawCircle(color = stormColor, radius = 60f, center = Offset(stormOff1 - 20f, height * 0.25f))
                drawCircle(color = stormColor, radius = 100f, center = Offset(stormOff2 - 150f, height * 0.15f))
            }

            // Mountain fill
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(mountainPeakColor, mountainMidColor, accentColor),
                    startY = peakY,
                    endY = startY
                )
            )

            // Trail line
            drawPath(
                path = trailPath,
                color = Color.White.copy(alpha = 0.5f),
                style = Stroke(
                    width = 4.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                )
            )

            // Trees at base (FIXED: pointing UPWARD, not inverted)
            for (i in 0..5) {
                val tx = startX + (width * 0.8f * (i / 5f))
                drawPath(
                    path = Path().apply {
                        moveTo(tx, startY - 40f) // Apex at top
                        lineTo(tx - 20f, startY) // Base left
                        lineTo(tx + 20f, startY) // Base right
                        close()
                    },
                    color = Color(0xFF1B5E20)
                )
            }

            // Summit Flag
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    textSize = 40.sp.toPx()
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawText("🚩", peakX, peakY - 10f, paint)
            }

            // Milestone Tents
            milestones.forEach { (_, offset) ->
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        textSize = 30.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    drawText("⛺", offset.x, offset.y - 10f, paint)
                }
            }

            // Climber — FIXED: proper centering + clamped rotation + camp mode swap
            val rawAngle = atan2(climberTan[1].toDouble(), climberTan[0].toDouble())
            val angleDegrees = Math.toDegrees(rawAngle).toFloat().coerceIn(-15f, 15f)

            val isClimbing = kotlin.math.abs(animatedProgress - progress.coerceIn(0f, 1f)) > 0.001f
            val bounceOffset = if (isClimbing) sin(animationTime * 20f) * 6f else 0f

            val emojiSize = 36.sp.toPx()
            // Swap to camp emoji in focus mode
            val climberEmoji = if (isFocusMode) "⛺" else "🧗"

            withTransform({
                translate(
                    climberPos[0].coerceIn(0f, width),
                    (climberPos[1] + bounceOffset).coerceIn(0f, height)
                )
                rotate(degrees = angleDegrees, pivot = Offset.Zero)
            }) {
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        textSize = emojiSize
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    // Draw at Y = emojiSize/3 to vertically center the emoji on the path point
                    drawText(climberEmoji, 0f, emojiSize / 3f, paint)
                }
            }
        }

        // Overlay signposts as actual composables for clicking
        notePositions.forEach { (note, offset) ->
            Box(
                modifier = Modifier
                    .offset { IntOffset(offset.x.toInt() - 30, offset.y.toInt() - 40) }
                    .size(60.dp)
                    .clickable { onSignpostClick(note) },
                contentAlignment = Alignment.Center
            ) {
                Text("🪧", fontSize = 24.sp)
            }
        }

        // Overlay interactive milestone camps
        milestones.forEach { (fraction, offset) ->
            Box(
                modifier = Modifier
                    .offset { IntOffset(offset.x.toInt() - 30, offset.y.toInt() - 40) }
                    .size(60.dp)
                    .clickable { onMilestoneClick(fraction) }
            )
        }
    }
}

package com.example.summitread.ui.mountain

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.summitread.theme.AppSpacing
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.theme.rememberAppHaptics
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

/** Confetti piece; positions are fractions of the screen and never change after creation (pure function of time). */
private class ConfettiPiece(
    val x: Float,
    val startY: Float,
    val speed: Float,
    val sway: Float,
    val colorIndex: Int,
    val size: Float,
    val isCircle: Boolean
)

private const val CONFETTI_COUNT = 48

/**
 * Completion overlay (#33): a calm dark scrim, a glowing gold trophy, gold "Summit Conquered" title and the exact
 * congratulation [message] from the ViewModel. Plays the success haptic once. Tap anywhere to continue.
 */
@Composable
fun SummitConqueredAnimation(
    message: String,
    relicEmoji: String,
    bookTitle: String,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    val haptics = rememberAppHaptics()
    var showContent by remember { mutableStateOf(false) }
    val trophyScale by animateFloatAsState(
        targetValue = if (showContent) 1f else 0.4f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow),
        label = "trophy_scale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(700, delayMillis = 250),
        label = "content_alpha"
    )

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
        haptics.success()
    }

    // Confetti in a restrained gold / white palette (gold from the design system plus warm neutrals)
    val palette = remember(colors.gold) {
        arrayOf(colors.gold, Color(0xFFFFE9A8), Color.White, colors.gold.copy(alpha = 0.7f), Color(0xFFFFC857))
    }
    val pieces = remember {
        val r = Random.Default
        List(CONFETTI_COUNT) {
            ConfettiPiece(
                x = r.nextFloat(),
                startY = -r.nextFloat(),
                speed = 0.35f + r.nextFloat() * 0.45f,
                sway = r.nextFloat() * 6.28f,
                colorIndex = r.nextInt(palette.size),
                size = 3f + r.nextFloat() * 4f,
                isCircle = r.nextBoolean()
            )
        }
    }

    // One looping clock (seconds); the confetti is a pure function of it
    val clock = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        clock.animateTo(
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(1_000_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    val glow = remember(colors.gold) {
        Brush.radialGradient(listOf(colors.gold.copy(alpha = 0.38f), Color.Transparent))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.74f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClickLabel = "Continue",
                role = Role.Button
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // Confetti: drifts down slowly, loops
        Canvas(modifier = Modifier.fillMaxSize()) {
            val time = clock.value
            val w = size.width
            val h = size.height
            for (i in pieces.indices) {
                val p = pieces[i]
                val y = ((p.startY + time * p.speed * 0.12f) % 1.15f + 1.15f) % 1.15f
                val x = p.x + sin(time * 0.9f + p.sway) * 0.025f
                val color = palette[p.colorIndex].copy(alpha = 0.85f)
                val px = x * w
                val py = y * h
                val r = p.size * density
                if (p.isCircle) {
                    drawCircle(color, r * 0.6f, Offset(px, py))
                } else {
                    drawRect(color, Offset(px - r / 2f, py - r / 2f), Size(r, r * 0.55f))
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = AppSpacing.xl, vertical = AppSpacing.xxl)
        ) {
            // Trophy on a soft gold glow
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .drawBehind { drawCircle(glow) }
                    .graphicsLayer {
                        scaleX = trophyScale
                        scaleY = trophyScale
                        alpha = (trophyScale * 2f).coerceIn(0f, 1f)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🏆", fontSize = 80.sp)
            }

            Text(
                text = "Summit Conquered!",
                style = AppType.LargeTitle,
                color = colors.gold,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = contentAlpha }
            )

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            Text(
                text = bookTitle,
                style = AppType.Title3,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = contentAlpha }
            )

            Spacer(modifier = Modifier.height(AppSpacing.lg))

            // Relic badge
            Text(
                text = relicEmoji,
                fontSize = 44.sp,
                modifier = Modifier.graphicsLayer {
                    alpha = contentAlpha
                    scaleX = trophyScale
                    scaleY = trophyScale
                }
            )

            Spacer(modifier = Modifier.height(AppSpacing.xs))

            // The exact congratulation text (#33), supplied by the ViewModel
            Text(
                text = message,
                style = AppType.Callout.copy(fontWeight = FontWeight.Medium),
                color = Color.White.copy(alpha = 0.92f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = AppSpacing.xs)
                    .graphicsLayer { alpha = contentAlpha }
            )

            Spacer(modifier = Modifier.height(AppSpacing.xl))

            Text(
                text = "Tap to continue",
                style = AppType.Footnote,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.graphicsLayer { alpha = contentAlpha }
            )
        }
    }
}

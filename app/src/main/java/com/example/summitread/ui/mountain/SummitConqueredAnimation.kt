package com.example.summitread.ui.mountain

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticle(
    var x: Float,
    var y: Float,
    val speedX: Float,
    val speedY: Float,
    val color: Color,
    val size: Float,
    val isCircle: Boolean
)

@Composable
fun SummitConqueredAnimation(
    bookTitle: String,
    onDismiss: () -> Unit
) {
    val relicEmojis = listOf("🗡️", "📚", "🔮", "🌟", "⭐", "🏔️")
    val relic = remember { relicEmojis[Random.nextInt(relicEmojis.size)] }

    var showContent by remember { mutableStateOf(false) }
    val trophyScale by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(800),
        label = "trophy_scale"
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(1000, delayMillis = 400),
        label = "text_alpha"
    )

    LaunchedEffect(Unit) {
        delay(100)
        showContent = true
    }

    // Confetti particles
    val confettiColors = listOf(
        Color(0xFFFFD700), Color(0xFFFF6B6B), Color(0xFF4ECDC4),
        Color(0xFFFF69B4), Color(0xFF87CEEB), Color(0xFFFFA07A),
        Color(0xFF98FB98), Color(0xFFDDA0DD)
    )

    val particles = remember {
        List(60) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -1f,
                speedX = (Random.nextFloat() - 0.5f) * 0.005f,
                speedY = Random.nextFloat() * 0.008f + 0.002f,
                color = confettiColors[Random.nextInt(confettiColors.size)],
                size = Random.nextFloat() * 8f + 4f,
                isCircle = Random.nextBoolean()
            )
        }
    }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(4000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // Dark overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Color.Black.copy(alpha = 0.7f))
        }

        // Confetti canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val time = animProgress.value
            particles.forEach { p ->
                val currentY = (p.y + time * p.speedY * 300f) % 1.2f
                val currentX = p.x + sin(time * 10f + p.x * 20f).toFloat() * 0.02f
                val px = currentX * size.width
                val py = currentY * size.height

                if (p.isCircle) {
                    drawCircle(
                        color = p.color,
                        radius = p.size,
                        center = Offset(px, py)
                    )
                } else {
                    drawRect(
                        color = p.color,
                        topLeft = Offset(px - p.size / 2, py - p.size / 2),
                        size = androidx.compose.ui.geometry.Size(p.size, p.size)
                    )
                }
            }
        }

        // Content
        AnimatedVisibility(
            visible = showContent,
            enter = fadeIn(tween(600)) + scaleIn(tween(600))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                // Trophy
                Text(
                    text = "🏆",
                    fontSize = 72.sp,
                    modifier = Modifier.graphicsLayer {
                        scaleX = trophyScale
                        scaleY = trophyScale
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Headline
                Text(
                    text = "Summit Conquered!",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFD700),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer { alpha = textAlpha }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Book title
                Text(
                    text = bookTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer { alpha = textAlpha }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Relic badge
                Text(
                    text = relic,
                    fontSize = 48.sp,
                    modifier = Modifier.graphicsLayer {
                        alpha = textAlpha
                        scaleX = trophyScale
                        scaleY = trophyScale
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Relic Earned!",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFFD700).copy(alpha = 0.8f),
                    modifier = Modifier.graphicsLayer { alpha = textAlpha }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Tap to continue
                Text(
                    text = "Tap to continue",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.graphicsLayer { alpha = textAlpha }
                )
            }
        }
    }
}

package com.example.summitread.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val GoldLight = Color(0xFFFFE9A0)
private val Gold = Color(0xFFE0B030)
private val GoldDark = Color(0xFF9A6B00)

/** Shimmer phase provider: a value in 0..1 read only while drawing, so it never recomposes anything. */
typealias ShimmerPhase = () -> Float

val NoShimmer: ShimmerPhase = { 0f }

/**
 * One infinite transition shared by every gold foil on the screen (never one per book). Returns [NoShimmer]
 * (and runs no animation) while [enabled] is false, i.e. when no book is completed.
 */
@Composable
fun rememberGoldShimmer(enabled: Boolean): ShimmerPhase {
    if (!enabled) return NoShimmer
    val transition = rememberInfiniteTransition(label = "goldShimmer")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart),
        label = "goldShimmerPhase"
    )
    return remember(phase) { { phase.value } }
}

/** Gold foil fill with a diagonal highlight that sweeps across it (the sweep is parked off-shape part of the loop). */
private fun Modifier.goldFoil(phase: ShimmerPhase): Modifier = drawWithCache {
    val w = size.width
    val h = size.height
    val base = Brush.linearGradient(
        colors = listOf(GoldLight, Gold, GoldDark, Gold),
        start = Offset.Zero,
        end = Offset(w, h)
    )
    val sweep = Brush.linearGradient(
        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.75f), Color.Transparent),
        start = Offset.Zero,
        end = Offset(w, h)
    )
    onDrawBehind {
        drawRect(base)
        // Sweep moves from -1.5w to +1.5w over the loop; most of that range is outside the shape (a pause).
        translate(left = (phase() * 3f - 1.5f) * w) { drawRect(sweep) }
    }
}

/** Round gold foil seal with a star, used on completed (100%) books. */
@Composable
fun GoldFoilSeal(
    shimmer: ShimmerPhase,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .goldFoil(shimmer)
            .border(0.75.dp, GoldDark, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = GoldDark,
            modifier = Modifier.size(size * 0.6f)
        )
    }
}

/** Thin horizontal gold foil strip across a spine. */
@Composable
fun GoldFoilBand(
    shimmer: ShimmerPhase,
    modifier: Modifier = Modifier,
    thickness: Dp = 3.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .goldFoil(shimmer)
    )
}

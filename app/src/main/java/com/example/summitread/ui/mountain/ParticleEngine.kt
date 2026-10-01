package com.example.summitread.ui.mountain

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.random.Random

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val size: Float,
    val lifeTime: Float,
    var age: Float = 0f
)

@Composable
fun ParticleExplosionOverlay(
    trigger: Boolean,
    origin: Offset,
    target: Offset,
    onAnimationEnd: () -> Unit
) {
    val particles = remember { mutableStateListOf<Particle>() }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger) {
            particles.clear()
            val colors = listOf(Color(0xFFFFA726), Color(0xFFFF7043), Color(0xFFFFCA28))
            repeat(40) {
                particles.add(
                    Particle(
                        x = origin.x,
                        y = origin.y,
                        vx = (Random.nextFloat() - 0.5f) * 600f,
                        vy = (Random.nextFloat() - 0.5f) * 600f - 200f,
                        color = colors[Random.nextInt(colors.size)],
                        size = Random.nextFloat() * 10f + 5f,
                        lifeTime = Random.nextFloat() * 0.5f + 0.5f
                    )
                )
            }
            
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
            )
            onAnimationEnd()
        }
    }

    if (progress.value > 0f && progress.value < 1f) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dt = 0.016f // Roughly 60fps delta
            
            particles.forEach { p ->
                if (p.age < p.lifeTime) {
                    p.age += dt
                    
                    // Add gravity and pull towards target
                    val tx = target.x - p.x
                    val ty = target.y - p.y
                    
                    p.vx += tx * 2f * dt
                    p.vy += ty * 2f * dt
                    
                    p.x += p.vx * dt
                    p.y += p.vy * dt
                    
                    val alpha = (1f - (p.age / p.lifeTime)).coerceIn(0f, 1f)
                    
                    drawCircle(
                        color = p.color.copy(alpha = alpha),
                        radius = p.size * alpha,
                        center = Offset(p.x, p.y)
                    )
                }
            }
        }
    }
}

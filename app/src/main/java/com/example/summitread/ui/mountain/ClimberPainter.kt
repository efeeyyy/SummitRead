package com.example.summitread.ui.mountain

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Animation state of the climber figure (#17). */
enum class ClimbingState { IDLE, CLIMBING }

private val SkinColor = Color(0xFFFFCCBC)
private val JacketColor = Color(0xFFFF7043)
private val PantsColor = Color(0xFF37474F)
private val PackColor = Color(0xFF1565C0)
private val HatColor = Color(0xFFD32F2F)
private val PoleColor = Color(0xFFCFD8DC)

private const val DEG = (PI / 180.0).toFloat()

/**
 * Draws the stick-figure climber with feet at ([x], [y]). The whole figure is rotated by the trail tangent
 * ([tangentDeg], the PathMeasure slope) so it stands on the slope, and the torso leans forward more the
 * steeper the slope is. While [state] is CLIMBING the legs and arms swing and the body bounces; while IDLE
 * it only breathes. Nothing is allocated here, so it is safe to call every frame.
 *
 * @param unit pixels per dp
 * @param time seconds from the shared animation clock
 */
fun DrawScope.drawClimber(
    x: Float,
    y: Float,
    unit: Float,
    tangentDeg: Float,
    time: Float,
    state: ClimbingState,
    alpha: Float = 1f
) {
    if (alpha <= 0.01f) return
    val climbing = state == ClimbingState.CLIMBING
    val phase = time * 9f
    val height = 32f * unit
    val slope = tangentDeg.coerceIn(-50f, 20f)
    val lean = (-slope * 0.85f + 6f) * DEG
    val bounce = if (climbing) -abs(sin(phase)) * 2f * unit else sin(time * 2f) * 0.5f * unit

    val canvas = drawContext.canvas
    canvas.save()
    canvas.translate(x, y)
    canvas.rotate(slope)

    val thigh = height * 0.21f
    val shin = height * 0.21f
    val hip = Offset(0f, -(thigh + shin) * 0.97f + bounce)
    val torsoLen = height * 0.30f
    val shoulder = Offset(hip.x + sin(lean) * torsoLen, hip.y - cos(lean) * torsoLen)
    val headR = height * 0.10f
    val head = Offset(shoulder.x + sin(lean) * headR * 1.5f, shoulder.y - cos(lean) * headR * 1.5f)
    val limb = 2.2f * unit

    // Legs
    for (leg in 0..1) {
        val ph = phase + leg * PI.toFloat()
        val swing = if (climbing) sin(ph) * 38f * DEG else (if (leg == 0) 8f else -8f) * DEG
        val bend = (if (climbing) maxOf(0f, cos(ph)) * 45f + 10f else 10f) * DEG
        val knee = Offset(hip.x + sin(swing) * thigh, hip.y + cos(swing) * thigh)
        val shinA = swing - bend
        val foot = Offset(knee.x + sin(shinA) * shin, knee.y + cos(shinA) * shin)
        val tint = if (leg == 0) PantsColor else PantsColor.copy(alpha = 0.8f)
        drawLine(tint.copy(alpha = tint.alpha * alpha), hip, knee, limb * 1.2f, StrokeCap.Round)
        drawLine(tint.copy(alpha = tint.alpha * alpha), knee, foot, limb * 1.2f, StrokeCap.Round)
    }

    // Backpack behind the torso
    drawLine(
        PackColor.copy(alpha = alpha),
        Offset(shoulder.x - height * 0.07f, shoulder.y + height * 0.03f),
        Offset(hip.x - height * 0.07f, hip.y - height * 0.06f),
        4.5f * unit,
        StrokeCap.Round
    )

    // Torso
    drawLine(JacketColor.copy(alpha = alpha), hip, shoulder, 4f * unit, StrokeCap.Round)

    // Arms (swing opposite to the legs); the front arm holds a trekking pole
    val upper = height * 0.17f
    val fore = height * 0.15f
    for (arm in 0..1) {
        val ph = phase + (arm + 1) * PI.toFloat()
        val swing = if (climbing) sin(ph) * 40f * DEG else (if (arm == 0) 14f else -6f) * DEG
        val elbow = Offset(shoulder.x + sin(swing) * upper, shoulder.y + cos(swing) * upper)
        val handA = swing + (if (climbing) 0.5f else 0.3f)
        val hand = Offset(elbow.x + sin(handA) * fore, elbow.y + cos(handA) * fore)
        drawLine(JacketColor.copy(alpha = alpha * 0.9f), shoulder, elbow, limb, StrokeCap.Round)
        drawLine(SkinColor.copy(alpha = alpha), elbow, hand, limb, StrokeCap.Round)
        if (arm == 0) {
            drawLine(
                PoleColor.copy(alpha = alpha),
                hand,
                Offset(hand.x + height * 0.12f, 0f),
                1.4f * unit,
                StrokeCap.Round
            )
        }
    }

    // Head and hat
    drawCircle(SkinColor.copy(alpha = alpha), headR, head)
    drawLine(
        HatColor.copy(alpha = alpha),
        Offset(head.x - headR * 1.1f, head.y - headR * 0.55f),
        Offset(head.x + headR * 1.1f, head.y - headR * 0.55f),
        headR * 0.8f,
        StrokeCap.Round
    )

    canvas.restore()
}

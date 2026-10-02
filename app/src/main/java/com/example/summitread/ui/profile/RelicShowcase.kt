package com.example.summitread.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.summitread.data.db.RelicEntity
import com.example.summitread.theme.AppShapes
import com.example.summitread.theme.AppSpacing
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.ui.components.pressableScale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MAX_TILT_DEGREES = 25f

/** The card itself leans at most this much (the sensor range is 25, a full lean looked like a flipped card). */
private const val MAX_CARD_ROTATION_DEGREES = 10f
/** The soft shadow never drifts further than this from its resting offset (iOS-subtle, no slanted slab). */
private val MAX_SHADOW_SHIFT = 4.dp

/** The shimmer band slides by at most this fraction of the card size. */
private const val MAX_SHIMMER_SHIFT_FRACTION = 0.3f

/**
 * Medal card for one relic. Tilt comes from ONE hoisted [TiltState]; it is only read inside
 * layer/draw lambdas so a sensor event never recomposes the card. The drop shadow shifts WITH the
 * tilt and the metallic shimmer moves in the OPPOSITE direction.
 */
@Composable
fun RelicCardNode(relic: RelicEntity, tilt: TiltState, onClick: () -> Unit) {
    val colors = AppTheme.colors
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val cardShape = AppShapes.large
    // Black shadow is invisible on the true-black dark canvas, so dark mode uses a soft gold glow.
    val shadowColor = if (colors.isDark) colors.gold.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.45f)
    val shimmerAlpha = if (colors.isDark) 0.16f else 0.32f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp)
            .graphicsLayer {
                rotationX = shiftOf(tilt.pitch) * MAX_CARD_ROTATION_DEGREES
                rotationY = shiftOf(tilt.roll) * MAX_CARD_ROTATION_DEGREES
                cameraDistance = 12f * density
            }
    ) {
        // Soft blurred shadow (a real elevation shadow, not a hard slab). Its offset follows the tilt but is
        // bounded to a few dp, and it is a no-op while the phone is upright.
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    val max = MAX_SHADOW_SHIFT.toPx()
                    translationX = shiftOf(tilt.roll) * max
                    translationY = 2.dp.toPx() + shiftOf(tilt.pitch) * max
                    shadowElevation = 10.dp.toPx()
                    shape = cardShape
                    clip = false
                    ambientShadowColor = shadowColor
                    spotShadowColor = shadowColor
                }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pressableScale(
                    onClick = onClick,
                    onClickLabel = "Open book journey",
                    role = Role.Button
                )
                .clip(cardShape)
                .background(colors.surface)
                .border(1.dp, colors.gold.copy(alpha = 0.45f), cardShape)
                .drawWithContent {
                    drawContent()
                    // Glossy metallic shimmer, shifted opposite to the shadow.
                    val dx = -shiftOf(tilt.roll) * size.width * MAX_SHIMMER_SHIFT_FRACTION
                    val dy = -shiftOf(tilt.pitch) * size.height * MAX_SHIMMER_SHIFT_FRACTION
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = shimmerAlpha),
                                Color.Transparent
                            ),
                            start = Offset(dx, dy),
                            end = Offset(size.width + dx, size.height + dy)
                        )
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Medal disc with a gold rim; the emoji drifts slightly against the tilt (parallax).
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(colors.gold.copy(alpha = 0.30f), colors.gold.copy(alpha = 0.10f))
                            )
                        )
                        .border(2.dp, colors.gold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = relic.emoji,
                        fontSize = 32.sp,
                        modifier = Modifier.graphicsLayer {
                            translationX = -tilt.roll * 0.5f
                            translationY = -tilt.pitch * 0.5f
                        }
                    )
                }
                Text(
                    text = relic.title,
                    style = AppType.Subheadline,
                    color = colors.label,
                    maxLines = 2,
                    minLines = 2,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = AppSpacing.sm)
                )
                Text(
                    text = dateFormat.format(Date(relic.dateEarned)),
                    style = AppType.Caption1,
                    color = colors.secondaryLabel,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

/** Tilt in degrees as a bounded -1..1 factor; non-finite input (a sensor glitch) counts as upright. */
private fun shiftOf(degrees: Float): Float =
    if (degrees.isFinite()) (degrees / MAX_TILT_DEGREES).coerceIn(-1f, 1f) else 0f

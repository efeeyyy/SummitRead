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
private const val SHADOW_SHIFT_PX_PER_DEGREE = 1.2f
private const val SHIMMER_SHIFT_PX_PER_DEGREE = 12f

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
    val shadowColor = if (colors.isDark) colors.gold.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.18f)
    val shimmerAlpha = if (colors.isDark) 0.16f else 0.32f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 6.dp, end = 6.dp)
            .graphicsLayer {
                rotationX = tilt.pitch.coerceIn(-MAX_TILT_DEGREES, MAX_TILT_DEGREES)
                rotationY = tilt.roll.coerceIn(-MAX_TILT_DEGREES, MAX_TILT_DEGREES)
                cameraDistance = 12f * density
            }
    ) {
        // Shadow layer: offset follows the tilt (light comes from the opposite side).
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    translationX = 3f * density + tilt.roll * SHADOW_SHIFT_PX_PER_DEGREE
                    translationY = 3f * density + tilt.pitch * SHADOW_SHIFT_PX_PER_DEGREE
                }
                .background(shadowColor, cardShape)
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
                    val dx = -tilt.roll * SHIMMER_SHIFT_PX_PER_DEGREE
                    val dy = -tilt.pitch * SHIMMER_SHIFT_PX_PER_DEGREE
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

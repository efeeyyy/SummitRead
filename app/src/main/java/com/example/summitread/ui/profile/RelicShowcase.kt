package com.example.summitread.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.summitread.data.db.RelicEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MAX_TILT_DEGREES = 25f
private const val SHADOW_SHIFT_PX_PER_DEGREE = 1.2f
private const val SHIMMER_SHIFT_PX_PER_DEGREE = 12f

/**
 * Relic card. Tilt comes from ONE hoisted [TiltState]; it is only read inside layer/draw
 * lambdas so a sensor event never recomposes the card. The drop shadow shifts WITH the tilt and
 * the metallic shimmer moves in the OPPOSITE direction.
 */
@Composable
fun RelicCardNode(relic: RelicEntity, tilt: TiltState, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val cardShape = RoundedCornerShape(12.dp)
    val shadowColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
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
        Card(
            shape = cardShape,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClickLabel = "Open book journey") { onClick() }
        ) {
            Box(
                modifier = Modifier.drawWithContent {
                    drawContent()
                    // Glossy metallic shimmer, shifted opposite to the shadow.
                    val dx = -tilt.roll * SHIMMER_SHIFT_PX_PER_DEGREE
                    val dy = -tilt.pitch * SHIMMER_SHIFT_PX_PER_DEGREE
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.32f),
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
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = relic.emoji,
                        fontSize = 40.sp,
                        modifier = Modifier.graphicsLayer {
                            translationX = -tilt.roll * 0.5f
                            translationY = -tilt.pitch * 0.5f
                        }
                    )
                    Text(
                        text = relic.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = dateFormat.format(Date(relic.dateEarned)),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

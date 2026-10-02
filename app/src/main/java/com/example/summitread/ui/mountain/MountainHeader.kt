package com.example.summitread.ui.mountain

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.summitread.theme.AppSpacing
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.theme.tabularNumbers
import com.example.summitread.ui.components.GlassState
import com.example.summitread.ui.components.GlassThickness
import com.example.summitread.ui.components.HairlineWidth
import com.example.summitread.ui.components.IconButtonStyle
import com.example.summitread.ui.components.IconCircleButton
import com.example.summitread.ui.components.glassBackground

/** Height of the control row of the header (below the status bar). */
internal val HeaderRowHeight = 44.dp

/** Distance between the status bar and the control row, and below the row: the canvas inset is statusBar + this. */
internal val HeaderVerticalPadding = 4.dp

/** Space the header takes below the status bar. */
internal val HeaderBodyHeight = HeaderRowHeight + HeaderVerticalPadding * 2

/**
 * Floating glass header over the full-bleed canvas (iOS 26 style): a back circle, a title capsule
 * ("[Book Title]" with the subtitle "Summit Climb"), the streak pill and the Notes action.
 * Over the sky each control floats on its own glass; once the canvas has scrolled away ([collapse] near 1) they
 * dissolve into one full-width glass bar with a hairline, so list content never runs under the status bar.
 *
 * @param collapse 0 while the canvas is fully visible, 1 once it has scrolled out of view
 * @param flameTarget receives the window position of the flame: the sparks fly there
 * @param flameScale the heartbeat scale of the flame when sparks hit it
 */
@Composable
internal fun MountainHeader(
    title: String,
    streak: Int,
    glass: GlassState,
    collapse: () -> Float,
    flameScale: () -> Float,
    flameTarget: WindowPoint,
    onBack: () -> Unit,
    onOpenNotes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val density = LocalDensity.current
    val statusTop: Dp = with(density) { WindowInsets.statusBars.getTop(density).toDp() }

    Box(modifier.fillMaxWidth()) {
        // Full-width glass bar, faded in as the canvas scrolls away
        Box(
            Modifier
                .fillMaxWidth()
                .height(statusTop + HeaderBodyHeight)
                .graphicsLayer { alpha = collapse().coerceIn(0f, 1f) }
                .glassBackground(glass)
        ) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(HairlineWidth)
                    .background(colors.separator)
            )
        }

        Row(
            modifier = Modifier
                .padding(top = statusTop + HeaderVerticalPadding)
                .padding(horizontal = AppSpacing.sm)
                .height(HeaderRowHeight)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            GlassIconButton(
                glass = glass,
                collapse = collapse,
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = "Back",
                tint = colors.label,
                onClick = onBack
            )

            // Title capsule
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = 1f - collapse().coerceIn(0f, 1f) }
                        .glassBackground(glass, CircleShape, GlassThickness.Thin)
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = AppSpacing.sm)
                        .semantics(mergeDescendants = true) {
                            heading()
                            contentDescription = "$title - Summit Climb"
                        }
                ) {
                    Text(
                        text = title,
                        style = AppType.Subheadline.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                        color = colors.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Summit Climb",
                        style = AppType.Caption2,
                        color = colors.secondaryLabel,
                        maxLines = 1
                    )
                }
            }

            // Streak pill
            Box(contentAlignment = Alignment.Center, modifier = Modifier.height(40.dp)) {
                Box(
                    Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = 1f - collapse().coerceIn(0f, 1f) }
                        .glassBackground(glass, CircleShape, GlassThickness.Thin)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .padding(horizontal = AppSpacing.sm)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "Reading streak: $streak ${if (streak == 1) "day" else "days"}"
                        }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        tint = colors.warning,
                        modifier = Modifier
                            .size(20.dp)
                            .onGloballyPositioned { coordinates ->
                                val pos = coordinates.positionInWindow()
                                flameTarget.x = pos.x + coordinates.size.width / 2f
                                flameTarget.y = pos.y + coordinates.size.height / 2f
                                flameTarget.isSet = true
                            }
                            .graphicsLayer {
                                val s = flameScale()
                                scaleX = s
                                scaleY = s
                            }
                    )
                    Text(
                        text = streak.toString(),
                        style = AppType.Subheadline.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold).tabularNumbers(),
                        color = colors.label,
                        maxLines = 1
                    )
                }
            }

            GlassIconButton(
                glass = glass,
                collapse = collapse,
                icon = Icons.Rounded.EditNote,
                contentDescription = "Open notes and characters",
                tint = colors.tint,
                onClick = onOpenNotes
            )
        }
    }
}

@Composable
private fun GlassIconButton(
    glass: GlassState,
    collapse: () -> Float,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(40.dp)
                .graphicsLayer { alpha = 1f - collapse().coerceIn(0f, 1f) }
                .glassBackground(glass, CircleShape, GlassThickness.Thin)
        )
        IconCircleButton(
            icon = icon,
            contentDescription = contentDescription,
            onClick = onClick,
            style = IconButtonStyle.Plain,
            size = 40.dp,
            tint = tint
        )
    }
}


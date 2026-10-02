package com.example.summitread.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.summitread.theme.AppMotion
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.theme.rememberAppHaptics

private val TrackShape = RoundedCornerShape(9.dp)
private val ThumbShape = RoundedCornerShape(7.dp)

/**
 * iOS segmented control: grey track with a white pill that springs to the selected segment.
 * Keep labels short (1-2 words); for more than ~4 options use a list or menu instead.
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    require(options.isNotEmpty()) { "SegmentedControl needs at least one option" }
    val colors = AppTheme.colors
    val haptics = rememberAppHaptics()

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(TrackShape)
            .background(colors.fill)
            .padding(2.dp)
    ) {
        val segmentWidth = maxWidth / options.size
        val thumbOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex.coerceIn(0, options.lastIndex),
            animationSpec = AppMotion.snappy(),
            label = "segmentThumb"
        )
        Box(
            Modifier
                .offset { IntOffset(thumbOffset.roundToPx(), 0) }
                .width(segmentWidth)
                .fillMaxHeight()
                .appShadow(elevation = 2.dp, shape = ThumbShape, ambientAlpha = 0.08f, spotAlpha = 0.12f)
                .clip(ThumbShape)
                .background(if (colors.isDark) Color(0xFF636366) else Color.White)
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight().selectableGroup()) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                val textColor by animateColorAsState(
                    targetValue = if (enabled) colors.label else colors.tertiaryLabel,
                    animationSpec = tween(120),
                    label = "segmentText"
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = selected,
                            enabled = enabled,
                            role = Role.Tab,
                            onClick = {
                                if (!selected) haptics.light()
                                onSelect(index)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = AppType.Footnote.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@LightDarkPreview
@Composable
private fun SegmentedControlPreview() = PreviewSurface {
    var selected by remember { mutableIntStateOf(1) }
    SegmentedControl(listOf("System", "Light", "Dark"), selected, { selected = it })
}

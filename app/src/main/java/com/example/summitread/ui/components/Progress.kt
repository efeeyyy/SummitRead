package com.example.summitread.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.summitread.theme.AppMotion
import com.example.summitread.theme.AppShapes
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.theme.tabularNumbers
import kotlin.math.roundToInt

/** Circular progress with rounded caps; [content] is centred inside (e.g. a percentage). */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    strokeWidth: Dp = 8.dp,
    color: Color = AppTheme.colors.tint,
    trackColor: Color = AppTheme.colors.fill,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(target, AppMotion.gentle(), label = "ring")
    Box(
        modifier
            .size(size)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f)
                stateDescription = "${(target * 100).roundToInt()} percent"
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(size)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = strokeWidth.toPx() / 2f
            val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(trackColor, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
            if (animated > 0f) {
                drawArc(color, -90f, 360f * animated, useCenter = false, topLeft = topLeft, size = arcSize, style = stroke)
            }
        }
        content()
    }
}

/** Slim capsule progress bar. Never shows a sliver narrower than its own height. */
@Composable
fun LinearProgressCapsule(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    color: Color = AppTheme.colors.tint,
    trackColor: Color = AppTheme.colors.fill
) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(target, AppMotion.standard(), label = "capsule")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(AppShapes.pill)
            .background(trackColor)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f)
                stateDescription = "${(target * 100).roundToInt()} percent"
            }
    ) {
        if (animated > 0f) {
            Box(
                Modifier
                    .fillMaxWidth(animated)
                    .widthIn(min = height)
                    .fillMaxHeight()
                    .clip(AppShapes.pill)
                    .background(color)
            )
        }
    }
}

@LightDarkPreview
@Composable
private fun ProgressPreview() = PreviewSurface {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ProgressRing(0.68f, size = 96.dp, strokeWidth = 10.dp) {
            Text("68%", style = AppType.Headline.tabularNumbers(), color = AppTheme.colors.label)
        }
        LinearProgressCapsule(0.35f)
        LinearProgressCapsule(0.02f)
    }
}

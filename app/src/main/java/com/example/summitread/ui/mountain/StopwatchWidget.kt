package com.example.summitread.ui.mountain

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.summitread.data.PageMath
import com.example.summitread.services.ReadingTimerService
import com.example.summitread.services.TimerStatus
import com.example.summitread.theme.AppMotion
import com.example.summitread.theme.AppSpacing
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.theme.tabularNumbers
import com.example.summitread.ui.components.CardContainer
import com.example.summitread.ui.components.PillButton
import com.example.summitread.ui.components.PillSize
import com.example.summitread.ui.components.PillStyle

/**
 * Premium stopwatch card of one book (#26). Large tabular digits; while idle a full-width Start pill, once the
 * timer runs (or a stopped session waits to be logged) the card expands and the Pause/Resume/Stop pills slide in.
 * The timer service is app-wide and bound to a single book: while it is bound to another book this widget says
 * so and offers no controls. Stop needs no callback: the elapsed time is persisted as a pending session that
 * [MountainViewModel] picks up (suggested pages + timeSpentMillis on the next log), and it is shown here as
 * [pendingTimeMillis] with the suggested page count in a tinted chip.
 */
@Composable
fun StopwatchWidget(
    bookId: Long,
    modifier: Modifier = Modifier,
    pendingTimeMillis: Long = 0L,
    onUseSuggestion: (pages: Int) -> Unit = {},
    onDiscardPending: () -> Unit = {}
) {
    val context = LocalContext.current
    val timer by ReadingTimerService.state.collectAsState()
    val colors = AppTheme.colors

    val boundToThisBook = timer.status != TimerStatus.IDLE && timer.bookId == bookId
    val boundToOtherBook = timer.status != TimerStatus.IDLE && timer.bookId != bookId
    val running = boundToThisBook && timer.status == TimerStatus.RUNNING
    val paused = boundToThisBook && timer.status == TimerStatus.PAUSED
    val hasPending = !boundToThisBook && !boundToOtherBook && pendingTimeMillis > 0L

    val label = when {
        boundToOtherBook -> "Timer running for another book"
        paused -> "Paused"
        running -> "Reading now"
        hasPending -> "Last session"
        else -> "Reading timer"
    }
    val shownMillis = when {
        boundToThisBook -> timer.elapsedMillis
        hasPending -> pendingTimeMillis
        else -> 0L
    }

    CardContainer(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(AppMotion.standard())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (running) {
                LiveDot()
                Spacer(Modifier.width(AppSpacing.xs))
            }
            Text(
                text = label,
                style = AppType.Subheadline.copy(fontWeight = FontWeight.Medium),
                color = if (running) colors.destructive else colors.secondaryLabel
            )
        }

        if (!boundToOtherBook) {
            val time = ReadingTimerService.formatElapsed(shownMillis)
            Text(
                text = time,
                style = AppType.LargeTitle.copy(fontSize = 48.sp, lineHeight = 56.sp, fontWeight = FontWeight.SemiBold)
                    .tabularNumbers(),
                color = if (shownMillis > 0L) colors.label else colors.tertiaryLabel,
                maxLines = 1,
                modifier = Modifier
                    .padding(top = AppSpacing.xxs)
                    .semantics { contentDescription = "$label, $time" }
            )
        }

        // Idle: one big Start pill
        AnimatedVisibility(
            visible = !boundToThisBook && !hasPending && !boundToOtherBook,
            enter = expandVertically(AppMotion.standard()) + fadeIn(),
            exit = shrinkVertically(AppMotion.standard()) + fadeOut()
        ) {
            PillButton(
                text = "Start",
                onClick = { ReadingTimerService.start(context, bookId) },
                leadingIcon = Icons.Rounded.PlayArrow,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppSpacing.sm)
            )
        }

        // Running / paused controls slide in
        AnimatedVisibility(
            visible = boundToThisBook,
            enter = expandVertically(AppMotion.standard()) + fadeIn(),
            exit = shrinkVertically(AppMotion.standard()) + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                if (running) {
                    PillButton(
                        text = "Pause",
                        onClick = { ReadingTimerService.pause(context) },
                        style = PillStyle.Tinted,
                        leadingIcon = Icons.Rounded.Pause,
                        modifier = Modifier.weight(1f)
                    )
                    PillButton(
                        text = "Stop",
                        onClick = { ReadingTimerService.stop(context) },
                        destructive = true,
                        leadingIcon = Icons.Rounded.Stop,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    PillButton(
                        text = "Resume",
                        onClick = { ReadingTimerService.resume(context) },
                        leadingIcon = Icons.Rounded.PlayArrow,
                        modifier = Modifier.weight(1f)
                    )
                    PillButton(
                        text = "Stop",
                        onClick = { ReadingTimerService.stop(context) },
                        style = PillStyle.Tinted,
                        destructive = true,
                        leadingIcon = Icons.Rounded.Stop,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // A stopped session waiting to be logged: suggested pages (1.5 min per page)
        AnimatedVisibility(
            visible = hasPending,
            enter = expandVertically(AppMotion.standard()) + fadeIn(),
            exit = shrinkVertically(AppMotion.standard()) + fadeOut()
        ) {
            val suggested = PageMath.suggestedPages(pendingTimeMillis)
            Column(modifier = Modifier.padding(top = AppSpacing.sm)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    MountainChip(
                        text = "Suggested: $suggested pages",
                        leadingIcon = Icons.Rounded.AutoAwesome
                    )
                }
                Text(
                    text = "The time is saved with your next log.",
                    style = AppType.Footnote,
                    color = colors.secondaryLabel,
                    modifier = Modifier.padding(top = 6.dp)
                )
                PillButton(
                    text = "Use $suggested pages",
                    onClick = { onUseSuggestion(suggested) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppSpacing.sm)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppSpacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                ) {
                    PillButton(
                        text = "Start again",
                        onClick = { ReadingTimerService.start(context, bookId) },
                        style = PillStyle.Tinted,
                        size = PillSize.Small,
                        modifier = Modifier.weight(1f)
                    )
                    PillButton(
                        text = "Discard",
                        onClick = onDiscardPending,
                        style = PillStyle.Plain,
                        size = PillSize.Small,
                        destructive = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** Recording-style dot: a solid core with a soft ring that expands and fades while the timer runs. */
@Composable
private fun LiveDot() {
    val color = AppTheme.colors.destructive
    val transition = rememberInfiniteTransition(label = "live_dot")
    val ring by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "live_ring"
    )
    Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(16.dp)
                .graphicsLayer {
                    val s = 0.5f + 0.5f * ring
                    scaleX = s
                    scaleY = s
                    alpha = 0.45f * (1f - ring)
                }
                .background(color, CircleShape)
        )
        Box(Modifier.size(8.dp).background(color, CircleShape))
    }
}

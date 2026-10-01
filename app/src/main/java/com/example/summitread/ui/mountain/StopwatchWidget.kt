package com.example.summitread.ui.mountain

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.summitread.data.PageMath
import com.example.summitread.services.ReadingTimerService
import com.example.summitread.services.TimerStatus

/**
 * Sliding / expanding stopwatch of one book (#26). While idle it is a compact row with a Start button; once
 * the timer runs (or a stopped session waits to be logged) the card expands and the Pause/Resume/Stop
 * controls slide in. The timer service is app-wide and bound to a single book: while it is bound to another
 * book this widget says so and offers no controls. Stop needs no callback: the elapsed time is persisted as a
 * pending session that [MountainViewModel] picks up (suggested pages + timeSpentMillis on the next log), and
 * it is shown here as [pendingTimeMillis] with the suggested page count.
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
    val contrast = mountainContrast()

    val boundToThisBook = timer.status != TimerStatus.IDLE && timer.bookId == bookId
    val boundToOtherBook = timer.status != TimerStatus.IDLE && timer.bookId != bookId
    val running = boundToThisBook && timer.status == TimerStatus.RUNNING
    val paused = boundToThisBook && timer.status == TimerStatus.PAUSED
    val hasPending = !boundToThisBook && !boundToOtherBook && pendingTimeMillis > 0L
    val expanded = boundToThisBook || hasPending

    val label = when {
        boundToOtherBook -> "Timer running for another book"
        paused -> "Paused"
        running -> "Reading now"
        hasPending -> "Last session"
        else -> "Reading timer"
    }

    // Live indicator: a dot that pulses while the timer is running
    val pulse by rememberInfiniteTransition(label = "live_dot").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "live_dot_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(spring(stiffness = 400f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (running) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .graphicsLayer { alpha = pulse }
                                .background(MaterialTheme.colorScheme.error, CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Column {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = contrast.secondaryText
                        )
                        if (boundToThisBook) {
                            Text(
                                text = ReadingTimerService.formatElapsed(timer.elapsedMillis),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = contrast.primaryText
                            )
                        } else if (hasPending) {
                            Text(
                                text = ReadingTimerService.formatElapsed(pendingTimeMillis),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = contrast.primaryText
                            )
                        }
                    }
                }

                // Compact Start button (idle only); the controls below take over once expanded
                if (!expanded && !boundToOtherBook) {
                    FilledTonalButton(onClick = { ReadingTimerService.start(context, bookId) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Start")
                    }
                }
            }

            // Running / paused controls slide in from the side
            AnimatedVisibility(
                visible = boundToThisBook,
                enter = expandVertically() + fadeIn() + slideInHorizontally { it / 2 },
                exit = shrinkVertically() + fadeOut() + slideOutHorizontally { it / 2 }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (running) {
                        FilledTonalButton(
                            onClick = { ReadingTimerService.pause(context) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Pause")
                        }
                    } else {
                        Button(
                            onClick = { ReadingTimerService.resume(context) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Resume")
                        }
                    }
                    Button(
                        onClick = { ReadingTimerService.stop(context) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Stop")
                    }
                }
            }

            // A stopped session waiting to be logged: suggested pages (1.5 min per page)
            AnimatedVisibility(
                visible = hasPending,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val suggested = PageMath.suggestedPages(pendingTimeMillis)
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = "Suggested: $suggested pages. The time is saved with your next log.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = contrast.primaryText
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(onClick = { onUseSuggestion(suggested) }) {
                            Text("Use $suggested pages")
                        }
                        OutlinedButton(onClick = { ReadingTimerService.start(context, bookId) }) {
                            Text("Start again")
                        }
                        TextButton(onClick = onDiscardPending) {
                            Text("Discard")
                        }
                    }
                }
            }
        }
    }
}

package com.example.summitread.ui.mountain

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.summitread.services.ReadingTimerService
import com.example.summitread.services.TimerStatus

/**
 * Stopwatch of one book. The timer service is app-wide and bound to a single book: while it is bound
 * to another book this widget shows that fact and offers no controls. Stop does not need a callback:
 * the elapsed time is persisted as a pending session that [MountainViewModel] picks up for this book
 * (suggested pages + timeSpentMillis on the next log).
 */
@Composable
fun StopwatchWidget(
    bookId: Long,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val timer by ReadingTimerService.state.collectAsState()

    val boundToThisBook = timer.status != TimerStatus.IDLE && timer.bookId == bookId
    val boundToOtherBook = timer.status != TimerStatus.IDLE && timer.bookId != bookId
    val timeString = ReadingTimerService.formatElapsed(if (boundToThisBook) timer.elapsedMillis else 0L)

    val label = when {
        boundToOtherBook -> "Timer running for another book"
        boundToThisBook && timer.status == TimerStatus.PAUSED -> "Paused"
        else -> "Live Session"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = timeString,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (!boundToOtherBook) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when {
                        !boundToThisBook -> FilledIconButton(
                            onClick = { ReadingTimerService.start(context, bookId) },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                        }

                        timer.status == TimerStatus.RUNNING -> FilledIconButton(
                            onClick = { ReadingTimerService.pause(context) },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = "Pause")
                        }

                        else -> FilledIconButton(
                            onClick = { ReadingTimerService.resume(context) },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                        }
                    }

                    if (boundToThisBook) {
                        FilledIconButton(
                            onClick = { ReadingTimerService.stop(context) },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop")
                        }
                    }
                }
            }
        }
    }
}

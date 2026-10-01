package com.example.summitread.ui.mountain

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

import androidx.compose.ui.platform.LocalContext
import com.example.summitread.services.ReadingTimerService
import android.content.Intent

@Composable
fun StopwatchWidget(
    bookId: Long,
    onStopSession: (pagesEstimate: Int, timeSpentMillis: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isRunning by ReadingTimerService.isRunning.collectAsState()
    val elapsedTimeMillis by ReadingTimerService.elapsedTime.collectAsState()
    val elapsedSeconds = elapsedTimeMillis / 1000L



    val hours = elapsedSeconds / 3600
    val minutes = (elapsedSeconds % 3600) / 60
    val seconds = elapsedSeconds % 60
    val timeString = String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)

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
                    text = "Live Session",
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isRunning) {
                    FilledIconButton(
                        onClick = { 
                            val stopIntent = Intent(context, ReadingTimerService::class.java).apply {
                                action = ReadingTimerService.ACTION_STOP
                            }
                            context.startService(stopIntent)
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause")
                    }
                } else {
                    FilledIconButton(
                        onClick = { 
                            val startIntent = Intent(context, ReadingTimerService::class.java).apply {
                                action = ReadingTimerService.ACTION_START
                                putExtra(ReadingTimerService.EXTRA_BOOK_ID, bookId)
                            }
                            context.startService(startIntent)
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play")
                    }
                }

                if (elapsedSeconds > 0) {
                    FilledIconButton(
                        onClick = {
                            val stopIntent = Intent(context, ReadingTimerService::class.java).apply {
                                action = ReadingTimerService.ACTION_STOP
                            }
                            context.startService(stopIntent)
                            
                            val pagesEstimate = (elapsedSeconds / 60.0 / 1.5).roundToInt().coerceAtLeast(1)
                            onStopSession(pagesEstimate, elapsedTimeMillis)
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop")
                    }
                }
            }
        }
    }
}

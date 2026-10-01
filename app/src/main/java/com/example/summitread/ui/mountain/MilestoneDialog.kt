package com.example.summitread.ui.mountain

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.summitread.data.db.NoteEntity
import com.example.summitread.services.ReadingTimerService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Dialog opened by tapping a camp (25 / 50 / 75 %): a motivational line plus the stats up to that point (#22). */
@Composable
fun MilestoneDialog(
    bookId: Long,
    stats: MilestoneStats,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val percent = (stats.fraction * 100).toInt()
    val quote = remember(bookId, stats.fraction) { MilestoneQuotes.quoteFor(stats.fraction, bookId) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⛺ ${MilestoneQuotes.campName(stats.fraction)} - $percent%") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "\"$quote\"",
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic
                )
                StatRow("Pages at this camp", "${stats.pagesAtMilestone}")
                if (stats.reached) {
                    StatRow("Status", "Reached")
                    stats.reachedOnMillis?.let { StatRow("Reached on", dateFormat.format(Date(it))) }
                    StatRow("Sessions to get here", "${stats.sessionsToReach}")
                    if (stats.timeSpentMillis > 0L) {
                        StatRow("Time reading", ReadingTimerService.formatElapsed(stats.timeSpentMillis))
                    }
                } else {
                    StatRow("Status", "Not reached yet")
                    StatRow("Pages to go", "${stats.pagesToGo}")
                    StatRow("Sessions so far", "${stats.sessionsToReach}")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Keep climbing") }
        }
    )
}

/** Shows the text of a note when its signpost on the trail is tapped. */
@Composable
fun NoteSignpostDialog(
    note: NoteEntity,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🪧 Page ${note.pageNumber}") },
        text = { Text(note.content, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

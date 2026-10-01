package com.example.summitread.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.summitread.data.db.BookEntity
import com.example.summitread.data.db.NoteEntity
import com.example.summitread.data.db.ReadingSessionEntity
import com.example.summitread.data.db.RelicEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Book Journey (#40/#41): book facts, completion date, total time, Summit Logbook review and
 * rating, then the chronological sessions with the notes that belong to each session.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyTimelineSheet(
    relic: RelicEntity,
    book: BookEntity?,
    totalTimeMillis: Long,
    sessions: List<ReadingSessionEntity>,
    notes: List<NoteEntity>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val sortedSessions = remember(sessions) { ProfileLogic.sortedSessions(sessions) }
    val grouped = remember(sessions, notes) { ProfileLogic.groupNotes(sessions, notes) }
    val notesBySession = grouped.first
    val otherNotes = grouped.second
    val completedAt = book?.completedAtMillis ?: relic.dateEarned
    val title = book?.title?.takeIf { it.isNotBlank() } ?: relic.title

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "${relic.emoji} $title",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    val author = book?.author.orEmpty()
                    if (author.isNotBlank()) {
                        Text(
                            text = "by $author",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Completed: ${dateFormat.format(Date(completedAt))}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = if (totalTimeMillis > 0) {
                            "Total time spent: ${ProfileLogic.formatTimedDuration(totalTimeMillis)}"
                        } else {
                            "Total time spent: no timed sessions"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Summit Logbook",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (relic.rating > 0f) {
                                "Rating: ${"★".repeat(relic.rating.toInt().coerceIn(0, 5))} (${relic.rating.toInt()} / 5)"
                            } else {
                                "Not rated"
                            },
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (relic.review.isNotEmpty()) "\"${relic.review}\"" else "No review written.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Chronological Timeline",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(sortedSessions, key = { "session_${it.id}" }) { session ->
                TimelineItem(
                    session = session,
                    notes = notesBySession[session.id].orEmpty(),
                    dateFormat = dateFormat
                )
            }

            if (sortedSessions.isEmpty()) {
                item {
                    Text(
                        text = "No recorded sessions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (otherNotes.isNotEmpty()) {
                item {
                    Text(
                        text = "Other notes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(otherNotes, key = { "note_${it.id}" }) { note ->
                    NoteLine(note)
                }
            }
        }
    }
}

@Composable
private fun NoteLine(note: NoteEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "Note on pg ${note.pageNumber}: ${note.content}",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
private fun TimelineItem(
    session: ReadingSessionEntity,
    notes: List<NoteEntity>,
    dateFormat: SimpleDateFormat
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(40.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }

        Column(modifier = Modifier
            .weight(1f)
            .padding(bottom = 8.dp)) {
            Text(
                text = dateFormat.format(Date(session.date)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (session.pagesRead == 1) "Read 1 page" else "Read ${session.pagesRead} pages",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (session.timeSpentMillis > 0) {
                Text(
                    text = "Time spent: ${ProfileLogic.formatTimedDuration(session.timeSpentMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            if (notes.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                notes.forEach { NoteLine(it) }
            }
        }
    }
}

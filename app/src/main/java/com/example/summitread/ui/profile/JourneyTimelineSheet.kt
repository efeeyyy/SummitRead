package com.example.summitread.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.summitread.data.db.NoteEntity
import com.example.summitread.data.db.ReadingSessionEntity
import com.example.summitread.data.db.RelicEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyTimelineSheet(
    relic: RelicEntity,
    sessions: List<ReadingSessionEntity>,
    notes: List<NoteEntity>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "${relic.emoji} ${relic.title} - Book Journey",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (relic.review.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Final Thoughts",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"${relic.review}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        if (relic.rating > 0f) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Rating: ${relic.rating.toInt()} / 5",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Text(
                text = "Chronological Timeline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val sortedSessions = sessions.sortedBy { it.date }
                
                items(sortedSessions) { session ->
                    val sessionNotes = notes.filter { 
                        it.sessionId == session.id
                    }
                    
                    TimelineItem(
                        session = session,
                        notes = sessionNotes,
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
            }
        }
    }
}

@Composable
private fun TimelineItem(
    session: ReadingSessionEntity,
    notes: List<NoteEntity>,
    dateFormat: SimpleDateFormat
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(60.dp)
        ) {
            Box(
                modifier = Modifier.size(12.dp).background(MaterialTheme.colorScheme.primary, shape = androidx.compose.foundation.shape.CircleShape)
            )
            Box(
                modifier = Modifier.width(2.dp).height(40.dp).background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
        
        Column(modifier = Modifier.weight(1f).padding(bottom = 16.dp)) {
            Text(
                text = dateFormat.format(Date(session.date)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Read ${session.pagesRead} pages",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (session.timeSpentMillis > 0) {
                val mins = session.timeSpentMillis / (1000 * 60)
                Text(
                    text = "Time spent: $mins mins",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            if (notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                notes.forEach { note ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "Note on pg ${note.pageNumber}: ${note.content}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

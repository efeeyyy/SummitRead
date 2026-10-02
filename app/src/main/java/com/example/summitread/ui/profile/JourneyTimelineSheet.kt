package com.example.summitread.ui.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.summitread.data.db.BookEntity
import com.example.summitread.data.db.NoteEntity
import com.example.summitread.data.db.ReadingSessionEntity
import com.example.summitread.data.db.RelicEntity
import com.example.summitread.theme.AppShapes
import com.example.summitread.theme.AppSpacing
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.ui.components.AppSheet
import com.example.summitread.ui.components.CardContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Book Journey (#40/#41): book facts, completion date, total time, Summit Logbook review and
 * rating, then the chronological sessions with the notes that belong to each session.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun JourneyTimelineSheet(
    relic: RelicEntity,
    book: BookEntity?,
    totalTimeMillis: Long,
    sessions: List<ReadingSessionEntity>,
    notes: List<NoteEntity>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val sortedSessions = remember(sessions) { ProfileLogic.sortedSessions(sessions) }
    val grouped = remember(sessions, notes) { ProfileLogic.groupNotes(sessions, notes) }
    val notesBySession = grouped.first
    val otherNotes = grouped.second
    val completedAt = book?.completedAtMillis ?: relic.dateEarned
    val title = book?.title?.takeIf { it.isNotBlank() } ?: relic.title
    val author = book?.author.orEmpty()

    AppSheet(
        onDismissRequest = onDismiss,
        title = "Journey",
        containerColor = colors.groupedBackground,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = AppSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            item(key = "summary") {
                CardContainer(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "${relic.emoji} $title",
                        style = AppType.Title2,
                        color = colors.label
                    )
                    if (author.isNotBlank()) {
                        Text("by $author", style = AppType.Subheadline, color = colors.secondaryLabel)
                    }
                    Spacer(Modifier.height(AppSpacing.sm))
                    Text(
                        text = "Summit reached ${dateFormat.format(Date(completedAt))}",
                        style = AppType.Subheadline,
                        color = colors.gold
                    )
                    Text(
                        text = if (totalTimeMillis > 0) {
                            "Total time spent: ${ProfileLogic.formatTimedDuration(totalTimeMillis)}"
                        } else {
                            "Total time spent: no timed sessions"
                        },
                        style = AppType.Subheadline,
                        color = colors.secondaryLabel
                    )
                }
            }

            item(key = "logbook") {
                CardContainer(modifier = Modifier.fillMaxWidth()) {
                    Text("Summit Logbook", style = AppType.Footnote, color = colors.secondaryLabel)
                    Spacer(Modifier.height(AppSpacing.xxs))
                    Text(
                        text = if (relic.rating > 0f) {
                            "Rating: ${"★".repeat(relic.rating.toInt().coerceIn(0, 5))} (${relic.rating.toInt()} / 5)"
                        } else {
                            "Not rated"
                        },
                        style = AppType.Headline,
                        color = if (relic.rating > 0f) colors.gold else colors.label
                    )
                    Spacer(Modifier.height(AppSpacing.xxs))
                    Text(
                        text = if (relic.review.isNotEmpty()) "\"${relic.review}\"" else "No review written.",
                        style = AppType.Body,
                        color = colors.label
                    )
                }
            }

            item(key = "timeline_header") {
                Text(
                    text = "Chronological Timeline",
                    style = AppType.Title3,
                    color = colors.label,
                    modifier = Modifier
                        .padding(start = AppSpacing.xs, top = AppSpacing.xs)
                        .semantics { heading() }
                )
            }

            if (sortedSessions.isEmpty()) {
                item(key = "no_sessions") {
                    Text(
                        text = "No recorded sessions.",
                        style = AppType.Subheadline,
                        color = colors.secondaryLabel,
                        modifier = Modifier.padding(start = AppSpacing.xs)
                    )
                }
            } else {
                itemsIndexed(sortedSessions, key = { _, s -> "session_${s.id}" }) { index, session ->
                    TimelineItem(
                        session = session,
                        notes = notesBySession[session.id].orEmpty(),
                        dateFormat = dateFormat,
                        isFirst = index == 0,
                        isLast = index == sortedSessions.lastIndex,
                        summit = index == sortedSessions.lastIndex && book?.isCompleted == true
                    )
                }
            }

            if (otherNotes.isNotEmpty()) {
                item(key = "other_header") {
                    Text(
                        text = "Other notes",
                        style = AppType.Title3,
                        color = colors.label,
                        modifier = Modifier
                            .padding(start = AppSpacing.xs, top = AppSpacing.xs)
                            .semantics { heading() }
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
private fun NoteLine(note: NoteEntity, modifier: Modifier = Modifier) {
    Text(
        text = "Note on pg ${note.pageNumber}: ${note.content}",
        style = AppType.Footnote,
        color = AppTheme.colors.label,
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.small)
            .background(AppTheme.colors.fill)
            .padding(AppSpacing.sm)
    )
}

/** One session row on a vertical rail: dot (gold for the completing summit) + connecting line. */
@Composable
private fun TimelineItem(
    session: ReadingSessionEntity,
    notes: List<NoteEntity>,
    dateFormat: SimpleDateFormat,
    isFirst: Boolean,
    isLast: Boolean,
    summit: Boolean
) {
    val colors = AppTheme.colors
    val dotColor = if (summit) colors.gold else colors.tint
    val lineColor = colors.separator
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Canvas(
            modifier = Modifier
                .width(28.dp)
                .fillMaxHeight()
        ) {
            val cx = size.width / 2f
            val dotY = 14.dp.toPx()
            val stroke = 2.dp.toPx()
            if (!isFirst) drawLine(lineColor, Offset(cx, 0f), Offset(cx, dotY), stroke)
            if (!isLast) drawLine(lineColor, Offset(cx, dotY), Offset(cx, size.height), stroke)
            drawCircle(dotColor, radius = if (summit) 7.dp.toPx() else 5.dp.toPx(), center = Offset(cx, dotY))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = AppSpacing.sm)
        ) {
            CardContainer(
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.medium
            ) {
                Text(
                    text = dateFormat.format(Date(session.date)),
                    style = AppType.Footnote,
                    color = if (summit) colors.gold else colors.secondaryLabel
                )
                Text(
                    text = if (session.pagesRead == 1) "Read 1 page" else "Read ${session.pagesRead} pages",
                    style = AppType.Headline,
                    color = colors.label
                )
                if (session.timeSpentMillis > 0) {
                    Text(
                        text = "Time spent: ${ProfileLogic.formatTimedDuration(session.timeSpentMillis)}",
                        style = AppType.Footnote,
                        color = colors.secondaryLabel
                    )
                }
                if (notes.isNotEmpty()) {
                    Spacer(Modifier.height(AppSpacing.xs))
                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                        notes.forEach { NoteLine(it) }
                    }
                }
            }
        }
    }
}

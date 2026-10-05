package com.example.summitread.ui.mountain

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.summitread.data.ElapsedTimeFormat
import com.example.summitread.data.db.NoteEntity
import com.example.summitread.platform.rememberDateFormatter
import com.example.summitread.theme.AppSpacing
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.ui.components.AppSheet
import com.example.summitread.ui.components.GroupedRow
import com.example.summitread.ui.components.InsetGroupedSection
import com.example.summitread.ui.components.PillButton
import com.example.summitread.ui.components.PillStyle
import com.example.summitread.ui.components.ProgressRing
import com.example.summitread.ui.components.RowTrailing

/**
 * Sheet opened by tapping a camp (25 / 50 / 75 %): a progress ring, a motivational line and the stats up to that
 * point in an iOS grouped list (#22).
 */
@Composable
fun MilestoneDialog(
    bookId: Long,
    stats: MilestoneStats,
    onDismiss: () -> Unit
) {
    val colors = AppTheme.colors
    val dateFormat = rememberDateFormatter("MMM d, yyyy")
    val percent = (stats.fraction * 100).toInt()
    val quote = remember(bookId, stats.fraction) { MilestoneQuotes.quoteFor(stats.fraction, bookId) }

    AppSheet(onDismissRequest = onDismiss, title = "⛺ ${MilestoneQuotes.campName(stats.fraction)}") {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProgressRing(
                progress = if (stats.reached) 1f else stats.fraction,
                size = 88.dp,
                strokeWidth = 9.dp,
                color = if (stats.reached) colors.success else colors.tint
            ) {
                Text("$percent%", style = AppType.Title3, color = colors.label)
            }
            Text(
                text = "\"$quote\"",
                style = AppType.Callout,
                fontStyle = FontStyle.Italic,
                color = colors.secondaryLabel,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = AppSpacing.sm, bottom = AppSpacing.md, start = AppSpacing.xs, end = AppSpacing.xs)
            )
        }

        InsetGroupedSection {
            row { GroupedRow("Pages at this camp", trailing = RowTrailing.Value("${stats.pagesAtMilestone}")) }
            if (stats.reached) {
                row { GroupedRow("Status", trailing = RowTrailing.Value("Reached")) }
                stats.reachedOnMillis?.let {
                    row { GroupedRow("Reached on", trailing = RowTrailing.Value(dateFormat.format(it))) }
                }
                row { GroupedRow("Sessions to get here", trailing = RowTrailing.Value("${stats.sessionsToReach}")) }
                if (stats.timeSpentMillis > 0L) {
                    row {
                        GroupedRow(
                            "Time reading",
                            trailing = RowTrailing.Value(ElapsedTimeFormat.format(stats.timeSpentMillis))
                        )
                    }
                }
            } else {
                row { GroupedRow("Status", trailing = RowTrailing.Value("Not reached yet")) }
                row { GroupedRow("Pages to go", trailing = RowTrailing.Value("${stats.pagesToGo}")) }
                row { GroupedRow("Sessions so far", trailing = RowTrailing.Value("${stats.sessionsToReach}")) }
            }
        }

        PillButton(
            text = "Keep climbing",
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppSpacing.md)
        )
    }
}

/** Shows the text of a note when its signpost on the trail is tapped. */
@Composable
fun NoteSignpostDialog(
    note: NoteEntity,
    onDismiss: () -> Unit
) {
    AppSheet(onDismissRequest = onDismiss, title = "🪧 Page ${note.pageNumber}") {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            NoteCard(note = note)
            PillButton(
                text = "Close",
                onClick = onDismiss,
                style = PillStyle.Tinted,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

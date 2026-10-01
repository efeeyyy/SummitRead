package com.example.summitread.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.summitread.data.db.BookEntity
import androidx.compose.foundation.layout.Column

/** Asks before a book is archived; archiving only hides it, sessions, notes and relics are kept. */
@Composable
fun ArchiveConfirmDialog(
    bookTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Archive book?") },
        text = {
            Text(
                "\"$bookTitle\" will be hidden from your shelf and list. " +
                    "Your reading sessions, notes and relics are kept, and you can unarchive it any time " +
                    "from the Archived screen."
            )
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Archive") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** The archived books with an "Unarchive" action each. */
@Composable
fun ArchivedBooksList(
    books: List<BookEntity>,
    onUnarchive: (BookEntity) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    if (books.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "No archived books.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = contentPadding) {
        items(books, key = { it.id }) { book ->
            val baseColor = bookBaseColor(book)
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CoverImage(
                        path = book.coverImagePath,
                        contentDescription = "Cover of ${book.title}",
                        modifier = Modifier
                            .width(40.dp)
                            .height(60.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        fallback = { VectorCover(base = baseColor) }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${book.author} · ${bookProgressPercent(book)}% Read",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TextButton(
                        onClick = { onUnarchive(book) },
                        modifier = Modifier.semantics { contentDescription = "Unarchive ${book.title}" }
                    ) {
                        Text("Unarchive")
                    }
                }
            }
        }
    }
}


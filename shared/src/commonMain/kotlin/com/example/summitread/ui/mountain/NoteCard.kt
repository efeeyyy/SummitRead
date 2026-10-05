package com.example.summitread.ui.mountain

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.summitread.data.db.NoteEntity
import com.example.summitread.platform.rememberDateFormatter
import com.example.summitread.theme.AppMotion
import com.example.summitread.theme.AppSpacing
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.ui.components.CardContainer
import com.example.summitread.ui.components.IconButtonStyle
import com.example.summitread.ui.components.IconCircleButton

/**
 * Premium note card: page badge + date, the note text and, when there is one, the photo as a slightly tilted
 * polaroid. Tap to expand long text (or [onSignpostClick] when given); [onDelete] adds a trash button.
 */
@Composable
fun NoteCard(
    note: NoteEntity,
    onSignpostClick: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    var isExpanded by remember { mutableStateOf(false) }

    // Slight, deterministic tilt of the polaroid (from the note id)
    val tilt = remember(note.id) {
        when ((note.id % 5).toInt()) {
            0 -> -2.0f
            1 -> 1.5f
            2 -> -1.0f
            3 -> 2.2f
            else -> -1.6f
        }
    }

    val dateFormat = rememberDateFormatter("MMM dd, yyyy")

    CardContainer(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(AppMotion.standard()),
        onClick = onSignpostClick ?: { isExpanded = !isExpanded }
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                MountainChip(text = "p.${note.pageNumber}")
                Text(
                    text = dateFormat.format(note.dateCreated),
                    style = AppType.Footnote,
                    color = colors.secondaryLabel
                )
            }
            if (onDelete != null) {
                IconCircleButton(
                    icon = Icons.Rounded.Delete,
                    contentDescription = "Delete note on page ${note.pageNumber}",
                    onClick = onDelete,
                    style = IconButtonStyle.Plain,
                    tint = colors.destructive,
                    size = 32.dp
                )
            }
        }

        Text(
            text = note.content,
            style = AppType.Body,
            color = colors.label,
            maxLines = if (isExpanded) Int.MAX_VALUE else 5,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = AppSpacing.xs)
        )

        val photoPath = note.photoPath
        if (photoPath != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppSpacing.sm),
                contentAlignment = Alignment.Center
            ) {
                PolaroidPhoto(
                    imagePath = photoPath,
                    pageNumber = note.pageNumber,
                    modifier = Modifier.graphicsLayer { rotationZ = tilt }
                )
            }
        }
    }
}


package com.example.summitread.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.summitread.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.example.summitread.theme.AppShapes
import com.example.summitread.data.db.BookEntity
import com.example.summitread.theme.AppSpacing
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.ui.components.AlertAction
import com.example.summitread.ui.components.AlertActionStyle
import com.example.summitread.ui.components.AppAlertDialog
import com.example.summitread.ui.components.InsetGroupedSection
import com.example.summitread.ui.components.PillButton
import com.example.summitread.ui.components.PillSize
import com.example.summitread.ui.components.PillStyle

/** Asks before a book is archived; archiving only hides it, sessions, notes and relics are kept. */
@Composable
fun ArchiveConfirmDialog(
    bookTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AppAlertDialog(
        title = stringResource(Res.string.archive_book_title),
        message = stringResource(Res.string.archive_book_message, bookTitle),
        onDismissRequest = onDismiss,
        actions = listOf(
            AlertAction(stringResource(Res.string.archive), onConfirm, AlertActionStyle.Destructive),
            AlertAction(stringResource(Res.string.cancel), onDismiss, AlertActionStyle.Cancel)
        )
    )
}

/** The archived books as an inset-grouped list with an "Unarchive" action per row. */
@Composable
fun ArchivedBooksList(
    books: List<BookEntity>,
    onUnarchive: (BookEntity) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues()
) {
    if (books.isEmpty()) {
        HomeEmptyState(
            icon = Icons.Rounded.Inventory2,
            title = stringResource(Res.string.empty_archive_title),
            message = stringResource(Res.string.empty_archive_message),
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding)
        )
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = AppSpacing.screenHorizontal,
            end = AppSpacing.screenHorizontal,
            top = contentPadding.calculateTopPadding() + AppSpacing.xs,
            bottom = contentPadding.calculateBottomPadding() + AppSpacing.xl
        )
    ) {
        item(key = "archived-section") {
            InsetGroupedSection(
                footer = stringResource(Res.string.archived_footer),
                separatorInset = 68.dp
            ) {
                books.forEach { book -> row { ArchivedRow(book, onUnarchive = { onUnarchive(book) }) } }
            }
        }
    }
}

@Composable
private fun ArchivedRow(book: BookEntity, onUnarchive: () -> Unit) {
    val colors = AppTheme.colors
    val unarchiveLabel = stringResource(Res.string.unarchive_book, book.title)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = AppSpacing.md, top = 10.dp, bottom = 10.dp, end = AppSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        CoverThumbnail(book = book, width = 40.dp, height = 60.dp, corner = 6.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = book.title,
                style = AppType.Headline,
                color = colors.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(Res.string.author_and_percent, book.author, bookProgressPercent(book)),
                style = AppType.Subheadline,
                color = colors.secondaryLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        PillButton(
            text = stringResource(Res.string.unarchive),
            onClick = onUnarchive,
            style = PillStyle.Tinted,
            size = PillSize.Small,
            modifier = Modifier.semantics { contentDescription = unarchiveLabel }
        )
    }
}

/** Calm empty state: round icon badge, title, one line of explanation and an optional action. */
@Composable
fun HomeEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    actionIcon: ImageVector? = null
) {
    val colors = AppTheme.colors
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(horizontal = AppSpacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)
        ) {
            Box(
                Modifier
                    .padding(bottom = AppSpacing.sm)
                    .size(96.dp)
                    .clip(AppShapes.pill)
                    .background(colors.tintContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.tint,
                    modifier = Modifier.size(44.dp)
                )
            }
            Text(
                text = title,
                style = AppType.Title2,
                color = colors.label,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = AppType.Subheadline,
                color = colors.secondaryLabel,
                textAlign = TextAlign.Center
            )
            if (actionText != null && onAction != null) {
                PillButton(
                    text = actionText,
                    onClick = onAction,
                    leadingIcon = actionIcon,
                    modifier = Modifier.padding(top = AppSpacing.md)
                )
            }
        }
    }
}

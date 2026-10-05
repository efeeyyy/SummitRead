package com.example.summitread.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import org.jetbrains.compose.resources.pluralStringResource
import com.example.summitread.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.summitread.theme.AppMotion
import com.example.summitread.theme.AppShapes
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.theme.rememberAppHaptics
import com.example.summitread.ui.components.AppMenu
import com.example.summitread.ui.components.AppMenuItem
import com.example.summitread.ui.components.GroupedRow
import com.example.summitread.ui.components.HairlineWidth
import com.example.summitread.ui.components.InsetGroupedSection
import com.example.summitread.ui.components.RowTrailing
import com.example.summitread.ui.components.appShadow
import com.example.summitread.ui.components.pressableScale
import com.example.summitread.data.db.BookEntity
import com.example.summitread.theme.tabularNumbers

/**
 * Streak flame as a refined pill: tinted capsule with the flame and the day count. The flame is grey while
 * there is no streak and does a small spring "heartbeat" whenever the number changes.
 */
@Composable
fun StreakBadge(streak: Int, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    val active = streak > 0
    val bump = remember { Animatable(1f) }
    LaunchedEffect(streak) {
        if (streak > 0) {
            bump.animateTo(1.28f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
            bump.animateTo(1f, AppMotion.standard())
        }
    }
    val description = pluralStringResource(Res.plurals.streak_days, streak, streak)
    Row(
        modifier = modifier
            .clip(AppShapes.pill)
            .background(
                if (active) colors.warning.copy(alpha = if (colors.isDark) 0.22f else 0.14f) else colors.fill
            )
            .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 6.dp)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.LocalFireDepartment,
            contentDescription = null,
            tint = if (active) colors.warning else colors.tertiaryLabel,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer {
                    scaleX = bump.value
                    scaleY = bump.value
                }
        )
        Text(
            text = streak.toString(),
            style = AppType.Subheadline.copy(fontWeight = FontWeight.SemiBold).tabularNumbers(),
            color = if (active) colors.label else colors.secondaryLabel,
            maxLines = 1
        )
    }
}

/** "Sort: Manual Order" capsule that opens the sort menu. Shown above the list view. */
@Composable
fun SortChip(
    current: SortOption,
    onSelect: (SortOption) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val haptics = rememberAppHaptics()
    var open by remember { mutableStateOf(false) }
    val label = stringResource(Res.string.sort_prefix, current.label)
    Box(modifier) {
        Row(
            modifier = Modifier
                .clip(AppShapes.pill)
                .background(colors.fill)
                .pressableScale(
                    onClick = {
                        haptics.light()
                        open = true
                    },
                    role = Role.Button
                )
                .semantics { contentDescription = label }
                .padding(start = 10.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Rounded.SwapVert,
                contentDescription = null,
                tint = colors.tint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = AppType.Subheadline.copy(fontWeight = FontWeight.Medium),
                color = colors.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clearAndSetSemantics { }
            )
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = colors.secondaryLabel,
                modifier = Modifier.size(18.dp)
            )
        }
        AppMenu(expanded = open, onDismissRequest = { open = false }) {
            SortOption.entries.forEach { option ->
                AppMenuItem(
                    text = option.label,
                    selected = option == current,
                    onClick = {
                        haptics.light()
                        onSelect(option)
                        open = false
                    }
                )
            }
        }
    }
}

/** "12 books" caption, singular aware. */
@Composable
fun bookCountText(count: Int): String =
    pluralStringResource(Res.plurals.book_count, count, count)

/** Entry row to the archived books at the end of the library (inset grouped, like an iOS Settings row). */
@Composable
fun ArchivedEntry(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    InsetGroupedSection(modifier = modifier, separatorInset = 58.dp) {
        row {
            GroupedRow(
                title = stringResource(Res.string.archived_entry),
                leadingIcon = Icons.Rounded.Inventory2,
                leadingTint = AppTheme.colors.secondaryLabel,
                trailing = RowTrailing.ValueChevron(count.toString()),
                onClick = onClick
            )
        }
    }
}

/** A cover (image or vector fallback) with rounded corners, a hairline edge and a soft shadow. */
@Composable
fun CoverThumbnail(
    book: BookEntity,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    corner: Dp = 8.dp,
    describe: Boolean = true
) {
    val shape = RoundedCornerShape(corner)
    CoverImage(
        path = book.coverImagePath,
        contentDescription = if (describe) stringResource(Res.string.cover_of, book.title) else null,
        modifier = modifier
            .size(width, height)
            .appShadow(elevation = 4.dp, shape = shape)
            .clip(shape)
            .border(HairlineWidth, AppTheme.colors.separator, shape),
        fallback = { VectorCover(base = bookBaseColor(book)) }
    )
}


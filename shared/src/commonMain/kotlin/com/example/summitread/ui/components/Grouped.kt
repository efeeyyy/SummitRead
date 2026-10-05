package com.example.summitread.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.summitread.theme.AppShapes
import com.example.summitread.theme.AppSpacing
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.theme.rememberAppHaptics

object GroupedRowDefaults {
    /** Separator inset for rows without a leading icon. */
    val SeparatorInset: Dp = 16.dp

    /** Separator inset for rows with a 30dp icon badge (aligns with the text). */
    val IconSeparatorInset: Dp = 58.dp
}

/** Collects the rows of an [InsetGroupedSection] so separators can be drawn between them. */
class GroupedListScope internal constructor() {
    internal val rows = mutableListOf<@Composable () -> Unit>()

    /** Adds one row. Use [GroupedRow] or any custom composable. */
    fun row(content: @Composable () -> Unit) {
        rows += content
    }
}

/**
 * iOS Settings-style section: optional small-caps header, rounded white (dark: 1C1C1E) card with
 * hairline separators between rows, optional footer text.
 *
 * ```
 * InsetGroupedSection(header = "Appearance") {
 *     row { GroupedRow("Theme", leadingIcon = Icons.Rounded.Palette, trailing = RowTrailing.ValueChevron("System"), onClick = ...) }
 *     row { GroupedRow("Notifications", trailing = RowTrailing.Switch(on, ::setOn)) }
 * }
 * ```
 */
@Composable
fun InsetGroupedSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    separatorInset: Dp = GroupedRowDefaults.SeparatorInset,
    content: GroupedListScope.() -> Unit
) {
    val colors = AppTheme.colors
    val rows = GroupedListScope().apply(content).rows
    Column(modifier.fillMaxWidth()) {
        if (header != null) {
            Text(
                text = header.uppercase(),
                style = AppType.Footnote,
                color = colors.secondaryLabel,
                modifier = Modifier
                    .padding(start = AppSpacing.lg, end = AppSpacing.md, bottom = 6.dp)
                    .semantics { heading() }
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(AppShapes.medium)
                .background(colors.surface)
        ) {
            rows.forEachIndexed { index, row ->
                row()
                if (index < rows.lastIndex) {
                    Box(
                        Modifier
                            .padding(start = separatorInset)
                            .fillMaxWidth()
                            .height(HairlineWidth)
                            .background(colors.separator)
                    )
                }
            }
        }
        if (footer != null) {
            Text(
                text = footer,
                style = AppType.Footnote,
                color = colors.secondaryLabel,
                modifier = Modifier.padding(start = AppSpacing.lg, end = AppSpacing.md, top = 6.dp)
            )
        }
    }
}

/** What sits at the end of a [GroupedRow]. */
sealed interface RowTrailing {
    data object None : RowTrailing
    data object Chevron : RowTrailing
    data class Value(val text: String) : RowTrailing
    data class ValueChevron(val text: String) : RowTrailing
    data class Switch(val checked: Boolean, val onCheckedChange: (Boolean) -> Unit) : RowTrailing
    data class Custom(val content: @Composable () -> Unit) : RowTrailing
}

/**
 * One row of an [InsetGroupedSection]. With [onClick] the row is tappable (press-scale-free
 * highlight keeps it feeling like a list); a [RowTrailing.Switch] makes the whole row toggleable.
 */
@Composable
fun GroupedRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    leadingTint: Color = AppTheme.colors.tint,
    trailing: RowTrailing = RowTrailing.None,
    destructive: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val colors = AppTheme.colors
    val haptics = rememberAppHaptics()

    val rowModifier = when {
        trailing is RowTrailing.Switch -> Modifier.toggleable(
            value = trailing.checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = {
                haptics.toggle(it)
                trailing.onCheckedChange(it)
            }
        )
        onClick != null -> Modifier.clickable(
            enabled = enabled,
            role = Role.Button,
            onClick = onClick
        )
        else -> Modifier
    }

    Row(
        modifier
            .fillMaxWidth()
            .then(rowModifier)
            .defaultMinSize(minHeight = if (subtitle == null) 44.dp else 56.dp)
            .padding(horizontal = AppSpacing.md, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
    ) {
        if (leadingIcon != null) IconBadge(leadingIcon, color = leadingTint)
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = AppType.Body,
                color = if (destructive) colors.destructive else colors.label,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = AppType.Footnote,
                    color = colors.secondaryLabel,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        when (trailing) {
            RowTrailing.None -> Unit
            RowTrailing.Chevron -> ChevronIcon()
            is RowTrailing.Value -> TrailingValue(trailing.text)
            is RowTrailing.ValueChevron -> {
                TrailingValue(trailing.text)
                ChevronIcon()
            }
            is RowTrailing.Switch -> AppSwitch(checked = trailing.checked, onCheckedChange = null, enabled = enabled)
            is RowTrailing.Custom -> trailing.content()
        }
    }
}

@Composable
private fun TrailingValue(text: String) {
    Text(
        text = text,
        style = AppType.Body,
        color = AppTheme.colors.secondaryLabel,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.widthIn(max = 160.dp)
    )
}

@Composable
private fun ChevronIcon() {
    Icon(
        imageVector = Icons.Rounded.ChevronRight,
        contentDescription = null,
        tint = AppTheme.colors.tertiaryLabel,
        modifier = Modifier.size(20.dp)
    )
}

/** iOS Settings-style 30dp rounded-square icon badge with a white glyph on a solid colour. */
@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.tint,
    size: Dp = 30.dp
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.23f))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (color == AppTheme.colors.tint) AppTheme.colors.onTint else Color.White,
            modifier = Modifier.size(size * 0.6f)
        )
    }
}

/**
 * iOS-looking switch built on the Material one (same 52x32 footprint): white thumb, tint track,
 * no outline. Pass `onCheckedChange = null` when a parent row owns the toggle.
 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = AppTheme.colors
    val haptics = rememberAppHaptics()
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange?.let { cb ->
            { value ->
                haptics.toggle(value)
                cb(value)
            }
        },
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = colors.tint,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = if (colors.isDark) Color(0xFF39393D) else Color(0xFFE5E5EA),
            uncheckedBorderColor = Color.Transparent,
            disabledCheckedTrackColor = colors.tint.copy(alpha = 0.4f),
            disabledUncheckedTrackColor = colors.fill,
            disabledCheckedThumbColor = Color.White,
            disabledUncheckedThumbColor = Color.White,
            disabledCheckedBorderColor = Color.Transparent,
            disabledUncheckedBorderColor = Color.Transparent
        )
    )
}

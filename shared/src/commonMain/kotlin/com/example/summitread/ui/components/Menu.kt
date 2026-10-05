package com.example.summitread.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.summitread.theme.AppShapes
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.theme.rememberAppHaptics

/**
 * iOS-style context / pull-down menu: 14dp rounded panel on the opaque elevated surface (a popup is its own
 * window, so it cannot blur), hairline edge, soft shadow, Inter rows with a trailing icon. Use [AppMenuItem] rows
 * and [AppMenuDivider] between groups.
 */
@Composable
fun AppMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.widthIn(min = 220.dp),
        shape = AppShapes.medium,
        containerColor = AppTheme.colors.elevatedSurface,
        tonalElevation = 0.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(HairlineWidth, AppTheme.colors.separator),
        content = content
    )
}

/**
 * One row of an [AppMenu]. [icon] is shown trailing (iOS puts glyphs on the right); a [selected] row shows a tint
 * check mark instead. [destructive] colours the row red.
 */
@Composable
fun AppMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    selected: Boolean = false,
    destructive: Boolean = false
) {
    val colors = AppTheme.colors
    val haptics = rememberAppHaptics()
    val textColor = if (destructive) colors.destructive else colors.label
    val trailing = if (selected) Icons.Rounded.Check else icon
    DropdownMenuItem(
        text = {
            Text(
                text,
                style = AppType.Body,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingIcon = trailing?.let {
            {
                Icon(
                    it,
                    contentDescription = null,
                    tint = if (selected) colors.tint else if (destructive) colors.destructive else colors.secondaryLabel,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        onClick = {
            haptics.light()
            onClick()
        },
        colors = MenuDefaults.itemColors(textColor = textColor),
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .semantics {
                role = Role.Button
                this.selected = selected
            }
    )
}

/** Hairline between menu groups. */
@Composable
fun AppMenuDivider() {
    HorizontalDivider(thickness = HairlineWidth, color = AppTheme.colors.separator)
}

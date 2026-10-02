package com.example.summitread.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.summitread.theme.AppShapes
import com.example.summitread.theme.AppSpacing
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.theme.tabularNumbers

/**
 * Premium card: surface fill, soft shadow in light theme, lighter elevated fill in dark theme.
 * Pass [onClick] to make it tappable (spring press-scale, no ripple).
 */
@Composable
fun CardContainer(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    shape: Shape = AppShapes.large,
    contentPadding: PaddingValues = PaddingValues(AppSpacing.md),
    containerColor: Color = AppTheme.colors.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .then(if (onClick != null) Modifier.pressableScale(onClick, onClickLabel = onClickLabel) else Modifier)
            .appShadow(elevation = 8.dp, shape = shape)
            .clip(shape)
            .background(containerColor)
            .padding(contentPadding),
        content = content
    )
}

/** Compact metric card: optional icon, big number, caption. */
@Composable
fun StatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = AppTheme.colors.tint,
    onClick: (() -> Unit)? = null
) {
    val colors = AppTheme.colors
    CardContainer(
        modifier = modifier.semantics(mergeDescendants = true) { },
        onClick = onClick,
        contentPadding = PaddingValues(AppSpacing.md)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = AppSpacing.xs))
        }
        Text(
            text = value,
            style = AppType.Title1.tabularNumbers(),
            color = colors.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = AppType.Footnote,
            color = colors.secondaryLabel,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@LightDarkPreview
@Composable
private fun CardsPreview() = PreviewSurface {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            StatTile("12", "Day streak", Modifier.weight(1f), icon = Icons.Rounded.LocalFireDepartment, iconTint = AppTheme.colors.warning)
            StatTile("4h 20m", "This week", Modifier.weight(1f), icon = Icons.Rounded.Schedule)
        }
        CardContainer(Modifier.fillMaxWidth(), onClick = {}) {
            Text("The Hobbit", style = AppType.Headline, color = AppTheme.colors.label)
            Text("212 of 310 pages", style = AppType.Subheadline, color = AppTheme.colors.secondaryLabel)
        }
    }
}

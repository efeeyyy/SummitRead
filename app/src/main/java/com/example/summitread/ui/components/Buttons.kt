package com.example.summitread.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Share
import com.example.summitread.theme.AppShapes
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.theme.rememberAppHaptics

enum class PillStyle {
    /** Solid tint, high emphasis (primary action). */
    Filled,
    /** Tint at ~15% with tint text (secondary action). */
    Tinted,
    /** Text only (tertiary action). */
    Plain
}

enum class PillSize { Large, Small }

/**
 * iOS capsule button. [destructive] swaps the tint for the destructive red.
 * Large is 50dp tall (full-width friendly), Small is 34dp (a 48dp touch target is still kept).
 */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PillStyle = PillStyle.Filled,
    size: PillSize = PillSize.Large,
    enabled: Boolean = true,
    destructive: Boolean = false,
    leadingIcon: ImageVector? = null,
    haptic: Boolean = true
) {
    val colors = AppTheme.colors
    val haptics = rememberAppHaptics()
    val accent = if (destructive) colors.destructive else colors.tint
    val onAccent = if (destructive) Color.White else colors.onTint
    val background = when (style) {
        PillStyle.Filled -> accent
        PillStyle.Tinted -> accent.copy(alpha = if (colors.isDark) 0.22f else 0.14f)
        PillStyle.Plain -> Color.Transparent
    }
    val content = if (style == PillStyle.Filled) onAccent else accent
    val height = if (size == PillSize.Large) 50.dp else 34.dp
    val horizontal = if (size == PillSize.Large) 24.dp else 16.dp
    val textStyle = if (size == PillSize.Large) AppType.Headline else AppType.Subheadline.copy(
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
    )
    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .then(if (size == PillSize.Small) Modifier.minimumInteractiveComponentSize() else Modifier)
            .alpha(if (enabled) 1f else 0.4f)
            .pressScale(interaction, enabled = enabled)
            .defaultMinSize(minHeight = height)
            .clip(AppShapes.pill)
            .background(background)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    if (haptic) haptics.light()
                    onClick()
                }
            )
            .padding(horizontal = horizontal),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.defaultMinSize(minHeight = height)
        ) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
            }
            Text(text = text, style = textStyle, color = content, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

enum class IconButtonStyle {
    /** Grey translucent circle (default; works on any background). */
    Tinted,
    /** Solid tint circle. */
    Filled,
    /** Glyph only. */
    Plain
}

/**
 * Circular icon button. [contentDescription] is mandatory: icon-only controls must be labelled.
 * [size] is the visible circle; the touch target is always >= 48dp.
 */
@Composable
fun IconCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: IconButtonStyle = IconButtonStyle.Tinted,
    size: androidx.compose.ui.unit.Dp = 36.dp,
    enabled: Boolean = true,
    tint: Color = AppTheme.colors.tint
) {
    val colors = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val haptics = rememberAppHaptics()
    val (background, glyph) = when (style) {
        IconButtonStyle.Tinted -> colors.fill to tint
        IconButtonStyle.Filled -> tint to colors.onTint
        IconButtonStyle.Plain -> Color.Transparent to tint
    }
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .alpha(if (enabled) 1f else 0.4f)
            .pressScale(interaction, enabled = enabled)
            .size(size)
            .clip(AppShapes.pill)
            .background(background)
            .semantics { this.contentDescription = contentDescription }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    haptics.light()
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = glyph, modifier = Modifier.size(size * 0.5f))
    }
}

@LightDarkPreview
@Composable
private fun ButtonsPreview() = PreviewSurface {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PillButton("Continue reading", onClick = {}, modifier = Modifier.width(300.dp))
        PillButton("Add book", onClick = {}, style = PillStyle.Tinted, leadingIcon = Icons.Rounded.Add)
        PillButton("Not now", onClick = {}, style = PillStyle.Plain)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PillButton("Save", onClick = {}, size = PillSize.Small)
            PillButton("Delete", onClick = {}, size = PillSize.Small, style = PillStyle.Tinted, destructive = true)
            PillButton("Disabled", onClick = {}, size = PillSize.Small, enabled = false)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconCircleButton(Icons.Rounded.Share, "Share", onClick = {})
            IconCircleButton(Icons.Rounded.Check, "Done", onClick = {}, style = IconButtonStyle.Filled)
            IconCircleButton(Icons.Rounded.Close, "Close", onClick = {}, style = IconButtonStyle.Plain)
        }
    }
}

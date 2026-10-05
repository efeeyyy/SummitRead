package com.example.summitread.ui.mountain

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.summitread.theme.AppShapes
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.theme.rememberAppHaptics
import com.example.summitread.ui.components.pressableScale

/**
 * True when the app's own theme (not the system setting) is dark. The theme mode can be forced by the user,
 * so [androidx.compose.foundation.isSystemInDarkTheme] would be wrong here (#14, #15).
 */
@Composable
fun isAppDarkTheme(): Boolean = AppTheme.isDark

/**
 * Small capsule used on the Mountain screen: quick-add pages (+5 / +10 / +25), the timer's suggestion chip and
 * the page badge of a note. With [onClick] it is a tappable chip with press-scale and a light haptic;
 * without it, a static tinted badge.
 */
@Composable
internal fun MountainChip(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentDescription: String? = null,
    leadingIcon: ImageVector? = null,
    tint: Color = AppTheme.colors.tint,
    filled: Boolean = false
) {
    val colors = AppTheme.colors
    val haptics = rememberAppHaptics()
    val background = if (filled) tint else tint.copy(alpha = if (colors.isDark) 0.22f else 0.13f)
    val content = if (filled) colors.onTint else tint
    Row(
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier.pressableScale(
                        onClick = {
                            haptics.light()
                            onClick()
                        },
                        role = Role.Button
                    )
                } else Modifier
            )
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier)
            .defaultMinSize(minHeight = if (onClick != null) 44.dp else 24.dp)
            .clip(AppShapes.pill)
            .background(background)
            .padding(horizontal = if (onClick != null) 14.dp else 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        }
        Text(
            text = text,
            style = if (onClick != null) AppType.Subheadline.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            else AppType.Caption1.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

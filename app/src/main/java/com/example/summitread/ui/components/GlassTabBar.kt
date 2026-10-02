package com.example.summitread.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.summitread.theme.AppShapes
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.theme.rememberAppHaptics
import androidx.compose.animation.core.tween

@Immutable
data class GlassTabItem(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
)

/**
 * Bottom tab bar on frosted glass; replaces the Material NavigationBar.
 *
 * [floating] true (default) is the iOS 26 capsule that hovers above the content;
 * false is the classic edge-to-edge bar with a hairline on top.
 * Pass the same [glassState] you gave to `Modifier.glassSource` on the screen content.
 * Inside `LargeTitleScaffold(bottomBar = ...)` it is picked up automatically.
 */
@Composable
fun GlassTabBar(
    items: List<GlassTabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    glassState: GlassState? = LocalGlassState.current,
    floating: Boolean = true
) {
    val colors = AppTheme.colors
    val haptics = rememberAppHaptics()
    val select: (Int) -> Unit = { index ->
        if (index != selectedIndex) haptics.light()
        onSelect(index)
    }

    if (floating) {
        Box(
            modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .appShadow(elevation = 14.dp, shape = AppShapes.pill)
                    .glassBackground(glassState, AppShapes.pill, GlassThickness.Regular)
                    .border(HairlineWidth, colors.separator, AppShapes.pill)
                    .padding(4.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items.forEachIndexed { index, item ->
                    TabItem(item, index == selectedIndex, { select(index) }, floating = true)
                }
            }
        }
    } else {
        Column(modifier.fillMaxWidth().glassBackground(glassState, thickness = GlassThickness.Regular)) {
            Box(Modifier.fillMaxWidth().height(HairlineWidth).background(colors.separator))
            Row(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 8.dp)
                    .selectableGroup()
            ) {
                items.forEachIndexed { index, item ->
                    TabItem(item, index == selectedIndex, { select(index) }, floating = false)
                }
            }
        }
    }
}

@Composable
private fun RowScope.TabItem(
    item: GlassTabItem,
    selected: Boolean,
    onClick: () -> Unit,
    floating: Boolean
) {
    val colors = AppTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.tint else colors.secondaryLabel,
        animationSpec = tween(150),
        label = "tabColor"
    )
    val indicator by animateColorAsState(
        targetValue = if (selected && floating) colors.tint.copy(alpha = if (colors.isDark) 0.22f else 0.13f)
        else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = tween(180),
        label = "tabIndicator"
    )
    Column(
        Modifier
            .weight(1f)
            .height(if (floating) 52.dp else 56.dp)
            .clip(AppShapes.pill)
            .background(indicator)
            .pressScale(interaction)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (selected) item.selectedIcon else item.icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = item.label,
            style = AppType.TabLabel,
            color = contentColor,
            maxLines = 1,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@LightDarkPreview
@Composable
private fun GlassTabBarPreview() = PreviewSurface {
    var selected by remember { mutableIntStateOf(1) }
    val items = listOf(
        GlassTabItem("Library", Icons.Rounded.AutoStories),
        GlassTabItem("Mountain", Icons.Rounded.Landscape),
        GlassTabItem("Profile", Icons.Rounded.Person)
    )
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GlassTabBar(items, selected, { selected = it }, glassState = null)
        GlassTabBar(items, selected, { selected = it }, glassState = null, floating = false)
    }
}

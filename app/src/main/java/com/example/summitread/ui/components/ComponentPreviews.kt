package com.example.summitread.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.summitread.theme.AppMotion
import com.example.summitread.theme.AppShapes
import com.example.summitread.theme.AppSpacing
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.theme.rememberAppHaptics
import com.example.summitread.theme.tabularNumbers
import kotlin.math.roundToInt
import androidx.compose.ui.tooling.preview.Preview
import com.example.summitread.theme.SummitReadTheme

@LightDarkPreview
@Composable
private fun AlertPreview() = PreviewSurface {
    Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)) {
        AlertContent(
            title = "Delete this book?",
            message = "Your reading sessions for it will be removed too.",
            actions = listOf(
                AlertAction("Delete", {}, AlertActionStyle.Destructive),
                AlertAction("Cancel", {}, AlertActionStyle.Cancel)
            )
        )
        AlertContent(
            title = "Summit reached",
            message = null,
            actions = listOf(
                AlertAction("Share", {}),
                AlertAction("Add another book", {}),
                AlertAction("Done", {}, AlertActionStyle.Cancel)
            )
        )
    }
}

@LightDarkPreview
@Composable
private fun AppSheetPreview() = PreviewSurface {
    Column(
        Modifier
            .fillMaxWidth()
            .background(AppTheme.colors.elevatedSurface, AppShapes.sheet)
            .padding(AppSpacing.md),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SheetGrabber()
        Text("Log reading", style = AppType.Headline, color = AppTheme.colors.label)
        Box(Modifier.height(AppSpacing.md))
        PillButton("Save", onClick = {}, modifier = Modifier.fillMaxWidth())
    }
}

@LightDarkPreview
@Composable
private fun AppTextFieldPreview() = PreviewSurface {
    var text by remember { mutableStateOf("") }
    var filled by remember { mutableStateOf("The Hobbit") }
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        AppTextField(text, { text = it }, placeholder = "Search books", leadingIcon = Icons.Rounded.Search)
        AppTextField(filled, { filled = it }, placeholder = "Title")
        AppTextField("", {}, placeholder = "Pages", isError = true, supportingText = "Enter a number")
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

@LightDarkPreview
@Composable
private fun GroupedPreview() = PreviewSurface {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
        InsetGroupedSection(
            header = "Appearance",
            footer = "The accent colour is used for buttons and highlights.",
            separatorInset = GroupedRowDefaults.IconSeparatorInset
        ) {
            row {
                GroupedRow(
                    "Theme", leadingIcon = Icons.Rounded.Palette,
                    trailing = RowTrailing.ValueChevron("System"), onClick = {}
                )
            }
            row {
                GroupedRow(
                    "Reminders", subtitle = "Every evening at 20:00", leadingIcon = Icons.Rounded.Notifications,
                    leadingTint = Color(0xFFFF9500), trailing = RowTrailing.Switch(true) {}
                )
            }
            row {
                GroupedRow(
                    "Reading goal", leadingIcon = Icons.Rounded.Timer,
                    leadingTint = Color(0xFF5856D6), trailing = RowTrailing.Value("30 min"), onClick = {}
                )
            }
        }
        InsetGroupedSection {
            row { GroupedRow("Delete all data", destructive = true, onClick = {}) }
        }
    }
}

@LightDarkPreview
@Composable
private fun LargeTitleScaffoldPreview() {
    com.example.summitread.theme.SummitReadTheme {
        LargeTitleScaffold(
            title = "Library",
            actions = {
                IconCircleButton(Icons.Rounded.Add, "Add book", onClick = {})
                IconCircleButton(Icons.Rounded.MoreHoriz, "More", onClick = {})
            }
        ) { padding ->
            LazyColumn(contentPadding = padding) {
                items((1..20).toList()) { i ->
                    Text(
                        "Book $i",
                        style = AppType.Body,
                        color = AppTheme.colors.label,
                        modifier = Modifier.padding(AppSpacing.md)
                    )
                }
            }
        }
    }
}

@LightDarkPreview
@Composable
private fun ProgressPreview() = PreviewSurface {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ProgressRing(0.68f, size = 96.dp, strokeWidth = 10.dp) {
            Text("68%", style = AppType.Headline.tabularNumbers(), color = AppTheme.colors.label)
        }
        LinearProgressCapsule(0.35f)
        LinearProgressCapsule(0.02f)
    }
}

@LightDarkPreview
@Composable
private fun SegmentedControlPreview() = PreviewSurface {
    var selected by remember { mutableIntStateOf(1) }
    SegmentedControl(listOf("System", "Light", "Dark"), selected, { selected = it })
}


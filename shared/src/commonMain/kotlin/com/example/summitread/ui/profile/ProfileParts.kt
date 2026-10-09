package com.example.summitread.ui.profile

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.summitread.data.ThemeMode
import com.example.summitread.theme.AccentColor
import com.example.summitread.theme.AppMotion
import com.example.summitread.theme.AppShapes
import com.example.summitread.theme.AppSpacing
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.theme.rememberAppHaptics
import com.example.summitread.theme.tabularNumbers
import com.example.summitread.ui.components.AppSheet
import com.example.summitread.ui.components.AppTextField
import com.example.summitread.ui.components.GroupedRow
import com.example.summitread.ui.components.GroupedRowDefaults
import com.example.summitread.ui.components.IconCircleButton
import com.example.summitread.ui.components.InsetGroupedSection
import com.example.summitread.ui.components.PillButton
import com.example.summitread.ui.components.PillStyle
import com.example.summitread.ui.components.RowTrailing
import com.example.summitread.ui.components.SegmentedControl
import com.example.summitread.ui.components.pressableScale
import kotlinx.coroutines.delay

// ---------------------------------------------------------------------------------------------
// Hero
// ---------------------------------------------------------------------------------------------

/** Tier colour language: Novice quiet, Expedition Leader tinted, Elite Alpinist gold. */
@Composable
private fun tierColor(tier: AvatarTier): Color = when (tier) {
    AvatarTier.NOVICE -> AppTheme.colors.secondaryLabel
    AvatarTier.EXPEDITION_LEADER -> AppTheme.colors.tint
    AvatarTier.ELITE_ALPINIST -> AppTheme.colors.gold
}

@Composable
internal fun ProfileHero(
    username: String,
    tier: AvatarTier,
    completedBooks: Int,
    streak: Int,
    breathing: Boolean
) {
    val colors = AppTheme.colors
    val accent = tierColor(tier)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ClimberAvatar(tier = tier, breathing = breathing)
        Spacer(Modifier.height(AppSpacing.md))
        Text(
            text = username.ifBlank { "Climber" },
            style = AppType.Title2,
            color = colors.label,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(AppSpacing.xs))
        Box(
            modifier = Modifier
                .clip(AppShapes.pill)
                .background(accent.copy(alpha = 0.16f))
                .padding(horizontal = AppSpacing.sm, vertical = 4.dp)
        ) {
            Text(text = tier.displayName, style = AppType.Subheadline, color = accent)
        }
        Spacer(Modifier.height(AppSpacing.xs))
        val next = tier.next
        val progress = if (next != null) {
            val left = next.minBooks - completedBooks
            "$left more ${if (left == 1) "book" else "books"} to ${next.displayName}"
        } else {
            "Highest tier reached"
        }
        Text(
            text = "$progress  ·  $streak-day streak",
            style = AppType.Footnote,
            color = colors.secondaryLabel,
            textAlign = TextAlign.Center
        )
    }
}

/** Avatar per tier: Novice (climber), Expedition Leader (+ backpack), Elite Alpinist (+ helmet and ice axe). */
@Composable
private fun ClimberAvatar(tier: AvatarTier, breathing: Boolean) {
    val accent = tierColor(tier)
    val scale = if (breathing) {
        val transition = rememberInfiniteTransition(label = "avatar_breathe")
        val value by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.07f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "avatar_scale"
        )
        value
    } else {
        1f
    }

    Box(
        modifier = Modifier
            .size(120.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.14f))
            .then(
                if (tier == AvatarTier.NOVICE) Modifier
                else Modifier.border(3.dp, accent, CircleShape)
            )
            .semantics { contentDescription = "Climber avatar, ${tier.displayName}" },
        contentAlignment = Alignment.Center
    ) {
        Text(text = "🧗", fontSize = 60.sp, modifier = Modifier.clearAndSetSemantics { })
        if (tier.tierNumber >= 2) {
            Text(
                text = "🎒",
                fontSize = 26.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp, bottom = 12.dp)
                    .clearAndSetSemantics { }
            )
        }
        if (tier.tierNumber >= 3) {
            Text(
                text = "⛑️",
                fontSize = 26.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp)
                    .clearAndSetSemantics { }
            )
            Text(
                text = "🪓",
                fontSize = 26.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = 12.dp)
                    .clearAndSetSemantics { }
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Goal sheet
// ---------------------------------------------------------------------------------------------

@Composable
internal fun GoalSheet(currentGoal: Int, year: Int, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    val haptics = rememberAppHaptics()
    var input by rememberSaveable { mutableStateOf(currentGoal.toString()) }
    val value = input.toIntOrNull() ?: 0
    fun setValue(v: Int) {
        input = v.coerceIn(1, 999).toString()
    }

    AppSheet(onDismissRequest = onDismiss, title = "Reading goal $year") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconCircleButton(
                icon = Icons.Rounded.Remove,
                contentDescription = "Decrease goal",
                onClick = { setValue(value - 1) },
                size = 44.dp,
                enabled = value > 1
            )
            Column(
                modifier = Modifier
                    .padding(horizontal = AppSpacing.xl)
                    .semantics(mergeDescendants = true) { },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (value > 0) value.toString() else "-",
                    style = AppType.LargeTitle.tabularNumbers(),
                    color = colors.label
                )
                Text("books this year", style = AppType.Footnote, color = colors.secondaryLabel)
            }
            IconCircleButton(
                icon = Icons.Rounded.Add,
                contentDescription = "Increase goal",
                onClick = { setValue(value + 1) },
                size = 44.dp,
                enabled = value < 999
            )
        }
        Spacer(Modifier.height(AppSpacing.md))
        PillButton(
            text = "Save",
            enabled = value > 0,
            onClick = {
                haptics.logged()
                onSave(value)
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth()
        )
        PillButton(
            text = "Cancel",
            style = PillStyle.Plain,
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Settings
// ---------------------------------------------------------------------------------------------

@Composable
internal fun ProfileSettings(
    themeMode: ThemeMode,
    onThemeMode: (ThemeMode) -> Unit,
    accentColorIndex: Int,
    onAccent: (Int) -> Unit,
    notificationsEnabled: Boolean,
    onNotifications: (Boolean) -> Unit,
    username: String,
    onUsername: (String) -> Unit
) {
    val modes = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
    val labels = listOf("System", "Light", "Dark")
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)) {
        // One InsetGroupedSection draws the whole card with a single surface fill, so Dark mode has
        // no darker lower band (the old Card + ListItem stack mixed two container colours).
        InsetGroupedSection(header = "Appearance") {
            row {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
                ) {
                    Text("Theme", style = AppType.Body, color = AppTheme.colors.label)
                    Spacer(Modifier.height(AppSpacing.xs))
                    SegmentedControl(
                        options = labels,
                        selectedIndex = modes.indexOf(themeMode).coerceAtLeast(0),
                        onSelect = { onThemeMode(modes[it]) }
                    )
                }
            }
            row {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm)
                ) {
                    Text("Accent Color", style = AppType.Body, color = AppTheme.colors.label)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AccentColor.entries.forEachIndexed { index, color ->
                            AccentSwatch(
                                color = color,
                                selected = accentColorIndex == index,
                                onClick = { onAccent(index) }
                            )
                        }
                    }
                }
            }
        }

        InsetGroupedSection(
            header = "Notifications",
            footer = "Streak reminders that nudge you to read today.",
            separatorInset = GroupedRowDefaults.IconSeparatorInset
        ) {
            row {
                // Writes UserPreferences.notificationsEnabled, which the streak reminder
                // scheduler/worker read.
                GroupedRow(
                    title = "Reading reminders",
                    leadingIcon = Icons.Rounded.Notifications,
                    leadingTint = AppTheme.colors.destructive,
                    trailing = RowTrailing.Switch(notificationsEnabled, onNotifications)
                )
            }
        }

        InsetGroupedSection(header = "Account") {
            row {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs)
                ) {
                    UsernameField(savedName = username, onSave = onUsername)
                }
            }
        }
    }
}

/** Local text state, saved debounced / on leave, so typing never fights the async DataStore flow. */
@Composable
private fun UsernameField(savedName: String, onSave: (String) -> Unit) {
    var edited by rememberSaveable { mutableStateOf<String?>(null) }
    val latestEdited by rememberUpdatedState(edited)
    val latestSave by rememberUpdatedState(onSave)

    LaunchedEffect(edited) {
        val text = edited ?: return@LaunchedEffect
        delay(500)
        latestSave(text)
    }
    DisposableEffect(Unit) {
        onDispose { latestEdited?.let { latestSave(it) } }
    }

    AppTextField(
        value = edited ?: savedName,
        onValueChange = { edited = it },
        placeholder = "Username",
        leadingIcon = Icons.Rounded.Person,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AccentSwatch(color: AccentColor, selected: Boolean, onClick: () -> Unit) {
    val haptics = rememberAppHaptics()
    val fill = if (AppTheme.isDark) color.darkPrimary else color.lightPrimary
    val onFill = if (AppTheme.isDark) color.darkOnPrimary else color.lightOnPrimary
    val ring by animateColorAsState(
        targetValue = if (selected) AppTheme.colors.label else Color.Transparent,
        animationSpec = AppMotion.fade(),
        label = "swatchRing"
    )
    // 30dp swatch inside a 48dp touch target.
    Box(
        modifier = Modifier
            .size(48.dp)
            .pressableScale(onClick = {
                haptics.light()
                onClick()
            }, role = Role.RadioButton)
            .semantics {
                this.selected = selected
                contentDescription = "${color.displayName} accent color"
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .border(2.dp, ring, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(fill),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = onFill,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

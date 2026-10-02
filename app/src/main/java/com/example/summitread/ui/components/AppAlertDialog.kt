package com.example.summitread.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.summitread.theme.AppMotion
import com.example.summitread.theme.AppShapes
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.theme.rememberAppHaptics

enum class AlertActionStyle {
    /** Regular tinted action. */
    Default,
    /** The safe choice: semibold. Put it last (right when two actions are side by side). */
    Cancel,
    /** Red text for destructive confirmation. */
    Destructive
}

@Immutable
data class AlertAction(
    val text: String,
    val onClick: () -> Unit,
    val style: AlertActionStyle = AlertActionStyle.Default
)

/**
 * iOS alert: 280dp-wide centred panel, title + message, hairline-separated actions.
 * Two actions sit side by side, three or more stack. Prefer [AppSheet] for anything with inputs.
 *
 * An optional [content] slot (centred, below the message) holds small custom bodies such as a stats list.
 *
 * Every action must dismiss the dialog itself (set the state that shows it to false).
 */
@Composable
fun AppAlertDialog(
    title: String,
    onDismissRequest: () -> Unit,
    actions: List<AlertAction>,
    message: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { shown = true }
        val progress by animateFloatAsState(if (shown) 1f else 0f, AppMotion.snappy(), label = "alertIn")
        Box(
            Modifier.graphicsLayer {
                val s = 0.92f + 0.08f * progress
                scaleX = s
                scaleY = s
                alpha = progress.coerceIn(0f, 1f)
            }
        ) {
            AlertContent(title, message, actions, content)
        }
    }
}

@Composable
private fun AlertContent(
    title: String,
    message: String?,
    actions: List<AlertAction>,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    val colors = AppTheme.colors
    Column(
        Modifier
            .width(280.dp)
            .appShadow(elevation = 24.dp, shape = AppShapes.alert)
            .clip(AppShapes.alert)
            .background(colors.elevatedSurface)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = AppType.Headline,
                color = colors.label,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            if (message != null) {
                Text(
                    text = message,
                    style = AppType.Footnote,
                    color = colors.label,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (content != null) content()
        }
        Hairline()
        if (actions.size == 2) {
            Row(Modifier.height(IntrinsicSize.Min)) {
                AlertButton(actions[0], Modifier.weight(1f))
                Box(Modifier.width(HairlineWidth).fillMaxHeight().background(colors.separator))
                AlertButton(actions[1], Modifier.weight(1f))
            }
        } else {
            actions.forEachIndexed { index, action ->
                AlertButton(action, Modifier.fillMaxWidth())
                if (index < actions.lastIndex) Hairline()
            }
        }
    }
}

@Composable
private fun Hairline() {
    Box(Modifier.fillMaxWidth().height(HairlineWidth).background(AppTheme.colors.separator))
}

@Composable
private fun AlertButton(action: AlertAction, modifier: Modifier) {
    val colors = AppTheme.colors
    val haptics = rememberAppHaptics()
    val color = if (action.style == AlertActionStyle.Destructive) colors.destructive else colors.tint
    Box(
        modifier
            .defaultMinSize(minHeight = 44.dp)
            .clickable(role = Role.Button) {
                if (action.style == AlertActionStyle.Destructive) haptics.warning()
                action.onClick()
            }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = action.text,
            style = AppType.Body.copy(
                fontWeight = if (action.style == AlertActionStyle.Cancel) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = color,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

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

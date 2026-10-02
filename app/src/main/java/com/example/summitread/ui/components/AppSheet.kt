package com.example.summitread.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.summitread.theme.AppShapes
import com.example.summitread.theme.AppSpacing
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType

/**
 * iOS-style modal sheet: 28dp top corners, small grabber, elevated background, centred title.
 * The sheet lives in its own window, so it cannot blur the screen behind it; it uses the opaque
 * elevated surface instead (glass is for in-window bars only).
 *
 * Use it instead of full-screen dialogs for forms and pickers. Content is a normal Column:
 * add [PillButton]s and [InsetGroupedSection]s directly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor: Color = AppTheme.colors.elevatedSurface,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = AppShapes.sheet,
        containerColor = containerColor,
        contentColor = AppTheme.colors.label,
        tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = 0.32f),
        dragHandle = { SheetGrabber() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.md)
                .padding(bottom = AppSpacing.md)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = AppType.Headline,
                    color = AppTheme.colors.label,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = AppSpacing.md)
                        .semantics { heading() }
                )
            }
            content()
        }
    }
}

@Composable
internal fun SheetGrabber(modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(vertical = 8.dp)
            .width(36.dp)
            .height(5.dp)
            .background(AppTheme.colors.tertiaryLabel.copy(alpha = 0.6f), AppShapes.pill)
    )
}

/** Static look of a sheet for previews (ModalBottomSheet itself needs a window). */
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

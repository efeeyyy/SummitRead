package com.example.summitread.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.summitread.theme.AppShapes
import com.example.summitread.theme.AppSpacing
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType

/**
 * Rounded filled text field with placeholder and optional leading icon. A 1.5dp tint ring appears
 * on focus (destructive colour when [isError]). The [placeholder] doubles as the accessibility label.
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
) {
    val colors = AppTheme.colors
    val focused by interactionSource.collectIsFocusedAsState()
    val ring by animateColorAsState(
        targetValue = when {
            isError -> colors.destructive
            focused -> colors.tint
            else -> androidx.compose.ui.graphics.Color.Transparent
        },
        animationSpec = tween(150),
        label = "fieldRing"
    )

    Column(modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { if (placeholder.isNotEmpty()) contentDescription = placeholder },
            enabled = enabled,
            singleLine = singleLine,
            minLines = minLines,
            maxLines = maxLines,
            textStyle = AppType.Body.copy(color = colors.label),
            cursorBrush = SolidColor(colors.tint),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            decorationBox = { inner ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(AppShapes.medium)
                        .background(colors.fill)
                        .border(1.5.dp, ring, AppShapes.medium)
                        .padding(horizontal = AppSpacing.sm, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                ) {
                    if (leadingIcon != null) {
                        Icon(
                            leadingIcon,
                            contentDescription = null,
                            tint = colors.secondaryLabel,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(placeholder, style = AppType.Body, color = colors.tertiaryLabel, maxLines = 1)
                        }
                        inner()
                    }
                }
            }
        )
        if (supportingText != null) {
            Text(
                text = supportingText,
                style = AppType.Caption1,
                color = if (isError) colors.destructive else colors.secondaryLabel,
                modifier = Modifier.padding(start = AppSpacing.sm, top = 4.dp)
            )
        }
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

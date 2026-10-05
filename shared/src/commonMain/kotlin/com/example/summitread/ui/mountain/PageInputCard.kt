package com.example.summitread.ui.mountain

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.summitread.platform.rememberDateFormatter
import com.example.summitread.theme.AppSpacing
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.theme.tabularNumbers
import com.example.summitread.ui.components.CardContainer
import com.example.summitread.ui.components.IconButtonStyle
import com.example.summitread.ui.components.IconCircleButton
import com.example.summitread.ui.components.LinearProgressCapsule
import com.example.summitread.ui.components.PillButton
import kotlin.math.roundToInt

/**
 * Page logging card: an iOS-stepper-like control (big centred number you can also type into, minus / plus
 * circle buttons), quick-add chips, a capsule progress bar with "X / Y pages - Z%" and a filled Log Pages pill
 * that is only enabled for a valid amount. The success haptic and the sparks are fired by the screen once the
 * ViewModel has actually saved the log.
 */
@Composable
fun PageInputCard(
    selectedDate: Long,
    pagesInput: String,
    onPagesInputChange: (String) -> Unit,
    onLogPages: () -> Unit,
    currentPages: Int,
    totalPages: Int,
    isLogging: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val dateFormat = rememberDateFormatter("MMM dd")
    val formattedDate = remember(selectedDate) { dateFormat.format(selectedDate) }
    val progress = if (totalPages > 0) (currentPages.toFloat() / totalPages.toFloat()).coerceIn(0f, 1f) else 0f
    val percent = (progress * 100).roundToInt()
    val value = pagesInput.toIntOrNull() ?: 0
    val pagesLeft = (totalPages - currentPages).coerceAtLeast(0)

    CardContainer(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Pages read on $formattedDate",
            style = AppType.Headline,
            color = colors.label
        )

        // Stepper: minus | big number | plus
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppSpacing.sm)
        ) {
            IconCircleButton(
                icon = Icons.Rounded.Remove,
                contentDescription = "Decrease pages",
                onClick = { if (value > 0) onPagesInputChange((value - 1).toString()) },
                size = 44.dp,
                enabled = value > 0
            )

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                BasicTextField(
                    value = pagesInput,
                    onValueChange = { text ->
                        if (text.length <= 5 && text.all { it.isDigit() }) onPagesInputChange(text)
                    },
                    textStyle = AppType.LargeTitle
                        .copy(fontFamily = LocalTextStyle.current.fontFamily, fontSize = 52.sp, lineHeight = 60.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, color = colors.label)
                        .tabularNumbers(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    cursorBrush = SolidColor(colors.tint),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Pages read" },
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.Center) {
                            if (pagesInput.isEmpty()) {
                                Text(
                                    text = "0",
                                    style = AppType.LargeTitle
                                        .copy(fontFamily = LocalTextStyle.current.fontFamily, fontSize = 52.sp, lineHeight = 60.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                                        .tabularNumbers(),
                                    color = colors.tertiaryLabel
                                )
                            }
                            inner()
                        }
                    }
                )
            }

            IconCircleButton(
                icon = Icons.Rounded.Add,
                contentDescription = "Increase pages",
                onClick = { onPagesInputChange((value + 1).toString()) },
                style = IconButtonStyle.Filled,
                size = 44.dp
            )
        }

        // Quick add
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs, Alignment.CenterHorizontally)
        ) {
            for (amount in QUICK_ADD_AMOUNTS) {
                MountainChip(
                    text = "+$amount",
                    onClick = { onPagesInputChange((value + amount).toString()) },
                    contentDescription = "Add $amount pages"
                )
            }
        }

        // Progress
        Text(
            text = "$currentPages / $totalPages pages · $percent%",
            style = AppType.Footnote.tabularNumbers(),
            color = colors.secondaryLabel,
            modifier = Modifier.padding(top = AppSpacing.md)
        )
        LinearProgressCapsule(
            progress = progress,
            color = if (progress >= 1f) colors.gold else colors.tint,
            modifier = Modifier.padding(top = 6.dp)
        )

        if (totalPages > 0 && value > pagesLeft) {
            Text(
                text = if (pagesLeft > 0) "Only $pagesLeft ${if (pagesLeft == 1) "page" else "pages"} left, extra pages are ignored."
                else "Every page is already read.",
                style = AppType.Footnote,
                color = colors.warning,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        PillButton(
            text = if (isLogging) "Logging..." else "Log Pages",
            onClick = onLogPages,
            enabled = !isLogging && value > 0,
            haptic = false, // the screen plays the success haptic once the log is saved
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppSpacing.md)
        )
    }
}

private val QUICK_ADD_AMOUNTS = intArrayOf(5, 10, 25)

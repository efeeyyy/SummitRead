package com.example.summitread.ui.mountain

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.summitread.theme.AppSpacing
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.theme.rememberAppHaptics
import com.example.summitread.ui.components.AppSheet
import com.example.summitread.ui.components.AppTextField
import com.example.summitread.ui.components.PillButton
import com.example.summitread.ui.components.PillStyle
import com.example.summitread.ui.components.pressableScale

/** Summit Logbook (#32): a 1 to 5 star rating and a review, shown as a sheet once the book is finished. */
@Composable
fun SummitLogbookDialog(
    bookTitle: String,
    onSave: (Float, String) -> Unit,
    onDismissRequest: () -> Unit
) {
    val colors = AppTheme.colors
    val haptics = rememberAppHaptics()
    var review by rememberSaveable { mutableStateOf("") }
    var rating by rememberSaveable { mutableFloatStateOf(5f) }

    AppSheet(onDismissRequest = onDismissRequest, title = "Sign the Summit Logbook") {
        Text(
            text = "You've conquered $bookTitle! Leave your thoughts and a rating below.",
            style = AppType.Subheadline,
            color = colors.secondaryLabel,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppSpacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (star in 1..5) {
                    val filled = star <= rating.toInt()
                    Icon(
                        imageVector = if (filled) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = null,
                        tint = if (filled) colors.gold else colors.tertiaryLabel,
                        modifier = Modifier
                            .size(44.dp)
                            .semantics {
                                contentDescription = "$star ${if (star == 1) "star" else "stars"}"
                                selected = filled
                            }
                            .pressableScale(
                                onClick = {
                                    haptics.light()
                                    rating = star.toFloat()
                                },
                                role = Role.Button
                            )
                    )
                }
            }
            Text(
                text = "Rating: ${rating.toInt()}/5",
                style = AppType.Footnote,
                color = colors.secondaryLabel,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        AppTextField(
            value = review,
            onValueChange = { review = it },
            placeholder = "Your Review",
            singleLine = false,
            minLines = 4,
            maxLines = 6,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppSpacing.md)
        )

        PillButton(
            text = "Sign Logbook",
            onClick = { onSave(rating, review) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppSpacing.md)
        )
        PillButton(
            text = "Skip",
            onClick = onDismissRequest,
            style = PillStyle.Plain,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

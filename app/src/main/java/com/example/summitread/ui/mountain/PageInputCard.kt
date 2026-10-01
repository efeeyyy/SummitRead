package com.example.summitread.ui.mountain

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    val formattedDate = remember(selectedDate) { dateFormat.format(Date(selectedDate)) }
    val progress = if (totalPages > 0) currentPages.toFloat() / totalPages.toFloat() else 0f
    val contrast = mountainContrast()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, if (contrast.isDark) contrast.border.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Text(
                text = "Pages read on $formattedDate",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = contrast.primaryText
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Input row: minus | textfield | plus
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                FilledIconButton(
                    onClick = {
                        val current = pagesInput.toIntOrNull() ?: 0
                        if (current > 0) {
                            onPagesInputChange((current - 1).toString())
                        }
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("−", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(12.dp))

                OutlinedTextField(
                    value = pagesInput,
                    onValueChange = { value ->
                        if (value.isEmpty() || value.all { it.isDigit() }) {
                            onPagesInputChange(value)
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = contrast.primaryText
                    ),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = contrast.border,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedTextColor = contrast.primaryText,
                        unfocusedTextColor = contrast.primaryText
                    ),
                    singleLine = true,
                    modifier = Modifier.width(96.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                FilledIconButton(
                    onClick = {
                        val current = pagesInput.toIntOrNull() ?: 0
                        onPagesInputChange((current + 1).toString())
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Increment")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress text + bar
            Text(
                text = "$currentPages / $totalPages pages",
                style = MaterialTheme.typography.bodySmall,
                color = contrast.secondaryText
            )

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = if (contrast.isDark) contrast.border.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Log Pages button (disabled while a log is being written: no double submit)
            Button(
                onClick = onLogPages,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLogging && (pagesInput.toIntOrNull() ?: 0) > 0
            ) {
                Text(if (isLogging) "Logging..." else "Log Pages", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

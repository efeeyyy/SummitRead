package com.example.summitread.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.summitread.R
import com.example.summitread.data.ImageStorage
import com.example.summitread.theme.AccentColor
import kotlinx.coroutines.launch

@Composable
fun AddBookDialog(
    onDismissRequest: () -> Unit,
    onSave: (
        title: String,
        author: String,
        totalPages: Int,
        coverImagePath: String?,
        accentColorIndex: Int,
        coverColorArgb: Int?
    ) -> Unit
) {
    // Draft state survives rotation and process death (rememberSaveable).
    var title by rememberSaveable { mutableStateOf("") }
    var author by rememberSaveable { mutableStateOf("") }
    var totalPages by rememberSaveable { mutableStateOf("") }
    var coverImagePath by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedColorIndex by rememberSaveable { mutableIntStateOf(0) }
    // True once the wheel was used: then the exact colour is stored in coverColorArgb. Presets store only the index.
    var useCustomColor by rememberSaveable { mutableStateOf(false) }
    val initialHsv = remember { AccentColor.entries[0].lightPrimary.toHsv() }
    var hue by rememberSaveable { mutableFloatStateOf(initialHsv.hue) }
    var saturation by rememberSaveable { mutableFloatStateOf(initialHsv.saturation) }
    var brightness by rememberSaveable { mutableFloatStateOf(initialHsv.value) }
    val hsv = Hsv(hue, saturation, brightness)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val copied = ImageStorage.copyImageToInternalStorage(context, uri, "cover")
                if (copied != null) {
                    ImageStorage.delete(coverImagePath) // replaced selection: drop the earlier copy
                    coverImagePath = copied
                }
            }
        }
    }

    val pickedColor = if (useCustomColor) hsv.toColor() else AccentColor.entries[selectedColorIndex].lightPrimary

    fun dismiss() {
        ImageStorage.delete(coverImagePath) // not saved: do not leave the copied cover behind
        onDismissRequest()
    }

    val pages = totalPages.toIntOrNull() ?: 0
    val canSave = title.isNotBlank() && author.isNotBlank() && pages > 0

    AlertDialog(
        onDismissRequest = ::dismiss,
        title = { Text(stringResource(R.string.add_book)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.title)) },
                    leadingIcon = { Icon(Icons.Default.Book, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text(stringResource(R.string.author)) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = totalPages,
                    onValueChange = { totalPages = it.filter { char -> char.isDigit() }.take(6) },
                    label = { Text(stringResource(R.string.total_pages)) },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.select_cover))
                }

                // Cover preview: the selected image, or the vector cover in the picked colour. Rounded corners.
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CoverImage(
                        path = coverImagePath,
                        contentDescription = if (coverImagePath != null) "Cover preview" else "Colour preview",
                        modifier = Modifier
                            .size(100.dp, 150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                        fallback = { VectorCover(base = pickedColor) }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(stringResource(R.string.accent_color), style = MaterialTheme.typography.labelMedium)

                // Quick presets (the original palette) ...
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AccentColor.entries.forEachIndexed { index, color ->
                        val selected = !useCustomColor && selectedColorIndex == index
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color.lightPrimary)
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = {
                                        selectedColorIndex = index
                                        useCustomColor = false
                                        val preset = color.lightPrimary.toHsv()
                                        hue = preset.hue
                                        saturation = preset.saturation
                                        brightness = preset.value
                                    }
                                )
                                .semantics { contentDescription = "${color.displayName} colour" }
                                .border(
                                    width = if (selected) 3.dp else 0.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else androidx.compose.ui.graphics.Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = color.lightOnPrimary
                                )
                            }
                        }
                    }
                }

                // ... and the full colour wheel for any other colour.
                ColorWheelPicker(
                    hsv = hsv,
                    onChange = {
                        useCustomColor = true
                        hue = it.hue
                        saturation = it.saturation
                        brightness = it.value
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (canSave) {
                        onSave(
                            title.trim(),
                            author.trim(),
                            pages,
                            coverImagePath,
                            selectedColorIndex,
                            if (useCustomColor) hsv.toColor().toArgb() else null
                        )
                    }
                },
                enabled = canSave
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = ::dismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

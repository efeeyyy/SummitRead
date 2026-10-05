package com.example.summitread.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.summitread.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.summitread.data.ImageStorage
import com.example.summitread.platform.rememberImagePicker
import com.example.summitread.theme.AccentColor
import com.example.summitread.theme.AppShapes
import com.example.summitread.theme.AppSpacing
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.theme.rememberAppHaptics
import com.example.summitread.ui.components.AppSheet
import com.example.summitread.ui.components.AppTextField
import com.example.summitread.ui.components.rememberAppSheetState
import com.example.summitread.ui.components.HairlineWidth
import com.example.summitread.ui.components.PillButton
import com.example.summitread.ui.components.PillSize
import com.example.summitread.ui.components.PillStyle
import com.example.summitread.ui.components.appShadow
import com.example.summitread.ui.components.pressableScale
import kotlinx.coroutines.launch

/**
 * "Add Book" bottom sheet: cover picker with a rounded preview, three fields, the colour presets plus the
 * colour wheel (scrolls inside the sheet) and a pinned primary button.
 */
@Composable
fun AddBookSheet(
    imageStorage: ImageStorage,
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
    val colors = AppTheme.colors
    val haptics = rememberAppHaptics()

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
    var saving by rememberSaveable { mutableStateOf(false) }
    val hsv = Hsv(hue, saturation, brightness)

    val scope = rememberCoroutineScope()
    val sheetState = rememberAppSheetState()

    val imagePicker = rememberImagePicker { uri ->
        scope.launch {
            val copied = imageStorage.copyToStorage(uri, "cover")
            if (copied != null) {
                imageStorage.delete(coverImagePath) // replaced selection: drop the earlier copy
                coverImagePath = copied
            }
        }
    }
    val pickCover = { imagePicker.pickImage() }

    val pickedColor = if (useCustomColor) hsv.toColor() else AccentColor.entries[selectedColorIndex].lightPrimary

    // Called when the sheet was already swiped / tapped away.
    fun dismissed() {
        if (!saving) imageStorage.delete(coverImagePath) // not saved: do not leave the copied cover behind
        onDismissRequest()
    }

    val pages = totalPages.toIntOrNull() ?: 0
    val canSave = title.isNotBlank() && author.isNotBlank() && pages > 0 && !saving

    AppSheet(
        onDismissRequest = ::dismissed,
        title = stringResource(Res.string.add_book),
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            // Cover: rounded preview (the picked image, or the vector cover in the picked colour) + actions.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val previewShape = AppShapes.medium
                CoverImage(
                    path = coverImagePath,
                    contentDescription = stringResource(
                        if (coverImagePath != null) Res.string.cover_preview else Res.string.color_preview
                    ),
                    modifier = Modifier
                        .size(80.dp, 120.dp)
                        .pressableScale(
                            onClick = {
                                haptics.light()
                                pickCover()
                            },
                            onClickLabel = stringResource(Res.string.select_cover),
                            role = Role.Button
                        )
                        .appShadow(elevation = 10.dp, shape = previewShape)
                        .clip(previewShape)
                        .border(HairlineWidth, colors.separator, previewShape),
                    fallback = { VectorCover(base = pickedColor) }
                )
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)) {
                    PillButton(
                        text = stringResource(Res.string.select_cover),
                        onClick = { pickCover() },
                        style = PillStyle.Tinted,
                        size = PillSize.Small,
                        leadingIcon = Icons.Rounded.Image
                    )
                    if (coverImagePath != null) {
                        PillButton(
                            text = stringResource(Res.string.remove_cover),
                            onClick = {
                                imageStorage.delete(coverImagePath)
                                coverImagePath = null
                            },
                            style = PillStyle.Plain,
                            size = PillSize.Small,
                            destructive = true
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                AppTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = stringResource(Res.string.title),
                    leadingIcon = Icons.Rounded.Book,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )
                AppTextField(
                    value = author,
                    onValueChange = { author = it },
                    placeholder = stringResource(Res.string.author),
                    leadingIcon = Icons.Rounded.Person,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )
                AppTextField(
                    value = totalPages,
                    onValueChange = { totalPages = it.filter { char -> char.isDigit() }.take(6) },
                    placeholder = stringResource(Res.string.total_pages),
                    leadingIcon = Icons.Rounded.Description,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                )
            }

            // Colour: quick presets (the original palette) and the full colour wheel for any other colour.
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(Res.string.cover_color).uppercase(),
                    style = AppType.Footnote,
                    color = colors.secondaryLabel,
                    modifier = Modifier.padding(start = AppSpacing.sm)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppShapes.medium)
                        .background(colors.fill)
                        .padding(AppSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectableGroup(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AccentColor.entries.forEachIndexed { index, color ->
                            val selected = !useCustomColor && selectedColorIndex == index
                            val swatchDescription = stringResource(Res.string.color_swatch, color.displayName)
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .selectable(
                                        selected = selected,
                                        role = Role.RadioButton,
                                        onClick = {
                                            haptics.light()
                                            selectedColorIndex = index
                                            useCustomColor = false
                                            val preset = color.lightPrimary.toHsv()
                                            hue = preset.hue
                                            saturation = preset.saturation
                                            brightness = preset.value
                                        }
                                    )
                                    .semantics { contentDescription = swatchDescription }
                                    .border(
                                        width = 2.dp,
                                        color = if (selected) colors.tint else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color.lightPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = color.lightOnPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

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
            }
        }

        // Pinned below the scrolling form, so the primary action is always reachable.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = AppSpacing.md)
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs)
        ) {
            PillButton(
                text = stringResource(Res.string.add_book),
                onClick = {
                    if (canSave) {
                        saving = true
                        val savedTitle = title.trim()
                        val savedAuthor = author.trim()
                        val savedCover = coverImagePath
                        val savedColorIndex = selectedColorIndex
                        val savedArgb = if (useCustomColor) hsv.toColor().toArgb() else null
                        scope.launch {
                            // Slide the sheet away first, then hand the book over (the caller removes the sheet).
                            sheetState.hide()
                            onSave(savedTitle, savedAuthor, pages, savedCover, savedColorIndex, savedArgb)
                        }
                    }
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            )
            PillButton(
                text = stringResource(Res.string.cancel),
                onClick = {
                    scope.launch {
                        sheetState.hide()
                        dismissed()
                    }
                },
                style = PillStyle.Plain,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

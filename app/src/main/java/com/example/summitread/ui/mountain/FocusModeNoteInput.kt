package com.example.summitread.ui.mountain

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.summitread.R
import com.example.summitread.platform.AndroidAppContainer
import com.example.summitread.data.db.BookEntity
import com.example.summitread.data.db.CharacterEntity
import com.example.summitread.data.db.NoteEntity
import com.example.summitread.theme.AppShapes
import com.example.summitread.theme.AppSpacing
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.ui.components.AppTextField
import com.example.summitread.ui.components.CardContainer
import com.example.summitread.ui.components.IconButtonStyle
import com.example.summitread.ui.components.IconCircleButton
import com.example.summitread.ui.components.PillButton
import com.example.summitread.ui.components.PillSize
import com.example.summitread.ui.components.PillStyle
import com.example.summitread.ui.components.SegmentedControl
import com.example.summitread.ui.components.appShadow
import kotlinx.coroutines.launch

/**
 * Focus-mode panel with the Notes and Characters tabs, drawn as a sheet that docks under the zoomed camp
 * (28dp top corners, opaque elevated surface). All drafts live here (not in the tabs) and are
 * `rememberSaveable`, so switching tabs or rotating the device never loses what was typed.
 */
@Composable
fun FocusModeNoteInput(
    book: BookEntity,
    notes: List<NoteEntity>,
    characters: List<CharacterEntity>,
    onAddNote: (content: String, pageNumber: Int, photoPath: String?) -> Unit,
    onDeleteNote: (NoteEntity) -> Unit,
    onAddCharacter: (name: String, role: String, description: String) -> Unit,
    onDeleteCharacter: (CharacterEntity) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var noteContent by rememberSaveable { mutableStateOf("") }
    var notePage by rememberSaveable { mutableStateOf("") }
    var notePhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var characterName by rememberSaveable { mutableStateOf("") }
    var characterRole by rememberSaveable { mutableStateOf("") }
    var characterDescription by rememberSaveable { mutableStateOf("") }
    val tabs = listOf("Notes", "Characters")

    Column(
        modifier = modifier
            .fillMaxSize()
            .appShadow(elevation = 16.dp, shape = AppShapes.sheet)
            .clip(AppShapes.sheet)
            .background(colors.elevatedSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = AppSpacing.lg, end = AppSpacing.xs, top = AppSpacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = AppType.Headline,
                    color = colors.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Camp notes and characters",
                    style = AppType.Footnote,
                    color = colors.secondaryLabel
                )
            }
            // Works on both tabs: the way out of focus mode
            IconCircleButton(
                icon = Icons.Rounded.Close,
                contentDescription = "Exit focus mode",
                onClick = onDismiss
            )
        }

        SegmentedControl(
            options = tabs,
            selectedIndex = selectedTabIndex,
            onSelect = { selectedTabIndex = it },
            modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.xs)
        )

        if (selectedTabIndex == 0) {
            NotesTab(
                notes = notes,
                content = noteContent,
                onContentChange = { noteContent = it },
                pageInput = notePage,
                onPageInputChange = { notePage = it.filter { char -> char.isDigit() } },
                photoPath = notePhoto,
                onPhotoPathChange = { notePhoto = it },
                onAddNote = onAddNote,
                onDeleteNote = onDeleteNote
            )
        } else {
            CharactersTab(
                characters = characters,
                name = characterName,
                onNameChange = { characterName = it },
                role = characterRole,
                onRoleChange = { characterRole = it },
                description = characterDescription,
                onDescriptionChange = { characterDescription = it },
                onAddCharacter = onAddCharacter,
                onDeleteCharacter = onDeleteCharacter
            )
        }
    }
}

@Composable
private fun tabContentPadding(): PaddingValues {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return PaddingValues(
        start = AppSpacing.md,
        end = AppSpacing.md,
        top = AppSpacing.xs,
        bottom = navBottom + AppSpacing.xl
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = AppType.Subheadline,
        color = AppTheme.colors.secondaryLabel,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = AppSpacing.md)
    )
}

@Composable
private fun NotesTab(
    notes: List<NoteEntity>,
    content: String,
    onContentChange: (String) -> Unit,
    pageInput: String,
    onPageInputChange: (String) -> Unit,
    photoPath: String?,
    onPhotoPathChange: (String?) -> Unit,
    onAddNote: (String, Int, String?) -> Unit,
    onDeleteNote: (NoteEntity) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                onPhotoPathChange(AndroidAppContainer.get(context).imageStorage.copyToStorage(uri.toString(), "note"))
            }
        }
    }
    val canSave = content.isNotBlank() && (pageInput.toIntOrNull() ?: 0) > 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = tabContentPadding(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        item {
            CardContainer(modifier = Modifier.fillMaxWidth(), containerColor = AppTheme.colors.groupedBackground) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    AppTextField(
                        value = content,
                        onValueChange = onContentChange,
                        placeholder = stringResource(R.string.note_content),
                        singleLine = false,
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs)
                    ) {
                        AppTextField(
                            value = pageInput,
                            onValueChange = onPageInputChange,
                            placeholder = "Page",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        IconCircleButton(
                            icon = Icons.Rounded.CameraAlt,
                            contentDescription = stringResource(R.string.attach_photo),
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            size = 44.dp
                        )
                    }

                    if (photoPath != null) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            PolaroidPhoto(
                                imagePath = photoPath,
                                pageNumber = pageInput.toIntOrNull() ?: 0,
                                modifier = Modifier.graphicsLayer { rotationZ = -1.5f }
                            )
                        }
                        PillButton(
                            text = "Remove photo",
                            onClick = { onPhotoPathChange(null) },
                            style = PillStyle.Plain,
                            size = PillSize.Small,
                            destructive = true,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }

                    PillButton(
                        text = stringResource(R.string.save),
                        onClick = {
                            val page = pageInput.toIntOrNull() ?: 0
                            if (content.isNotBlank() && page > 0) {
                                onAddNote(content, page, photoPath)
                                onContentChange("")
                                onPageInputChange("")
                                onPhotoPathChange(null)
                            }
                        },
                        enabled = canSave,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (notes.isEmpty()) {
            item { EmptyHint("No notes yet. Capture a thought from the trail, with the page it belongs to.") }
        }
        items(notes, key = { it.id }) { note ->
            NoteCard(
                note = note,
                onDelete = { onDeleteNote(note) }
            )
        }
    }
}

@Composable
private fun CharactersTab(
    characters: List<CharacterEntity>,
    name: String,
    onNameChange: (String) -> Unit,
    role: String,
    onRoleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    onAddCharacter: (String, String, String) -> Unit,
    onDeleteCharacter: (CharacterEntity) -> Unit
) {
    val colors = AppTheme.colors
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = tabContentPadding(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
    ) {
        item {
            CardContainer(modifier = Modifier.fillMaxWidth(), containerColor = colors.groupedBackground) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    Text(
                        text = stringResource(R.string.add_character),
                        style = AppType.Headline,
                        color = colors.label,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    AppTextField(
                        value = name,
                        onValueChange = onNameChange,
                        placeholder = stringResource(R.string.character_name),
                        modifier = Modifier.fillMaxWidth()
                    )
                    AppTextField(
                        value = role,
                        onValueChange = onRoleChange,
                        placeholder = stringResource(R.string.character_role),
                        modifier = Modifier.fillMaxWidth()
                    )
                    AppTextField(
                        value = description,
                        onValueChange = onDescriptionChange,
                        placeholder = stringResource(R.string.character_description),
                        singleLine = false,
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                    PillButton(
                        text = stringResource(R.string.save),
                        onClick = {
                            if (name.isNotBlank()) {
                                onAddCharacter(name, role, description)
                                onNameChange("")
                                onRoleChange("")
                                onDescriptionChange("")
                            }
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (characters.isEmpty()) {
            item { EmptyHint("No characters yet. Keep track of who is who as the story unfolds.") }
        }
        items(characters, key = { it.id }) { character ->
            CardContainer(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = character.name,
                        style = AppType.Headline,
                        color = colors.label,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconCircleButton(
                        icon = Icons.Rounded.Delete,
                        contentDescription = "Delete character ${character.name}",
                        onClick = { onDeleteCharacter(character) },
                        style = IconButtonStyle.Plain,
                        tint = colors.destructive,
                        size = 32.dp
                    )
                }
                if (character.role.isNotBlank()) {
                    MountainChip(text = character.role, modifier = Modifier.padding(top = 2.dp))
                }
                if (character.description.isNotBlank()) {
                    Text(
                        text = character.description,
                        style = AppType.Body,
                        color = colors.label,
                        modifier = Modifier.padding(top = AppSpacing.xs)
                    )
                }
            }
        }
    }
}

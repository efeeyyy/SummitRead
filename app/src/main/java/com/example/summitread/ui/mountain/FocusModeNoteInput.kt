package com.example.summitread.ui.mountain

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.summitread.R
import com.example.summitread.data.ImageStorage
import com.example.summitread.data.db.BookEntity
import com.example.summitread.data.db.CharacterEntity
import com.example.summitread.data.db.NoteEntity
import kotlinx.coroutines.launch

/**
 * Focus-mode panel with the Notes and Characters tabs. All drafts live here (not in the tabs) and are
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
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var noteContent by rememberSaveable { mutableStateOf("") }
    var notePage by rememberSaveable { mutableStateOf("") }
    var notePhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var characterName by rememberSaveable { mutableStateOf("") }
    var characterRole by rememberSaveable { mutableStateOf("") }
    var characterDescription by rememberSaveable { mutableStateOf("") }
    val tabs = listOf("📝 Notes", "👤 Characters")

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            )
            // Characters tab had no way out of focus mode; this works on both tabs
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Exit focus mode")
            }
        }

        PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

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
                onDeleteNote = onDeleteNote,
                onDismiss = onDismiss
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
private fun NotesTab(
    notes: List<NoteEntity>,
    content: String,
    onContentChange: (String) -> Unit,
    pageInput: String,
    onPageInputChange: (String) -> Unit,
    photoPath: String?,
    onPhotoPathChange: (String?) -> Unit,
    onAddNote: (String, Int, String?) -> Unit,
    onDeleteNote: (NoteEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                onPhotoPathChange(ImageStorage.copyImageToInternalStorage(context, uri, "note"))
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = content,
                        onValueChange = onContentChange,
                        label = { Text(stringResource(R.string.note_content)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = pageInput,
                            onValueChange = onPageInputChange,
                            label = { Text("Page") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        IconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = stringResource(R.string.attach_photo))
                        }
                    }

                    if (photoPath != null) {
                        PolaroidPhoto(
                            imagePath = photoPath,
                            pageNumber = pageInput.toIntOrNull() ?: 0,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = {
                                val page = pageInput.toIntOrNull() ?: 0
                                if (content.isNotBlank() && page > 0) {
                                    onAddNote(content, page, photoPath)
                                    onContentChange("")
                                    onPageInputChange("")
                                    onPhotoPathChange(null)
                                }
                            },
                            enabled = content.isNotBlank() && (pageInput.toIntOrNull() ?: 0) > 0
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            }
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(R.string.add_character), style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = { Text(stringResource(R.string.character_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = role,
                        onValueChange = onRoleChange,
                        label = { Text(stringResource(R.string.character_role)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = onDescriptionChange,
                        label = { Text(stringResource(R.string.character_description)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onAddCharacter(name, role, description)
                                onNameChange("")
                                onRoleChange("")
                                onDescriptionChange("")
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        enabled = name.isNotBlank()
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }

        items(characters, key = { it.id }) { character ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = character.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { onDeleteCharacter(character) }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (character.role.isNotBlank()) {
                        Text(
                            text = character.role,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    if (character.description.isNotBlank()) {
                        Text(
                            text = character.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

package com.example.summitread.ui.mountain

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.summitread.R
import com.example.summitread.data.BookRepository
import com.example.summitread.data.UserPreferences
import com.example.summitread.theme.AccentColor
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun MountainScreen(
    bookId: Long,
    bookRepository: BookRepository,
    userPreferences: UserPreferences,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onFocusModeChange: (Boolean) -> Unit,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: MountainViewModel = viewModel(
        key = "mountain_$bookId",
        factory = MountainViewModel.factory(bookRepository, userPreferences, bookId)
    )

    val activeBook by viewModel.activeBook.collectAsStateWithLifecycle()
    val readingSessions by viewModel.readingSessions.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val characters by viewModel.characters.collectAsStateWithLifecycle()
    val isFocusMode by viewModel.isFocusMode.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val showSummitAnimation by viewModel.showSummitAnimation.collectAsStateWithLifecycle()
    val pagesInput by viewModel.pagesInput.collectAsStateWithLifecycle()
    val hoursSinceLastSession by viewModel.hoursSinceLastSession.collectAsStateWithLifecycle()
    val currentStreak by userPreferences.currentStreak.collectAsStateWithLifecycle(initialValue = 0)

    var showSummitLogbook by remember { mutableStateOf(false) }
    var particleTrigger by remember { mutableStateOf(false) }
    var particleOrigin by remember { mutableStateOf(Offset.Zero) }
    var particleTarget by remember { mutableStateOf(Offset.Zero) }
    var triggerFlamePulse by remember { mutableStateOf(false) }
    
    val flameScale by animateFloatAsState(
        targetValue = if (triggerFlamePulse) 1.5f else 1f,
        animationSpec = tween(300),
        finishedListener = { if (it == 1.5f) triggerFlamePulse = false }
    )

    LaunchedEffect(Unit) {
        viewModel.triggerParticles.collectLatest {
            particleTrigger = true
        }
    }
    
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    
    // Update parent about focus mode change
    androidx.compose.runtime.LaunchedEffect(isFocusMode) {
        onFocusModeChange(isFocusMode)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (activeBook != null && !isFocusMode) {
                TopAppBar(
                    title = { Text("${activeBook!!.title} - Summit Climb", maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak",
                                tint = Color(0xFFFFA726),
                                modifier = Modifier
                                    .onGloballyPositioned { coordinates ->
                                        particleTarget = coordinates.positionInWindow()
                                    }
                                    .graphicsLayer {
                                        scaleX = flameScale
                                        scaleY = flameScale
                                    }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentStreak.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFFFA726)
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (activeBook != null && !isFocusMode) {
                FloatingActionButton(onClick = { viewModel.toggleFocusMode() }) {
                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.add_note))
                }
            }
        }
    ) { paddingValues ->
        if (activeBook == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.no_active_book),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            val book = activeBook!!
            val accentColor = AccentColor.entries.getOrNull(book.accentColorIndex)?.lightPrimary ?: AccentColor.FOREST_GREEN.lightPrimary
            val progress = if (book.totalPages > 0) book.currentPages.toFloat() / book.totalPages else 0f

            with(sharedTransitionScope) {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "book_container_${book.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                    ) {
                    // Always show mountain canvas, but height/scale changes
                    MountainCanvas(
                        progress = progress,
                        notes = notes,
                        totalPages = book.totalPages,
                        isFocusMode = isFocusMode,
                        accentColor = accentColor,
                        surfaceColor = MaterialTheme.colorScheme.surface,
                        onSurfaceColor = MaterialTheme.colorScheme.onSurface,
                        hoursSinceLastSession = hoursSinceLastSession,
                        onSignpostClick = { /* Handle signpost click later */ },
                        onMilestoneClick = { fraction ->
                            // TODO: Show milestone dialog
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isFocusMode) 200.dp else 250.dp)
                    )

                    AnimatedVisibility(
                        visible = !isFocusMode,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            CalendarStrip(
                                selectedDate = selectedDate,
                                sessions = readingSessions,
                                onDateSelected = viewModel::setSelectedDate,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            )
                            
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                item {
                                    StopwatchWidget(
                                        bookId = book.id,
                                        onStopSession = { pagesEstimate, _ ->
                                            viewModel.setPagesInput(pagesEstimate.toString())
                                        }
                                    )
                                }
                                
                                item {
                                    PageInputCard(
                                        selectedDate = selectedDate,
                                        pagesInput = pagesInput,
                                        onPagesInputChange = viewModel::setPagesInput,
                                        onLogPages = {
                                            scope.launch {
                                                viewModel.logPages()
                                            }
                                        },
                                        currentPages = book.currentPages,
                                        totalPages = book.totalPages,
                                        modifier = Modifier.onGloballyPositioned { coordinates ->
                                            val pos = coordinates.positionInWindow()
                                            particleOrigin = Offset(pos.x + coordinates.size.width / 2f, pos.y)
                                        }
                                    )
                                }
                                
                                items(notes, key = { it.id }) { note ->
                                    NoteCard(note = note, onDelete = null)
                                }
                                
                                item {
                                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(80.dp))
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isFocusMode,
                        enter = fadeIn() + slideInVertically { it / 2 },
                        exit = fadeOut() + slideOutVertically { it / 2 }
                    ) {
                        FocusModeNoteInput(
                            book = book,
                            notes = notes,
                            characters = characters,
                            onAddNote = { content, page, photo -> 
                                            scope.launch {
                                                viewModel.addNote(content, page, photo)
                                            }
                                        },
                                        onDeleteNote = { note ->
                                            scope.launch {
                                                viewModel.deleteNote(note)
                                            }
                                        },
                                        onAddCharacter = { name, role, desc ->
                                            scope.launch {
                                                viewModel.addCharacter(name, role, desc)
                                            }
                                        },
                                        onDeleteCharacter = { character ->
                                            scope.launch {
                                                viewModel.deleteCharacter(character)
                                            }
                                        },
                            onDismiss = viewModel::toggleFocusMode,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (showSummitAnimation) {
                    SummitConqueredAnimation(
                        bookTitle = book.title,
                        onDismiss = {
                            viewModel.dismissSummitAnimation()
                            showSummitLogbook = true
                        }
                    )
                }

                if (showSummitLogbook) {
                    SummitLogbookDialog(
                        bookTitle = book.title,
                        onSave = { rating, review ->
                            scope.launch {
                                viewModel.saveSummitLogbook(rating, review)
                                showSummitLogbook = false
                            }
                        },
                        onDismissRequest = { showSummitLogbook = false }
                    )
                }
                
                if (particleTrigger) {
                    ParticleExplosionOverlay(
                        trigger = particleTrigger,
                        origin = particleOrigin,
                        target = particleTarget,
                        onAnimationEnd = {
                            particleTrigger = false
                            triggerFlamePulse = true
                        }
                    )
                }
            }
            }
        }
    }
}


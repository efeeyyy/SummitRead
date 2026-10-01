package com.example.summitread.ui.mountain

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.summitread.R
import com.example.summitread.data.BookRepository
import com.example.summitread.data.UserPreferences
import com.example.summitread.services.ReadingTimerService
import com.example.summitread.services.TimerStatus
import com.example.summitread.theme.AccentColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
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
    val today by viewModel.today.collectAsStateWithLifecycle()
    val showSummitAnimation by viewModel.showSummitAnimation.collectAsStateWithLifecycle()
    val completedSummit by viewModel.completedSummit.collectAsStateWithLifecycle()
    val pagesInput by viewModel.pagesInput.collectAsStateWithLifecycle()
    val isLogging by viewModel.isLogging.collectAsStateWithLifecycle()
    val pendingTimeMillis by viewModel.pendingTimeMillis.collectAsStateWithLifecycle()
    val hoursSinceLastSession by viewModel.hoursSinceLastSession.collectAsStateWithLifecycle()
    val currentStreak by viewModel.currentStreak.collectAsStateWithLifecycle()

    // Only changes when this book's timer starts/stops running, so the screen does not recompose every second
    val timerRunning by remember(bookId) {
        ReadingTimerService.state
            .map { it.status == TimerStatus.RUNNING && it.bookId == bookId }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)

    // Dialog state survives rotation / process recreation
    var milestoneFraction by rememberSaveable { mutableStateOf<Float?>(null) }
    var signpostNoteId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Spark burst -> flame: positions are window coordinates held outside Compose state (no recomposition on scroll)
    val particleOrigin = remember { WindowPoint() }
    val particleTarget = remember { WindowPoint() }
    var burstId by remember { mutableIntStateOf(0) }
    var heartbeat by remember { mutableIntStateOf(0) }
    val flameScale = remember { Animatable(1f) }

    LaunchedEffect(viewModel) {
        viewModel.triggerParticles.collect { burstId++ }
    }
    // Heartbeat when the sparks hit the flame: lub-dub
    LaunchedEffect(heartbeat) {
        if (heartbeat > 0) {
            flameScale.animateTo(1.5f, tween(110))
            flameScale.animateTo(1f, tween(110))
            flameScale.animateTo(1.3f, tween(110))
            flameScale.animateTo(1f, tween(220))
        }
    }

    // Keep the bottom bar state in sync with focus mode, and always restore it when the screen is left (#Y1)
    LaunchedEffect(isFocusMode) { onFocusModeChange(isFocusMode) }
    DisposableEffect(Unit) {
        onDispose { onFocusModeChange(false) }
    }
    BackHandler(enabled = isFocusMode) { viewModel.toggleFocusMode() }
    BackHandler(enabled = showSummitAnimation) { viewModel.dismissSummitAnimation() }

    // 3D book opening (#4): the content swings open around the left (spine) edge while the shared bounds move
    val bookClosed by animatedVisibilityScope.transition.animateFloat(
        transitionSpec = { tween(700, easing = FastOutSlowInEasing) },
        label = "book_closed"
    ) { state -> if (state == EnterExitState.Visible) 0f else 1f }

    val dark = isAppDarkTheme()

    Scaffold(
        modifier = modifier,
        topBar = {
            val book = activeBook
            if (book != null && !isFocusMode) {
                TopAppBar(
                    title = {
                        Text(
                            "${book.title} - Summit Climb",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                                        val pos = coordinates.positionInWindow()
                                        particleTarget.x = pos.x + coordinates.size.width / 2f
                                        particleTarget.y = pos.y + coordinates.size.height / 2f
                                        particleTarget.isSet = true
                                    }
                                    .graphicsLayer {
                                        scaleX = flameScale.value
                                        scaleY = flameScale.value
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
                    Icon(Icons.Default.Edit, contentDescription = "Focus mode: notes and characters")
                }
            }
        }
    ) { paddingValues ->
        val book = activeBook
        if (book == null) {
            LoadingOrMissing(modifier = Modifier.fillMaxSize().padding(paddingValues))
        } else {
            val accentColor = remember(book.coverColorArgb, book.accentColorIndex, dark) {
                book.coverColorArgb?.let { Color(it) }
                    ?: AccentColor.entries.getOrNull(book.accentColorIndex)
                        ?.let { if (dark) it.darkPrimary else it.lightPrimary }
                    ?: AccentColor.FOREST_GREEN.lightPrimary
            }
            val groundColor = lerp(MaterialTheme.colorScheme.background, accentColor, 0.18f)
            val progress = if (book.totalPages > 0) book.currentPages.toFloat() / book.totalPages else 0f
            val canvasHeight by animateDpAsState(
                targetValue = if (isFocusMode) 200.dp else 250.dp,
                animationSpec = tween(400),
                label = "canvas_height"
            )

            with(sharedTransitionScope) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .graphicsLayer {
                            // Hinge on the left edge; rotationY is 0 once the screen is fully entered
                            rotationY = -80f * bookClosed
                            cameraDistance = size.width * 1.6f
                            transformOrigin = TransformOrigin(0f, 0.5f)
                        }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "book_container_${book.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                    ) {
                        MountainCanvas(
                            progress = progress,
                            notes = notes,
                            totalPages = book.totalPages,
                            isFocusMode = isFocusMode,
                            accentColor = accentColor,
                            groundColor = groundColor,
                            isDark = dark,
                            hoursSinceLastSession = hoursSinceLastSession,
                            streakActive = currentStreak > 0,
                            isReadingTimerRunning = timerRunning,
                            onSignpostClick = { signpostNoteId = it.id },
                            onMilestoneClick = { milestoneFraction = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(canvasHeight)
                        )

                        AnimatedVisibility(
                            visible = !isFocusMode,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                CalendarStrip(
                                    today = today,
                                    selectedDate = selectedDate,
                                    sessions = readingSessions,
                                    onDateSelected = viewModel::setSelectedDate,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                )

                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .imePadding(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    item {
                                        StopwatchWidget(
                                            bookId = book.id,
                                            pendingTimeMillis = pendingTimeMillis,
                                            onUseSuggestion = { viewModel.setPagesInput(it.toString()) },
                                            onDiscardPending = viewModel::discardPendingSession
                                        )
                                    }

                                    item {
                                        PageInputCard(
                                            selectedDate = selectedDate,
                                            pagesInput = pagesInput,
                                            onPagesInputChange = viewModel::setPagesInput,
                                            onLogPages = viewModel::logPages,
                                            currentPages = book.currentPages,
                                            totalPages = book.totalPages,
                                            isLogging = isLogging,
                                            modifier = Modifier.onGloballyPositioned { coordinates ->
                                                val pos = coordinates.positionInWindow()
                                                particleOrigin.x = pos.x + coordinates.size.width / 2f
                                                particleOrigin.y = pos.y
                                                particleOrigin.isSet = true
                                            }
                                        )
                                    }

                                    items(notes, key = { it.id }) { note ->
                                        NoteCard(note = note, onDelete = null)
                                    }

                                    item {
                                        Spacer(modifier = Modifier.height(80.dp))
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = isFocusMode,
                            enter = fadeIn() + slideInVertically { it / 2 },
                            exit = fadeOut() + slideOutVertically { it / 2 },
                            modifier = Modifier.weight(1f)
                        ) {
                            FocusModeNoteInput(
                                book = book,
                                notes = notes,
                                characters = characters,
                                onAddNote = viewModel::addNote,
                                onDeleteNote = viewModel::deleteNote,
                                onAddCharacter = viewModel::addCharacter,
                                onDeleteCharacter = viewModel::deleteCharacter,
                                onDismiss = viewModel::toggleFocusMode,
                                modifier = Modifier.imePadding()
                            )
                        }
                    }

                    // Sparks fly from the page card to the streak flame; the flame beats on impact (#31)
                    ParticleBurstOverlay(
                        burstId = burstId,
                        origin = particleOrigin,
                        target = particleTarget,
                        onImpact = { heartbeat++ }
                    )

                    // Completion card (#33), then the Summit Logbook (#32)
                    val completion = completedSummit
                    if (showSummitAnimation && completion != null) {
                        SummitConqueredAnimation(
                            message = completion.message,
                            relicEmoji = completion.relicEmoji,
                            bookTitle = completion.title,
                            onDismiss = viewModel::dismissSummitAnimation
                        )
                    }

                    // Derived from ViewModel state, so it also survives rotation
                    if (!showSummitAnimation && completion != null) {
                        SummitLogbookDialog(
                            bookTitle = completion.title,
                            onSave = { rating, review -> viewModel.saveSummitLogbook(rating, review) },
                            onDismissRequest = viewModel::clearCompletedSummit
                        )
                    }
                }
            }

            milestoneFraction?.let { fraction ->
                MilestoneDialog(
                    bookId = book.id,
                    stats = remember(fraction, book.totalPages, book.currentPages, readingSessions) {
                        MilestoneStats.compute(fraction, book.totalPages, book.currentPages, readingSessions)
                    },
                    onDismiss = { milestoneFraction = null }
                )
            }
            signpostNoteId?.let { id ->
                notes.firstOrNull { it.id == id }?.let { note ->
                    NoteSignpostDialog(note = note, onDismiss = { signpostNoteId = null })
                }
            }
        }
    }
}

/** Room has not delivered the book yet (spinner) or the book does not exist (message after a moment). */
@Composable
private fun LoadingOrMissing(modifier: Modifier = Modifier) {
    var showMissing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(1200)
        showMissing = true
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (showMissing) {
            Text(
                text = stringResource(R.string.no_active_book),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(32.dp)
            )
        } else {
            CircularProgressIndicator()
        }
    }
}

package com.example.summitread.ui.mountain

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.summitread.R
import com.example.summitread.data.BookRepository
import com.example.summitread.data.UserPreferences
import com.example.summitread.services.ReadingTimerService
import com.example.summitread.services.TimerStatus
import com.example.summitread.theme.AccentColor
import com.example.summitread.theme.AppSpacing
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.theme.rememberAppHaptics
import com.example.summitread.ui.components.IconCircleButton
import com.example.summitread.ui.components.LocalBottomBarHeight
import com.example.summitread.ui.components.PillButton
import com.example.summitread.ui.components.PillSize
import com.example.summitread.ui.components.PillStyle
import com.example.summitread.ui.components.glassSource
import com.example.summitread.ui.components.rememberGlassState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Height of the mountain scene itself; the canvas is this plus the status bar and the glass header. */
private val SceneHeight = 236.dp

/** In focus mode the notes sheet starts at this fraction of the canvas height (the camp stays visible above it). */
private const val FOCUS_SHEET_TOP = 0.62f

@OptIn(ExperimentalSharedTransitionApi::class)
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

    val haptics = rememberAppHaptics()

    // Spark burst -> flame: positions are window coordinates held outside Compose state (no recomposition on scroll)
    val particleOrigin = remember { WindowPoint() }
    val particleTarget = remember { WindowPoint() }
    var burstId by remember { mutableIntStateOf(0) }
    var heartbeat by remember { mutableIntStateOf(0) }
    val flameScale = remember { Animatable(1f) }

    LaunchedEffect(viewModel) {
        viewModel.triggerParticles.collect {
            burstId++
            haptics.logged() // the log is saved: confirm with a haptic as the sparks fly
        }
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

    // Status bar icons follow the sky behind them (the canvas runs under the status bar); restored on leave
    var skyIsLight by remember { mutableStateOf<Boolean?>(null) }
    StatusBarIconsFollowSky(skyIsLight)

    // 3D book opening (#4): the content swings open around the left (spine) edge while the shared bounds move
    val bookClosed by animatedVisibilityScope.transition.animateFloat(
        transitionSpec = { tween(700, easing = FastOutSlowInEasing) },
        label = "book_closed"
    ) { state -> if (state == EnterExitState.Visible) 0f else 1f }

    val colors = AppTheme.colors
    val dark = colors.isDark
    val book = activeBook

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.groupedBackground)
    ) {
        if (book == null) {
            LoadingOrMissing(onBack = onNavigateBack, modifier = Modifier.fillMaxSize())
        } else {
            val accentColor = remember(book.coverColorArgb, book.accentColorIndex, dark) {
                book.coverColorArgb?.let { Color(it) }
                    ?: AccentColor.entries.getOrNull(book.accentColorIndex)
                        ?.let { if (dark) it.darkPrimary else it.lightPrimary }
                    ?: AccentColor.FOREST_GREEN.lightPrimary
            }
            val progress = if (book.totalPages > 0) book.currentPages.toFloat() / book.totalPages else 0f

            val density = LocalDensity.current
            val statusTop = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
            val navBottom = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
            val bottomBar = LocalBottomBarHeight.current
            val topInset = statusTop + HeaderBodyHeight
            val canvasHeight = topInset + SceneHeight
            val glass = rememberGlassState()
            val scrollState = rememberScrollState()

            // 0 while the canvas is on screen, 1 once it has scrolled under the header
            val canvasHeightPx = with(density) { canvasHeight.toPx() }
            val headerPx = with(density) { (statusTop + HeaderBodyHeight).toPx() }
            val collapse = remember(scrollState, canvasHeightPx, headerPx) {
                derivedStateOf {
                    (scrollState.value / (canvasHeightPx - headerPx).coerceAtLeast(1f)).coerceIn(0f, 1f)
                }
            }

            // Keyboard open: keep the list tight above it; closed: leave room for the floating tab bar
            val ime = WindowInsets.ime
            val imeOpen by remember(ime, density) { derivedStateOf { ime.getBottom(density) > 0 } }
            val listBottomPadding = if (imeOpen) AppSpacing.md else maxOf(navBottom, bottomBar) + AppSpacing.xl

            // Focus mode shows the canvas from the top, with the notes sheet docked under the camp
            LaunchedEffect(isFocusMode) {
                if (isFocusMode) scrollState.animateScrollTo(0)
            }

            with(sharedTransitionScope) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            // Hinge on the left edge; rotationY is 0 once the screen is fully entered
                            rotationY = -80f * bookClosed
                            cameraDistance = size.width * 1.6f
                            transformOrigin = TransformOrigin(0f, 0.5f)
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .sharedBounds(
                                sharedContentState = rememberSharedContentState(key = "book_container_${book.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                    ) {
                        // Canvas + sections scroll together; the whole list is what the glass header blurs
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .glassSource(glass)
                                .imePadding()
                                .verticalScroll(scrollState, enabled = !isFocusMode)
                                .padding(bottom = listBottomPadding),
                            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                        ) {
                                MountainCanvas(
                                    progress = progress,
                                    notes = notes,
                                    totalPages = book.totalPages,
                                    isFocusMode = isFocusMode,
                                    accentColor = accentColor,
                                    isDark = dark,
                                    hoursSinceLastSession = hoursSinceLastSession,
                                    streakActive = currentStreak > 0,
                                    isReadingTimerRunning = timerRunning,
                                    topInset = topInset,
                                    onSignpostClick = { signpostNoteId = it.id },
                                    onMilestoneClick = { milestoneFraction = it },
                                    onSkyLightnessChange = { skyIsLight = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(canvasHeight)
                                )

                                CalendarStrip(
                                    today = today,
                                    selectedDate = selectedDate,
                                    sessions = readingSessions,
                                    onDateSelected = viewModel::setSelectedDate,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                StopwatchWidget(
                                    bookId = book.id,
                                    pendingTimeMillis = pendingTimeMillis,
                                    onUseSuggestion = { viewModel.setPagesInput(it.toString()) },
                                    onDiscardPending = viewModel::discardPendingSession,
                                    modifier = Modifier.padding(horizontal = AppSpacing.screenHorizontal)
                                )

                                PageInputCard(
                                    selectedDate = selectedDate,
                                    pagesInput = pagesInput,
                                    onPagesInputChange = viewModel::setPagesInput,
                                    onLogPages = viewModel::logPages,
                                    currentPages = book.currentPages,
                                    totalPages = book.totalPages,
                                    isLogging = isLogging,
                                    modifier = Modifier
                                        .padding(horizontal = AppSpacing.screenHorizontal)
                                        .onGloballyPositioned { coordinates ->
                                            val pos = coordinates.positionInWindow()
                                            particleOrigin.x = pos.x + coordinates.size.width / 2f
                                            particleOrigin.y = pos.y
                                            particleOrigin.isSet = true
                                        }
                                )

                                NotesHeader(
                                    count = notes.size,
                                    onOpen = viewModel::toggleFocusMode,
                                    modifier = Modifier.padding(horizontal = AppSpacing.screenHorizontal)
                                )

                            for (note in notes) key(note.id) {
                                NoteCard(
                                    note = note,
                                    onDelete = null,
                                    modifier = Modifier.padding(horizontal = AppSpacing.screenHorizontal)
                                )
                            }
                        }

                        // Glass header over the canvas; hidden in focus mode
                        AnimatedVisibility(
                            visible = !isFocusMode,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.align(Alignment.TopCenter)
                        ) {
                            MountainHeader(
                                title = book.title,
                                streak = currentStreak,
                                glass = glass,
                                collapse = { collapse.value },
                                flameScale = { flameScale.value },
                                flameTarget = particleTarget,
                                onBack = onNavigateBack,
                                onOpenNotes = viewModel::toggleFocusMode
                            )
                        }

                        // Sparks fly from the page card to the streak flame; the flame beats on impact (#31)
                        ParticleBurstOverlay(
                            burstId = burstId,
                            origin = particleOrigin,
                            target = particleTarget,
                            onImpact = { heartbeat++ }
                        )

                        // Focus mode: the notes / characters sheet docks under the zoomed camp
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = canvasHeight * FOCUS_SHEET_TOP)
                                .imePadding()
                        ) {
                            AnimatedVisibility(
                                visible = isFocusMode,
                                enter = fadeIn() + slideInVertically(tween(420, easing = FastOutSlowInEasing)) { it / 2 },
                                exit = fadeOut() + slideOutVertically(tween(300)) { it / 2 }
                            ) {
                                FocusModeNoteInput(
                                    book = book,
                                    notes = notes,
                                    characters = characters,
                                    onAddNote = viewModel::addNote,
                                    onDeleteNote = viewModel::deleteNote,
                                    onAddCharacter = viewModel::addCharacter,
                                    onDeleteCharacter = viewModel::deleteCharacter,
                                    onDismiss = viewModel::toggleFocusMode
                                )
                            }
                        }

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

/** "Notes" section header with the entry point to focus mode (replaces the old floating pencil button). */
@Composable
private fun NotesHeader(count: Int, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = if (count > 0) "Notes · $count" else "Notes",
            style = AppType.Title3,
            color = colors.label
        )
        PillButton(
            text = "Add note",
            onClick = onOpen,
            style = PillStyle.Tinted,
            size = PillSize.Small,
            leadingIcon = Icons.Rounded.EditNote
        )
    }
}

/** Room has not delivered the book yet (spinner) or the book does not exist (message after a moment). */
@Composable
private fun LoadingOrMissing(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    var showMissing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(1200)
        showMissing = true
    }
    val statusTop = with(LocalDensity.current) { WindowInsets.statusBars.getTop(this).toDp() }
    Box(modifier = modifier) {
        IconCircleButton(
            icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
            contentDescription = "Back",
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = AppSpacing.xs, top = statusTop + HeaderVerticalPadding)
        )
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (showMissing) {
                Text(
                    text = stringResource(R.string.no_active_book),
                    textAlign = TextAlign.Center,
                    style = AppType.Body,
                    color = colors.secondaryLabel,
                    modifier = Modifier.padding(AppSpacing.xxl)
                )
            } else {
                CircularProgressIndicator(color = colors.tint)
            }
        }
    }
}

// ---- Status bar icon colour -------------------------------------------------------------------------------------


private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/**
 * The canvas is full-bleed, so the status bar sits on the sky: dark icons on a light sky, light icons on a dark
 * one. The previous appearance is restored when the screen is left.
 */
@Composable
private fun StatusBarIconsFollowSky(light: Boolean?) {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val original = controller?.isAppearanceLightStatusBars
        onDispose {
            if (controller != null && original != null) controller.isAppearanceLightStatusBars = original
        }
    }
    DisposableEffect(view, light) {
        if (light != null) {
            val window = view.context.findActivity()?.window
            if (window != null) WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = light
        }
        onDispose { }
    }
}

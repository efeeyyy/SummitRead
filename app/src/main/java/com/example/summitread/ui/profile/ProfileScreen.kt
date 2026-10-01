package com.example.summitread.ui.profile

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.summitread.R
import com.example.summitread.data.BookRepository
import com.example.summitread.data.ThemeMode
import com.example.summitread.data.UserPreferences
import com.example.summitread.services.ReadingTimerService
import com.example.summitread.services.TimerStatus
import com.example.summitread.theme.AccentColor
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    bookRepository: BookRepository,
    userPreferences: UserPreferences,
    modifier: Modifier = Modifier
) {
    val viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.factory(bookRepository, userPreferences)
    )

    val username by viewModel.username.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val accentColorIndex by viewModel.accentColorIndex.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val annualGoal by viewModel.annualGoal.collectAsStateWithLifecycle()
    val relics by viewModel.relics.collectAsStateWithLifecycle()
    val completedBooksCount by viewModel.completedBooksCount.collectAsStateWithLifecycle()
    val completedThisYear by viewModel.completedBooksThisYear.collectAsStateWithLifecycle()
    val totalPagesRead by viewModel.totalPagesRead.collectAsStateWithLifecycle()
    val totalTimeRead by viewModel.totalTimeRead.collectAsStateWithLifecycle()
    val totalTimedReadMillis by viewModel.totalTimedReadMillis.collectAsStateWithLifecycle()
    val selectedJourneyBookId by viewModel.selectedJourneyBookId.collectAsStateWithLifecycle()
    val journeySessions by viewModel.journeySessions.collectAsStateWithLifecycle()
    val journeyNotes by viewModel.journeyNotes.collectAsStateWithLifecycle()
    val journeyBook by viewModel.journeyBook.collectAsStateWithLifecycle()
    val journeyTotalTimeMillis by viewModel.journeyTotalTimeMillis.collectAsStateWithLifecycle()
    val timerState by ReadingTimerService.state.collectAsStateWithLifecycle()
    val isTimerRunning = timerState.status == TimerStatus.RUNNING

    var showTimeExplanationDialog by rememberSaveable { mutableStateOf(false) }
    var showGoalDialog by rememberSaveable { mutableStateOf(false) }

    // ONE sensor listener for every relic card (#39); lifecycle-aware, 0.5 deg threshold.
    val tilt = rememberTiltState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.profile)) })
        }
    ) { paddingValues ->
        // Single LazyColumn (#43): relic cards are chunked into rows via items(), nothing scrolls inside it.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "avatar") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val tier = AvatarTier.forCompletedBooks(completedBooksCount)
                        ClimberAvatar(tier = tier, breathing = isTimerRunning)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = tier.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val next = tier.next
                        Text(
                            text = if (next != null) {
                                val left = next.minBooks - completedBooksCount
                                "$left more ${if (left == 1) "book" else "books"} to ${next.displayName}"
                            } else {
                                "Highest tier reached"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        UsernameField(
                            savedName = username,
                            onSave = viewModel::updateUsername
                        )
                    }
                }
            }

            item(key = "stats") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        value = completedBooksCount.toString(),
                        label = stringResource(R.string.books_completed),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        value = totalPagesRead.toString(),
                        label = stringResource(R.string.total_pages_read),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "climbtime") {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Explain how climb time is calculated"
                        ) { showTimeExplanationDialog = true }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⏱️", fontSize = 24.sp, modifier = Modifier.clearAndSetSemantics { })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = totalTimeRead,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Timed sessions: ${ProfileLogic.formatTimedDuration(totalTimedReadMillis)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item(key = "goal") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    val progress = ProfileLogic.goalProgress(completedThisYear, annualGoal)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Annual Reading Goal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$completedThisYear / $annualGoal Books this year",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(64.dp)
                                .semantics {
                                    contentDescription = "${(progress * 100).toInt()} percent of the yearly goal"
                                }
                        ) {
                            CircularProgressIndicator(
                                progress = { 1f },
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.primary,
                                strokeCap = StrokeCap.Round
                            )
                            Text(
                                text = "${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    TextButton(
                        onClick = { showGoalDialog = true },
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                    ) {
                        Text("Edit Goal")
                    }
                }
            }

            item(key = "appearance") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = stringResource(R.string.appearance),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            Text(
                                text = stringResource(R.string.dark_mode),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ThemeModeSelector(
                                selected = themeMode,
                                onSelect = viewModel::setThemeMode
                            )
                        }
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.accent_color)) },
                            trailingContent = {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    AccentColor.entries.forEachIndexed { index, color ->
                                        AccentSwatch(
                                            color = color,
                                            selected = accentColorIndex == index,
                                            onClick = { viewModel.setAccentColorIndex(index) }
                                        )
                                    }
                                }
                            }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.notifications)) },
                            trailingContent = {
                                // Writes UserPreferences.notificationsEnabled, which the streak reminder
                                // scheduler/worker read.
                                Switch(
                                    checked = notificationsEnabled,
                                    onCheckedChange = viewModel::setNotificationsEnabled
                                )
                            }
                        )
                    }
                }
            }

            item(key = "relic_header") {
                Text(
                    text = "🏆 " + stringResource(R.string.relic_showcase),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (relics.isEmpty()) {
                item(key = "relic_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_relics_yet),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val chunkedRelics = relics.chunked(RELICS_PER_ROW)
                items(
                    items = chunkedRelics,
                    key = { row -> "relic_row_${row.first().id}" }
                ) { rowRelics ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowRelics.forEach { relic ->
                            Box(modifier = Modifier.weight(1f)) {
                                RelicCardNode(
                                    relic = relic,
                                    tilt = tilt,
                                    onClick = { viewModel.selectJourney(relic.bookId) }
                                )
                            }
                        }
                        repeat(RELICS_PER_ROW - rowRelics.size) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        if (showTimeExplanationDialog) {
            AlertDialog(
                onDismissRequest = { showTimeExplanationDialog = false },
                title = { Text(text = "How Climb Time Is Calculated") },
                text = {
                    Text(
                        text = "Climb time is an estimate: every page you read counts as 1.5 minutes " +
                            "(an average reading pace). The timed sessions line below it is the real " +
                            "time you recorded with the stopwatch."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showTimeExplanationDialog = false }) {
                        Text(text = "Got it")
                    }
                }
            )
        }

        if (showGoalDialog) {
            var goalInput by rememberSaveable { mutableStateOf(annualGoal.toString()) }
            AlertDialog(
                onDismissRequest = { showGoalDialog = false },
                title = { Text("Set Annual Goal") },
                text = {
                    OutlinedTextField(
                        value = goalInput,
                        onValueChange = { goalInput = it.filter { char -> char.isDigit() }.take(4) },
                        label = { Text("Number of books") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        goalInput.toIntOrNull()?.let { if (it > 0) viewModel.setAnnualGoal(it) }
                        showGoalDialog = false
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGoalDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        val journeyId = selectedJourneyBookId
        if (journeyId != null) {
            val relic = relics.find { it.bookId == journeyId }
            if (relic != null) {
                JourneyTimelineSheet(
                    relic = relic,
                    book = journeyBook,
                    totalTimeMillis = journeyTotalTimeMillis,
                    sessions = journeySessions,
                    notes = journeyNotes,
                    onDismiss = { viewModel.selectJourney(null) }
                )
            }
        }
    }
}

private const val RELICS_PER_ROW = 3

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Local text state, saved debounced / on leave, so typing never fights the async DataStore flow. */
@Composable
private fun UsernameField(savedName: String, onSave: (String) -> Unit) {
    var edited by rememberSaveable { mutableStateOf<String?>(null) }
    val latestEdited by rememberUpdatedState(edited)
    val latestSave by rememberUpdatedState(onSave)

    LaunchedEffect(edited) {
        val text = edited ?: return@LaunchedEffect
        delay(500)
        latestSave(text)
    }
    DisposableEffect(Unit) {
        onDispose { latestEdited?.let { latestSave(it) } }
    }

    OutlinedTextField(
        value = edited ?: savedName,
        onValueChange = { edited = it },
        label = { Text(stringResource(R.string.username)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Avatar per tier: Novice (climber), Expedition Leader (+ backpack), Elite Alpinist (+ helmet and ice axe). */
@Composable
private fun ClimberAvatar(tier: AvatarTier, breathing: Boolean) {
    val ringColor = when (tier) {
        AvatarTier.NOVICE -> MaterialTheme.colorScheme.outline
        AvatarTier.EXPEDITION_LEADER -> MaterialTheme.colorScheme.secondary
        AvatarTier.ELITE_ALPINIST -> MaterialTheme.colorScheme.primary
    }
    val scale = if (breathing) {
        val transition = rememberInfiniteTransition(label = "avatar_breathe")
        val value by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.07f,
            animationSpec = infiniteRepeatable(
                animation = tween(1600, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "avatar_scale"
        )
        value
    } else {
        1f
    }

    Box(
        modifier = Modifier
            .size(112.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(
                BorderStroke(if (tier == AvatarTier.NOVICE) 2.dp else 4.dp, ringColor),
                CircleShape
            )
            .semantics { contentDescription = "Climber avatar, ${tier.displayName}" },
        contentAlignment = Alignment.Center
    ) {
        Text(text = "🧗", fontSize = 56.sp, modifier = Modifier.clearAndSetSemantics { })
        if (tier.tierNumber >= 2) {
            Text(
                text = "🎒",
                fontSize = 26.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 6.dp, bottom = 10.dp)
                    .clearAndSetSemantics { }
            )
        }
        if (tier.tierNumber >= 3) {
            Text(
                text = "⛑️",
                fontSize = 26.sp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
                    .clearAndSetSemantics { }
            )
            Text(
                text = "🪓",
                fontSize = 26.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 6.dp, bottom = 10.dp)
                    .clearAndSetSemantics { }
            )
        }
    }
}

/** System / Light / Dark. SYSTEM is the real default, so the control never claims a lie. */
@Composable
private fun ThemeModeSelector(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        ThemeMode.SYSTEM to "System",
        ThemeMode.LIGHT to "Light",
        ThemeMode.DARK to "Dark"
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun AccentSwatch(color: AccentColor, selected: Boolean, onClick: () -> Unit) {
    // 24dp visuals inside a 40dp touch target.
    Box(
        modifier = Modifier
            .size(40.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = "${color.displayName} accent color" },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(color.lightPrimary)
                .border(
                    width = if (selected) 2.dp else 0.dp,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = color.lightOnPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

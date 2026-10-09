package com.example.summitread.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import com.example.summitread.theme.tabularNumbers
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.summitread.data.BookRepository
import com.example.summitread.data.UserPreferences
import com.example.summitread.platform.LocalReadingTimer
import com.example.summitread.platform.TimerStatus
import com.example.summitread.resources.Res
import com.example.summitread.resources.books_completed
import com.example.summitread.resources.no_relics_yet
import com.example.summitread.resources.profile
import com.example.summitread.resources.relic_showcase
import com.example.summitread.resources.total_pages_read
import kotlin.time.Clock
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import com.example.summitread.theme.AppSpacing
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.theme.rememberAppHaptics
import com.example.summitread.ui.components.AlertAction
import com.example.summitread.ui.components.AlertActionStyle
import com.example.summitread.ui.components.AppAlertDialog
import com.example.summitread.ui.components.CardContainer
import com.example.summitread.ui.components.IconCircleButton
import com.example.summitread.ui.components.LargeTitleScaffold
import com.example.summitread.ui.components.LocalBottomBarHeight
import com.example.summitread.ui.components.ProgressRing
import com.example.summitread.ui.components.StatTile

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
    val currentStreak by viewModel.currentStreak.collectAsStateWithLifecycle()
    val selectedJourneyBookId by viewModel.selectedJourneyBookId.collectAsStateWithLifecycle()
    val journeySessions by viewModel.journeySessions.collectAsStateWithLifecycle()
    val journeyNotes by viewModel.journeyNotes.collectAsStateWithLifecycle()
    val journeyBook by viewModel.journeyBook.collectAsStateWithLifecycle()
    val journeyTotalTimeMillis by viewModel.journeyTotalTimeMillis.collectAsStateWithLifecycle()
    val readingTimer = LocalReadingTimer.current
    // Only flips when the timer starts/stops running, so the screen does not recompose every second.
    val isTimerRunning by remember(readingTimer) {
        readingTimer.state.map { it.status == TimerStatus.RUNNING }.distinctUntilChanged()
    }.collectAsStateWithLifecycle(initialValue = false)

    var showTimeExplanationDialog by rememberSaveable { mutableStateOf(false) }
    var showGoalSheet by rememberSaveable { mutableStateOf(false) }

    // ONE sensor listener for every relic card (#39); lifecycle-aware, 0.5 deg threshold.
    val tilt = rememberTiltState()
    val haptics = rememberAppHaptics()
    val bottomBar = LocalBottomBarHeight.current
    val year = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year }

    LargeTitleScaffold(
        title = stringResource(Res.string.profile),
        modifier = modifier
    ) { padding ->
        // Single LazyColumn (#43): relic cards are chunked into rows via items(), nothing scrolls inside it.
        // The scaffold padding already clears the glass bars and LocalBottomBarHeight.
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = AppSpacing.screenHorizontal,
                end = AppSpacing.screenHorizontal,
                top = padding.calculateTopPadding() + AppSpacing.xs,
                bottom = maxOf(padding.calculateBottomPadding(), bottomBar) + AppSpacing.xl
            ),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.xl)
        ) {
            item(key = "hero") {
                ProfileHero(
                    username = username,
                    tier = AvatarTier.forCompletedBooks(completedBooksCount),
                    completedBooks = completedBooksCount,
                    streak = currentStreak,
                    breathing = isTimerRunning
                )
            }

            item(key = "stats") {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                        StatRow {
                            StatTile(
                                value = completedBooksCount.toString(),
                                label = stringResource(Res.string.books_completed),
                                icon = Icons.AutoMirrored.Rounded.MenuBook,
                                iconTint = AppTheme.colors.gold,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                            StatTile(
                                value = totalPagesRead.toString(),
                                label = stringResource(Res.string.total_pages_read),
                                icon = Icons.Rounded.AutoStories,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                        StatRow {
                            val climbCompact = ProfileLogic.formatClimbTimeCompact(totalPagesRead)
                            StatTile(
                                value = climbCompact,
                                label = "ClimbTime",
                                icon = Icons.Rounded.Timer,
                                iconTint = AppTheme.colors.success,
                                onClickLabel = "Explain ClimbTime",
                                onClick = {
                                    haptics.light()
                                    showTimeExplanationDialog = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .semantics { contentDescription = totalTimeRead }
                            )
                            StatTile(
                                value = currentStreak.toString(),
                                label = if (currentStreak == 1) "Day streak" else "Days streak",
                                icon = Icons.Rounded.LocalFireDepartment,
                                iconTint = AppTheme.colors.warning,
                                modifier = Modifier.weight(1f).fillMaxHeight()
                            )
                        }
                    }
                    Text(
                        text = "$totalTimeRead  ·  Timed sessions: " +
                            ProfileLogic.formatTimedDuration(totalTimedReadMillis),
                        style = AppType.Footnote,
                        color = AppTheme.colors.secondaryLabel,
                        modifier = Modifier.padding(horizontal = AppSpacing.xs, vertical = AppSpacing.xxs)
                    )
                }
            }

            item(key = "goal") {
                GoalCard(
                    completed = completedThisYear,
                    goal = annualGoal,
                    year = year,
                    onEdit = {
                        haptics.light()
                        showGoalSheet = true
                    }
                )
            }

            item(key = "relic_header") {
                SectionTitle(
                    text = stringResource(Res.string.relic_showcase),
                    modifier = Modifier.padding(start = AppSpacing.xs)
                )
            }

            if (relics.isEmpty()) {
                item(key = "relic_empty") {
                    CardContainer(modifier = Modifier.fillMaxWidth()) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = stringResource(Res.string.no_relics_yet),
                                style = AppType.Subheadline,
                                textAlign = TextAlign.Center,
                                color = AppTheme.colors.secondaryLabel,
                                modifier = Modifier.padding(vertical = AppSpacing.md)
                            )
                        }
                    }
                }
            } else {
                items(
                    items = relics.chunked(RELICS_PER_ROW),
                    key = { row -> "relic_row_${row.first().id}" }
                ) { rowRelics ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        rowRelics.forEach { relic ->
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                RelicCardNode(
                                    relic = relic,
                                    tilt = tilt,
                                    onClick = {
                                        haptics.light()
                                        viewModel.selectJourney(relic.bookId)
                                    }
                                )
                            }
                        }
                        repeat(RELICS_PER_ROW - rowRelics.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            item(key = "settings") {
                ProfileSettings(
                    themeMode = themeMode,
                    onThemeMode = viewModel::setThemeMode,
                    accentColorIndex = accentColorIndex,
                    onAccent = viewModel::setAccentColorIndex,
                    notificationsEnabled = notificationsEnabled,
                    onNotifications = viewModel::setNotificationsEnabled,
                    username = username,
                    onUsername = viewModel::updateUsername
                )
            }
        }

        if (showTimeExplanationDialog) {
            AppAlertDialog(
                title = "How ClimbTime Is Calculated",
                message = "ClimbTime is an estimate: every page you read counts as 1.5 minutes " +
                    "(an average reading pace). The timed sessions line is the real " +
                    "time you recorded with the stopwatch.",
                onDismissRequest = { showTimeExplanationDialog = false },
                actions = listOf(
                    AlertAction("Got it", { showTimeExplanationDialog = false }, AlertActionStyle.Cancel)
                )
            )
        }

        if (showGoalSheet) {
            GoalSheet(
                currentGoal = annualGoal,
                year = year,
                onSave = { viewModel.setAnnualGoal(it) },
                onDismiss = { showGoalSheet = false }
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

private const val RELICS_PER_ROW = 2

@Composable
private fun StatRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        content = content
    )
}

@Composable
internal fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = AppType.Title3,
        color = AppTheme.colors.label,
        modifier = modifier.semantics { heading() }
    )
}

@Composable
private fun GoalCard(completed: Int, goal: Int, year: Int, onEdit: () -> Unit) {
    val colors = AppTheme.colors
    val progress = ProfileLogic.goalProgress(completed, goal)
    val percent = (progress * 100).toInt()
    CardContainer(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            ProgressRing(
                progress = progress,
                size = 76.dp,
                strokeWidth = 9.dp,
                color = if (progress >= 1f) colors.gold else colors.tint,
                modifier = Modifier.semantics { contentDescription = "$percent percent of the yearly goal" }
            ) {
                Text(
                    text = "$percent%",
                    style = AppType.Headline.tabularNumbers(),
                    color = colors.label,
                    modifier = Modifier.clearAndSetSemantics { }
                )
            }
            Column(Modifier.weight(1f)) {
                Text("Annual Reading Goal", style = AppType.Footnote, color = colors.secondaryLabel)
                Text(
                    text = "$completed of $goal books in $year",
                    style = AppType.Title3,
                    color = colors.label
                )
            }
            IconCircleButton(
                icon = Icons.Rounded.Edit,
                contentDescription = "Edit annual goal",
                onClick = onEdit
            )
        }
    }
}

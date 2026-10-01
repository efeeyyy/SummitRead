package com.example.summitread.ui.profile

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.graphics.graphicsLayer
import com.example.summitread.services.ReadingTimerService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.summitread.R
import com.example.summitread.data.BookRepository
import com.example.summitread.data.ThemeMode
import com.example.summitread.data.UserPreferences
import com.example.summitread.theme.AccentColor

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
    val totalPagesRead by viewModel.totalPagesRead.collectAsStateWithLifecycle()
    val totalTimeRead by viewModel.totalTimeRead.collectAsStateWithLifecycle()
    val selectedJourneyBookId by viewModel.selectedJourneyBookId.collectAsStateWithLifecycle()
    val journeySessions by viewModel.journeySessions.collectAsStateWithLifecycle()
    val journeyNotes by viewModel.journeyNotes.collectAsStateWithLifecycle()

    var showTimeExplanationDialog by remember { mutableStateOf(false) }
    val isTimerRunning by ReadingTimerService.isRunning.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.profile)) })
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val avatarText = when {
                            completedBooksCount >= 6 -> "🧗🪖🪓" // Elite Alpinist
                            completedBooksCount >= 2 -> "🧗🎒" // Expedition Leader
                            else -> "🧗" // Novice
                        }

                        val avatarModifier = if (isTimerRunning) {
                            val infiniteTransition = rememberInfiniteTransition(label = "avatar_breathe")
                            val shiftAnim by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing)
                                ),
                                label = "avatar_shift"
                            )
                            Modifier.graphicsLayer {
                                translationY = (Math.sin(shiftAnim.toDouble() * Math.PI * 2) * 8f).toFloat()
                                rotationZ = (Math.cos(shiftAnim.toDouble() * Math.PI * 2) * 5f).toFloat()
                            }
                        } else {
                            Modifier
                        }

                        Box(modifier = avatarModifier) {
                            Text(text = avatarText, fontSize = 64.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = username,
                            onValueChange = viewModel::updateUsername,
                            label = { Text(stringResource(R.string.username)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.material3.ElevatedCard(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = completedBooksCount.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.books_completed),
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                    androidx.compose.material3.ElevatedCard(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = totalPagesRead.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(R.string.total_pages_read),
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            item {
                androidx.compose.material3.ElevatedCard(
                    modifier = Modifier.fillMaxWidth().clickable { showTimeExplanationDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "⏱️", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = totalTimeRead,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
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
                                text = "$completedBooksCount / $annualGoal Books",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                            androidx.compose.material3.CircularProgressIndicator(
                                progress = { 1f },
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                            androidx.compose.material3.CircularProgressIndicator(
                                progress = { (completedBooksCount.toFloat() / annualGoal.coerceAtLeast(1)).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.primary,
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                            Text(
                                text = "${((completedBooksCount.toFloat() / annualGoal.coerceAtLeast(1)).coerceIn(0f, 1f) * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    var showGoalDialog by remember { mutableStateOf(false) }
                    TextButton(
                        onClick = { showGoalDialog = true },
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                    ) {
                        Text("Edit Goal")
                    }
                    
                    if (showGoalDialog) {
                        var goalInput by remember { mutableStateOf(annualGoal.toString()) }
                        androidx.compose.material3.AlertDialog(
                            onDismissRequest = { showGoalDialog = false },
                            title = { Text("Set Annual Goal") },
                            text = {
                                OutlinedTextField(
                                    value = goalInput,
                                    onValueChange = { goalInput = it.filter { char -> char.isDigit() } },
                                    label = { Text("Number of books") },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                    singleLine = true
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    goalInput.toIntOrNull()?.let { 
                                        if (it > 0) viewModel.setAnnualGoal(it) 
                                    }
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
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            text = stringResource(R.string.appearance),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.dark_mode)) },
                            trailingContent = {
                                Switch(
                                    checked = themeMode == ThemeMode.DARK,
                                    onCheckedChange = { isDark ->
                                        viewModel.setThemeMode(if (isDark) ThemeMode.DARK else ThemeMode.LIGHT)
                                    }
                                )
                            }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.accent_color)) },
                            trailingContent = {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    AccentColor.entries.forEachIndexed { index, color ->
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(color.lightPrimary)
                                                .clickable { viewModel.setAccentColorIndex(index) }
                                                .border(
                                                    width = if (accentColorIndex == index) 2.dp else 0.dp,
                                                    color = if (accentColorIndex == index) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (accentColorIndex == index) {
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
                            }
                        )
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.notifications)) },
                            trailingContent = {
                                Switch(
                                    checked = notificationsEnabled,
                                    onCheckedChange = viewModel::setNotificationsEnabled
                                )
                            }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "🏆 " + stringResource(R.string.relic_showcase),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (relics.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No summits conquered yet! Start reading 📖",
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val chunkedRelics = relics.chunked(3)
                items(
                    count = chunkedRelics.size,
                    key = { index -> "relic_row_$index" }
                ) { index ->
                    val rowRelics = chunkedRelics[index]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowRelics.forEach { relic ->
                            Box(modifier = Modifier.weight(1f)) {
                                RelicCardNode(
                                    relic = relic,
                                    onClick = { viewModel.selectJourney(relic.bookId) }
                                )
                            }
                        }
                        repeat(3 - rowRelics.size) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        if (showTimeExplanationDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showTimeExplanationDialog = false },
                title = { Text(text = "Estimated Time Calculation") },
                text = { Text(text = "This is an estimated reading time calculated based on an average reading pace of 1.5 minutes per page.") },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = { showTimeExplanationDialog = false }) {
                        Text(text = "Got it")
                    }
                }
            )
        }

        if (selectedJourneyBookId != null) {
            val relic = relics.find { it.bookId == selectedJourneyBookId }
            if (relic != null) {
                JourneyTimelineSheet(
                    relic = relic,
                    sessions = journeySessions,
                    notes = journeyNotes,
                    onDismiss = { viewModel.selectJourney(null) }
                )
            }
        }
    }
}

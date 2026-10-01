package com.example.summitread.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.summitread.R
import com.example.summitread.data.BookRepository
import com.example.summitread.data.UserPreferences
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    bookRepository: BookRepository,
    userPreferences: UserPreferences,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onNavigateToMountain: (bookId: Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.factory(bookRepository, userPreferences)
    )
    val allBooks by viewModel.allBooks.collectAsStateWithLifecycle()
    val archivedBooks by viewModel.archivedBooks.collectAsStateWithLifecycle()
    val booksLoaded by viewModel.booksLoaded.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    // null until DataStore answered, so the wrong view is not flashed first.
    val listViewPref by userPreferences.isListView.collectAsStateWithLifecycle<Boolean?>(initialValue = null)
    val currentStreak by viewModel.currentStreak.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // UI state that must survive rotation / process death.
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var showSortMenu by rememberSaveable { mutableStateOf(false) }
    var showOverflowMenu by rememberSaveable { mutableStateOf(false) }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var archiveTargetId by rememberSaveable { mutableStateOf<Long?>(null) }

    val isListView = listViewPref == true
    val shimmer = rememberGoldShimmer(enabled = allBooks.any { it.isCompleted })
    val openBook: (Long) -> Unit = { bookId ->
        viewModel.setActiveBook(bookId)
        onNavigateToMountain(bookId)
    }

    BackHandler(enabled = showArchived) { showArchived = false }

    Scaffold(
        modifier = modifier,
        topBar = {
            if (showArchived) {
                TopAppBar(
                    title = { Text("Archived books") },
                    navigationIcon = {
                        IconButton(onClick = { showArchived = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to bookshelf")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("📚 SummitRead") },
                    actions = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            // Streak flame: the value is the derived streak from the logged days.
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clearAndSetSemantics {
                                    contentDescription =
                                        "Current streak: $currentStreak ${if (currentStreak == 1) "day" else "days"}"
                                }
                            ) {
                                Text(text = "🔥", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentStreak.toString(),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))

                            if (isListView) {
                                Box {
                                    IconButton(onClick = { showSortMenu = true }) {
                                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort books")
                                    }
                                    DropdownMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = { showSortMenu = false }
                                    ) {
                                        SortOption.entries.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option.label) },
                                                trailingIcon = {
                                                    if (option == sortOption) {
                                                        Icon(Icons.Default.Check, contentDescription = "Selected")
                                                    }
                                                },
                                                onClick = {
                                                    viewModel.setSortOption(option)
                                                    showSortMenu = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            IconButton(onClick = {
                                scope.launch { userPreferences.setIsListView(!isListView) }
                            }) {
                                Icon(
                                    imageVector = if (isListView) Icons.Default.GridView else Icons.AutoMirrored.Filled.List,
                                    contentDescription = if (isListView) "Switch to shelf view" else "Switch to list view"
                                )
                            }

                            Box {
                                IconButton(onClick = { showOverflowMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                                }
                                DropdownMenu(
                                    expanded = showOverflowMenu,
                                    onDismissRequest = { showOverflowMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (archivedBooks.isEmpty()) "Archived"
                                                else "Archived (${archivedBooks.size})"
                                            )
                                        },
                                        onClick = {
                                            showOverflowMenu = false
                                            showArchived = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!showArchived) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_book))
                }
            }
        }
    ) { paddingValues ->
        val listPadding = PaddingValues(
            top = paddingValues.calculateTopPadding(),
            bottom = paddingValues.calculateBottomPadding() + 80.dp
        )

        when {
            showArchived -> ArchivedBooksList(
                books = archivedBooks,
                onUnarchive = { viewModel.archiveBook(it.id, archived = false) },
                contentPadding = listPadding
            )

            !booksLoaded || listViewPref == null -> Box(modifier = Modifier.fillMaxSize())

            allBooks.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_books_yet),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            isListView -> BookListLayout(
                books = allBooks,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                onBookClick = openBook,
                onReorder = viewModel::saveManualOrder,
                onArchiveBook = { archiveTargetId = it.id },
                contentPadding = listPadding,
                shimmer = shimmer,
                modifier = Modifier.fillMaxSize()
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = listPadding
            ) {
                // Group books into shelves of 4.
                items(allBooks.chunked(4), key = { shelf -> shelf.first().id }) { shelfBooks ->
                    Spacer(modifier = Modifier.height(24.dp))
                    BookshelfLayout(
                        books = shelfBooks,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onBookClick = openBook,
                        shimmer = shimmer,
                        onArchiveBook = { archiveTargetId = it.id }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddBookDialog(
                onDismissRequest = { showAddDialog = false },
                onSave = { title, author, totalPages, coverImagePath, accentColorIndex, coverColorArgb ->
                    viewModel.addBook(
                        title, author, totalPages, coverImagePath, accentColorIndex, coverColorArgb
                    )
                    showAddDialog = false
                }
            )
        }

        val archiveTarget = archiveTargetId?.let { id -> allBooks.firstOrNull { it.id == id } }
        // The book vanished (e.g. archived elsewhere): forget the pending confirmation once data is loaded.
        LaunchedEffect(archiveTarget, booksLoaded) {
            if (archiveTarget == null && booksLoaded && archiveTargetId != null) archiveTargetId = null
        }
        if (archiveTarget != null) {
            ArchiveConfirmDialog(
                bookTitle = archiveTarget.title,
                onConfirm = {
                    viewModel.archiveBook(archiveTarget.id, archived = true)
                    archiveTargetId = null
                },
                onDismiss = { archiveTargetId = null }
            )
        }
    }
}

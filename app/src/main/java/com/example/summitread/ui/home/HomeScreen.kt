package com.example.summitread.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.summitread.R
import com.example.summitread.data.BookRepository
import com.example.summitread.data.UserPreferences
import com.example.summitread.theme.AppSpacing
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.ui.components.IconButtonStyle
import com.example.summitread.ui.components.IconCircleButton
import com.example.summitread.ui.components.LargeTitleScaffold
import com.example.summitread.ui.components.rememberLargeTitleState
import kotlinx.coroutines.launch

/** Which body the Home screen shows; used to re-sync the large-title collapse when the scroll position resets. */
private const val MODE_SHELF = 0
private const val MODE_LIST = 1
private const val MODE_ARCHIVED = 2

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
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
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var archiveTargetId by rememberSaveable { mutableStateOf<Long?>(null) }

    val isListView = listViewPref == true
    val shimmer = rememberGoldShimmer(enabled = allBooks.any { it.isCompleted })
    val openBook: (Long) -> Unit = { bookId ->
        viewModel.setActiveBook(bookId)
        onNavigateToMountain(bookId)
    }

    // A different body starts at the top of its own list, so the large title must expand again. Compared against a
    // saved value, so a rotation (same body, restored scroll position) keeps the collapse as it was.
    val titleState = rememberLargeTitleState()
    val mode = when {
        showArchived -> MODE_ARCHIVED
        isListView -> MODE_LIST
        else -> MODE_SHELF
    }
    var lastMode by rememberSaveable { mutableIntStateOf(mode) }
    LaunchedEffect(mode) {
        if (mode != lastMode) {
            lastMode = mode
            titleState.reset()
        }
    }

    BackHandler(enabled = showArchived) { showArchived = false }

    LargeTitleScaffold(
        title = stringResource(if (showArchived) R.string.archived_title else R.string.library),
        modifier = modifier,
        state = titleState,
        navigationIcon = {
            if (showArchived) {
                IconCircleButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.back_to_library),
                    onClick = { showArchived = false }
                )
            } else {
                // Streak flame: the value is the derived streak from the logged days.
                StreakBadge(streak = currentStreak, modifier = Modifier.padding(start = AppSpacing.xs))
            }
        },
        actions = {
            if (!showArchived) {
                if (allBooks.isNotEmpty()) {
                    IconCircleButton(
                        icon = if (isListView) Icons.Rounded.GridView else Icons.AutoMirrored.Rounded.List,
                        contentDescription = stringResource(
                            if (isListView) R.string.switch_to_shelf_view else R.string.switch_to_list_view
                        ),
                        onClick = { scope.launch { userPreferences.setIsListView(!isListView) } }
                    )
                }
                IconCircleButton(
                    icon = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.add_book),
                    onClick = { showAddSheet = true },
                    style = IconButtonStyle.Filled
                )
            }
        }
    ) { padding ->
        // Everything scrolls under the glass bars; the scaffold padding already includes the tab bar height.
        val listPadding = PaddingValues(
            top = padding.calculateTopPadding(),
            bottom = padding.calculateBottomPadding() + AppSpacing.xl
        )
        val archivedEntry: (@Composable () -> Unit)? = if (archivedBooks.isNotEmpty()) {
            {
                ArchivedEntry(
                    count = archivedBooks.size,
                    onClick = { showArchived = true },
                    modifier = Modifier.padding(horizontal = AppSpacing.screenHorizontal, vertical = AppSpacing.xl)
                )
            }
        } else null
        val listHeader: @Composable () -> Unit = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpacing.screenHorizontal, vertical = AppSpacing.xxs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SortChip(current = sortOption, onSelect = viewModel::setSortOption)
                Text(
                    text = bookCountText(allBooks.size),
                    style = AppType.Footnote,
                    color = AppTheme.colors.secondaryLabel,
                    modifier = Modifier.padding(end = AppSpacing.xxs)
                )
            }
        }

        when {
            showArchived -> ArchivedBooksList(
                books = archivedBooks,
                onUnarchive = { viewModel.archiveBook(it.id, archived = false) },
                contentPadding = listPadding
            )

            !booksLoaded || listViewPref == null -> Box(modifier = Modifier.fillMaxSize())

            allBooks.isEmpty() -> Box(modifier = Modifier.fillMaxSize()) {
                HomeEmptyState(
                    icon = Icons.Rounded.AutoStories,
                    title = stringResource(R.string.empty_library_title),
                    message = stringResource(R.string.empty_library_message),
                    actionText = stringResource(R.string.add_book),
                    actionIcon = Icons.Rounded.Add,
                    onAction = { showAddSheet = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(listPadding)
                )
                archivedEntry?.let { entry ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = padding.calculateBottomPadding())
                    ) { entry() }
                }
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
                header = listHeader,
                footer = archivedEntry,
                modifier = Modifier.fillMaxSize()
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = listPadding
            ) {
                item(key = "header") { listHeader() }
                // Group books into shelves of 4.
                items(allBooks.chunked(4), key = { shelf -> shelf.first().id }) { shelfBooks ->
                    Spacer(modifier = Modifier.height(AppSpacing.md))
                    BookshelfLayout(
                        books = shelfBooks,
                        sharedTransitionScope = sharedTransitionScope,
                        animatedVisibilityScope = animatedVisibilityScope,
                        onBookClick = openBook,
                        shimmer = shimmer,
                        onArchiveBook = { archiveTargetId = it.id }
                    )
                }
                archivedEntry?.let { entry -> item(key = "footer") { entry() } }
            }
        }

        if (showAddSheet) {
            AddBookSheet(
                onDismissRequest = { showAddSheet = false },
                onSave = { title, author, totalPages, coverImagePath, accentColorIndex, coverColorArgb ->
                    viewModel.addBook(
                        title, author, totalPages, coverImagePath, accentColorIndex, coverColorArgb
                    )
                    showAddSheet = false
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

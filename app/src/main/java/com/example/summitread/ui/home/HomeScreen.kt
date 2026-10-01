package com.example.summitread.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.launch

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope

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
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val isListView by userPreferences.isListView.collectAsStateWithLifecycle(initialValue = false)
    val currentStreak by userPreferences.currentStreak.collectAsStateWithLifecycle(initialValue = 0)
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("📚 SummitRead") },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(text = "🔥", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentStreak.toString(),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        if (isListView) {
                            Box {
                                IconButton(onClick = { showSortMenu = true }) {
                                    Icon(Icons.Default.Sort, contentDescription = "Sort")
                                }
                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Manual Order") },
                                        onClick = { 
                                            viewModel.setSortOption(SortOption.MANUAL)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Newest First") },
                                        onClick = { 
                                            viewModel.setSortOption(SortOption.NEWEST_FIRST)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Recently Read") },
                                        onClick = { 
                                            viewModel.setSortOption(SortOption.RECENTLY_READ)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Progress Percentage") },
                                        onClick = { 
                                            viewModel.setSortOption(SortOption.PROGRESS)
                                            showSortMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Alphabetical") },
                                        onClick = { 
                                            viewModel.setSortOption(SortOption.ALPHABETICAL)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        IconButton(onClick = { 
                            scope.launch { userPreferences.setIsListView(!isListView) }
                        }) {
                            Icon(
                                imageVector = if (isListView) Icons.Default.GridView else Icons.Default.List,
                                contentDescription = "Toggle View"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_book))
            }
        }
    ) { paddingValues ->
        if (allBooks.isEmpty()) {
            Box(
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
        } else {
            if (isListView) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        BookListLayout(
                            books = allBooks,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onBookClick = { bookId ->
                                viewModel.setActiveBook(bookId)
                                onNavigateToMountain(bookId)
                            },
                            onSaveOrder = if (sortOption == SortOption.MANUAL) {
                                { orderedList -> viewModel.updateBookOrder(orderedList) }
                            } else null
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // Group books into rows of 4
                    val chunkedBooks = allBooks.chunked(4)
                    items(chunkedBooks) { shelfBooks ->
                        Spacer(modifier = Modifier.height(24.dp))
                        BookshelfLayout(
                            books = shelfBooks,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            onBookClick = { bookId ->
                                viewModel.setActiveBook(bookId)
                                onNavigateToMountain(bookId)
                            }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddBookDialog(
                onDismissRequest = { showAddDialog = false },
                onSave = { title, author, totalPages, coverImagePath, accentColorIndex ->
                    scope.launch {
                        viewModel.addBook(title, author, totalPages, coverImagePath, accentColorIndex)
                        showAddDialog = false
                    }
                }
            )
        }
    }
}

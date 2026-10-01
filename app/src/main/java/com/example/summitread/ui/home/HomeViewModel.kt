package com.example.summitread.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.summitread.data.BookRepository
import com.example.summitread.data.UserPreferences
import com.example.summitread.data.db.BookEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SortOption {
    MANUAL,
    NEWEST_FIRST,
    RECENTLY_READ,
    PROGRESS,
    ALPHABETICAL
}

class HomeViewModel(
    private val repository: BookRepository,
    private val preferences: UserPreferences
) : ViewModel() {

    private val _sortOption = MutableStateFlow(SortOption.MANUAL)
    val sortOption = _sortOption.asStateFlow()

    val allBooks: StateFlow<List<BookEntity>> = repository.allBooks
        .combine(_sortOption) { books, option ->
            when (option) {
                SortOption.MANUAL -> books.sortedBy { it.displayOrder }
                SortOption.NEWEST_FIRST -> books.sortedByDescending { it.createdAtMillis }
                SortOption.RECENTLY_READ -> books.sortedByDescending { it.lastActivityMillis }
                SortOption.PROGRESS -> books.sortedByDescending { 
                    if (it.totalPages > 0) it.currentPages.toFloat() / it.totalPages else 0f 
                }
                SortOption.ALPHABETICAL -> books.sortedBy { it.title }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    val activeBookId: StateFlow<Long> = preferences.activeBookId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), -1L)

    fun addBook(
        title: String,
        author: String,
        totalPages: Int,
        coverImagePath: String?,
        accentColorIndex: Int
    ) {
        viewModelScope.launch {
            repository.addBook(
                title = title,
                author = author,
                totalPages = totalPages,
                coverImagePath = coverImagePath,
                accentColorIndex = accentColorIndex
            )
        }
    }

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch {
            repository.deleteBook(book)
        }
    }

    fun updateBookOrder(orderedBooks: List<BookEntity>) {
        viewModelScope.launch {
            orderedBooks.forEachIndexed { index, book ->
                repository.updateBook(book.copy(displayOrder = index))
            }
        }
    }

    fun setActiveBook(bookId: Long) {
        viewModelScope.launch {
            preferences.setActiveBookId(bookId)
        }
    }

    companion object {
        fun factory(repository: BookRepository, preferences: UserPreferences): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { HomeViewModel(repository, preferences) }
            }
    }
}

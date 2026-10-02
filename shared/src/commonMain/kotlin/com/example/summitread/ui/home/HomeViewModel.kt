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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOption(val label: String) {
    MANUAL("Manual Order"),
    NEWEST_FIRST("Newest First"),
    RECENTLY_READ("Recently Read"),
    PROGRESS("Progress Percentage"),
    ALPHABETICAL("Alphabetical")
}

class HomeViewModel(
    private val repository: BookRepository,
    private val preferences: UserPreferences
) : ViewModel() {

    /** The chosen sort option, persisted across launches (MANUAL until the user picks another). */
    val sortOption: StateFlow<SortOption> = preferences.sortOptionName
        .map { name -> SortOption.entries.firstOrNull { it.name == name } ?: SortOption.MANUAL }
        .catch { emit(SortOption.MANUAL) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SortOption.MANUAL)

    /**
     * Shelf/list books (archived books are excluded), sorted by [sortOption]. null until the first database
     * emission: this single value is both the data and the "loaded" signal (see [homeBody]).
     */
    val books: StateFlow<List<BookEntity>?> = repository.allBooks
        .combine(sortOption) { books, option -> sortBooks(books, option) as List<BookEntity>? }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Archived books (history is kept); empty unless something was archived. */
    val archivedBooks: StateFlow<List<BookEntity>> = repository.archivedBooks
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Current streak in calendar days; derived from the logs, reads 0 as soon as a day was missed. */
    val currentStreak: StateFlow<Int> = repository.currentStreak
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setSortOption(option: SortOption) {
        viewModelScope.launch { preferences.setSortOptionName(option.name) }
    }

    fun addBook(
        title: String,
        author: String,
        totalPages: Int,
        coverImagePath: String?,
        accentColorIndex: Int,
        coverColorArgb: Int? = null
    ) {
        viewModelScope.launch {
            repository.addBook(
                title = title,
                author = author,
                totalPages = totalPages,
                coverImagePath = coverImagePath,
                accentColorIndex = accentColorIndex,
                coverColorArgb = coverColorArgb
            )
        }
    }

    /** Archives (or restores) a book. Sessions, notes and relics are always kept. */
    fun archiveBook(bookId: Long, archived: Boolean = true) {
        viewModelScope.launch { repository.archiveBook(bookId, archived) }
    }

    /** Hard delete (book + sessions + notes + characters + relic + images). Not exposed in the UI; prefer [archiveBook]. */
    fun deleteBook(book: BookEntity) {
        viewModelScope.launch { repository.deleteBook(book) }
    }

    /** Persists the manual order of the given books (their list index) in one transaction. */
    fun updateBookOrder(orderedBooks: List<BookEntity>) {
        viewModelScope.launch { repository.reorderBooks(orderedBooks.map { it.id }) }
    }

    /**
     * Persists a drag-and-drop result: the dropped order becomes the manual order and the sort switches to
     * [SortOption.MANUAL], so the shelf view reflects it immediately.
     */
    fun saveManualOrder(orderedIds: List<Long>) {
        viewModelScope.launch {
            repository.reorderBooks(orderedIds)
            preferences.setSortOptionName(SortOption.MANUAL.name)
        }
    }

    /** Remembers the last opened book; navigation itself passes the id through the route. */
    fun setActiveBook(bookId: Long) {
        viewModelScope.launch { preferences.setActiveBookId(bookId) }
    }

    companion object {
        fun factory(repository: BookRepository, preferences: UserPreferences): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { HomeViewModel(repository, preferences) }
            }
    }
}

package com.example.summitread.ui.mountain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.summitread.data.BookRepository
import com.example.summitread.data.UserPreferences
import com.example.summitread.data.db.BookEntity
import com.example.summitread.data.db.CharacterEntity
import com.example.summitread.data.db.NoteEntity
import com.example.summitread.data.db.ReadingSessionEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class MountainViewModel(
    private val repository: BookRepository,
    private val userPreferences: UserPreferences,
    private val bookId: Long
) : ViewModel() {

    val activeBook: StateFlow<BookEntity?> = repository.getBookById(bookId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val readingSessions: StateFlow<List<ReadingSessionEntity>> = repository.getSessionsForBook(bookId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = repository.getNotesForBook(bookId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val characters: StateFlow<List<CharacterEntity>> = repository.getCharactersForBook(bookId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isFocusMode = MutableStateFlow(false)
    val isFocusMode: StateFlow<Boolean> = _isFocusMode.asStateFlow()

    private val _selectedDate = MutableStateFlow(startOfDay(System.currentTimeMillis()))
    val selectedDate: StateFlow<Long> = _selectedDate.asStateFlow()

    private val _showSummitAnimation = MutableStateFlow(false)
    val showSummitAnimation: StateFlow<Boolean> = _showSummitAnimation.asStateFlow()

    private val _pagesInput = MutableStateFlow("")
    val pagesInput: StateFlow<String> = _pagesInput.asStateFlow()

    private val _triggerParticles = MutableSharedFlow<Unit>()
    val triggerParticles: SharedFlow<Unit> = _triggerParticles.asSharedFlow()

    val hoursSinceLastSession: StateFlow<Long> = userPreferences.lastReadDate
        .map { lastRead ->
            if (lastRead == 0L) 0L
            else java.time.Duration.between(Instant.ofEpochMilli(lastRead), Instant.now()).toHours()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    fun setSelectedDate(date: Long) {
        _selectedDate.value = date
    }

    fun setPagesInput(value: String) {
        _pagesInput.value = value
    }

    suspend fun logPages(timeSpentMillis: Long = 0L) {
        val pages = _pagesInput.value.toIntOrNull() ?: return
        if (pages <= 0) return
        val book = activeBook.value ?: return
        val completed = repository.logPages(book.id, _selectedDate.value, pages, timeSpentMillis)
        _pagesInput.value = ""
        
        _triggerParticles.emit(Unit)
        
        if (completed) {
            _showSummitAnimation.value = true
        }
        
        // Update streak
        val today = LocalDate.now()
        val loggedDate = Instant.ofEpochMilli(_selectedDate.value).atZone(ZoneId.systemDefault()).toLocalDate()
        val lastReadMillis = userPreferences.lastReadDate.first()
        val currentStreak = userPreferences.currentStreak.first()
        
        if (loggedDate == today) {
            if (lastReadMillis == 0L) {
                userPreferences.updateStreak(today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), 1)
            } else {
                val lastReadDate = Instant.ofEpochMilli(lastReadMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                if (lastReadDate == today.minusDays(1)) {
                    userPreferences.updateStreak(today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), currentStreak + 1)
                } else if (lastReadDate.isBefore(today.minusDays(1))) {
                    userPreferences.updateStreak(today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), 1)
                }
            }
        } else if (loggedDate == today.minusDays(1)) {
            val lastReadDate = if (lastReadMillis > 0L) Instant.ofEpochMilli(lastReadMillis).atZone(ZoneId.systemDefault()).toLocalDate() else null
            if (lastReadDate == null || lastReadDate.isBefore(loggedDate)) {
                // Logging for yesterday
                userPreferences.updateStreak(loggedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), 1)
            }
        }
    }

    fun toggleFocusMode() {
        _isFocusMode.value = !_isFocusMode.value
    }

    fun dismissSummitAnimation() {
        _showSummitAnimation.value = false
    }

    suspend fun saveSummitLogbook(rating: Float, review: String) {
        val book = activeBook.value ?: return
        val relic = repository.getRelicByBookId(book.id)
        if (relic != null) {
            repository.updateRelic(relic.copy(rating = rating, review = review))
        }
    }

    suspend fun addNote(content: String, pageNumber: Int, photoPath: String?) {
        val book = activeBook.value ?: return
        repository.insertNote(
            NoteEntity(
                bookId = book.id,
                pageNumber = pageNumber,
                content = content,
                photoPath = photoPath
            )
        )
    }

    suspend fun deleteNote(note: NoteEntity) {
        repository.deleteNote(note)
    }

    suspend fun addCharacter(name: String, role: String, description: String) {
        val book = activeBook.value ?: return
        repository.insertCharacter(
            CharacterEntity(
                bookId = book.id,
                name = name,
                role = role,
                description = description
            )
        )
    }

    suspend fun deleteCharacter(character: CharacterEntity) {
        repository.deleteCharacter(character)
    }

    companion object {
        fun factory(repository: BookRepository, userPreferences: UserPreferences, bookId: Long): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MountainViewModel(repository, userPreferences, bookId) as T
                }
            }
        }
    }
}

fun startOfDay(millis: Long): Long {
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

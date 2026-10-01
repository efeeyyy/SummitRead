package com.example.summitread.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.summitread.data.BookRepository
import com.example.summitread.data.PageMath
import com.example.summitread.data.ThemeMode
import com.example.summitread.data.UserPreferences
import com.example.summitread.data.db.BookEntity
import com.example.summitread.data.db.NoteEntity
import com.example.summitread.data.db.ReadingSessionEntity
import com.example.summitread.data.db.RelicEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel(
    private val repository: BookRepository,
    private val preferences: UserPreferences
) : ViewModel() {

    private val _selectedJourneyBookId = MutableStateFlow<Long?>(null)
    val selectedJourneyBookId = _selectedJourneyBookId.asStateFlow()

    // The Journey sheet needs a dynamic book id, so flatMapLatest is genuinely required here.

    val journeySessions: StateFlow<List<ReadingSessionEntity>> = _selectedJourneyBookId.flatMapLatest { id ->
        if (id != null) repository.getSessionsForBook(id) else flowOf(emptyList())
    }.catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val journeyNotes: StateFlow<List<NoteEntity>> = _selectedJourneyBookId.flatMapLatest { id ->
        if (id != null) repository.getNotesForBook(id) else flowOf(emptyList())
    }.catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** The book of the open Journey sheet (title, author, pages, completedAtMillis). */
    val journeyBook: StateFlow<BookEntity?> = _selectedJourneyBookId.flatMapLatest { id ->
        if (id != null) repository.getBookById(id) else flowOf(null)
    }.catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Real timed reading (sum of the stopwatch time of all sessions) of the Journey book, in millis. */
    val journeyTotalTimeMillis: StateFlow<Long> = _selectedJourneyBookId.flatMapLatest { id ->
        if (id != null) repository.getTotalTimeMillisForBook(id) else flowOf(0L)
    }.catch { emit(0L) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val relics: StateFlow<List<RelicEntity>> = repository.allRelics
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Completed books of all time: drives the avatar tier. */
    val completedBooksCount: StateFlow<Int> = repository.completedBooksCount
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Books completed in the current calendar year: drives the yearly goal ring (#37). */
    val completedBooksThisYear: StateFlow<Int> = repository.completedBooksThisYear
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalPagesRead: StateFlow<Int> = repository.totalPagesRead
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** ClimbTime (#35, as requested): total pages x 1.5 minutes, e.g. "1 Days, 3 Hours Climbed". */
    val totalTimeRead: StateFlow<String> = repository.totalPagesRead
        .map { pages -> formatClimbTime(pages) }
        .catch { emit(formatClimbTime(0)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), formatClimbTime(0))

    /** Real total timed reading (sum of all stopwatch sessions) in millis; separate from the ClimbTime estimate. */
    val totalTimedReadMillis: StateFlow<Long> = repository.totalTimeReadMillis
        .catch { emit(0L) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val username: StateFlow<String> = preferences.username
        .catch { emit("") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val themeMode: StateFlow<ThemeMode> = preferences.themeMode
        .catch { emit(ThemeMode.SYSTEM) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val accentColorIndex: StateFlow<Int> = preferences.accentColorIndex
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val notificationsEnabled: StateFlow<Boolean> = preferences.notificationsEnabled
        .catch { emit(true) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val annualGoal: StateFlow<Int> = preferences.annualGoal
        .catch { emit(20) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 20)

    fun selectJourney(bookId: Long?) {
        _selectedJourneyBookId.value = bookId
    }

    fun updateUsername(name: String) {
        viewModelScope.launch {
            preferences.setUsername(name)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
        }
    }

    fun setAccentColorIndex(index: Int) {
        viewModelScope.launch {
            preferences.setAccentColorIndex(index)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setNotificationsEnabled(enabled)
        }
    }

    fun setAnnualGoal(goal: Int) {
        viewModelScope.launch {
            preferences.setAnnualGoal(goal)
        }
    }

    companion object {
        fun factory(repository: BookRepository, preferences: UserPreferences) = viewModelFactory {
            initializer { ProfileViewModel(repository, preferences) }
        }

        internal fun formatClimbTime(pages: Int): String {
            val totalMinutes = (pages * PageMath.MINUTES_PER_PAGE).toInt()
            if (totalMinutes == 0) return "0 Hours Climbed"
            val days = totalMinutes / (24 * 60)
            val hours = (totalMinutes % (24 * 60)) / 60
            val mins = totalMinutes % 60

            return buildString {
                if (days > 0) append("$days Days, ")
                if (hours > 0) append("$hours Hours, ")
                if (mins > 0 && days == 0) append("$mins Mins, ")
                if (isEmpty()) append("0 Hours, ")
                setLength(length - 2)
                append(" Climbed")
            }
        }
    }
}

package com.example.summitread.ui.mountain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.summitread.data.BookRepository
import com.example.summitread.data.DayClock
import com.example.summitread.data.PageMath
import com.example.summitread.data.StreakCalculator
import com.example.summitread.data.UserPreferences
import com.example.summitread.data.currentTimeMillis
import com.example.summitread.data.db.BookEntity
import com.example.summitread.data.db.CharacterEntity
import com.example.summitread.data.db.NoteEntity
import com.example.summitread.data.db.ReadingSessionEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the UI needs to celebrate a book that has just reached 100% (#33). */
data class SummitCompletion(
    val bookId: Long,
    val title: String,
    val completedAtMillis: Long,
    val relicEmoji: String
) {
    /** The congratulation text required by #33. */
    val message: String
        get() = "Congratulations! You have successfully conquered this summit and added the $title Relic to your profile showcase."
}

/**
 * ViewModel of one book's Mountain screen. The book comes from the route argument ([bookId]),
 * never from a global "active book" preference.
 */
class MountainViewModel(
    private val repository: BookRepository,
    private val userPreferences: UserPreferences,
    private val bookId: Long
) : ViewModel() {

    private fun <T> Flow<T>.state(initial: T): StateFlow<T> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initial)

    val activeBook: StateFlow<BookEntity?> = repository.getBookById(bookId)
        .catch { emit(null) }
        .state(null)

    val readingSessions: StateFlow<List<ReadingSessionEntity>> = repository.getSessionsForBook(bookId)
        .catch { emit(emptyList()) }
        .state(emptyList())

    val notes: StateFlow<List<NoteEntity>> = repository.getNotesForBook(bookId)
        .catch { emit(emptyList()) }
        .state(emptyList())

    val characters: StateFlow<List<CharacterEntity>> = repository.getCharactersForBook(bookId)
        .catch { emit(emptyList()) }
        .state(emptyList())

    /** Current streak in calendar days, derived from the logs; reads 0 as soon as a day was missed. */
    val currentStreak: StateFlow<Int> = repository.currentStreak
        .catch { emit(0) }
        .state(0)

    /** Hours since the last logged session (any book), refreshed every minute; 0 if nothing was ever logged. */
    val hoursSinceLastSession: StateFlow<Long> =
        combine(repository.lastSessionLoggedAtMillis, DayClock.minuteTicks()) { lastLogged, now ->
            if (lastLogged == null || lastLogged <= 0L) 0L
            else ((now - lastLogged) / (60L * 60L * 1000L)).coerceAtLeast(0L)
        }
            .catch { emit(0L) }
            .state(0L)

    private val _isFocusMode = MutableStateFlow(false)
    val isFocusMode: StateFlow<Boolean> = _isFocusMode.asStateFlow()

    /** Start-of-day millis of today; follows the clock while the screen is open (flips at midnight). */
    val today: StateFlow<Long> = DayClock.today()
        .map { StreakCalculator.startOfDayMillis(it) }
        .state(StreakCalculator.startOfDayMillis(StreakCalculator.toLocalDate(currentTimeMillis())))

    /** A past day picked on the calendar strip; null means "follow today". */
    private val selectedPastDay = MutableStateFlow<Long?>(null)

    /** Start-of-day millis of the day pages are logged for: today unless a past day was picked. */
    val selectedDate: StateFlow<Long> = combine(selectedPastDay, today) { past, today -> past ?: today }
        .state(StreakCalculator.startOfDayMillis(StreakCalculator.toLocalDate(currentTimeMillis())))

    private val _showSummitAnimation = MutableStateFlow(false)
    val showSummitAnimation: StateFlow<Boolean> = _showSummitAnimation.asStateFlow()

    private val _completedSummit = MutableStateFlow<SummitCompletion?>(null)
    /** Set when logging took the book to 100%; the relic exists exactly once. Cleared by [clearCompletedSummit]. */
    val completedSummit: StateFlow<SummitCompletion?> = _completedSummit.asStateFlow()

    private val _pagesInput = MutableStateFlow("")
    val pagesInput: StateFlow<String> = _pagesInput.asStateFlow()

    private val _isLogging = MutableStateFlow(false)
    /** True while a log is being written; the UI should disable the button (double-submit is also guarded here). */
    val isLogging: StateFlow<Boolean> = _isLogging.asStateFlow()

    private val _triggerParticles = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val triggerParticles: SharedFlow<Unit> = _triggerParticles.asSharedFlow()

    /**
     * Stopwatch time of this book that was stopped but not logged yet (persisted, 0 if none).
     * It is stored as timeSpentMillis on the session created by the next [logPages].
     */
    val pendingTimeMillis: StateFlow<Long> = userPreferences.pendingTimerSession
        .map { if (it?.bookId == bookId) it.elapsedMillis else 0L }
        .catch { emit(0L) }
        .state(0L)

    init {
        // A stopped stopwatch of this book pre-fills the suggested pages (never overwrites what was typed).
        viewModelScope.launch {
            userPreferences.pendingTimerSession
                .catch { }
                .filterNotNull()
                .collect { pending ->
                    if (pending.bookId == bookId && _pagesInput.value.isBlank()) {
                        _pagesInput.value = PageMath.suggestedPages(pending.elapsedMillis).toString()
                    }
                }
        }
    }

    /** Picks the day to log for. Future days are ignored; picking today goes back to following today. */
    fun setSelectedDate(date: Long) {
        val picked = StreakCalculator.startOfDayMillis(StreakCalculator.toLocalDate(date))
        val todayStart = StreakCalculator.startOfDayMillis(StreakCalculator.toLocalDate(currentTimeMillis()))
        if (picked > todayStart) return
        selectedPastDay.value = if (picked == todayStart) null else picked
    }

    fun setPagesInput(value: String) {
        _pagesInput.value = value
    }

    /**
     * Logs the typed pages for [selectedDate] in one transaction (clamped so the book never exceeds its
     * total pages), together with any pending stopwatch time. Runs in [viewModelScope]; repeated calls
     * while a log is in flight are ignored.
     */
    fun logPages() {
        val pages = _pagesInput.value.trim().toIntOrNull() ?: return
        if (pages <= 0) return
        if (!_isLogging.compareAndSet(expect = false, update = true)) return

        viewModelScope.launch {
            try {
                val book = activeBook.value ?: return@launch
                val dateMillis = selectedPastDay.value
                    ?: StreakCalculator.startOfDayMillis(StreakCalculator.toLocalDate(currentTimeMillis()))
                val pending = userPreferences.pendingTimerSession.first()
                val timeMillis = if (pending?.bookId == bookId) pending.elapsedMillis else 0L
                val result = repository.logPages(book.id, dateMillis, pages, timeMillis) ?: return@launch

                _pagesInput.value = ""
                if (timeMillis > 0L) userPreferences.clearPendingTimerSession()
                _triggerParticles.tryEmit(Unit)

                if (result.completedBook) {
                    _completedSummit.value = SummitCompletion(
                        bookId = book.id,
                        title = result.bookTitle,
                        completedAtMillis = result.completedAtMillis ?: currentTimeMillis(),
                        relicEmoji = result.relicEmoji ?: "🏔️"
                    )
                    _showSummitAnimation.value = true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // keep the typed input so the user can retry
            } finally {
                _isLogging.value = false
            }
        }
    }

    /** Drops the stopped-stopwatch time of this book without logging it. */
    fun discardPendingSession() {
        viewModelScope.launch { userPreferences.clearPendingTimerSession() }
    }

    fun toggleFocusMode() {
        _isFocusMode.value = !_isFocusMode.value
    }

    fun dismissSummitAnimation() {
        _showSummitAnimation.value = false
    }

    fun clearCompletedSummit() {
        _completedSummit.value = null
    }

    fun saveSummitLogbook(rating: Float, review: String) {
        viewModelScope.launch {
            repository.updateRelicLogbook(bookId, rating, review)
            _completedSummit.value = null
        }
    }

    /** The note is linked at creation to this book's latest session of today, if one exists. */
    fun addNote(content: String, pageNumber: Int, photoPath: String?) {
        viewModelScope.launch {
            repository.addNote(bookId, pageNumber, content, photoPath)
        }
    }

    /** Deletes the note and its photo file. */
    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch { repository.deleteNote(note) }
    }

    fun addCharacter(name: String, role: String, description: String) {
        viewModelScope.launch {
            repository.insertCharacter(
                CharacterEntity(bookId = bookId, name = name, role = role, description = description)
            )
        }
    }

    fun deleteCharacter(character: CharacterEntity) {
        viewModelScope.launch { repository.deleteCharacter(character) }
    }

    companion object {
        fun factory(
            repository: BookRepository,
            userPreferences: UserPreferences,
            bookId: Long
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { MountainViewModel(repository, userPreferences, bookId) }
        }
    }
}

fun startOfDay(millis: Long): Long =
    StreakCalculator.startOfDayMillis(StreakCalculator.toLocalDate(millis))

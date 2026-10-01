package com.example.summitread.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.summitread.data.BookRepository
import com.example.summitread.data.ThemeMode
import com.example.summitread.data.UserPreferences
import com.example.summitread.data.db.RelicEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: BookRepository,
    private val preferences: UserPreferences
) : ViewModel() {

    private val _selectedJourneyBookId = MutableStateFlow<Long?>(null)
    val selectedJourneyBookId = _selectedJourneyBookId.asStateFlow()

    val journeySessions = _selectedJourneyBookId.flatMapLatest { id ->
        if (id != null) repository.getSessionsForBook(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val journeyNotes = _selectedJourneyBookId.flatMapLatest { id ->
        if (id != null) repository.getNotesForBook(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val relics: StateFlow<List<RelicEntity>> = repository.allRelics
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedBooksCount: StateFlow<Int> = repository.completedBooksCount
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalPagesRead: StateFlow<Int> = repository.totalPagesRead
        .catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalTimeRead: StateFlow<String> = repository.totalPagesRead
        .combine(repository.totalTimeReadMillis) { pages, _ ->
            val totalMinutes = (pages * 1.5).toInt()
            if (totalMinutes == 0) return@combine "0 Hours Climbed"
            val days = totalMinutes / (24 * 60)
            val hours = (totalMinutes % (24 * 60)) / 60
            val mins = totalMinutes % 60

            buildString {
                if (days > 0) append("$days Days, ")
                if (hours > 0) append("$hours Hours, ")
                if (mins > 0 && days == 0) append("$mins Mins, ")
                if (isEmpty()) append("0 Hours, ")
                setLength(length - 2)
                append(" Climbed")
            }
        }
        .catch { emit("0 Hours Climbed") }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0 Hours Climbed")

    val username: StateFlow<String> = preferences.username
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Climber")

    val themeMode: StateFlow<ThemeMode> = preferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.LIGHT)

    val accentColorIndex: StateFlow<Int> = preferences.accentColorIndex
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val notificationsEnabled: StateFlow<Boolean> = preferences.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val annualGoal: StateFlow<Int> = preferences.annualGoal
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
    }
}

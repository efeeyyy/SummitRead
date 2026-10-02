package com.example.summitread.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

enum class ThemeMode { LIGHT, DARK, SYSTEM }

/** A stopped stopwatch session waiting to be stored with the next page log of [bookId]. */
data class PendingTimerSession(val bookId: Long, val elapsedMillis: Long)

/** File name of the preferences store. Android keeps it at `filesDir/datastore/` exactly as before (same file). */
const val USER_PREFERENCES_FILE_NAME = "user_preferences.preferences_pb"

/** Builds a preferences DataStore backed by the file at [producePath] (must end in `.preferences_pb`). */
fun createUserPreferencesDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })

/**
 * Typed access to the persisted user preferences. [dataStore] must be the ONE DataStore instance of the process
 * for the preferences file (the platform containers own it), because two instances on a file would conflict.
 */
class UserPreferences(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR_INDEX = intPreferencesKey("accent_color_index")
        val USERNAME = stringPreferencesKey("username")
        val ACTIVE_BOOK_ID = longPreferencesKey("active_book_id")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val SORT_OPTION = stringPreferencesKey("sort_option")
        val PENDING_TIMER_BOOK_ID = longPreferencesKey("pending_timer_book_id")
        val PENDING_TIMER_MILLIS = longPreferencesKey("pending_timer_millis")
        val IS_LIST_VIEW = booleanPreferencesKey("is_list_view")
        val ANNUAL_GOAL = intPreferencesKey("annual_goal")
    }

    // ── Theme Mode ────────────────────────────────────────

    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        val name = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(name)
        } catch (_: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    // ── Accent Color Index ────────────────────────────────

    val accentColorIndex: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.ACCENT_COLOR_INDEX] ?: 0
    }

    suspend fun setAccentColorIndex(index: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.ACCENT_COLOR_INDEX] = index
        }
    }

    // ── Username ──────────────────────────────────────────

    val username: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.USERNAME] ?: ""
    }

    suspend fun setUsername(name: String) {
        dataStore.edit { prefs ->
            prefs[Keys.USERNAME] = name
        }
    }

    // ── Active Book ID ────────────────────────────────────

    val activeBookId: Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_BOOK_ID] ?: -1L
    }

    suspend fun setActiveBookId(id: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.ACTIVE_BOOK_ID] = id
        }
    }

    // ── Notifications Enabled ─────────────────────────────

    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATIONS_ENABLED] ?: true
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    // ── Sort Option (Home list) ───────────────────────────

    /** Raw name of the persisted Home sort option; null until the user picks one. */
    val sortOptionName: Flow<String?> = dataStore.data.map { prefs -> prefs[Keys.SORT_OPTION] }
        .distinctUntilChanged()

    suspend fun setSortOptionName(name: String) {
        dataStore.edit { prefs -> prefs[Keys.SORT_OPTION] = name }
    }

    // ── Pending timer session ─────────────────────────────

    /**
     * Elapsed reading time of a stopped stopwatch that has not been logged yet. Persisted so it survives
     * the timer service being destroyed (or the screen being closed) before the UI reads it.
     */
    val pendingTimerSession: Flow<PendingTimerSession?> = dataStore.data.map { prefs ->
        val bookId = prefs[Keys.PENDING_TIMER_BOOK_ID]
        val millis = prefs[Keys.PENDING_TIMER_MILLIS]
        if (bookId != null && millis != null && millis > 0L) PendingTimerSession(bookId, millis) else null
    }.distinctUntilChanged()

    /** Adds [elapsedMillis] to the pending time of [bookId] (replaces a pending session of another book). */
    suspend fun addPendingTimerSession(bookId: Long, elapsedMillis: Long) {
        dataStore.edit { prefs ->
            val sameBook = prefs[Keys.PENDING_TIMER_BOOK_ID] == bookId
            val existing = if (sameBook) prefs[Keys.PENDING_TIMER_MILLIS] ?: 0L else 0L
            prefs[Keys.PENDING_TIMER_BOOK_ID] = bookId
            prefs[Keys.PENDING_TIMER_MILLIS] = existing + elapsedMillis
        }
    }

    suspend fun clearPendingTimerSession() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.PENDING_TIMER_BOOK_ID)
            prefs.remove(Keys.PENDING_TIMER_MILLIS)
        }
    }

    // ── View Mode ─────────────────────────────────────────

    val isListView: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.IS_LIST_VIEW] ?: false
    }

    suspend fun setIsListView(isList: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.IS_LIST_VIEW] = isList
        }
    }

    // ── Annual Goal ───────────────────────────────────────

    val annualGoal: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.ANNUAL_GOAL] ?: 20 // Default 20
    }

    suspend fun setAnnualGoal(goal: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.ANNUAL_GOAL] = goal
        }
    }
}

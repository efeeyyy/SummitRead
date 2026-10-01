package com.example.summitread.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

enum class ThemeMode { LIGHT, DARK, SYSTEM }

/** A stopped stopwatch session waiting to be stored with the next page log of [bookId]. */
data class PendingTimerSession(val bookId: Long, val elapsedMillis: Long)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {

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

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val name = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(name)
        } catch (_: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    // ── Accent Color Index ────────────────────────────────

    val accentColorIndex: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACCENT_COLOR_INDEX] ?: 0
    }

    suspend fun setAccentColorIndex(index: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCENT_COLOR_INDEX] = index
        }
    }

    // ── Username ──────────────────────────────────────────

    val username: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.USERNAME] ?: ""
    }

    suspend fun setUsername(name: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USERNAME] = name
        }
    }

    // ── Active Book ID ────────────────────────────────────

    val activeBookId: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_BOOK_ID] ?: -1L
    }

    suspend fun setActiveBookId(id: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACTIVE_BOOK_ID] = id
        }
    }

    // ── Notifications Enabled ─────────────────────────────

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATIONS_ENABLED] ?: true
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    // ── Sort Option (Home list) ───────────────────────────

    /** Raw name of the persisted Home sort option; null until the user picks one. */
    val sortOptionName: Flow<String?> = context.dataStore.data.map { prefs -> prefs[Keys.SORT_OPTION] }
        .distinctUntilChanged()

    suspend fun setSortOptionName(name: String) {
        context.dataStore.edit { prefs -> prefs[Keys.SORT_OPTION] = name }
    }

    // ── Pending timer session ─────────────────────────────

    /**
     * Elapsed reading time of a stopped stopwatch that has not been logged yet. Persisted so it survives
     * the timer service being destroyed (or the screen being closed) before the UI reads it.
     */
    val pendingTimerSession: Flow<PendingTimerSession?> = context.dataStore.data.map { prefs ->
        val bookId = prefs[Keys.PENDING_TIMER_BOOK_ID]
        val millis = prefs[Keys.PENDING_TIMER_MILLIS]
        if (bookId != null && millis != null && millis > 0L) PendingTimerSession(bookId, millis) else null
    }.distinctUntilChanged()

    /** Adds [elapsedMillis] to the pending time of [bookId] (replaces a pending session of another book). */
    suspend fun addPendingTimerSession(bookId: Long, elapsedMillis: Long) {
        context.dataStore.edit { prefs ->
            val sameBook = prefs[Keys.PENDING_TIMER_BOOK_ID] == bookId
            val existing = if (sameBook) prefs[Keys.PENDING_TIMER_MILLIS] ?: 0L else 0L
            prefs[Keys.PENDING_TIMER_BOOK_ID] = bookId
            prefs[Keys.PENDING_TIMER_MILLIS] = existing + elapsedMillis
        }
    }

    suspend fun clearPendingTimerSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.PENDING_TIMER_BOOK_ID)
            prefs.remove(Keys.PENDING_TIMER_MILLIS)
        }
    }

    // ── View Mode ─────────────────────────────────────────

    val isListView: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.IS_LIST_VIEW] ?: false
    }

    suspend fun setIsListView(isList: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_LIST_VIEW] = isList
        }
    }

    // ── Annual Goal ───────────────────────────────────────

    val annualGoal: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.ANNUAL_GOAL] ?: 20 // Default 20
    }

    suspend fun setAnnualGoal(goal: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ANNUAL_GOAL] = goal
        }
    }
}

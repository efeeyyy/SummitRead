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
import kotlinx.coroutines.flow.map

enum class ThemeMode { LIGHT, DARK, SYSTEM }

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR_INDEX = intPreferencesKey("accent_color_index")
        val USERNAME = stringPreferencesKey("username")
        val ACTIVE_BOOK_ID = longPreferencesKey("active_book_id")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val LAST_READ_DATE = longPreferencesKey("last_read_date")
        val CURRENT_STREAK = intPreferencesKey("current_streak")
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

    // ── Streak ──────────────────────────────────────────

    val lastReadDate: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_READ_DATE] ?: 0L
    }

    val currentStreak: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.CURRENT_STREAK] ?: 0
    }

    suspend fun updateStreak(date: Long, streak: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_READ_DATE] = date
            prefs[Keys.CURRENT_STREAK] = streak
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

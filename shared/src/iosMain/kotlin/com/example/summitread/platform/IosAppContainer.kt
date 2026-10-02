package com.example.summitread.platform

import androidx.room.Room
import com.example.summitread.data.USER_PREFERENCES_FILE_NAME
import com.example.summitread.data.UserPreferences
import com.example.summitread.data.createUserPreferencesDataStore
import com.example.summitread.data.db.AppDatabase
import com.example.summitread.data.db.DATABASE_NAME
import com.example.summitread.data.db.buildAppDatabase

/**
 * iOS entry point of the shared data layer (UNVERIFIED on a Mac). Database, preferences and pictures all live in the
 * app's Documents directory. From Swift: `IosPlatform.shared` (see [IosPlatform]).
 */
internal object IosAppContainer {

    val container: AppContainer by lazy {
        val documents = documentDirectoryPath()
        val database: AppDatabase = buildAppDatabase(
            Room.databaseBuilder<AppDatabase>(name = "$documents/$DATABASE_NAME")
        )
        val dataStore = createUserPreferencesDataStore { "$documents/$USER_PREFERENCES_FILE_NAME" }
        AppContainer(
            database = database,
            preferences = UserPreferences(dataStore),
            imageStorage = IosImageStorage()
        )
    }
}

/**
 * Everything the SwiftUI host needs from the shared module, created once (UNVERIFIED on a Mac).
 * Swift: `let platform = IosPlatform.shared`.
 */
object IosPlatform {
    val container: AppContainer get() = IosAppContainer.container

    val readingTimer: ReadingTimer by lazy { IosReadingTimer(container.preferences) }

    val reminderScheduler: ReminderScheduler by lazy { IosReminderScheduler() }

    val haptics: Haptics by lazy { IosHaptics() }

    /** A new sensor wrapper per screen; call start/stop with the screen's lifecycle. */
    fun createTiltSensor(): TiltSensor = IosTiltSensor()
}

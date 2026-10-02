package com.example.summitread.platform

import android.content.Context
import androidx.room.Room
import com.example.summitread.data.UserPreferences
import com.example.summitread.data.USER_PREFERENCES_FILE_NAME
import com.example.summitread.data.createUserPreferencesDataStore
import com.example.summitread.data.db.AppDatabase
import com.example.summitread.data.db.DATABASE_NAME
import com.example.summitread.data.db.buildAppDatabase
import java.io.File

/**
 * Android entry point of the shared data layer. Holds the process-wide singletons that the Activity, the foreground
 * service and the WorkManager worker all use (a second DataStore instance on the same file would throw).
 *
 * File locations are exactly the ones the app used before the KMP migration, so existing installs keep their data:
 * - database: `context.getDatabasePath("summitread_database")` (`databases/summitread_database`)
 * - preferences: `filesDir/datastore/user_preferences.preferences_pb`
 * - pictures: `filesDir/images`
 */
object AndroidAppContainer {

    @Volatile
    private var instance: AppContainer? = null

    fun get(context: Context): AppContainer =
        instance ?: synchronized(this) {
            instance ?: create(context.applicationContext).also { instance = it }
        }

    private fun create(appContext: Context): AppContainer {
        val database: AppDatabase = buildAppDatabase(
            Room.databaseBuilder<AppDatabase>(
                context = appContext,
                name = appContext.getDatabasePath(DATABASE_NAME).absolutePath
            )
        )
        val dataStore = createUserPreferencesDataStore {
            File(appContext.filesDir, "datastore/$USER_PREFERENCES_FILE_NAME").absolutePath
        }
        return AppContainer(
            database = database,
            preferences = UserPreferences(dataStore),
            imageStorage = AndroidImageStorage(appContext)
        )
    }
}

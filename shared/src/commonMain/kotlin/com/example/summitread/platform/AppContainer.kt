package com.example.summitread.platform

import com.example.summitread.data.BookRepository
import com.example.summitread.data.ImageStorage
import com.example.summitread.data.UserPreferences
import com.example.summitread.data.db.AppDatabase

/**
 * The process-wide data layer: ONE database, ONE preferences DataStore, one repository.
 * Built per platform (`AndroidAppContainer`, `IosAppContainer`) because the file locations differ.
 */
class AppContainer(
    val database: AppDatabase,
    val preferences: UserPreferences,
    val imageStorage: ImageStorage
) {
    val repository: BookRepository = BookRepository(database.bookDao(), imageStorage)
}

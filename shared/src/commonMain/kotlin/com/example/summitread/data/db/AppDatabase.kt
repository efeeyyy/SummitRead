package com.example.summitread.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.example.summitread.platform.ioDispatcher

/** File name of the SQLite database. Must never change: existing Android installs keep their data here. */
const val DATABASE_NAME = "summitread_database"

@Database(
    entities = [
        BookEntity::class,
        ReadingSessionEntity::class,
        NoteEntity::class,
        CharacterEntity::class,
        RelicEntity::class
    ],
    version = 5,
    exportSchema = true
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
}

/** Room generates the `actual` object for every target (KSP). */
@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

/**
 * v4 -> v5 (data-preserving):
 *  - books: coverColorArgb, isArchived, completedAtMillis (backfilled from relics for finished books)
 *  - reading_sessions: createdAtMillis (backfilled with the session date) + (bookId, date) index
 *  - notes / characters: bookId index
 *  - relics: de-duplicated, then unique(bookId) so a book has exactly one relic
 */
val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE books ADD COLUMN coverColorArgb INTEGER")
        connection.execSQL("ALTER TABLE books ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("ALTER TABLE books ADD COLUMN completedAtMillis INTEGER")
        connection.execSQL(
            "UPDATE books SET completedAtMillis = COALESCE(" +
                "(SELECT MIN(dateEarned) FROM relics WHERE relics.bookId = books.id), lastActivityMillis) " +
                "WHERE isCompleted = 1"
        )

        connection.execSQL("ALTER TABLE reading_sessions ADD COLUMN createdAtMillis INTEGER NOT NULL DEFAULT 0")
        connection.execSQL("UPDATE reading_sessions SET createdAtMillis = date")

        connection.execSQL("DELETE FROM relics WHERE id NOT IN (SELECT MIN(id) FROM relics GROUP BY bookId)")

        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_reading_sessions_bookId_date` " +
                "ON `reading_sessions` (`bookId`, `date`)"
        )
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_bookId` ON `notes` (`bookId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_characters_bookId` ON `characters` (`bookId`)")
        connection.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_relics_bookId` ON `relics` (`bookId`)")
    }
}

/**
 * Finishes a platform-specific builder (Android: context path, iOS: Documents path) with the shared
 * configuration: bundled SQLite driver (same engine on every platform), IO dispatcher and the migration.
 */
fun buildAppDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(ioDispatcher)
        .addMigrations(MIGRATION_4_5)
        // Only versions 1-3 (never shipped) fall back to a wipe; 4 -> 5 keeps all data.
        .fallbackToDestructiveMigration(dropAllTables = false)
        .build()

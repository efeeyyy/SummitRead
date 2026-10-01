package com.example.summitread.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
abstract class AppDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao

    companion object {
        /**
         * v4 -> v5 (data-preserving):
         *  - books: coverColorArgb, isArchived, completedAtMillis (backfilled from relics for finished books)
         *  - reading_sessions: createdAtMillis (backfilled with the session date) + (bookId, date) index
         *  - notes / characters: bookId index
         *  - relics: de-duplicated, then unique(bookId) so a book has exactly one relic
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN coverColorArgb INTEGER")
                db.execSQL("ALTER TABLE books ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE books ADD COLUMN completedAtMillis INTEGER")
                db.execSQL(
                    "UPDATE books SET completedAtMillis = COALESCE(" +
                        "(SELECT MIN(dateEarned) FROM relics WHERE relics.bookId = books.id), lastActivityMillis) " +
                        "WHERE isCompleted = 1"
                )

                db.execSQL("ALTER TABLE reading_sessions ADD COLUMN createdAtMillis INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE reading_sessions SET createdAtMillis = date")

                db.execSQL("DELETE FROM relics WHERE id NOT IN (SELECT MIN(id) FROM relics GROUP BY bookId)")

                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_reading_sessions_bookId_date` " +
                        "ON `reading_sessions` (`bookId`, `date`)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_bookId` ON `notes` (`bookId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_characters_bookId` ON `characters` (`bookId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_relics_bookId` ON `relics` (`bookId`)")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "summitread_database"
                )
                    .addMigrations(MIGRATION_4_5)
                    // Only versions 1-3 (never shipped) fall back to a wipe; 4 -> 5 keeps all data.
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

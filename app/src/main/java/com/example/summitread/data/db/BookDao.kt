package com.example.summitread.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.summitread.data.PageMath
import com.example.summitread.data.RelicEmojis
import kotlinx.coroutines.flow.Flow

/** Result of a successful [BookDao.logPagesTransaction]. */
data class LogPagesResult(
    val sessionId: Long,
    /** Pages actually stored (requested pages clamped to what remained in the book). */
    val pagesLogged: Int,
    val bookTitle: String,
    /** True only for the session that took the book to 100%. */
    val completedBook: Boolean,
    val completedAtMillis: Long?,
    val relicEmoji: String?
)

@Dao
abstract class BookDao {

    // ── Books ──────────────────────────────────────────────

    /** Shelf/list query: archived books are hidden. */
    @Query("SELECT * FROM books WHERE isArchived = 0 ORDER BY dateAdded DESC")
    abstract fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isArchived = 1 ORDER BY lastActivityMillis DESC")
    abstract fun getArchivedBooks(): Flow<List<BookEntity>>

    /** Returns the book even when archived (Mountain and Journey must still open it). */
    @Query("SELECT * FROM books WHERE id = :id")
    abstract fun getBookById(id: Long): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE id = :id")
    abstract suspend fun getBookOnce(id: Long): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertBook(book: BookEntity): Long

    @Update
    abstract suspend fun updateBook(book: BookEntity)

    @Query("UPDATE books SET isArchived = :archived WHERE id = :id")
    abstract suspend fun setArchived(id: Long, archived: Boolean)

    @Query("UPDATE books SET displayOrder = :order WHERE id = :id")
    abstract suspend fun setDisplayOrder(id: Long, order: Int)

    /** Writes the manual order of all given ids in one transaction (one Flow emission for observers). */
    @Transaction
    open suspend fun setDisplayOrders(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> setDisplayOrder(id, index) }
    }

    @Query("DELETE FROM books WHERE id = :bookId")
    abstract suspend fun deleteBookRow(bookId: Long)

    @Query("DELETE FROM reading_sessions WHERE bookId = :bookId")
    abstract suspend fun deleteSessionsForBook(bookId: Long)

    @Query("DELETE FROM notes WHERE bookId = :bookId")
    abstract suspend fun deleteNotesForBook(bookId: Long)

    @Query("DELETE FROM characters WHERE bookId = :bookId")
    abstract suspend fun deleteCharactersForBook(bookId: Long)

    @Query("DELETE FROM relics WHERE bookId = :bookId")
    abstract suspend fun deleteRelicsForBook(bookId: Long)

    @Query("SELECT photoPath FROM notes WHERE bookId = :bookId AND photoPath IS NOT NULL")
    abstract suspend fun getNotePhotoPaths(bookId: Long): List<String>

    /**
     * Hard delete of a book and everything attached to it (no foreign keys, so done by hand).
     * Archive is the user-facing action; this is kept for completeness.
     * @return image file paths (cover + note photos) the caller should delete from disk.
     */
    @Transaction
    open suspend fun deleteBookCascade(bookId: Long): List<String> {
        val book = getBookOnce(bookId) ?: return emptyList()
        val files = listOfNotNull(book.coverImagePath) + getNotePhotoPaths(bookId)
        deleteNotesForBook(bookId)
        deleteSessionsForBook(bookId)
        deleteCharactersForBook(bookId)
        deleteRelicsForBook(bookId)
        deleteBookRow(bookId)
        return files
    }

    // ── Reading Sessions ──────────────────────────────────

    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY date DESC, id DESC")
    abstract fun getSessionsForBook(bookId: Long): Flow<List<ReadingSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertSession(session: ReadingSessionEntity): Long

    @Query("SELECT COALESCE(SUM(pagesRead), 0) FROM reading_sessions WHERE bookId = :bookId")
    abstract suspend fun getSessionPagesSum(bookId: Long): Int

    @Query(
        "SELECT id FROM reading_sessions WHERE bookId = :bookId AND date >= :dayStart AND date < :dayEnd " +
            "ORDER BY createdAtMillis DESC, id DESC LIMIT 1"
    )
    abstract suspend fun findLatestSessionIdForDay(bookId: Long, dayStart: Long, dayEnd: Long): Long?

    /** Distinct logged days (start-of-day millis) across all books: the streak is derived from these. */
    @Query("SELECT DISTINCT date FROM reading_sessions")
    abstract fun getLoggedDays(): Flow<List<Long>>

    @Query("SELECT DISTINCT date FROM reading_sessions")
    abstract suspend fun getLoggedDaysOnce(): List<Long>

    @Query("SELECT MAX(createdAtMillis) FROM reading_sessions")
    abstract fun getLastSessionLoggedAt(): Flow<Long?>

    @Query("SELECT COALESCE(SUM(timeSpentMillis), 0) FROM reading_sessions WHERE bookId = :bookId")
    abstract fun getTotalTimeMillisForBook(bookId: Long): Flow<Long>

    /**
     * Atomically logs pages: clamps to the remaining pages, inserts the session, links the same-day
     * loose notes, updates the book (progress, completion, lastActivity) and creates the relic exactly once.
     *
     * @return null when nothing was logged (book missing, already complete, or pages <= 0).
     */
    @Transaction
    open suspend fun logPagesTransaction(
        bookId: Long,
        date: Long,
        dayEnd: Long,
        pagesRequested: Int,
        timeSpentMillis: Long,
        nowMillis: Long
    ): LogPagesResult? {
        val book = getBookOnce(bookId) ?: return null
        val readBefore = getSessionPagesSum(bookId)
        val pages = PageMath.clampToRemaining(pagesRequested, book.totalPages, readBefore)
        if (pages <= 0) return null

        val sessionId = insertSession(
            ReadingSessionEntity(
                bookId = bookId,
                date = date,
                pagesRead = pages,
                timeSpentMillis = timeSpentMillis.coerceAtLeast(0L),
                createdAtMillis = nowMillis
            )
        )
        linkLooseNotesToSession(bookId, sessionId, date, dayEnd)

        val readAfter = readBefore + pages
        val nowCompleted = readAfter >= book.totalPages
        val justCompleted = nowCompleted && !book.isCompleted
        val completedAt = if (nowCompleted) (book.completedAtMillis ?: nowMillis) else null
        updateBook(
            book.copy(
                currentPages = readAfter.coerceAtMost(book.totalPages),
                isCompleted = nowCompleted,
                lastActivityMillis = nowMillis,
                completedAtMillis = completedAt
            )
        )

        var relicEmoji: String? = null
        if (justCompleted) {
            insertRelicIgnore(
                RelicEntity(
                    bookId = bookId,
                    title = book.title,
                    emoji = RelicEmojis.forBook(bookId),
                    dateEarned = nowMillis
                )
            )
            relicEmoji = getRelicByBookId(bookId)?.emoji
        }
        return LogPagesResult(
            sessionId = sessionId,
            pagesLogged = pages,
            bookTitle = book.title,
            completedBook = justCompleted,
            completedAtMillis = if (justCompleted) completedAt else null,
            relicEmoji = relicEmoji
        )
    }

    // ── Notes ─────────────────────────────────────────────

    @Query("SELECT * FROM notes WHERE bookId = :bookId ORDER BY dateCreated DESC")
    abstract fun getNotesForBook(bookId: Long): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertNote(note: NoteEntity): Long

    /** Creates a note already linked to the latest session of the given day for that book, if one exists. */
    @Transaction
    open suspend fun insertNoteLinked(note: NoteEntity, dayStart: Long, dayEnd: Long): Long {
        val sessionId = note.sessionId ?: findLatestSessionIdForDay(note.bookId, dayStart, dayEnd)
        return insertNote(note.copy(sessionId = sessionId))
    }

    @Delete
    abstract suspend fun deleteNote(note: NoteEntity)

    /**
     * Notes written earlier the same day, before any session existed, join the first session of that day.
     * Notes of other days are never touched.
     */
    @Query(
        "UPDATE notes SET sessionId = :sessionId WHERE bookId = :bookId AND sessionId IS NULL " +
            "AND dateCreated >= :dayStart AND dateCreated < :dayEnd"
    )
    abstract suspend fun linkLooseNotesToSession(bookId: Long, sessionId: Long, dayStart: Long, dayEnd: Long)

    // ── Characters ────────────────────────────────────────

    @Query("SELECT * FROM characters WHERE bookId = :bookId ORDER BY name ASC")
    abstract fun getCharactersForBook(bookId: Long): Flow<List<CharacterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertCharacter(character: CharacterEntity)

    @Delete
    abstract suspend fun deleteCharacter(character: CharacterEntity)

    // ── Relics ────────────────────────────────────────────

    @Query("SELECT * FROM relics ORDER BY dateEarned DESC")
    abstract fun getAllRelics(): Flow<List<RelicEntity>>

    /** IGNORE plus unique(bookId) means a book can never get a second relic. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertRelicIgnore(relic: RelicEntity): Long

    @Query("SELECT * FROM relics WHERE bookId = :bookId LIMIT 1")
    abstract suspend fun getRelicByBookId(bookId: Long): RelicEntity?

    @Query("UPDATE relics SET rating = :rating, review = :review WHERE bookId = :bookId")
    abstract suspend fun updateRelicLogbook(bookId: Long, rating: Float, review: String)

    // ── Aggregate Queries ─────────────────────────────────

    @Query("SELECT COUNT(*) FROM books WHERE isCompleted = 1")
    abstract fun getCompletedBooksCount(): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM books WHERE isCompleted = 1 " +
            "AND completedAtMillis >= :fromMillis AND completedAtMillis < :toMillis"
    )
    abstract fun getCompletedBooksBetween(fromMillis: Long, toMillis: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(pagesRead), 0) FROM reading_sessions")
    abstract fun getTotalPagesRead(): Flow<Int>

    @Query("SELECT COALESCE(SUM(timeSpentMillis), 0) FROM reading_sessions")
    abstract fun getTotalTimeReadMillis(): Flow<Long>
}

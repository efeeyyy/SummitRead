package com.example.summitread.data

import com.example.summitread.data.db.BookDao
import com.example.summitread.data.db.BookEntity
import com.example.summitread.data.db.CharacterEntity
import com.example.summitread.data.db.NoteEntity
import com.example.summitread.data.db.ReadingSessionEntity
import com.example.summitread.data.db.RelicEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class BookRepository(private val dao: BookDao) {

    // ── Books ──────────────────────────────────────────────

    val allBooks: Flow<List<BookEntity>> = dao.getAllBooks()

    fun getBookById(id: Long): Flow<BookEntity?> = dao.getBookById(id)

    suspend fun addBook(
        title: String,
        author: String,
        totalPages: Int,
        coverImagePath: String? = null,
        accentColorIndex: Int = 0
    ): Long {
        val book = BookEntity(
            title = title,
            author = author,
            totalPages = totalPages,
            coverImagePath = coverImagePath,
            accentColorIndex = accentColorIndex
        )
        return dao.insertBook(book)
    }

    suspend fun updateBook(book: BookEntity) {
        dao.updateBook(book)
    }

    suspend fun deleteBook(book: BookEntity) {
        dao.deleteBook(book)
    }

    // ── Reading Sessions ──────────────────────────────────

    fun getSessionsForBook(bookId: Long): Flow<List<ReadingSessionEntity>> =
        dao.getSessionsForBook(bookId)

    fun getSessionsByBookAndDate(bookId: Long, date: Long): Flow<List<ReadingSessionEntity>> =
        dao.getSessionsByBookAndDate(bookId, date)

    fun getAllSessionsForBook(bookId: Long): Flow<List<ReadingSessionEntity>> =
        dao.getAllSessionsForBook(bookId)

    /**
     * Logs pages read for a book session.
     * Updates the book's currentPages to the sum of all sessions.
     * If the book reaches its totalPages, marks it as completed and inserts a Relic.
     *
     * @return true if the book was just completed by this session.
     */
    suspend fun logPages(bookId: Long, date: Long, pagesRead: Int, timeSpentMillis: Long = 0L): Boolean {
        // Insert the reading session
        val sessionId = dao.insertSession(
            ReadingSessionEntity(
                bookId = bookId,
                date = date,
                pagesRead = pagesRead,
                timeSpentMillis = timeSpentMillis
            )
        )

        // Link any loose notes added during this session
        dao.linkLooseNotesToSession(bookId, sessionId)

        // Calculate total pages read across all sessions for this book
        val allSessions = dao.getAllSessionsForBook(bookId).first()
        val totalRead = allSessions.sumOf { it.pagesRead }

        // Get the current book
        val book = dao.getBookById(bookId).first() ?: return false
        val wasAlreadyCompleted = book.isCompleted
        val isNowCompleted = totalRead >= book.totalPages

        // Update the book
        dao.updateBook(
            book.copy(
                currentPages = totalRead.coerceAtMost(book.totalPages),
                isCompleted = isNowCompleted
            )
        )

        // If just completed, insert a relic
        if (isNowCompleted && !wasAlreadyCompleted) {
            dao.insertRelic(
                RelicEntity(
                    bookId = bookId,
                    title = book.title
                )
            )
            return true
        }

        return false
    }

    // ── Notes ─────────────────────────────────────────────

    fun getNotesForBook(bookId: Long): Flow<List<NoteEntity>> =
        dao.getNotesForBook(bookId)

    suspend fun insertNote(note: NoteEntity) {
        dao.insertNote(note)
    }

    suspend fun deleteNote(note: NoteEntity) {
        dao.deleteNote(note)
    }

    // ── Characters ────────────────────────────────────────

    fun getCharactersForBook(bookId: Long): Flow<List<CharacterEntity>> =
        dao.getCharactersForBook(bookId)

    suspend fun insertCharacter(character: CharacterEntity) {
        dao.insertCharacter(character)
    }

    suspend fun deleteCharacter(character: CharacterEntity) {
        dao.deleteCharacter(character)
    }

    // ── Relics ────────────────────────────────────────────

    val allRelics: Flow<List<RelicEntity>> = dao.getAllRelics()

    suspend fun insertRelic(relic: RelicEntity) {
        dao.insertRelic(relic)
    }

    suspend fun getRelicByBookId(bookId: Long): RelicEntity? {
        return dao.getRelicByBookId(bookId)
    }

    suspend fun updateRelic(relic: RelicEntity) {
        dao.updateRelic(relic)
    }

    // ── Aggregate ─────────────────────────────────────────

    val completedBooksCount: Flow<Int> = dao.getCompletedBooksCount()

    val totalPagesRead: Flow<Int> = dao.getTotalPagesRead()

    val totalTimeReadMillis: Flow<Long> = dao.getTotalTimeReadMillis()
}

package com.example.summitread.data

import com.example.summitread.data.db.BookDao
import com.example.summitread.data.db.BookEntity
import com.example.summitread.data.db.CharacterEntity
import com.example.summitread.data.db.LogPagesResult
import com.example.summitread.data.db.NoteEntity
import com.example.summitread.data.db.ReadingSessionEntity
import com.example.summitread.data.db.RelicEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock

class BookRepository(
    private val dao: BookDao,
    private val imageStorage: ImageStorage
) {

    // ── Books ──────────────────────────────────────────────

    /** Shelf/list books; archived books are excluded. */
    val allBooks: Flow<List<BookEntity>> = dao.getAllBooks()

    val archivedBooks: Flow<List<BookEntity>> = dao.getArchivedBooks()

    /** Emits the book even when it is archived. */
    fun getBookById(id: Long): Flow<BookEntity?> = dao.getBookById(id)

    suspend fun addBook(
        title: String,
        author: String,
        totalPages: Int,
        coverImagePath: String? = null,
        accentColorIndex: Int = 0,
        coverColorArgb: Int? = null
    ): Long {
        val book = BookEntity(
            title = title,
            author = author,
            totalPages = totalPages,
            coverImagePath = coverImagePath,
            accentColorIndex = accentColorIndex,
            coverColorArgb = coverColorArgb
        )
        return dao.insertBook(book)
    }

    suspend fun updateBook(book: BookEntity) {
        dao.updateBook(book)
    }

    /** Archive hides the book from the shelf/list; sessions, notes and relics are kept. */
    suspend fun archiveBook(id: Long, archived: Boolean) {
        dao.setArchived(id, archived)
    }

    /** Persists the manual order (index in [orderedIds]) in a single transaction. */
    suspend fun reorderBooks(orderedIds: List<Long>) {
        dao.setDisplayOrders(orderedIds)
    }

    /** Hard delete: removes the book with its sessions, notes, characters, relic and image files. */
    suspend fun deleteBook(book: BookEntity) {
        val files = dao.deleteBookCascade(book.id)
        imageStorage.deleteAll(files)
    }

    // ── Reading Sessions ──────────────────────────────────

    fun getSessionsForBook(bookId: Long): Flow<List<ReadingSessionEntity>> =
        dao.getSessionsForBook(bookId)

    /**
     * Logs pages read for a book in ONE transaction: clamps to the remaining pages, inserts the session
     * (with its stopwatch time), links same-day notes, updates progress / completion / lastActivity and
     * creates the relic exactly once when the book reaches 100%.
     *
     * @param date any instant inside the day to log for (normalised to the start of that day).
     * @return null if nothing was logged (future date, unknown book, book already complete, pages <= 0).
     */
    suspend fun logPages(
        bookId: Long,
        date: Long,
        pagesRead: Int,
        timeSpentMillis: Long = 0L
    ): LogPagesResult? {
        val zone = TimeZone.currentSystemDefault()
        val day = StreakCalculator.toLocalDate(date, zone)
        if (day > Clock.System.todayIn(zone)) return null // no logging for future days
        return dao.logPagesTransaction(
            bookId = bookId,
            date = StreakCalculator.startOfDayMillis(day, zone),
            dayEnd = StreakCalculator.startOfDayMillis(day.plus(1, DateTimeUnit.DAY), zone),
            pagesRequested = pagesRead,
            timeSpentMillis = timeSpentMillis,
            nowMillis = currentTimeMillis()
        )
    }

    // ── Notes ─────────────────────────────────────────────

    fun getNotesForBook(bookId: Long): Flow<List<NoteEntity>> =
        dao.getNotesForBook(bookId)

    /** Creates a note; it is linked at creation to the book's latest session of today (if any). */
    suspend fun addNote(bookId: Long, pageNumber: Int, content: String, photoPath: String?): Long {
        val zone = TimeZone.currentSystemDefault()
        val today = Clock.System.todayIn(zone)
        return dao.insertNoteLinked(
            NoteEntity(bookId = bookId, pageNumber = pageNumber, content = content, photoPath = photoPath),
            dayStart = StreakCalculator.startOfDayMillis(today, zone),
            dayEnd = StreakCalculator.startOfDayMillis(today.plus(1, DateTimeUnit.DAY), zone)
        )
    }

    /** Deletes the note and its photo file. */
    suspend fun deleteNote(note: NoteEntity) {
        dao.deleteNote(note)
        imageStorage.deleteAll(listOf(note.photoPath))
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

    suspend fun getRelicByBookId(bookId: Long): RelicEntity? = dao.getRelicByBookId(bookId)

    suspend fun updateRelicLogbook(bookId: Long, rating: Float, review: String) {
        dao.updateRelicLogbook(bookId, rating, review)
    }

    // ── Streak (derived from logged days, never stored) ──────

    /** Distinct calendar days with at least one logged session (any book). */
    val loggedDays: Flow<Set<LocalDate>> = dao.getLoggedDays()
        .map { days -> days.mapTo(HashSet()) { StreakCalculator.toLocalDate(it) } }

    suspend fun getLoggedDaysOnce(): Set<LocalDate> =
        dao.getLoggedDaysOnce().mapTo(HashSet()) { StreakCalculator.toLocalDate(it) }

    /**
     * Current streak in calendar days. Follows the clock: it flips to 0 by itself once a whole day
     * has been missed, even if the screen stays open across midnight.
     */
    val currentStreak: Flow<Int> =
        combine(loggedDays, DayClock.today()) { days, today ->
            StreakCalculator.currentStreak(days, today)
        }.distinctUntilChanged()

    /** Wall-clock moment of the last logged session (any book), null if nothing was ever logged. */
    val lastSessionLoggedAtMillis: Flow<Long?> = dao.getLastSessionLoggedAt()

    // ── Aggregate ─────────────────────────────────────────

    /** Completed books of all time (archived books included): drives the avatar tier. */
    val completedBooksCount: Flow<Int> = dao.getCompletedBooksCount()

    /** Books completed in the current calendar year (follows the clock across New Year). */
    @OptIn(ExperimentalCoroutinesApi::class)
    val completedBooksThisYear: Flow<Int> = DayClock.today()
        .map { it.year }
        .distinctUntilChanged()
        .flatMapLatest { year ->
            val zone = TimeZone.currentSystemDefault()
            dao.getCompletedBooksBetween(
                StreakCalculator.startOfDayMillis(LocalDate(year, 1, 1), zone),
                StreakCalculator.startOfDayMillis(LocalDate(year + 1, 1, 1), zone)
            )
        }

    /** Sum of the real stopwatch time of all sessions (not the 1.5 min/page estimate). */
    val totalTimeReadMillis: Flow<Long> = dao.getTotalTimeReadMillis()

    /** Real timed reading (sum of timeSpentMillis) of one book, for the Journey sheet. */
    fun getTotalTimeMillisForBook(bookId: Long): Flow<Long> = dao.getTotalTimeMillisForBook(bookId)
}

package com.example.summitread.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    // ── Books ──────────────────────────────────────────────

    @Query("SELECT * FROM books ORDER BY dateAdded DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun getBookById(id: Long): Flow<BookEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long

    @Update
    suspend fun updateBook(book: BookEntity)

    @Delete
    suspend fun deleteBook(book: BookEntity)

    // ── Reading Sessions ──────────────────────────────────

    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY date DESC")
    fun getSessionsForBook(bookId: Long): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId AND date = :date")
    fun getSessionsByBookAndDate(bookId: Long, date: Long): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId")
    fun getAllSessionsForBook(bookId: Long): Flow<List<ReadingSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ReadingSessionEntity): Long

    // ── Notes ─────────────────────────────────────────────

    @Query("SELECT * FROM notes WHERE bookId = :bookId ORDER BY dateCreated DESC")
    fun getNotesForBook(bookId: Long): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("UPDATE notes SET sessionId = :sessionId WHERE bookId = :bookId AND sessionId IS NULL")
    suspend fun linkLooseNotesToSession(bookId: Long, sessionId: Long)

    // ── Characters ────────────────────────────────────────

    @Query("SELECT * FROM characters WHERE bookId = :bookId ORDER BY name ASC")
    fun getCharactersForBook(bookId: Long): Flow<List<CharacterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: CharacterEntity)

    @Delete
    suspend fun deleteCharacter(character: CharacterEntity)

    // ── Relics ────────────────────────────────────────────

    @Query("SELECT * FROM relics ORDER BY dateEarned DESC")
    fun getAllRelics(): Flow<List<RelicEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelic(relic: RelicEntity)

    @Query("SELECT * FROM relics WHERE bookId = :bookId LIMIT 1")
    suspend fun getRelicByBookId(bookId: Long): RelicEntity?

    @Update
    suspend fun updateRelic(relic: RelicEntity)

    // ── Aggregate Queries ─────────────────────────────────

    @Query("SELECT COUNT(*) FROM books WHERE isCompleted = 1")
    fun getCompletedBooksCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(pagesRead), 0) FROM reading_sessions")
    fun getTotalPagesRead(): Flow<Int>

    @Query("SELECT COALESCE(SUM(timeSpentMillis), 0) FROM reading_sessions")
    fun getTotalTimeReadMillis(): Flow<Long>
}

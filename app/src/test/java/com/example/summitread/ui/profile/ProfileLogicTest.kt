package com.example.summitread.ui.profile

import com.example.summitread.data.db.BookEntity
import com.example.summitread.data.db.NoteEntity
import com.example.summitread.data.db.ReadingSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileLogicTest {

    @Test
    fun tierBoundaries() {
        assertEquals(AvatarTier.NOVICE, AvatarTier.forCompletedBooks(0))
        assertEquals(AvatarTier.NOVICE, AvatarTier.forCompletedBooks(1))
        assertEquals(AvatarTier.EXPEDITION_LEADER, AvatarTier.forCompletedBooks(2))
        assertEquals(AvatarTier.EXPEDITION_LEADER, AvatarTier.forCompletedBooks(5))
        assertEquals(AvatarTier.ELITE_ALPINIST, AvatarTier.forCompletedBooks(6))
        assertEquals(AvatarTier.ELITE_ALPINIST, AvatarTier.forCompletedBooks(100))
        assertEquals("Elite Alpinist", AvatarTier.ELITE_ALPINIST.displayName)
        assertEquals(AvatarTier.EXPEDITION_LEADER, AvatarTier.NOVICE.next)
        assertEquals(null, AvatarTier.ELITE_ALPINIST.next)
    }

    @Test
    fun climbTimeFormat() {
        assertEquals("0 Hours Climbed", ProfileLogic.formatClimbTime(0))
        assertEquals("1 Min Climbed", ProfileLogic.formatClimbTime(1)) // 1.5 -> 1 min
        assertEquals("1 Hour Climbed", ProfileLogic.formatClimbTime(40)) // 60 min
        assertEquals("2 Hours, 30 Min Climbed", ProfileLogic.formatClimbTime(100)) // 150 min
        // 960 pages = 1440 min = exactly one day
        assertEquals("1 Day Climbed", ProfileLogic.formatClimbTime(960))
        // 1000 pages = 1500 min = 1 day 1 hour
        assertEquals("1 Day, 1 Hour Climbed", ProfileLogic.formatClimbTime(1000))
        assertEquals("2 Days, 3 Hours Climbed", ProfileLogic.formatClimbTime(2 * 960 + 120))
    }

    @Test
    fun timedDuration() {
        assertEquals("0m", ProfileLogic.formatTimedDuration(0))
        assertEquals("0m", ProfileLogic.formatTimedDuration(59_999))
        assertEquals("45m", ProfileLogic.formatTimedDuration(45 * 60_000L))
        assertEquals("3h 20m", ProfileLogic.formatTimedDuration((3 * 60 + 20) * 60_000L))
        assertEquals("0m", ProfileLogic.formatTimedDuration(-5))
    }

    @Test
    fun goalProgressClamps() {
        assertEquals(0f, ProfileLogic.goalProgress(0, 20), 0f)
        assertEquals(0.5f, ProfileLogic.goalProgress(10, 20), 0f)
        assertEquals(1f, ProfileLogic.goalProgress(30, 20), 0f)
        assertEquals(1f, ProfileLogic.goalProgress(1, 0), 0f) // goal 0 treated as 1
    }

    @Test
    fun tiltThreshold() {
        assertFalse(ProfileLogic.exceedsThreshold(0f, 0f, 0.4f, -0.4f))
        assertTrue(ProfileLogic.exceedsThreshold(0f, 0f, 0.5f, 0f))
        assertTrue(ProfileLogic.exceedsThreshold(10f, 10f, 10f, 9.4f))
    }

    @Test
    fun notesStayUnderTheirOwnSession() {
        val s1 = ReadingSessionEntity(id = 1, bookId = 1, date = 100, pagesRead = 10, createdAtMillis = 5)
        val s2 = ReadingSessionEntity(id = 2, bookId = 1, date = 50, pagesRead = 20, createdAtMillis = 1)
        val n1 = NoteEntity(id = 1, bookId = 1, pageNumber = 3, content = "a", sessionId = 1, dateCreated = 2)
        val n2 = NoteEntity(id = 2, bookId = 1, pageNumber = 4, content = "b", sessionId = 2, dateCreated = 3)
        val n3 = NoteEntity(id = 3, bookId = 1, pageNumber = 5, content = "c", sessionId = null, dateCreated = 4)
        val n4 = NoteEntity(id = 4, bookId = 1, pageNumber = 6, content = "d", sessionId = 99, dateCreated = 1)

        assertEquals(listOf(2L, 1L), ProfileLogic.sortedSessions(listOf(s1, s2)).map { it.id })
        val (bySession, other) = ProfileLogic.groupNotes(listOf(s1, s2), listOf(n1, n2, n3, n4))
        assertEquals(listOf(n1), bySession[1L])
        assertEquals(listOf(n2), bySession[2L])
        assertEquals(listOf(n4, n3), other) // unknown/missing session -> "Other notes", by creation time
    }

    @Test
    fun pagesReadSumsBookProgressClamped() {
        fun book(current: Int, total: Int) =
            BookEntity(title = "t", author = "a", totalPages = total, currentPages = current)
        // 162 + 1234 + 10 = 1406 (the emulator QA case).
        assertEquals(1406, ProfileLogic.pagesRead(listOf(book(162, 300), book(1234, 1234), book(10, 200))))
        // Progress beyond totalPages and negatives are clamped.
        assertEquals(100, ProfileLogic.pagesRead(listOf(book(150, 100), book(-5, 50))))
        assertEquals(0, ProfileLogic.pagesRead(emptyList()))
    }

    @Test
    fun compactClimbTime() {
        assertEquals("0m", ProfileLogic.formatClimbTimeCompact(0))
        assertEquals("1h 0m", ProfileLogic.formatClimbTimeCompact(40))
        assertEquals("2h 30m", ProfileLogic.formatClimbTimeCompact(100))
        assertEquals("1d 1h", ProfileLogic.formatClimbTimeCompact(1000)) // 1500 min
    }
}

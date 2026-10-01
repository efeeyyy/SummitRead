package com.example.summitread.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reading_sessions",
    indices = [Index(value = ["bookId", "date"])]
)
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    /** Start of the calendar day (system zone) the pages were logged for. */
    val date: Long,
    val pagesRead: Int,
    val timeSpentMillis: Long = 0L,
    /** Real wall-clock moment the session was logged (drives "hours since last session"). */
    @ColumnInfo(defaultValue = "0") val createdAtMillis: Long = System.currentTimeMillis()
)

package com.example.summitread.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_sessions")
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val date: Long,
    val pagesRead: Int,
    val timeSpentMillis: Long = 0L
)

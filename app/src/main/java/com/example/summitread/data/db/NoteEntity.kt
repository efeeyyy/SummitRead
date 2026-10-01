package com.example.summitread.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val pageNumber: Int,
    val content: String,
    val photoPath: String? = null,
    val dateCreated: Long = System.currentTimeMillis(),
    val sessionId: Long? = null
)

package com.example.summitread.data.db

import com.example.summitread.data.currentTimeMillis
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [Index(value = ["bookId"])]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val pageNumber: Int,
    val content: String,
    val photoPath: String? = null,
    val dateCreated: Long = currentTimeMillis(),
    val sessionId: Long? = null
)

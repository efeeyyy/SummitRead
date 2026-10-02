package com.example.summitread.data.db

import com.example.summitread.data.currentTimeMillis
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String,
    val totalPages: Int,
    val currentPages: Int = 0,
    val coverImagePath: String? = null,
    val dateAdded: Long = currentTimeMillis(),
    val isCompleted: Boolean = false,
    val accentColorIndex: Int = 0,
    val displayOrder: Int = 0,
    val createdAtMillis: Long = currentTimeMillis(),
    val lastActivityMillis: Long = currentTimeMillis(),
    /** Arbitrary cover colour (ARGB int) chosen with the colour picker; null = use [accentColorIndex]. */
    val coverColorArgb: Int? = null,
    /** Archived books are hidden from the shelf/list but keep sessions, notes and relics. */
    @ColumnInfo(defaultValue = "0") val isArchived: Boolean = false,
    /** Epoch millis when the book reached 100%; null while unfinished. */
    val completedAtMillis: Long? = null
)

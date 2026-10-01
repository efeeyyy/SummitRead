package com.example.summitread.data.db

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
    val dateAdded: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val accentColorIndex: Int = 0,
    val displayOrder: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val lastActivityMillis: Long = System.currentTimeMillis()
)

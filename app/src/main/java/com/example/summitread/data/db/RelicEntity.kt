package com.example.summitread.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One relic per completed book (unique index on [bookId] guarantees "exactly once"). */
@Entity(
    tableName = "relics",
    indices = [Index(value = ["bookId"], unique = true)]
)
data class RelicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val title: String,
    val emoji: String = "🏔️",
    val dateEarned: Long = System.currentTimeMillis(),
    val review: String = "",
    val rating: Float = 0f
)

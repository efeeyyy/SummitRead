package com.example.summitread.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "relics")
data class RelicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val title: String,
    val emoji: String = "\uD83C\uDFD4\uFE0F",
    val dateEarned: Long = System.currentTimeMillis(),
    val review: String = "",
    val rating: Float = 0f
)

package com.example.summitread.data

/** Relic emoji is picked once, deterministically per book, and stored on the relic row. */
object RelicEmojis {
    val all = listOf("🗡️", "📚", "🔮", "🌟", "⭐", "🏔️")

    fun forBook(bookId: Long): String = all[(bookId.coerceAtLeast(0) % all.size).toInt()]
}

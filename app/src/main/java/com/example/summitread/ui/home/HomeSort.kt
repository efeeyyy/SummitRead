package com.example.summitread.ui.home

import com.example.summitread.data.db.BookEntity

/** Fraction of the book that is read, 0..1 (0 when the page count is unknown). */
fun bookProgress(book: BookEntity): Float =
    if (book.totalPages > 0) (book.currentPages.toFloat() / book.totalPages).coerceIn(0f, 1f) else 0f

/** Percentage shown on the spine and in the list, e.g. 34 for "34% Read". */
fun bookProgressPercent(book: BookEntity): Int = (bookProgress(book) * 100).toInt()

/** Pure sort used by the home screen; ties fall back to a stable, predictable order. */
fun sortBooks(books: List<BookEntity>, option: SortOption): List<BookEntity> = when (option) {
    SortOption.MANUAL -> books.sortedWith(
        compareBy<BookEntity> { it.displayOrder }.thenByDescending { it.createdAtMillis }
    )
    SortOption.NEWEST_FIRST -> books.sortedWith(
        compareByDescending<BookEntity> { it.createdAtMillis }.thenByDescending { it.id }
    )
    SortOption.RECENTLY_READ -> books.sortedWith(
        compareByDescending<BookEntity> { it.lastActivityMillis }.thenByDescending { it.createdAtMillis }
    )
    SortOption.PROGRESS -> books.sortedWith(
        compareByDescending<BookEntity> { bookProgress(it) }.thenBy { it.title.lowercase() }
    )
    SortOption.ALPHABETICAL -> books.sortedWith(
        compareBy<BookEntity> { it.title.lowercase() }.thenBy { it.id }
    )
}

/** Returns [ids] with [id] moved to the position currently held by [targetId]; unchanged if either is missing. */
fun moveId(ids: List<Long>, id: Long, targetId: Long): List<Long> {
    val from = ids.indexOf(id)
    val to = ids.indexOf(targetId)
    if (from < 0 || to < 0 || from == to) return ids
    val result = ids.toMutableList()
    result.removeAt(from)
    result.add(to, id)
    return result
}

/**
 * Applies a temporary drag order to [books]. Books missing from [order] keep their relative position after the
 * ordered ones; ids in [order] that no longer exist are ignored. A null [order] returns [books] unchanged.
 */
fun applyOrder(books: List<BookEntity>, order: List<Long>?): List<BookEntity> {
    if (order == null) return books
    val byId = books.associateBy { it.id }
    val ordered = order.mapNotNull { byId[it] }
    val orderedIds = ordered.mapTo(HashSet()) { it.id }
    return ordered + books.filter { it.id !in orderedIds }
}

/** Vertical bounds of one visible list item, used for hit-testing while dragging. */
data class ItemBounds(val id: Long, val start: Int, val size: Int)

/** Id of the visible item (other than [draggedId]) whose bounds contain [centerY], or null. */
fun findDropTarget(items: List<ItemBounds>, draggedId: Long, centerY: Float): Long? =
    items.firstOrNull { it.id != draggedId && centerY >= it.start && centerY < it.start + it.size }?.id

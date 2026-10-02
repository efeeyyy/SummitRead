package com.example.summitread.ui.home

import com.example.summitread.data.db.BookEntity
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.Test

class HomeSortTest {

    private fun book(
        id: Long,
        title: String = "Book $id",
        total: Int = 100,
        current: Int = 0,
        order: Int = 0,
        created: Long = id,
        activity: Long = id
    ) = BookEntity(
        id = id,
        title = title,
        author = "A",
        totalPages = total,
        currentPages = current,
        displayOrder = order,
        createdAtMillis = created,
        lastActivityMillis = activity
    )

    private fun ids(books: List<BookEntity>) = books.map { it.id }

    @Test
    fun manualOrderUsesDisplayOrder() {
        val books = listOf(book(1, order = 2), book(2, order = 0), book(3, order = 1))
        assertEquals(listOf(2L, 3L, 1L), ids(sortBooks(books, SortOption.MANUAL)))
    }

    @Test
    fun newestFirstUsesCreationTime() {
        val books = listOf(book(1, created = 10), book(2, created = 30), book(3, created = 20))
        assertEquals(listOf(2L, 3L, 1L), ids(sortBooks(books, SortOption.NEWEST_FIRST)))
    }

    @Test
    fun recentlyReadUsesLastActivity() {
        val books = listOf(book(1, activity = 500), book(2, activity = 100), book(3, activity = 900))
        assertEquals(listOf(3L, 1L, 2L), ids(sortBooks(books, SortOption.RECENTLY_READ)))
    }

    @Test
    fun progressSortsByPercentageAndBreaksTiesAlphabetically() {
        val books = listOf(
            book(1, title = "Zed", total = 200, current = 100), // 50%
            book(2, title = "Alpha", total = 100, current = 50), // 50%
            book(3, title = "Mid", total = 100, current = 90), // 90%
            book(4, title = "Empty", total = 0, current = 0) // unknown pages -> 0%
        )
        assertEquals(listOf(3L, 2L, 1L, 4L), ids(sortBooks(books, SortOption.PROGRESS)))
    }

    @Test
    fun alphabeticalIgnoresCase() {
        val books = listOf(book(1, title = "banana"), book(2, title = "Apple"), book(3, title = "cherry"))
        assertEquals(listOf(2L, 1L, 3L), ids(sortBooks(books, SortOption.ALPHABETICAL)))
    }

    @Test
    fun progressPercentIsClampedAndFloored() {
        assertEquals(34, bookProgressPercent(book(1, total = 300, current = 103)))
        assertEquals(100, bookProgressPercent(book(1, total = 100, current = 150)))
        assertEquals(0, bookProgressPercent(book(1, total = 0, current = 5)))
    }

    @Test
    fun moveIdPlacesDraggedItemAtTargetPosition() {
        assertEquals(listOf(2L, 3L, 1L, 4L), moveId(listOf(1, 2, 3, 4), id = 1, targetId = 3))
        assertEquals(listOf(4L, 1L, 2L, 3L), moveId(listOf(1, 2, 3, 4), id = 4, targetId = 1))
    }

    @Test
    fun moveIdKeepsListWhenIdsAreMissingOrEqual() {
        val ids = listOf(1L, 2L, 3L)
        assertEquals(ids, moveId(ids, id = 2, targetId = 2))
        assertEquals(ids, moveId(ids, id = 9, targetId = 2))
        assertEquals(ids, moveId(ids, id = 2, targetId = 9))
    }

    @Test
    fun repeatedSwapsKeepTrackingTheSameItemById() {
        // Dragging book 1 all the way down must keep working after the first swap (the old index-based bug).
        var order = listOf(1L, 2L, 3L, 4L)
        order = moveId(order, 1, 2)
        order = moveId(order, 1, 3)
        order = moveId(order, 1, 4)
        assertEquals(listOf(2L, 3L, 4L, 1L), order)
    }

    @Test
    fun applyOrderFollowsDragOrderAndAppendsUnknownBooks() {
        val books = listOf(book(1), book(2), book(3))
        assertEquals(listOf(3L, 1L, 2L), ids(applyOrder(books, listOf(3, 1, 2))))
        assertEquals(listOf(3L, 1L, 2L), ids(applyOrder(books, listOf(3, 1)))) // 2 appended
        assertEquals(listOf(3L, 1L, 2L), ids(applyOrder(books, listOf(3, 99, 1)))) // 99 ignored
        assertEquals(ids(books), ids(applyOrder(books, null)))
    }

    @Test
    fun findDropTargetReturnsOtherItemUnderCenter() {
        val items = listOf(ItemBounds(1, 0, 100), ItemBounds(2, 100, 100), ItemBounds(3, 200, 100))
        assertEquals(3L, findDropTarget(items, draggedId = 1, centerY = 250f))
        assertNull(findDropTarget(items, draggedId = 2, centerY = 150f)) // only over itself
        assertNull(findDropTarget(items, draggedId = 1, centerY = 400f)) // below the list
    }
}

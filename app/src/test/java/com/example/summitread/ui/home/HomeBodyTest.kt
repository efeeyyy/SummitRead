package com.example.summitread.ui.home

import com.example.summitread.data.db.BookEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeBodyTest {

    private val book = BookEntity(id = 1, title = "t", author = "a", totalPages = 10)

    @Test
    fun loadingWhileBooksNotDelivered() {
        assertEquals(HomeBody.LOADING, homeBody(books = null, listViewPref = true))
    }

    @Test
    fun loadingWhileViewPreferenceUnknown() {
        assertEquals(HomeBody.LOADING, homeBody(books = listOf(book), listViewPref = null))
        assertEquals(HomeBody.LOADING, homeBody(books = emptyList(), listViewPref = null))
    }

    @Test
    fun emptyOnlyWhenLoadedAndZeroBooks() {
        assertEquals(HomeBody.EMPTY, homeBody(books = emptyList(), listViewPref = false))
    }

    @Test
    fun contentWhenThereAreBooks() {
        assertEquals(HomeBody.CONTENT, homeBody(books = listOf(book), listViewPref = false))
        assertEquals(HomeBody.CONTENT, homeBody(books = listOf(book), listViewPref = true))
    }
}

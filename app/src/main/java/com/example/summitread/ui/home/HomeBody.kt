package com.example.summitread.ui.home

import com.example.summitread.data.db.BookEntity

/** What the library shows below the title. */
enum class HomeBody { LOADING, EMPTY, CONTENT }

/**
 * [books] is null until the first database emission has been combined with the sort preference, so "loading" and
 * the book list come from ONE value: the empty state can never be shown just because the list flow has not
 * caught up with a separate "loaded" flag. [listViewPref] is null until DataStore answered.
 */
fun homeBody(books: List<BookEntity>?, listViewPref: Boolean?): HomeBody = when {
    books == null || listViewPref == null -> HomeBody.LOADING
    books.isEmpty() -> HomeBody.EMPTY
    else -> HomeBody.CONTENT
}

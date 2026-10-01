package com.example.summitread.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.summitread.data.db.BookEntity
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.shadow
import com.example.summitread.theme.ShelfBrown
import com.example.summitread.theme.ShelfBrownDark
import com.example.summitread.theme.ShelfBrownLight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun BookshelfLayout(
    books: List<BookEntity>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBookClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // The books row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            books.forEachIndexed { index, book ->
                // Varing height based on total pages, min 130, max 200
                val heightDp = (130 + (book.totalPages % 70)).dp
                // Varing width based on title length, min 45, max 65
                val widthDp = (45 + (book.title.length % 20)).dp
                
                BookSpineItem(
                    book = book,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onClick = { onBookClick(book.id) },
                    width = widthDp,
                    height = heightDp
                )
                
                if (index < books.size - 1) {
                    Spacer(modifier = Modifier.width(3.dp))
                }
            }
        }
        
        // The wooden shelf divider
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(ShelfBrownLight)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .shadow(4.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                ShelfBrownDark, ShelfBrown, ShelfBrownLight, ShelfBrown, ShelfBrownDark
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.3f))
            )
        }
    }
}

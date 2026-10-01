package com.example.summitread.ui.home

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.summitread.data.db.BookEntity
import com.example.summitread.theme.ShelfBrown
import com.example.summitread.theme.ShelfBrownDark
import com.example.summitread.theme.ShelfBrownLight

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun BookshelfLayout(
    books: List<BookEntity>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBookClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    shimmer: ShimmerPhase = NoShimmer,
    onArchiveBook: ((BookEntity) -> Unit)? = null
) {
    Column(modifier = modifier) {
        // The books row. zIndex keeps the spines (and their progress label/bar) above the shelf's drop shadow,
        // which would otherwise be painted over the bottom of the spines.
        Row(
            modifier = Modifier
                .zIndex(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            books.forEachIndexed { index, book ->
                // Height varies with the page count (150..219dp), width with the title length (54..73dp);
                // the minimum width fits the "100% Read" label.
                val heightDp = (150 + (book.totalPages % 70)).dp
                val widthDp = (54 + (book.title.length % 20)).dp

                BookSpineItem(
                    book = book,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope,
                    onClick = { onBookClick(book.id) },
                    width = widthDp,
                    height = heightDp,
                    shimmer = shimmer,
                    onArchive = onArchiveBook?.let { archive -> { archive(book) } }
                )

                if (index < books.size - 1) {
                    Spacer(modifier = Modifier.width(3.dp))
                }
            }
        }

        // The wooden shelf: light top edge, gradient front board with a soft shadow, dark bottom line.
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
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        }
    }
}

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.summitread.data.db.BookEntity
import com.example.summitread.theme.AppTheme

/**
 * Wood tones of the shelf (decorative art, not part of the iOS palette): a pale lit top edge, a walnut front
 * board with a soft vertical gradient and a gentle sheen. Dark mode uses deeper, less saturated wood.
 */
private class ShelfWood(
    val lip: List<Color>,
    val board: List<Color>,
    val underside: Color,
    val wallShade: Color,
    val castShadow: Color
)

private val LightWood = ShelfWood(
    lip = listOf(Color(0xFFE9D3AB), Color(0xFFD2AE78)),
    board = listOf(Color(0xFFC49A62), Color(0xFFA67845), Color(0xFF8A5F33)),
    underside = Color(0x33000000),
    wallShade = Color(0x0F000000),
    castShadow = Color(0x2E000000)
)

private val DarkWood = ShelfWood(
    lip = listOf(Color(0xFF8C6A42), Color(0xFF744F2D)),
    board = listOf(Color(0xFF6A4A2A), Color(0xFF58391F), Color(0xFF452B16)),
    underside = Color(0x66000000),
    wallShade = Color(0x14FFFFFF),
    castShadow = Color(0x99000000)
)

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
    val wood = if (AppTheme.isDark) DarkWood else LightWood
    val wall = remember(wood) {
        Brush.verticalGradient(listOf(Color.Transparent, wood.wallShade))
    }

    Column(modifier = modifier) {
        // The books row. zIndex keeps the spines (and their progress label/bar) above the shelf board.
        Box(
            modifier = Modifier
                .zIndex(1f)
                .fillMaxWidth()
                .background(wall)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 8.dp),
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
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }
        }

        // The wooden shelf: lit top edge, walnut front board, dark underside and a soft shadow on the wall.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(Brush.verticalGradient(wood.lip))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .background(Brush.verticalGradient(wood.board))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(wood.underside)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .background(Brush.verticalGradient(listOf(wood.castShadow, Color.Transparent)))
        )
    }
}

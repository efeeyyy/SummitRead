package com.example.summitread.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import coil3.compose.SubcomposeAsyncImage
import com.example.summitread.data.db.BookEntity
import com.example.summitread.theme.AccentColor
import java.io.File

/** The colour a book is drawn with: the colour-picker value when set, otherwise its accent palette entry. */
fun bookBaseColor(book: BookEntity): Color = baseColorOf(book.coverColorArgb, book.accentColorIndex)

fun baseColorOf(coverColorArgb: Int?, accentColorIndex: Int): Color =
    coverColorArgb?.let { Color(it) }
        ?: (AccentColor.entries.getOrNull(accentColorIndex) ?: AccentColor.FOREST_GREEN).lightPrimary

/** Readable text/icon colour on top of [background]. */
fun onBookColor(background: Color): Color =
    if (background.luminance() > 0.5f) Color(0xE6000000) else Color.White

/** Top-to-bottom gradient used for a book without cover image. */
fun bookGradient(base: Color): Brush = Brush.verticalGradient(
    listOf(lerp(base, Color.White, 0.12f), lerp(base, Color.Black, 0.28f))
)

/**
 * Loads a stored cover. The DB keeps a plain file path, which Coil 3 does not reliably treat as a file, so the
 * path is always wrapped in a [File] (Coil maps it to a file URI). [fallback] shows while loading, on error
 * and is the vector cover used when there is no image at all.
 */
@Composable
fun CoverImage(
    path: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fallback: @Composable () -> Unit
) {
    if (path.isNullOrEmpty()) {
        Box(modifier) { fallback() }
    } else {
        SubcomposeAsyncImage(
            model = File(path),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
            loading = { fallback() },
            error = { fallback() }
        )
    }
}

/** Vector cover: colour gradient with a book glyph. Fills its parent. */
@Composable
fun VectorCover(base: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bookGradient(base)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Book,
            contentDescription = null,
            tint = onBookColor(base).copy(alpha = 0.55f)
        )
    }
}

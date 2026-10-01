package com.example.summitread.ui.home

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.summitread.data.db.BookEntity

/** Height reserved at the bottom of a spine for the progress bar and its "34% Read" label. */
private val ProgressZoneHeight = 26.dp

/** Lays the child out rotated by -90 degrees; the child's length is bounded by the available height. */
private fun Modifier.verticalText(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(
        Constraints(
            minWidth = 0,
            maxWidth = constraints.maxHeight,
            minHeight = 0,
            maxHeight = constraints.maxWidth
        )
    )
    layout(placeable.height, placeable.width) {
        placeable.placeWithLayer(
            x = (placeable.height - placeable.width) / 2,
            y = (placeable.width - placeable.height) / 2
        ) { rotationZ = -90f }
    }
}

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun BookSpineItem(
    book: BookEntity,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    shimmer: ShimmerPhase = NoShimmer,
    onArchive: (() -> Unit)? = null
) {
    val baseColor = bookBaseColor(book)
    val textColor = onBookColor(baseColor)
    val percent = bookProgressPercent(book)
    val progress = bookProgress(book)
    val completed = book.isCompleted || progress >= 1f
    var menuOpen by remember { mutableStateOf(false) }

    val description = buildString {
        append(book.title).append(" by ").append(book.author)
        if (percent > 0) append(", ").append(percent).append("% read")
        if (completed) append(", completed")
    }

    Box(modifier = modifier.size(width, height)) {
        with(sharedTransitionScope) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "book_container_${book.id}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                    .shadow(3.dp, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .combinedClickable(
                        onClick = onClick,
                        onLongClickLabel = if (onArchive != null) "Archive book" else null,
                        onLongClick = if (onArchive != null) ({ menuOpen = true }) else null
                    )
                    .clearAndSetSemantics { contentDescription = description }
            ) {
                CoverImage(
                    path = book.coverImagePath,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    fallback = {
                        SpineFace(
                            book = book,
                            baseColor = baseColor,
                            textColor = textColor,
                            completed = completed,
                            height = height
                        )
                    }
                )

                if (progress > 0f) {
                    // Progress bar along the bottom edge, with its label above it inside the reserved zone.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(4.dp)
                            .background(if (completed) Color(0xFFFFD54F) else textColor.copy(alpha = 0.9f))
                            .align(Alignment.BottomStart)
                    )
                    Text(
                        text = "$percent% Read",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp, start = 2.dp, end = 2.dp)
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }

                if (completed) {
                    GoldFoilBand(shimmer = shimmer, thickness = 3.dp, modifier = Modifier.align(Alignment.TopCenter))
                    GoldFoilSeal(
                        shimmer = shimmer,
                        size = 20.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 7.dp)
                    )
                    GoldFoilBand(
                        shimmer = shimmer,
                        thickness = 1.5.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 32.dp)
                    )
                }
            }
        }

        if (onArchive != null) {
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text("Archive") },
                    onClick = {
                        menuOpen = false
                        onArchive()
                    }
                )
            }
        }
    }
}

/** Title, author initials and background of a spine without a cover image. */
@Composable
private fun SpineFace(
    book: BookEntity,
    baseColor: Color,
    textColor: Color,
    completed: Boolean,
    height: Dp
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bookGradient(baseColor))
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The foil seal occupies the top 34dp of completed books.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = if (completed) 38.dp else 8.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = book.title,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = if (height > 180.dp) 13.sp else 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.verticalText()
            )
        }
        Text(
            text = book.author.take(2).uppercase(),
            color = textColor.copy(alpha = 0.75f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(modifier = Modifier.height(ProgressZoneHeight))
    }
}


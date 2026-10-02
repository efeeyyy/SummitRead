package com.example.summitread.ui.home

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.summitread.R
import com.example.summitread.data.db.BookEntity
import com.example.summitread.theme.AppType
import com.example.summitread.theme.rememberAppHaptics
import com.example.summitread.ui.components.appShadow
import com.example.summitread.ui.components.pressScale

/** Height reserved at the bottom of a spine for the progress bar and its "34% Read" label. */
private val ProgressZoneHeight = 30.dp

private val SpineShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 1.dp, bottomEnd = 1.dp)

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

/** Soft cylindrical sheen: a lit left edge and a shaded right edge, so flat colours read as a real spine. */
private val SpineSheen = Brush.horizontalGradient(
    0f to Color.White.copy(alpha = 0.20f),
    0.10f to Color.White.copy(alpha = 0.05f),
    0.55f to Color.Transparent,
    1f to Color.Black.copy(alpha = 0.20f)
)

@OptIn(
    androidx.compose.animation.ExperimentalSharedTransitionApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)
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
    val interaction = remember { MutableInteractionSource() }
    val haptics = rememberAppHaptics()
    val platformHaptics = LocalHapticFeedback.current

    val byAuthor = stringResource(R.string.book_by_author, book.title, book.author)
    val readText = if (percent > 0) stringResource(R.string.book_read_percent, percent) else null
    val doneText = if (completed) stringResource(R.string.book_completed) else null
    val description = listOfNotNull(byAuthor, readText, doneText).joinToString(", ")

    Box(modifier = modifier.size(width, height)) {
        with(sharedTransitionScope) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "book_container_${book.id}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                    .pressScale(interaction)
                    .appShadow(elevation = 6.dp, shape = SpineShape, ambientAlpha = 0.10f, spotAlpha = 0.18f)
                    .clip(SpineShape)
                    .combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = {
                            haptics.light()
                            onClick()
                        },
                        onLongClickLabel = if (onArchive != null) stringResource(R.string.archive_book_label) else null,
                        onLongClick = if (onArchive != null) {
                            {
                                platformHaptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                menuOpen = true
                            }
                        } else null
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
                Box(Modifier.fillMaxSize().background(SpineSheen))

                if (progress > 0f) {
                    // Inset capsule along the bottom with its label above it, inside the reserved zone.
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(start = 6.dp, end = 6.dp, bottom = 6.dp)
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.28f))
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(progress)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(50))
                                .background(if (completed) Color(0xFFFFD54F) else Color.White)
                        )
                    }
                    ReadLabel(
                        text = stringResource(R.string.percent_read, percent),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 13.dp, start = 2.dp, end = 2.dp)
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
            HomeDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                HomeMenuItem(
                    text = stringResource(R.string.archive),
                    onClick = {
                        menuOpen = false
                        onArchive()
                    }
                )
            }
        }
    }
}

/** The "34% Read" label: tiny, tabular, on a dark glass pill so it stays readable on any cover. */
@Composable
private fun ReadLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = Color.White,
        style = AppType.Caption2.copy(fontSize = 8.sp, lineHeight = 10.sp, fontWeight = FontWeight.SemiBold),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.50f), RoundedCornerShape(50))
            .padding(horizontal = 4.dp, vertical = 1.5.dp)
    )
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
                .padding(top = if (completed) 38.dp else 10.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = book.title,
                color = textColor,
                style = AppType.Footnote.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (height > 180.dp) 14.sp else 13.sp,
                    letterSpacing = 0.1.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.verticalText()
            )
        }
        Text(
            text = book.author.take(2).uppercase(),
            color = textColor.copy(alpha = 0.75f),
            style = AppType.Caption2.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            maxLines = 1,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box(modifier = Modifier.height(ProgressZoneHeight))
    }
}

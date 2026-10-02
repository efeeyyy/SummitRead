package com.example.summitread.ui.home

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.summitread.R
import com.example.summitread.data.db.BookEntity
import com.example.summitread.theme.AppMotion
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.theme.rememberAppHaptics
import com.example.summitread.theme.tabularNumbers
import com.example.summitread.ui.components.CardContainer
import com.example.summitread.ui.components.IconButtonStyle
import com.example.summitread.ui.components.IconCircleButton
import com.example.summitread.ui.components.LinearProgressCapsule
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Vertical list of books with long-press drag-and-drop reordering.
 *
 * Drag works in every sort mode: when a drag ends the dropped order is handed to [onReorder], which persists it
 * and switches the sort to Manual Order. Items are keyed by book id and the gesture tracks the dragged book by
 * id (never by index), so repeated swaps within one drag keep working.
 */
@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun BookListLayout(
    books: List<BookEntity>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBookClick: (Long) -> Unit,
    onReorder: (List<Long>) -> Unit,
    onArchiveBook: (BookEntity) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    shimmer: ShimmerPhase = NoShimmer,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null
) {
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    // The header item sits before the books, which shifts every book's lazy-list index by one.
    val headerCount = if (header != null) 1 else 0
    val density = LocalDensity.current
    val edgePx = with(density) { 72.dp.toPx() }
    val topInsetPx = with(density) { contentPadding.calculateTopPadding().toPx() }
    val bottomInsetPx = with(density) { contentPadding.calculateBottomPadding().toPx() }

    // Working order while dragging / until the database catches up with the dropped order.
    var order by remember { mutableStateOf<List<Long>?>(null) }
    var startOrder by remember { mutableStateOf<List<Long>?>(null) }
    var draggingId by remember { mutableStateOf<Long?>(null) }
    // Top edge (in list coordinates) the dragged card should be drawn at; follows the finger.
    var dragTop by remember { mutableFloatStateOf(0f) }

    val shown = remember(books, order) { applyOrder(books, order) }
    val shownState by rememberUpdatedState(shown)
    val onReorderState by rememberUpdatedState(onReorder)

    // Drop the local order once the database shows it; give up after a moment if it never does.
    LaunchedEffect(books, order, draggingId) {
        val current = order
        if (draggingId == null && current != null) {
            if (books.map { it.id } == current) {
                order = null
            } else {
                delay(1500)
                order = null
            }
        }
    }

    // While dragging: once per frame swap with the item under the dragged card and auto-scroll near the edges.
    LaunchedEffect(draggingId) {
        val id = draggingId ?: return@LaunchedEffect
        while (true) {
            androidx.compose.runtime.withFrameNanos { }
            val info = listState.layoutInfo
            val visible = info.visibleItemsInfo
            val dragged = visible.firstOrNull { it.key == id } ?: continue
            val currentOrder = order ?: break
            // Only act on a layout that already reflects the current order; right after a swap the layout is
            // stale for one frame and would otherwise swap straight back.
            if (dragged.index - headerCount == currentOrder.indexOf(id)) {
                val centerY = dragTop + dragged.size / 2f
                val target = findDropTarget(
                    items = visible.mapNotNull { item ->
                        (item.key as? Long)?.let { ItemBounds(it, item.offset, item.size) }
                    },
                    draggedId = id,
                    centerY = centerY
                )
                if (target != null) order = moveId(currentOrder, id, target)
            }
            // The list runs under the glass bars, so the auto-scroll zones are measured from the visible area
            // (below the top bar, above the tab bar), not from the raw list edges.
            val visibleTop = topInsetPx
            val visibleBottom = info.viewportSize.height - bottomInsetPx
            val center = dragTop + dragged.size / 2f
            val scrollBy = when {
                center < visibleTop + edgePx -> -(visibleTop + edgePx - center) / edgePx * 24f
                center > visibleBottom - edgePx -> (center - (visibleBottom - edgePx)) / edgePx * 24f
                else -> 0f
            }.coerceIn(-36f, 36f)
            if (scrollBy != 0f) listState.scrollBy(scrollBy)
        }
    }

    LazyColumn(
        state = listState,
        contentPadding = contentPadding,
        modifier = modifier.pointerInput(Unit) {
            detectLongPressReorder(
                onStart = { offset ->
                    val hit = listState.layoutInfo.visibleItemsInfo.firstOrNull {
                        offset.y >= it.offset && offset.y < it.offset + it.size
                    }
                    val id = hit?.key as? Long
                    if (hit == null || id == null) {
                        false
                    } else {
                        dragTop = hit.offset.toFloat()
                        val ids = shownState.map { it.id }
                        startOrder = ids
                        order = ids
                        draggingId = id
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        true
                    }
                },
                onDrag = { dy -> dragTop += dy },
                onEnd = {
                    val final = order
                    draggingId = null
                    if (final != null && final != startOrder) onReorderState(final) else order = null
                },
                onCancel = {
                    draggingId = null
                    order = null
                }
            )
        }
    ) {
        if (header != null) {
            item(key = "header") { header() }
        }
        items(shown, key = { it.id }) { book ->
            val isDragged = draggingId == book.id
            val position = shown.indexOfFirst { it.id == book.id }
            BookListItem(
                book = book,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                shimmer = shimmer,
                isDragged = isDragged,
                onClick = { onBookClick(book.id) },
                onArchive = { onArchiveBook(book) },
                onMoveUp = if (position > 0) {
                    { onReorderState(moveId(shown.map { it.id }, book.id, shown[position - 1].id)) }
                } else null,
                onMoveDown = if (position in 0 until shown.size - 1) {
                    { onReorderState(moveId(shown.map { it.id }, book.id, shown[position + 1].id)) }
                } else null,
                modifier = if (isDragged) {
                    Modifier
                        .zIndex(1f)
                        .graphicsLayer {
                            val layoutTop = listState.layoutInfo.visibleItemsInfo
                                .firstOrNull { it.key == book.id }?.offset?.toFloat() ?: dragTop
                            translationY = dragTop - layoutTop
                        }
                } else {
                    Modifier.animateItem()
                }
            )
        }
    }
}

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
private fun BookListItem(
    book: BookEntity,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    shimmer: ShimmerPhase,
    isDragged: Boolean,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val baseColor = bookBaseColor(book)
    val percent = bookProgressPercent(book)
    val progress = bookProgress(book)
    val completed = book.isCompleted || progress >= 1f
    var menuOpen by remember { mutableStateOf(false) }
    val haptics = rememberAppHaptics()
    // The card lifts slightly while it is being dragged.
    val lift by animateFloatAsState(if (isDragged) 1.03f else 1f, AppMotion.snappy(), label = "dragLift")

    val moveUpLabel = stringResource(R.string.move_up)
    val moveDownLabel = stringResource(R.string.move_down)

    with(sharedTransitionScope) {
        Box(modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            CardContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = lift
                        scaleY = lift
                    }
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "book_container_${book.id}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                    .semantics {
                        customActions = listOfNotNull(
                            onMoveUp?.let { action ->
                                CustomAccessibilityAction(moveUpLabel) { action(); true }
                            },
                            onMoveDown?.let { action ->
                                CustomAccessibilityAction(moveDownLabel) { action(); true }
                            }
                        )
                    },
                onClick = {
                    haptics.light()
                    onClick()
                },
                onClickLabel = stringResource(R.string.open_book, book.title),
                contentPadding = PaddingValues(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Small cover: the real image when there is one, a vector cover otherwise.
                    Box {
                        CoverThumbnail(book = book, width = 56.dp, height = 84.dp)
                        if (completed) {
                            GoldFoilSeal(
                                shimmer = shimmer,
                                size = 24.dp,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 8.dp, y = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = book.title,
                            style = AppType.Headline,
                            color = colors.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = book.author,
                            style = AppType.Subheadline,
                            color = colors.secondaryLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressCapsule(
                                progress = progress,
                                modifier = Modifier.weight(1f),
                                height = 6.dp,
                                color = if (completed) colors.gold else baseColor
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.percent_read, percent),
                                style = AppType.Footnote.tabularNumbers(),
                                color = colors.secondaryLabel,
                                maxLines = 1
                            )
                        }
                    }

                    Box {
                        IconCircleButton(
                            icon = Icons.Rounded.MoreHoriz,
                            contentDescription = stringResource(R.string.more_options_for, book.title),
                            onClick = { menuOpen = true },
                            style = IconButtonStyle.Plain,
                            tint = colors.secondaryLabel
                        )
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
        }
    }
}

/**
 * Long-press-then-drag detector for the whole list. It watches the Initial pass (before the list's own scroll
 * handling sees the events) so that, once the long press fires, the drag cannot be stolen by scrolling and the
 * final release is consumed (no accidental click on the card that was dragged). Before the long press fires it
 * only observes, so taps and normal scrolling are untouched.
 *
 * [onStart] receives the press position and returns whether a drag actually started; [onDrag] gets the vertical
 * movement since the previous event.
 */
private suspend fun PointerInputScope.detectLongPressReorder(
    onStart: (Offset) -> Boolean,
    onDrag: (Float) -> Unit,
    onEnd: () -> Unit,
    onCancel: () -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val abortedEarly = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change == null ||
                    !change.pressed ||
                    (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                ) {
                    return@withTimeoutOrNull true
                }
            }
            @Suppress("UNREACHABLE_CODE")
            false
        }
        if (abortedEarly != null) return@awaitEachGesture
        if (!onStart(down.position)) return@awaitEachGesture

        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id }
            if (change == null) {
                onCancel()
                return@awaitEachGesture
            }
            if (!change.pressed) {
                change.consume()
                onEnd()
                return@awaitEachGesture
            }
            val dy = change.positionChange().y
            if (dy != 0f) onDrag(dy)
            change.consume()
        }
    }
}

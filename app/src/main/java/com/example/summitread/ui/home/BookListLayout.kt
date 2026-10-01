package com.example.summitread.ui.home

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.summitread.data.db.BookEntity
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
    shimmer: ShimmerPhase = NoShimmer
) {
    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val edgePx = with(LocalDensity.current) { 72.dp.toPx() }

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
            if (dragged.index == currentOrder.indexOf(id)) {
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
            val viewport = (info.viewportEndOffset - info.viewportStartOffset).toFloat()
            val center = dragTop + dragged.size / 2f
            val scrollBy = when {
                center < edgePx -> -(edgePx - center) / edgePx * 24f
                center > viewport - edgePx -> (center - (viewport - edgePx)) / edgePx * 24f
                else -> 0f
            }
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
    val baseColor = bookBaseColor(book)
    val percent = bookProgressPercent(book)
    val progress = bookProgress(book)
    val completed = book.isCompleted || progress >= 1f
    var menuOpen by remember { mutableStateOf(false) }

    with(sharedTransitionScope) {
        Box(modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .sharedBounds(
                        sharedContentState = rememberSharedContentState(key = "book_container_${book.id}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                    .semantics {
                        customActions = listOfNotNull(
                            onMoveUp?.let { action ->
                                CustomAccessibilityAction("Move up") { action(); true }
                            },
                            onMoveDown?.let { action ->
                                CustomAccessibilityAction("Move down") { action(); true }
                            }
                        )
                    }
                    .clickable(onClickLabel = "Open ${book.title}", onClick = onClick),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = if (isDragged) 10.dp else 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Small cover: the real image when there is one, a vector cover otherwise.
                    Box {
                        CoverImage(
                            path = book.coverImagePath,
                            contentDescription = "Cover of ${book.title}",
                            modifier = Modifier
                                .width(48.dp)
                                .height(72.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            fallback = { VectorCover(base = baseColor) }
                        )
                        if (completed) {
                            GoldFoilSeal(
                                shimmer = shimmer,
                                size = 22.dp,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = 8.dp, y = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = book.author,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp),
                                color = baseColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeCap = StrokeCap.Round
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$percent% Read",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options for ${book.title}"
                            )
                        }
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

package com.example.summitread.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Box
import com.example.summitread.data.db.BookEntity
import com.example.summitread.theme.AccentColor

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun BookListLayout(
    books: List<BookEntity>,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBookClick: (Long) -> Unit,
    onSaveOrder: ((List<BookEntity>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    with(sharedTransitionScope) {
        var currentList by remember(books) { mutableStateOf(books) }
        var draggedItemIndex by remember { mutableStateOf<Int?>(null) }
        var dragOffset by remember { mutableStateOf(0f) }

        Column(modifier = modifier.padding(horizontal = 16.dp)) {
            currentList.forEachIndexed { index, book ->
                val isDragged = draggedItemIndex == index
                val zIndex = if (isDragged) 1f else 0f
                val yOffset = if (isDragged) dragOffset else 0f

                ElevatedCard(
                    modifier = Modifier
                        .zIndex(zIndex)
                        .offset { IntOffset(0, yOffset.roundToInt()) }
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .sharedBounds(
                            sharedContentState = rememberSharedContentState(key = "book_container_${book.id}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        .pointerInput(book.id, onSaveOrder != null) {
                            if (onSaveOrder != null) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { 
                                        draggedItemIndex = index 
                                        dragOffset = 0f
                                    },
                                    onDrag = { change, dragAmount -> 
                                        change.consume()
                                        dragOffset += dragAmount.y
                                        // Dynamic threshold based on density
                                        val swapThreshold = 100 * density
                                        
                                        if (dragOffset > swapThreshold && index < currentList.size - 1) {
                                            val newList = currentList.toMutableList()
                                            val item = newList.removeAt(index)
                                            newList.add(index + 1, item)
                                            currentList = newList
                                            draggedItemIndex = index + 1
                                            dragOffset -= swapThreshold
                                        } else if (dragOffset < -swapThreshold && index > 0) {
                                            val newList = currentList.toMutableList()
                                            val item = newList.removeAt(index)
                                            newList.add(index - 1, item)
                                            currentList = newList
                                            draggedItemIndex = index - 1
                                            dragOffset += swapThreshold
                                        }
                                    },
                                    onDragEnd = {
                                        draggedItemIndex = null
                                        dragOffset = 0f
                                        onSaveOrder(currentList)
                                    },
                                    onDragCancel = {
                                        draggedItemIndex = null
                                        dragOffset = 0f
                                        // Optionally reset to original list here
                                        currentList = books
                                    }
                                )
                            }
                        }
                        .clickable { onBookClick(book.id) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val accentColor = AccentColor.entries.getOrNull(book.accentColorIndex)?.lightPrimary ?: AccentColor.FOREST_GREEN.lightPrimary
                    
                    // Small cover representation
                    Box {
                        if (!book.coverImagePath.isNullOrEmpty()) {
                        AsyncImage(
                            model = book.coverImagePath,
                            contentDescription = "Cover of ${book.title}",
                            modifier = Modifier
                                .width(48.dp)
                                .height(72.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .width(48.dp)
                                .height(72.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(accentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = null,
                                tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                    
                    if (book.isCompleted) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 8.dp, y = 8.dp)
                                .size(24.dp)
                                .background(androidx.compose.ui.graphics.Color(0xFFFFD700), CircleShape)
                                .border(1.dp, androidx.compose.ui.graphics.Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🏅", fontSize = 12.sp)
                        }
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
                        
                        val progress = if (book.totalPages > 0) book.currentPages.toFloat() / book.totalPages else 0f
                        val progressPercent = (progress * 100).toInt()
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LinearProgressIndicator(
                                progress = { progress.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp),
                                color = accentColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeCap = StrokeCap.Round
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$progressPercent%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

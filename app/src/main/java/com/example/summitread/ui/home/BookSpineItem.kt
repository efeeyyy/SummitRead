package com.example.summitread.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.summitread.data.db.BookEntity
import com.example.summitread.theme.AccentColor
import java.io.File
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun BookSpineItem(
    book: BookEntity,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onClick: () -> Unit,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier
) {
    val accentColor = AccentColor.entries.getOrNull(book.accentColorIndex) ?: AccentColor.FOREST_GREEN

    with(sharedTransitionScope) {
        Box(
            modifier = modifier
                .size(width, height)
                .sharedBounds(
                    sharedContentState = rememberSharedContentState(key = "book_container_${book.id}"),
                    animatedVisibilityScope = animatedVisibilityScope
                )
                .shadow(3.dp, RoundedCornerShape(2.dp))
                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                .clickable { onClick() }
        ) {
        if (book.coverImagePath != null) {
            AsyncImage(
                model = File(book.coverImagePath).toURI().toString(),
                contentDescription = book.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Gradient background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                accentColor.lightPrimary,
                                accentColor.darkPrimary
                            )
                        )
                    )
            )

            // Text content
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = book.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .graphicsLayer { rotationZ = -90f }
                            .requiredWidth(height - 30.dp) // constrain width based on rotated height
                    )
                }
                
                // Author initials at bottom
                Text(
                    text = book.author.take(2).uppercase(),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        // Progress bar at the very bottom
        val progress = if (book.totalPages > 0) book.currentPages.toFloat() / book.totalPages else 0f
        if (progress > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(4.dp)
                    .background(accentColor.lightPrimary)
                    .align(Alignment.BottomStart)
            )
            // Progress Text
            val percent = (progress * 100).toInt()
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$percent%",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            }
        
        if (book.isCompleted) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(16.dp)
                    .background(androidx.compose.ui.graphics.Color(0xFFFFD700), androidx.compose.foundation.shape.CircleShape)
                    .border(0.5.dp, Color.White, androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🏅", fontSize = 8.sp)
            }
        }
    }
    }
}


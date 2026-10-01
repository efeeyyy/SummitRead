package com.example.summitread.ui.mountain

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import java.io.File

@Composable
fun PolaroidPhoto(
    imagePath: String,
    pageNumber: Int,
    modifier: Modifier = Modifier
) {
    val sepiaMatrix = ColorMatrix(
        floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )

    Box(
        modifier = modifier
            .widthIn(max = 200.dp)
            .shadow(8.dp, RoundedCornerShape(2.dp))
            .background(Color.White, RoundedCornerShape(2.dp))
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AsyncImage(
            model = File(imagePath).toUri(),
            contentDescription = "Note photo on page $pageNumber",
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.colorMatrix(sepiaMatrix),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 180.dp)
        )

        Text(
            text = "p. $pageNumber",
            fontSize = 12.sp,
            fontStyle = FontStyle.Italic,
            fontFamily = FontFamily.Serif,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(top = 4.dp)
        )
    }
}

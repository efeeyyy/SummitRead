package com.example.summitread.ui.mountain

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.summitread.theme.AppType
import okio.Path.Companion.toPath

/**
 * A warm, sepia instant photo: soft shadow, wide white bottom border with the page number in a serif hand.
 * The paper stays white in both themes on purpose (it is a physical print).
 */
@Composable
fun PolaroidPhoto(
    imagePath: String,
    pageNumber: Int,
    modifier: Modifier = Modifier
) {
    val sepiaFilter = remember {
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    }
    val paper = RoundedCornerShape(3.dp)

    Column(
        modifier = modifier
            .widthIn(max = 220.dp)
            .shadow(10.dp, paper, ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.22f))
            .background(Color(0xFFFFFEFB), paper)
            .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = imagePath.toPath(),
            contentDescription = "Note photo on page $pageNumber",
            contentScale = ContentScale.Crop,
            colorFilter = sepiaFilter,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp, max = 180.dp)
                .clip(RoundedCornerShape(1.dp))
        )

        Text(
            text = "p. $pageNumber",
            style = AppType.Footnote,
            fontStyle = FontStyle.Italic,
            fontFamily = FontFamily.Serif,
            color = Color(0xFF6B6B70),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 8.dp, bottom = 4.dp)
        )
    }
}

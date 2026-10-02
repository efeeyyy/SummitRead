package com.example.summitread.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.SummitReadTheme

/** Renders every annotated preview in light and dark (system uiMode). */
@Preview(name = "Light", widthDp = 390)
@Preview(name = "Dark", widthDp = 390, uiMode = Configuration.UI_MODE_NIGHT_YES)
annotation class LightDarkPreview

/** Preview wrapper: themed, on the grouped background, with a standard margin. */
@Composable
internal fun PreviewSurface(content: @Composable () -> Unit) {
    SummitReadTheme {
        Box(Modifier.background(AppTheme.colors.groupedBackground).padding(16.dp)) {
            content()
        }
    }
}

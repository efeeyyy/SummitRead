package com.example.summitread.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.summitread.R

/**
 * Inter (SIL OFL 1.1, docs/licenses/Inter-OFL.txt) stands in for SF Pro, which cannot be bundled.
 * The only Android-specific line here is `R.font.*`; on Compose Multiplatform swap it for `Res.font.*`.
 */
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double) = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp
)

/**
 * iOS Dynamic Type ramp (default size category). Tracking follows Inter's recommended
 * optical tightening (negative on large sizes, ~0 at 12sp and below).
 */
object AppType {
    val LargeTitle = style(34, 41, FontWeight.Bold, -0.74)
    val Title1 = style(28, 34, FontWeight.Bold, -0.58)
    val Title2 = style(22, 28, FontWeight.SemiBold, -0.40)
    val Title3 = style(20, 25, FontWeight.SemiBold, -0.33)
    val Headline = style(17, 22, FontWeight.SemiBold, -0.22)
    val Body = style(17, 22, FontWeight.Normal, -0.22)
    val Callout = style(16, 21, FontWeight.Normal, -0.18)
    val Subheadline = style(15, 20, FontWeight.Normal, -0.13)
    val Footnote = style(13, 18, FontWeight.Normal, -0.04)
    val Caption1 = style(12, 16, FontWeight.Normal, 0.0)
    val Caption2 = style(11, 13, FontWeight.Medium, 0.05)

    /** Tab bar labels (iOS uses 10pt). */
    val TabLabel = style(10, 12, FontWeight.Medium, 0.1)

    /** Body copy with emphasised weight, for inline highlights. */
    val BodyEmphasized = Body.copy(fontWeight = FontWeight.SemiBold)
}

/** Fixed-width digits so timers and counters do not jitter. */
fun TextStyle.tabularNumbers(): TextStyle = copy(fontFeatureSettings = "tnum")

/** Maps the iOS ramp onto Material 3 so existing screens inherit the new look. */
val SummitReadTypography = Typography(
    displayLarge = AppType.LargeTitle,
    displayMedium = AppType.LargeTitle,
    displaySmall = AppType.Title1,
    headlineLarge = AppType.Title1,
    headlineMedium = AppType.Title2,
    headlineSmall = AppType.Title3,
    titleLarge = AppType.Title2,
    titleMedium = AppType.Headline,
    titleSmall = AppType.Subheadline.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = AppType.Body,
    bodyMedium = AppType.Subheadline,
    bodySmall = AppType.Footnote,
    labelLarge = AppType.Subheadline.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = AppType.Footnote.copy(fontWeight = FontWeight.Medium),
    labelSmall = AppType.Caption1.copy(fontWeight = FontWeight.Medium)
)

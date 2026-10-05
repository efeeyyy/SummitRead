package com.example.summitread.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.summitread.resources.Res
import com.example.summitread.resources.inter_bold
import com.example.summitread.resources.inter_medium
import com.example.summitread.resources.inter_regular
import com.example.summitread.resources.inter_semibold
import org.jetbrains.compose.resources.Font

/**
 * Inter (SIL OFL 1.1, composeResources/files/Inter-OFL.txt) stands in for SF Pro, which cannot be bundled.
 * Compose Multiplatform font resources can only be loaded from a composition, so the family is created by
 * [rememberInterFontFamily] and applied once in [SummitReadTheme] through the Material typography. The
 * [AppType] styles deliberately leave `fontFamily` unset and inherit it from `LocalTextStyle` (`Text`,
 * `ProvideTextStyle`); text primitives that do not inherit (BasicTextField) merge `LocalTextStyle` first.
 */
@Composable
fun rememberInterFontFamily(): FontFamily {
    val regular = Font(Res.font.inter_regular, FontWeight.Normal)
    val medium = Font(Res.font.inter_medium, FontWeight.Medium)
    val semiBold = Font(Res.font.inter_semibold, FontWeight.SemiBold)
    val bold = Font(Res.font.inter_bold, FontWeight.Bold)
    return remember(regular, medium, semiBold, bold) { FontFamily(regular, medium, semiBold, bold) }
}

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double) = TextStyle(
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

/** Maps the iOS ramp onto Material 3 so existing screens inherit the new look (and the Inter family). */
fun summitReadTypography(fontFamily: FontFamily): Typography {
    fun TextStyle.inter() = copy(fontFamily = fontFamily)
    return Typography(
        displayLarge = AppType.LargeTitle.inter(),
        displayMedium = AppType.LargeTitle.inter(),
        displaySmall = AppType.Title1.inter(),
        headlineLarge = AppType.Title1.inter(),
        headlineMedium = AppType.Title2.inter(),
        headlineSmall = AppType.Title3.inter(),
        titleLarge = AppType.Title2.inter(),
        titleMedium = AppType.Headline.inter(),
        titleSmall = AppType.Subheadline.copy(fontWeight = FontWeight.SemiBold).inter(),
        bodyLarge = AppType.Body.inter(),
        bodyMedium = AppType.Subheadline.inter(),
        bodySmall = AppType.Footnote.inter(),
        labelLarge = AppType.Subheadline.copy(fontWeight = FontWeight.SemiBold).inter(),
        labelMedium = AppType.Footnote.copy(fontWeight = FontWeight.Medium).inter(),
        labelSmall = AppType.Caption1.copy(fontWeight = FontWeight.Medium).inter()
    )
}

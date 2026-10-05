package com.example.summitread.ui.components

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.summitread.theme.AppTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * Frosted-glass material. The only file that touches the Haze library; screens and components
 * use [glassSource] / [glassBackground] only.
 *
 * Android 12+ blurs the content behind; older versions (and a missing [GlassState]) fall back
 * to a translucent surface tint, handled inside Haze.
 */
@Stable
class GlassState internal constructor(internal val haze: HazeState)

@Composable
fun rememberGlassState(): GlassState {
    val haze = remember { HazeState() }
    return remember(haze) { GlassState(haze) }
}

/** Set by a scaffold so glass bars inside its slots pick the right source automatically. */
val LocalGlassState = compositionLocalOf<GlassState?> { null }

/**
 * Height of a floating bottom bar owned by an ancestor (for example the root tab bar). Scaffolds
 * add it to their bottom content padding so lists can scroll under the bar and still end above it.
 */
val LocalBottomBarHeight = compositionLocalOf { 0.dp }

/** Marks this content as the thing glass surfaces blur. Put it on the scrolling content. */
fun Modifier.glassSource(state: GlassState): Modifier = hazeSource(state.haze)

/**
 * Material tokens, defined once for every glass surface (like the iOS system materials).
 * - [Thin]: small floating controls over artwork (header capsules).
 * - [Regular]: bars over scrolling content (tab bar, collapsed header): strong blur and a mostly opaque tint,
 *   so text underneath is never readable.
 * - [Thick]: overlays that must read as nearly solid.
 */
enum class GlassThickness(val blurDp: Float, val tintAlphaLight: Float, val tintAlphaDark: Float) {
    Thin(blurDp = 24f, tintAlphaLight = 0.66f, tintAlphaDark = 0.55f),
    Regular(blurDp = 40f, tintAlphaLight = 0.86f, tintAlphaDark = 0.80f),
    Thick(blurDp = 48f, tintAlphaLight = 0.93f, tintAlphaDark = 0.90f)
}

/**
 * Glass background for bars and overlays. [state] null means "no source available":
 * draws the same tint as a plain translucent surface.
 */
@Composable
fun Modifier.glassBackground(
    state: GlassState? = LocalGlassState.current,
    shape: Shape = RectangleShape,
    thickness: GlassThickness = GlassThickness.Regular
): Modifier {
    val colors = AppTheme.colors
    val tintAlpha = if (colors.isDark) thickness.tintAlphaDark else thickness.tintAlphaLight
    val base = colors.elevatedSurface
    if (state == null) {
        return this.clip(shape).background(base.copy(alpha = (tintAlpha + 0.2f).coerceAtMost(0.96f)))
    }
    val style = remember(base, tintAlpha, thickness) {
        HazeStyle(
            backgroundColor = base,
            tint = HazeTint(base.copy(alpha = tintAlpha)),
            blurRadius = thickness.blurDp.dp,
            noiseFactor = 0f
        )
    }
    return this.clip(shape).hazeEffect(state = state.haze, style = style)
}

package com.example.summitread.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.summitread.theme.AppSpacing
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType

/** Height of the always-visible inline bar (below the status bar). */
private val InlineBarHeight = 44.dp

/** Height reserved for the large title block under the inline bar. */
private val LargeTitleBlockHeight = 52.dp

/**
 * Tracks how far the large title has collapsed, by observing the content's scrolling through
 * nested scroll (it never consumes scroll, so it works with any scrollable). Saved across
 * configuration changes. If you scroll programmatically (scrollToItem), call [reset] or accept
 * that the bar re-syncs on the next manual scroll.
 */
@Stable
class LargeTitleState internal constructor(initialOffsetPx: Float = 0f) {
    internal var maxPx by mutableFloatStateOf(0f)
    var offsetPx by mutableFloatStateOf(initialOffsetPx)
        internal set

    /** 0 = large title fully shown, 1 = fully collapsed into the inline bar. */
    val collapseFraction: Float
        get() = if (maxPx <= 0f) 0f else (offsetPx / maxPx).coerceIn(0f, 1f)

    fun reset() {
        offsetPx = 0f
    }

    internal val connection = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            // Content scrolling up (consumed.y < 0) collapses the title.
            offsetPx = (offsetPx - consumed.y).coerceIn(0f, maxPx.coerceAtLeast(0f))
            return Offset.Zero
        }
    }

    companion object {
        val Saver: Saver<LargeTitleState, Float> =
            Saver(save = { it.offsetPx }, restore = { LargeTitleState(it) })
    }
}

@Composable
fun rememberLargeTitleState(): LargeTitleState =
    rememberSaveable(saver = LargeTitleState.Saver) { LargeTitleState() }

/**
 * iOS large-title screen: a big left-aligned title that scrolls away and turns into a centred
 * inline title on a frosted-glass bar. [content] receives padding that already clears the
 * bar(s); apply it as `contentPadding` of your scrolling list so content slides under the glass.
 *
 * Provides [LocalGlassState] to its slots, so a [GlassTabBar] passed as [bottomBar] blurs the content too.
 */
@Composable
fun LargeTitleScaffold(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    state: LargeTitleState = rememberLargeTitleState(),
    containerColor: Color = AppTheme.colors.groupedBackground,
    glassState: GlassState = rememberGlassState(),
    content: @Composable (contentPadding: PaddingValues) -> Unit
) {
    val density = LocalDensity.current
    val statusTop = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val navBottom = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    var bottomBarPx by remember { mutableIntStateOf(0) }
    val inheritedBar = LocalBottomBarHeight.current
    val bottomPadding =
        if (bottomBar != null) with(density) { bottomBarPx.toDp() } else maxOf(navBottom, inheritedBar)

    state.maxPx = with(density) { LargeTitleBlockHeight.toPx() }

    androidx.compose.runtime.CompositionLocalProvider(LocalGlassState provides glassState) {
        Box(modifier.fillMaxSize().background(containerColor)) {
            Box(
                Modifier
                    .fillMaxSize()
                    .nestedScroll(state.connection)
                    .glassSource(glassState)
            ) {
                content(
                    PaddingValues(
                        top = statusTop + InlineBarHeight + LargeTitleBlockHeight,
                        bottom = bottomPadding
                    )
                )
            }
            LargeTitleTopBar(
                title = title,
                collapseFraction = { state.collapseFraction },
                navigationIcon = navigationIcon,
                actions = actions,
                glassState = glassState,
                modifier = Modifier.align(Alignment.TopStart)
            )
            if (bottomBar != null) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .onSizeChanged { bottomBarPx = it.height }
                ) { bottomBar() }
            }
        }
    }
}

/**
 * The bar itself, for custom layouts. [collapseFraction] is 0 (large title visible) to 1
 * (inline title visible, glass background fully shown).
 */
@Composable
fun LargeTitleTopBar(
    title: String,
    collapseFraction: () -> Float,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    glassState: GlassState? = LocalGlassState.current
) {
    val colors = AppTheme.colors
    val density = LocalDensity.current
    val statusTop = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val barHeight = statusTop + InlineBarHeight
    val glassVisible by remember { derivedStateOf { collapseFraction() > 0.01f } }

    Box(modifier.fillMaxWidth()) {
        // Large title: lives under the bar, slides up with the content and fades out.
        Box(
            Modifier
                .padding(top = barHeight)
                .height(LargeTitleBlockHeight)
                .fillMaxWidth()
                .graphicsLayer {
                    val f = collapseFraction()
                    translationY = -f * LargeTitleBlockHeight.toPx()
                    alpha = (1f - f * 1.6f).coerceIn(0f, 1f)
                },
            contentAlignment = Alignment.BottomStart
        ) {
            Text(
                text = title,
                style = AppType.LargeTitle,
                color = colors.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = AppSpacing.md, end = AppSpacing.md, bottom = 6.dp)
                    .semantics { heading() }
            )
        }

        // Glass bar: fades in as the large title scrolls away.
        if (glassVisible) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .graphicsLayer { alpha = collapseFraction().coerceIn(0f, 1f) }
                    .glassBackground(glassState)
            ) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(HairlineWidth)
                        .background(colors.separator)
                )
            }
        }

        // Inline row: nav icon, centred title (fades in), actions.
        Box(
            Modifier
                .padding(top = statusTop)
                .fillMaxWidth()
                .height(InlineBarHeight)
        ) {
            Text(
                text = title,
                style = AppType.Headline,
                color = colors.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 96.dp)
                    .graphicsLayer { alpha = ((collapseFraction() - 0.6f) / 0.4f).coerceIn(0f, 1f) }
                    .clearAndSetSemantics { }
            )
            if (navigationIcon != null) {
                Box(
                    Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = AppSpacing.xs)
                ) { navigationIcon() }
            }
            Row(
                Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = AppSpacing.xs),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
    }
}

@LightDarkPreview
@Composable
private fun LargeTitleScaffoldPreview() {
    com.example.summitread.theme.SummitReadTheme {
        LargeTitleScaffold(
            title = "Library",
            actions = {
                IconCircleButton(Icons.Rounded.Add, "Add book", onClick = {})
                IconCircleButton(Icons.Rounded.MoreHoriz, "More", onClick = {})
            }
        ) { padding ->
            LazyColumn(contentPadding = padding) {
                items((1..20).toList()) { i ->
                    Text(
                        "Book $i",
                        style = AppType.Body,
                        color = AppTheme.colors.label,
                        modifier = Modifier.padding(AppSpacing.md)
                    )
                }
            }
        }
    }
}

package com.example.summitread.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.summitread.theme.AppMotion
import com.example.summitread.theme.AppTheme

/**
 * Scales the element down to [pressedScale] while [interactionSource] reports a press, with a
 * spring release. Use it on anything tappable instead of a ripple.
 */
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = AppMotion.PRESSED_SCALE,
    enabled: Boolean = true
): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = AppMotion.press(),
        label = "pressScale"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Ripple-free clickable with press-scale feedback: the iOS way to make a card, row or icon tappable.
 */
fun Modifier.pressableScale(
    onClick: () -> Unit,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    pressedScale: Float = AppMotion.PRESSED_SCALE
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    this
        .pressScale(interactionSource, pressedScale, enabled)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick
        )
}

/**
 * Soft iOS-style shadow for floating surfaces. Light theme only: in dark theme depth comes from
 * the lighter elevated fill, so this is a no-op there.
 */
fun Modifier.appShadow(
    elevation: Dp = 8.dp,
    shape: Shape = RectangleShape,
    ambientAlpha: Float = 0.06f,
    spotAlpha: Float = 0.10f
): Modifier = composed {
    if (AppTheme.isDark) {
        this
    } else {
        shadow(
            elevation = elevation,
            shape = shape,
            clip = false,
            ambientColor = Color.Black.copy(alpha = ambientAlpha),
            spotColor = Color.Black.copy(alpha = spotAlpha)
        )
    }
}

/** Thin line width: 0.5dp reads as a 1px hairline on most phones. */
val HairlineWidth: Dp = 0.5.dp

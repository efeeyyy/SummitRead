package com.example.summitread.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** iOS corner radii. (Compose has no continuous/squircle corner; RoundedCornerShape is the closest.) */
object AppShapes {
    val xSmall = RoundedCornerShape(6.dp)
    /** Badges, small chips, segmented-control inner pill. */
    val small = RoundedCornerShape(10.dp)
    /** Fields, small cards, buttons that are not capsules. */
    val medium = RoundedCornerShape(14.dp)
    /** Cards, grouped sections. */
    val large = RoundedCornerShape(20.dp)
    /** Bottom sheets (top corners only). */
    val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    /** Alerts and other floating panels. */
    val alert = RoundedCornerShape(14.dp)
    /** Capsule: buttons, tab bar, progress. */
    val pill = CircleShape
}

val SummitReadShapes = Shapes(
    extraSmall = AppShapes.xSmall,
    small = AppShapes.small,
    medium = AppShapes.medium,
    large = AppShapes.large,
    extraLarge = RoundedCornerShape(28.dp)
)

/** 4-pt spacing scale. */
object AppSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp

    /** Standard horizontal screen margin. */
    val screenHorizontal = 16.dp
}

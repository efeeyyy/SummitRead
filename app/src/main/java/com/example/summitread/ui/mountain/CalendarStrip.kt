package com.example.summitread.ui.mountain

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.summitread.data.db.ReadingSessionEntity
import com.example.summitread.theme.AppSpacing
import com.example.summitread.theme.AppTheme
import com.example.summitread.theme.AppType
import com.example.summitread.theme.rememberAppHaptics
import com.example.summitread.theme.tabularNumbers
import com.example.summitread.ui.components.pressScale
import androidx.compose.foundation.clickable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val PAST_DAYS = 30
private const val FUTURE_DAYS = 14

/**
 * 45-day strip of iOS-like day pills: the selected day is a solid tint circle, today has a soft tint disc and
 * tinted weekday, a dot marks days with logged pages. [today] comes from the ViewModel (start-of-day millis that
 * flips at midnight); future days are shown but disabled because pages cannot be logged ahead of time.
 */
@Composable
fun CalendarStrip(
    today: Long,
    selectedDate: Long,
    sessions: List<ReadingSessionEntity>,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val haptics = rememberAppHaptics()
    val days = remember(today) {
        val cal = Calendar.getInstance()
        (0..(PAST_DAYS + FUTURE_DAYS)).map { i ->
            cal.timeInMillis = today
            cal.add(Calendar.DAY_OF_YEAR, i - PAST_DAYS)
            startOfDay(cal.timeInMillis)
        }
    }

    val sessionDates = remember(sessions) {
        sessions.map { startOfDay(it.date) }.toSet()
    }

    val listState = rememberLazyListState()
    LaunchedEffect(today) {
        listState.animateScrollToItem(PAST_DAYS, scrollOffset = -200)
    }

    val dayOfWeekFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val dayNumberFormat = remember { SimpleDateFormat("d", Locale.getDefault()) }
    val fullFormat = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = AppSpacing.screenHorizontal),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
    ) {
        itemsIndexed(days, key = { _, day -> day }) { _, dayMillis ->
            val isToday = dayMillis == today
            val isFuture = dayMillis > today
            val isSelected = dayMillis == startOfDay(selectedDate)
            val hasSession = dayMillis in sessionDates
            val date = java.util.Date(dayMillis)

            val circleColor by animateColorAsState(
                targetValue = when {
                    isSelected -> colors.tint
                    isToday -> colors.tint.copy(alpha = if (colors.isDark) 0.24f else 0.14f)
                    else -> androidx.compose.ui.graphics.Color.Transparent
                },
                animationSpec = com.example.summitread.theme.AppMotion.snappy(),
                label = "dayCircle"
            )
            val numberColor = when {
                isSelected -> colors.onTint
                isToday -> colors.tint
                else -> colors.label
            }
            val interaction = remember { MutableInteractionSource() }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .width(46.dp)
                    .height(72.dp)
                    .alpha(if (isFuture) 0.38f else 1f)
                    .pressScale(interaction, enabled = !isFuture)
                    .semantics(mergeDescendants = true) {
                        contentDescription = buildString {
                            append(fullFormat.format(date))
                            if (isToday) append(", today")
                            if (hasSession) append(", pages logged")
                            if (isFuture) append(", not available yet")
                        }
                        selected = isSelected
                    }
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        enabled = !isFuture,
                        role = Role.Button
                    ) {
                        if (!isSelected) haptics.light()
                        onDateSelected(dayMillis)
                    }
            ) {
                Text(
                    text = dayOfWeekFormat.format(date).uppercase(),
                    style = AppType.Caption2,
                    color = if (isToday) colors.tint else colors.secondaryLabel
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(circleColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayNumberFormat.format(date),
                        style = AppType.Headline.tabularNumbers().copy(
                            fontWeight = if (isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = numberColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Dot: pages were logged that day
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(
                            if (hasSession) colors.tint else androidx.compose.ui.graphics.Color.Transparent,
                            CircleShape
                        )
                )
            }
        }
    }
}

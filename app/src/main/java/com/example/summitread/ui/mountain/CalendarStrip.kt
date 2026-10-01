package com.example.summitread.ui.mountain

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.summitread.data.db.ReadingSessionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val PAST_DAYS = 30
private const val FUTURE_DAYS = 14

/**
 * 45-day strip. [today] comes from the ViewModel (start-of-day millis that flips at midnight); future days
 * are shown but disabled because pages cannot be logged ahead of time.
 */
@Composable
fun CalendarStrip(
    today: Long,
    selectedDate: Long,
    sessions: List<ReadingSessionEntity>,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val contrast = mountainContrast()
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
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        itemsIndexed(days, key = { _, day -> day }) { _, dayMillis ->
            val isToday = dayMillis == today
            val isFuture = dayMillis > today
            val isSelected = dayMillis == startOfDay(selectedDate)
            val hasSession = dayMillis in sessionDates

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .width(48.dp)
                    .height(68.dp)
                    .alpha(if (isFuture) 0.4f else 1f)
                    .clip(MaterialTheme.shapes.small)
                    .then(
                        if (isToday) {
                            Modifier.background(
                                MaterialTheme.colorScheme.primary.copy(alpha = if (contrast.isDark) 0.28f else 0.15f),
                                MaterialTheme.shapes.small
                            )
                        } else Modifier
                    )
                    .then(
                        if (isSelected) {
                            Modifier.border(
                                if (isToday) 2.dp else 1.5.dp,
                                if (contrast.isDark) contrast.primaryText else MaterialTheme.colorScheme.primary,
                                MaterialTheme.shapes.small
                            )
                        } else Modifier
                    )
                    .semantics {
                        contentDescription = buildString {
                            append(fullFormat.format(java.util.Date(dayMillis)))
                            if (isToday) append(", today")
                            if (hasSession) append(", pages logged")
                            if (isFuture) append(", not available yet")
                        }
                        selected = isSelected
                    }
                    .clickable(enabled = !isFuture, role = Role.Button) { onDateSelected(dayMillis) }
            ) {
                Text(
                    text = dayOfWeekFormat.format(java.util.Date(dayMillis)).uppercase(),
                    fontSize = 10.sp,
                    color = contrast.secondaryText,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = dayNumberFormat.format(java.util.Date(dayMillis)),
                    fontSize = 16.sp,
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday && !contrast.isDark) MaterialTheme.colorScheme.primary
                    else contrast.primaryText
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (hasSession) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                } else {
                    Spacer(modifier = Modifier.size(6.dp))
                }
            }
        }
    }
}

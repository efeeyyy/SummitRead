package com.example.summitread.ui.mountain

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.summitread.data.db.ReadingSessionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CalendarStrip(
    selectedDate: Long,
    sessions: List<ReadingSessionEntity>,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { startOfDay(System.currentTimeMillis()) }
    val days = remember(today) {
        val cal = Calendar.getInstance()
        (0 until 45).map { i ->
            cal.timeInMillis = today
            cal.add(Calendar.DAY_OF_YEAR, i - 30)
            startOfDay(cal.timeInMillis)
        }
    }

    val sessionDates = remember(sessions) {
        sessions.map { startOfDay(it.date) }.toSet()
    }

    val todayIndex = 30 // today is at index 30 (after 30 past days)
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        listState.animateScrollToItem(todayIndex, scrollOffset = -200)
    }

    val dayOfWeekFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val dayNumberFormat = remember { SimpleDateFormat("d", Locale.getDefault()) }

    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        itemsIndexed(days) { _, dayMillis ->
            val isToday = dayMillis == today
            val isSelected = dayMillis == startOfDay(selectedDate)
            val hasSession = dayMillis in sessionDates

            val cal = Calendar.getInstance().apply { timeInMillis = dayMillis }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .width(48.dp)
                    .height(68.dp)
                    .clip(MaterialTheme.shapes.small)
                    .then(
                        if (isToday) {
                            Modifier.background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                MaterialTheme.shapes.small
                            )
                        } else Modifier
                    )
                    .then(
                        if (isSelected && !isToday) {
                            Modifier.border(
                                1.5.dp,
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.shapes.small
                            )
                        } else Modifier
                    )
                    .then(
                        if (isSelected && isToday) {
                            Modifier.border(
                                2.dp,
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.shapes.small
                            )
                        } else Modifier
                    )
                    .clickable { onDateSelected(dayMillis) }
            ) {
                Text(
                    text = dayOfWeekFormat.format(cal.time).uppercase(),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = dayNumberFormat.format(cal.time),
                    fontSize = 16.sp,
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
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

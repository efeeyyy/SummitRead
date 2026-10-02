package com.example.summitread.services

import android.content.Context
import com.example.summitread.platform.ReadingTimer
import com.example.summitread.platform.TimerState
import kotlinx.coroutines.flow.StateFlow

/** [ReadingTimer] of the Android app: a thin wrapper over the foreground [ReadingTimerService]. */
class AndroidReadingTimer(context: Context) : ReadingTimer {

    private val appContext = context.applicationContext

    override val state: StateFlow<TimerState> = ReadingTimerService.state

    override fun start(bookId: Long) = ReadingTimerService.start(appContext, bookId)

    override fun pause() = ReadingTimerService.pause(appContext)

    override fun resume() = ReadingTimerService.resume(appContext)

    override fun stop() = ReadingTimerService.stop(appContext)
}

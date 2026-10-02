package com.example.summitread.data

import kotlin.time.Clock

/** Epoch millis of "now" from the system clock (multiplatform replacement for System.currentTimeMillis()). */
fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

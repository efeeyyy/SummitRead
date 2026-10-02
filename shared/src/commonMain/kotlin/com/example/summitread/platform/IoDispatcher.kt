package com.example.summitread.platform

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Dispatcher for blocking disk work (database queries, file copies). `Dispatchers.IO` is not available from common
 * code on Kotlin/Native, so each platform provides it: Android uses `Dispatchers.IO`, iOS `Dispatchers.Default`.
 */
expect val ioDispatcher: CoroutineDispatcher

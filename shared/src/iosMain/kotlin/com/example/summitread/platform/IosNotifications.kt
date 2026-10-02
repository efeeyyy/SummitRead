package com.example.summitread.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** Thin wrapper over UNUserNotificationCenter local notifications (UNVERIFIED on a Mac). */
@OptIn(ExperimentalForeignApi::class)
internal object IosNotifications {

    private val center: UNUserNotificationCenter
        get() = UNUserNotificationCenter.currentNotificationCenter()

    /** Asks the user for permission (a no-op prompt after the first answer) and returns whether it was granted. */
    suspend fun requestAuthorization(): Boolean = suspendCoroutine { continuation ->
        center.requestAuthorizationWithOptions(UNAuthorizationOptionAlert or UNAuthorizationOptionBadge) { granted, _ ->
            continuation.resume(granted)
        }
    }

    /** Current permission state without prompting. */
    suspend fun isAuthorized(): Boolean = suspendCoroutine { continuation ->
        center.getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus
            continuation.resume(status == UNAuthorizationStatusAuthorized || status == UNAuthorizationStatusProvisional)
        }
    }

    /** Schedules (or replaces, same [identifier]) a one-shot notification [delaySeconds] from now. */
    fun schedule(identifier: String, title: String, body: String, delaySeconds: Double) {
        val content = UNMutableNotificationContent()
        content.setTitle(title)
        content.setBody(body)
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            timeInterval = delaySeconds.coerceAtLeast(1.0),
            repeats = false
        )
        val request = UNNotificationRequest.requestWithIdentifier(identifier, content = content, trigger = trigger)
        center.addNotificationRequest(request, withCompletionHandler = null)
    }

    fun cancel(identifiers: List<String>) {
        center.removePendingNotificationRequestsWithIdentifiers(identifiers)
    }
}

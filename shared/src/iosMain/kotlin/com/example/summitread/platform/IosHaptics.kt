package com.example.summitread.platform

import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UIKit.UISelectionFeedbackGenerator

/** UIKit feedback generators (UNVERIFIED on a Mac). */
class IosHaptics : Haptics {

    private val selection = UISelectionFeedbackGenerator()
    private val notification = UINotificationFeedbackGenerator()

    override fun light() = selection.selectionChanged()

    override fun toggle(on: Boolean) = selection.selectionChanged()

    override fun logged() =
        notification.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)

    override fun success() =
        notification.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)

    override fun warning() =
        notification.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeError)
}

package com.klt.pactping.notification

import platform.Foundation.NSDate
import platform.Foundation.NSError
import platform.Foundation.timeIntervalSince1970
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

/**
 * iOS implementation backed by `UNUserNotificationCenter`. Lazily requests
 * authorization on the first [schedule] call — denials are silent so a
 * follow-up schedule will simply do nothing (the in-app overdue UI still
 * fires regardless).
 *
 * Each pending notification uses the commitment id as its identifier, so
 * re-scheduling overwrites and [cancel] removes by id.
 */
class IosDeadlineNotifier : DeadlineNotifier {

  private val center = UNUserNotificationCenter.currentNotificationCenter()

  override fun schedule(id: String, deadlineEpochMillis: Long, goal: String) {
    val seconds = (deadlineEpochMillis - nowEpochMillis()) / 1000.0
    if (seconds <= 0.0) return

    requestAuthorizationIfNeeded { granted ->
      if (!granted) return@requestAuthorizationIfNeeded
      val content = UNMutableNotificationContent().apply {
        setTitle("Deadline blown.")
        setBody(if (goal.isBlank()) "Your promise is due." else goal)
      }
      val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
        timeInterval = seconds,
        repeats = false,
      )
      val request = UNNotificationRequest.requestWithIdentifier(
        identifier = id,
        content = content,
        trigger = trigger,
      )
      // Replace any in-flight request for the same id first.
      center.removePendingNotificationRequestsWithIdentifiers(listOf(id))
      center.addNotificationRequest(request) { _: NSError? -> /* best-effort */ }
    }
  }

  override fun cancel(id: String) {
    center.removePendingNotificationRequestsWithIdentifiers(listOf(id))
  }

  private fun requestAuthorizationIfNeeded(onResult: (Boolean) -> Unit) {
    val options = UNAuthorizationOptionAlert or
      UNAuthorizationOptionSound or
      UNAuthorizationOptionBadge
    center.requestAuthorizationWithOptions(options) { granted, _: NSError? ->
      onResult(granted)
    }
  }

  private fun nowEpochMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
}

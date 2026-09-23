package com.klt.pactping.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Wakes when AlarmManager elapses. Posts the "deadline blown" notification
 * for the commitment whose id is carried as [EXTRA_ID]. Idempotent — tapping
 * the alarm twice for the same id replaces the existing notification.
 *
 * The receiver is declared in [shared/src/androidMain/AndroidManifest.xml]
 * so it survives process death — the OS holds a wakelock long enough to
 * deliver the broadcast and post the notification.
 */
class DeadlineAlarmReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    val id = intent.getStringExtra(EXTRA_ID) ?: return
    val goal = intent.getStringExtra(EXTRA_GOAL).orEmpty()

    val nm = context.getSystemService(NotificationManager::class.java) ?: return
    // Channel creation is idempotent — safe to call every fire.
    ensureChannel(nm)
    if (!nm.areNotificationsEnabled()) return

    nm.notify(id.hashCode(), buildNotification(context, goal))
  }

  private fun buildNotification(context: Context, goal: String): Notification {
    val launchIntent = context.packageManager
      .getLaunchIntentForPackage(context.packageName)
      ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }

    val pending = launchIntent?.let {
      PendingIntent.getActivity(
        context,
        0,
        it,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
      )
    }

    return Notification.Builder(context, CHANNEL_ID)
      // System fallback — the library doesn't ship resources. The app can
      // override the channel later with a tasteful silhouette icon.
      .setSmallIcon(android.R.drawable.ic_dialog_alert)
      .setContentTitle("Deadline blown.")
      .setContentText(goal.ifBlank { "Your promise is due." })
      .setAutoCancel(true)
      .setContentIntent(pending)
      .setCategory(Notification.CATEGORY_REMINDER)
      .build()
  }

  private fun ensureChannel(nm: NotificationManager) {
    if (nm.getNotificationChannel(CHANNEL_ID) != null) return
    val channel = NotificationChannel(
      CHANNEL_ID,
      "Deadlines",
      NotificationManager.IMPORTANCE_HIGH,
    ).apply {
      description = "Fires when a Witness commitment's deadline arrives."
    }
    nm.createNotificationChannel(channel)
  }

  companion object {
    const val ACTION = "com.klt.pactping.notification.DEADLINE_FIRED"
    const val EXTRA_ID = "commitment_id"
    const val EXTRA_GOAL = "commitment_goal"
    const val CHANNEL_ID = "witness.deadlines"
  }
}

package com.klt.pactping.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

/**
 * Android implementation backed by [AlarmManager]. Uses
 * `setExactAndAllowWhileIdle` when SCHEDULE_EXACT_ALARM is granted (default
 * on API < 31, opt-in on 31+) and falls back to `setAndAllowWhileIdle`
 * otherwise so the alarm still fires — just within the OS's doze window.
 *
 * Each commitment id maps to a unique [PendingIntent] via the data URI so
 * scheduling the same id twice replaces, and cancel hits the right one.
 */
class AndroidDeadlineNotifier(private val context: Context) : DeadlineNotifier {

  private val alarmManager: AlarmManager? =
    context.getSystemService(AlarmManager::class.java)

  override fun schedule(id: String, deadlineEpochMillis: Long, goal: String) {
    val am = alarmManager ?: return
    if (deadlineEpochMillis <= System.currentTimeMillis()) return

    val pi = pendingIntent(id, goal)
    val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
    if (canExact) {
      am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, deadlineEpochMillis, pi)
    } else {
      am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, deadlineEpochMillis, pi)
    }
  }

  override fun cancel(id: String) {
    val am = alarmManager ?: return
    am.cancel(pendingIntent(id, goal = ""))
  }

  private fun pendingIntent(id: String, goal: String): PendingIntent {
    val intent = Intent(DeadlineAlarmReceiver.ACTION).apply {
      component = ComponentName(context, DeadlineAlarmReceiver::class.java)
      // Per-id data Uri keeps PendingIntents distinct. Extras don't
      // participate in equality, so cancel can reconstruct without them.
      data = Uri.parse("witness://commitment/$id")
      putExtra(DeadlineAlarmReceiver.EXTRA_ID, id)
      putExtra(DeadlineAlarmReceiver.EXTRA_GOAL, goal)
    }
    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    return PendingIntent.getBroadcast(context, 0, intent, flags)
  }
}

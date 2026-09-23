package com.klt.pactping.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Returns a stable `(to, body) -> Boolean` that launches the user's default SMS app
 * with the recipient + message body pre-filled. The user still has to tap send —
 * Witness deliberately doesn't auto-send so the witness sees a real message from
 * a real person, and we never need the SEND_SMS permission.
 *
 * Returns `true` if a messaging app was launched, `false` if none was available.
 */
@Composable
fun rememberSmsSender(): (to: String, body: String) -> Boolean {
  val context = LocalContext.current
  return remember(context) {
    { to, body -> context.sendSms(to, body) }
  }
}

private fun Context.sendSms(to: String, body: String): Boolean {
  val intent = Intent(Intent.ACTION_SENDTO).apply {
    data = Uri.parse("smsto:" + Uri.encode(to))
    putExtra("sms_body", body)
  }
  return try {
    startActivity(intent)
    true
  } catch (_: ActivityNotFoundException) {
    false
  }
}

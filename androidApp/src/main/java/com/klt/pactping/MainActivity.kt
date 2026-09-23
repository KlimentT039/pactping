package com.klt.pactping

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.klt.pactping.ui.PactPingApp
import com.klt.pactping.ui.theme.PactPingTheme

class MainActivity : ComponentActivity() {

  private val requestNotificationPermission =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* user can re-enable later in settings */ }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    maybeRequestNotificationPermission()
    setContent {
      PactPingTheme {
        PactPingApp()
      }
    }
  }

  /**
   * Ask once on first run on Android 13+. If the user denies, the
   * deadline notification scheduler silently no-ops — the in-app overdue
   * UI still does its job.
   */
  private fun maybeRequestNotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val granted = ContextCompat.checkSelfPermission(
      this,
      Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
    if (!granted) {
      requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }
}

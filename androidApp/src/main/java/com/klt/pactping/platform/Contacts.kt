package com.klt.pactping.platform

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.klt.pactping.domain.Witness

data class PickedContact(val name: String, val phoneNumber: String) {
  fun toWitness(idPrefix: String = "pc"): Witness =
    Witness(id = "$idPrefix-${phoneNumber.hashCode()}", name = name, phoneNumber = phoneNumber)
}

private class PickPhoneContact : ActivityResultContract<Unit, Uri?>() {
  override fun createIntent(context: Context, input: Unit): Intent =
    Intent(Intent.ACTION_PICK).apply {
      type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
    }

  override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
    if (resultCode == Activity.RESULT_OK) intent?.data else null
}

private fun ContentResolver.readContact(uri: Uri): PickedContact? {
  val projection = arrayOf(
    ContactsContract.CommonDataKinds.Phone.NUMBER,
    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
  )
  query(uri, projection, null, null, null)?.use { cursor ->
    if (cursor.moveToFirst()) {
      val number = cursor.getString(0) ?: return null
      val name = cursor.getString(1) ?: number
      return PickedContact(name = name, phoneNumber = number)
    }
  }
  return null
}

/**
 * Returns a stable lambda that, when invoked, launches the system contact picker.
 * Requests READ_CONTACTS at runtime if needed, then resolves the picked contact's
 * name and phone number and hands them to [onPicked]. No-ops if the user cancels.
 */
@Composable
fun rememberContactPicker(onPicked: (PickedContact) -> Unit): () -> Unit {
  val context = LocalContext.current

  val pickerLauncher = rememberLauncherForActivityResult(PickPhoneContact()) { uri ->
    if (uri != null) {
      context.contentResolver.readContact(uri)?.let(onPicked)
    }
  }

  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission(),
  ) { granted ->
    if (granted) pickerLauncher.launch(Unit)
  }

  return remember(context) {
    {
      val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_CONTACTS,
      ) == PackageManager.PERMISSION_GRANTED
      if (granted) pickerLauncher.launch(Unit)
      else permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }
  }
}

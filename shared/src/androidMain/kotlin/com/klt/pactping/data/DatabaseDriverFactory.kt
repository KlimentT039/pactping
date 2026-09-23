package com.klt.pactping.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.klt.pactping.db.PactPingDatabase

actual class DatabaseDriverFactory(private val context: Context) {
  actual fun create(): SqlDriver =
    AndroidSqliteDriver(
      schema = PactPingDatabase.Schema,
      context = context,
      name = DATABASE_NAME,
    )

  private companion object {
    const val DATABASE_NAME = "witness.db"
  }
}

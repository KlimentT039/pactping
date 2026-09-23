package com.klt.pactping.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.klt.pactping.db.PactPingDatabase

actual class DatabaseDriverFactory {
  actual fun create(): SqlDriver =
    NativeSqliteDriver(
      schema = PactPingDatabase.Schema,
      name = DATABASE_NAME,
    )

  private companion object {
    const val DATABASE_NAME = "witness.db"
  }
}

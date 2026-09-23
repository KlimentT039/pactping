package com.klt.pactping.data

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory {
  fun create(): SqlDriver
}

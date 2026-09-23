package com.klt.pactping.data

import app.cash.sqldelight.EnumColumnAdapter
import app.cash.sqldelight.db.SqlDriver
import com.klt.pactping.db.CommitmentRow
import com.klt.pactping.db.PactPingDatabase
import com.klt.pactping.domain.CommitmentStatus

fun createPactPingDatabase(driver: SqlDriver): PactPingDatabase = PactPingDatabase(
  driver = driver,
  CommitmentRowAdapter = CommitmentRow.Adapter(
    statusAdapter = EnumColumnAdapter<CommitmentStatus>(),
  ),
)

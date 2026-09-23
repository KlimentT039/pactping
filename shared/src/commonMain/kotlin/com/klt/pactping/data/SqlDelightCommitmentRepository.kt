package com.klt.pactping.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.klt.pactping.db.CommitmentRow
import com.klt.pactping.db.PactPingDatabase
import com.klt.pactping.domain.Commitment
import com.klt.pactping.domain.CommitmentRepository
import com.klt.pactping.domain.CommitmentStatus
import com.klt.pactping.domain.Record
import com.klt.pactping.domain.Witness
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class SqlDelightCommitmentRepository(
  private val database: PactPingDatabase,
  private val now: () -> Instant = { Clock.System.now() },
) : CommitmentRepository {

  private val queries = database.commitmentQueries

  override fun active(): Flow<List<Commitment>> =
    queries.selectActive()
      .asFlow()
      .mapToList(Dispatchers.Default)
      .map { rows -> rows.map(::toDomain) }

  override fun record(): Flow<Record> =
    queries.selectCounts()
      .asFlow()
      .mapToOne(Dispatchers.Default)
      .map { r ->
        Record(
          kept = (r.kept ?: 0L).toInt(),
          broken = (r.broken ?: 0L).toInt(),
        )
      }

  override fun byId(id: String): Flow<Commitment?> =
    queries.selectById(id)
      .asFlow()
      .mapToOneOrNull(Dispatchers.Default)
      .map { it?.let(::toDomain) }

  override fun all(): Flow<List<Commitment>> =
    queries.selectAll()
      .asFlow()
      .mapToList(Dispatchers.Default)
      .map { rows -> rows.map(::toDomain) }

  override suspend fun add(commitment: Commitment) {
    queries.insert(
      id = commitment.id,
      goal = commitment.goal,
      deadlineMillis = commitment.deadline.toEpochMilliseconds(),
      witnessId = commitment.witness.id,
      witnessName = commitment.witness.name,
      witnessPhone = commitment.witness.phoneNumber,
      confession = commitment.confession,
      status = commitment.status,
      resolvedAtMillis = commitment.resolvedAt?.toEpochMilliseconds(),
    )
  }

  override suspend fun markKept(id: String) {
    queries.updateStatus(
      status = CommitmentStatus.Kept,
      resolvedAtMillis = now().toEpochMilliseconds(),
      id = id,
    )
  }

  override suspend fun markMissed(id: String) {
    queries.updateStatus(
      status = CommitmentStatus.Missed,
      resolvedAtMillis = now().toEpochMilliseconds(),
      id = id,
    )
  }

  private fun toDomain(row: CommitmentRow): Commitment = Commitment(
    id = row.id,
    goal = row.goal,
    deadline = Instant.fromEpochMilliseconds(row.deadlineMillis),
    witness = Witness(
      id = row.witnessId,
      name = row.witnessName,
      phoneNumber = row.witnessPhone,
    ),
    confession = row.confession,
    status = row.status,
    resolvedAt = row.resolvedAtMillis?.let { Instant.fromEpochMilliseconds(it) },
  )
}

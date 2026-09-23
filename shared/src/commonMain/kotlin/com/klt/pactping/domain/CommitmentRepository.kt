package com.klt.pactping.domain

import kotlinx.coroutines.flow.Flow

interface CommitmentRepository {
  fun active(): Flow<List<Commitment>>
  fun record(): Flow<Record>
  fun byId(id: String): Flow<Commitment?>
  fun all(): Flow<List<Commitment>>
  suspend fun add(commitment: Commitment)
  suspend fun markKept(id: String)
  suspend fun markMissed(id: String)
}

data class Record(val kept: Int, val broken: Int) {
  val total: Int get() = kept + broken
  val keptRatio: Float get() = if (total == 0) 0f else kept.toFloat() / total
}

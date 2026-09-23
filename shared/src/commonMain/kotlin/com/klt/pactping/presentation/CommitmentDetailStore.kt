package com.klt.pactping.presentation

import com.klt.pactping.domain.Commitment
import com.klt.pactping.domain.CommitmentRepository
import com.klt.pactping.domain.Record
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

data class DetailUiState(
  val commitment: Commitment? = null,
  val record: Record = Record(0, 0),
  val countdown: CountdownParts = CountdownParts(0, 0, 0, 0),
  val isLoading: Boolean = true,
) {
  val timeToSpare: Duration?
    get() = commitment?.let { c ->
      val resolved = c.resolvedAt ?: return null
      c.deadline - resolved
    }
}

class CommitmentDetailStore(
  private val repo: CommitmentRepository,
  private val id: String,
  scope: CoroutineScope,
  private val now: () -> Instant = { Clock.System.now() },
) {
  val state: StateFlow<DetailUiState> = combine(
    repo.byId(id),
    repo.record(),
    ticker(),
  ) { commitment, record, _ ->
    val remaining = commitment?.let { it.deadline - now() } ?: Duration.ZERO
    DetailUiState(
      commitment = commitment,
      record = record,
      countdown = remaining.toCountdownParts(),
      isLoading = false,
    )
  }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

  suspend fun markKept() = repo.markKept(id)
  suspend fun markMissed() = repo.markMissed(id)
}

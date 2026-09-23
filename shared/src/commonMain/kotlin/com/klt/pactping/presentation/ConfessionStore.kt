package com.klt.pactping.presentation

import com.klt.pactping.domain.Commitment
import com.klt.pactping.domain.CommitmentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration

data class ConfessionUiState(
  val commitment: Commitment? = null,
  val elapsedSinceDeadline: Duration = Duration.ZERO,
  val isLoading: Boolean = true,
) {
  val isReady: Boolean get() = !isLoading && commitment != null
}

class ConfessionStore(
  private val repo: CommitmentRepository,
  private val id: String,
  scope: CoroutineScope,
  private val now: () -> Instant = { Clock.System.now() },
) {
  val state: StateFlow<ConfessionUiState> = combine(
    repo.byId(id),
    ticker(),
  ) { commitment, _ ->
    val elapsed = commitment?.let { now() - it.deadline } ?: Duration.ZERO
    ConfessionUiState(
      commitment = commitment,
      elapsedSinceDeadline = if (elapsed.isNegative()) Duration.ZERO else elapsed,
      isLoading = false,
    )
  }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), ConfessionUiState())

  suspend fun confirmMiss() = repo.markMissed(id)
}

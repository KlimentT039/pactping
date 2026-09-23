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
import kotlin.time.Duration.Companion.hours

data class HomeUiState(
  val record: Record = Record(0, 0),
  val active: List<CommitmentItem> = emptyList(),
  val isLoading: Boolean = true,
) {
  val isEmpty: Boolean get() = !isLoading && active.isEmpty()
}

data class CommitmentItem(
  val id: String,
  val goal: String,
  val witnessName: String,
  val witnessInitials: String,
  val countdown: CountdownParts,
  val isUrgent: Boolean,
  val isOverdue: Boolean,
)

class HomeStore(
  private val repo: CommitmentRepository,
  scope: CoroutineScope,
  private val now: () -> Instant = { Clock.System.now() },
) {
  val state: StateFlow<HomeUiState> = combine(
    repo.active(),
    repo.record(),
    ticker(),
  ) { active, record, _ ->
    val n = now()
    HomeUiState(
      record = record,
      active = active
        .sortedWith(
          compareByDescending<Commitment> { it.deadline < n }
            .thenBy { it.deadline },
        )
        .map { c ->
          val remaining = c.deadline - n
          val overdue = remaining.isNegative()
          val absParts = (if (overdue) -remaining else remaining).toCountdownParts()
          CommitmentItem(
            id = c.id,
            goal = c.goal,
            witnessName = c.witness.name,
            witnessInitials = c.witness.initials,
            countdown = absParts,
            isUrgent = !overdue && remaining < 24.hours,
            isOverdue = overdue,
          )
        },
      isLoading = false,
    )
  }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}

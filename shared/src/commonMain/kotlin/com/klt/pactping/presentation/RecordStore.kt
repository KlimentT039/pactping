package com.klt.pactping.presentation

import com.klt.pactping.domain.Commitment
import com.klt.pactping.domain.CommitmentRepository
import com.klt.pactping.domain.CommitmentStatus
import com.klt.pactping.domain.Record
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

enum class HistoryFilter { All, Kept, Broken }

data class WeekBucket(val kept: Int, val missed: Int) {
  val total: Int get() = kept + missed
}

data class HistoryEntry(
  val id: String,
  val goal: String,
  val isKept: Boolean,
  val resolvedAt: Instant,
  val wasLate: Boolean,
)

data class RecordUiState(
  val record: Record = Record(0, 0),
  val weeks: List<WeekBucket> = List(WEEKS) { WeekBucket(0, 0) },
  val history: List<HistoryEntry> = emptyList(),
  val streak: Int = 0,
  val filter: HistoryFilter = HistoryFilter.All,
  val isLoading: Boolean = true,
) {
  val hasHistory: Boolean get() = !isLoading && record.total > 0

  companion object {
    const val WEEKS = 8
  }
}

class RecordStore(
  private val repo: CommitmentRepository,
  scope: CoroutineScope,
  private val now: () -> Instant = { Clock.System.now() },
) {
  private val filterFlow = MutableStateFlow(HistoryFilter.All)

  val state: StateFlow<RecordUiState> = combine(
    repo.all(),
    repo.record(),
    filterFlow,
  ) { all, record, filter ->
    val resolved = all
      .filter { it.status != CommitmentStatus.Active && it.resolvedAt != null }
      .sortedByDescending { it.resolvedAt }

    val n = now()
    val weeks = computeWeekBuckets(resolved, n)
    val streak = resolved.takeWhile { it.status == CommitmentStatus.Kept }.size

    val filtered = when (filter) {
      HistoryFilter.All -> resolved
      HistoryFilter.Kept -> resolved.filter { it.status == CommitmentStatus.Kept }
      HistoryFilter.Broken -> resolved.filter { it.status == CommitmentStatus.Missed }
    }
    RecordUiState(
      record = record,
      weeks = weeks,
      history = filtered.map { it.toEntry() },
      streak = streak,
      filter = filter,
      isLoading = false,
    )
  }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), RecordUiState())

  fun setFilter(f: HistoryFilter) {
    filterFlow.value = f
  }

  private fun Commitment.toEntry() = HistoryEntry(
    id = id,
    goal = goal,
    isKept = status == CommitmentStatus.Kept,
    resolvedAt = resolvedAt ?: deadline,
    wasLate = resolvedAt != null && resolvedAt > deadline,
  )

  private fun computeWeekBuckets(commitments: List<Commitment>, now: Instant): List<WeekBucket> {
    val edges = (0..RecordUiState.WEEKS).map { i ->
      now - ((RecordUiState.WEEKS - i) * 7).days
    }
    return (0 until RecordUiState.WEEKS).map { i ->
      val start = edges[i]
      val end = edges[i + 1]
      val inWeek = commitments.filter { c ->
        val r = c.resolvedAt ?: return@filter false
        r >= start && r < end
      }
      WeekBucket(
        kept = inWeek.count { it.status == CommitmentStatus.Kept },
        missed = inWeek.count { it.status == CommitmentStatus.Missed },
      )
    }
  }
}

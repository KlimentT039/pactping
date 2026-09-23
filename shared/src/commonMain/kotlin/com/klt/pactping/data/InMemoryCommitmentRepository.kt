package com.klt.pactping.data

import com.klt.pactping.domain.Commitment
import com.klt.pactping.domain.CommitmentRepository
import com.klt.pactping.domain.CommitmentStatus
import com.klt.pactping.domain.Record
import com.klt.pactping.domain.Witness
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class InMemoryCommitmentRepository(
  private val now: () -> Instant = { Clock.System.now() },
) : CommitmentRepository {
  private val state = MutableStateFlow(seed(now()))

  override fun active(): Flow<List<Commitment>> =
    state.map { all -> all.filter { it.status == CommitmentStatus.Active } }

  override fun record(): Flow<Record> = state.map { all ->
    Record(
      kept = all.count { it.status == CommitmentStatus.Kept },
      broken = all.count { it.status == CommitmentStatus.Missed },
    )
  }

  override fun byId(id: String): Flow<Commitment?> =
    state.map { all -> all.firstOrNull { it.id == id } }

  override fun all(): Flow<List<Commitment>> = state

  override suspend fun add(commitment: Commitment) {
    state.value = state.value + commitment
  }

  override suspend fun markKept(id: String) {
    val resolved = now()
    state.value = state.value.map {
      if (it.id == id) it.copy(status = CommitmentStatus.Kept, resolvedAt = resolved) else it
    }
  }

  override suspend fun markMissed(id: String) {
    val resolved = now()
    state.value = state.value.map {
      if (it.id == id) it.copy(status = CommitmentStatus.Missed, resolvedAt = resolved) else it
    }
  }

  private companion object {
    fun seed(now: Instant): List<Commitment> {
      val mike = Witness("w-mike", "Mike Chen", "+1 (415) 555-0142")
      val alex = Witness("w-alex", "Alex Rivera", "+1 (628) 555-0193")
      val sarah = Witness("w-sarah", "Sarah Park", "+1 (415) 555-0188")

      val kept = List(13) { i ->
        val resolved = now - (i * 2 + 1).days
        Commitment(
          id = "k$i",
          goal = "Old win ${i + 1}",
          deadline = resolved + 3.hours,
          witness = if (i % 2 == 0) mike else sarah,
          confession = "—",
          status = CommitmentStatus.Kept,
          resolvedAt = resolved,
        )
      }
      val broken = List(3) { i ->
        val resolved = now - (i + 1).days
        Commitment(
          id = "m$i",
          goal = "Old miss ${i + 1}",
          deadline = resolved - 1.hours,
          witness = alex,
          confession = "—",
          status = CommitmentStatus.Missed,
          resolvedAt = resolved,
        )
      }
      val active = listOf(
        Commitment(
          id = "c1",
          goal = "Finish marketing deck",
          deadline = now + 2.days + 14.hours,
          witness = mike,
          confession = "Hey Mike — I told you I'd finish the marketing deck by Friday 8pm. " +
            "I didn't. No good excuse. Sorry for the noise.",
        ),
        Commitment(
          id = "c2",
          goal = "Pay back Alex (\$180)",
          deadline = now + 47.minutes,
          witness = alex,
          confession = "Hey Alex — I said I'd pay you back today. I didn't. Coming this week.",
        ),
      )
      return kept + broken + active
    }
  }
}

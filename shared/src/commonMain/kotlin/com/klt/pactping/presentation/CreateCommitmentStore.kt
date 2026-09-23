package com.klt.pactping.presentation

import com.klt.pactping.domain.Commitment
import com.klt.pactping.domain.CommitmentRepository
import com.klt.pactping.domain.Witness
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class CreateState(
  val goal: String = "",
  val deadline: Instant? = null,
  val witness: Witness? = null,
  val confession: String = "",
  // Reference time the deadline is validated against; refreshed on each edit.
  val now: Instant = Instant.DISTANT_PAST,
) {
  val hasGoal: Boolean get() = goal.trim().length >= 3
  // A deadline is only valid if it's still in the future — you can't commit
  // to something whose clock has already run out.
  val hasDeadline: Boolean get() = deadline != null && deadline > now
  val hasWitness: Boolean get() = witness != null
  val hasConfession: Boolean get() = confession.trim().length >= 10
  val canCommit: Boolean get() = hasGoal && hasDeadline && hasWitness && hasConfession
}

class CreateCommitmentStore(
  private val repo: CommitmentRepository,
  private val now: () -> Instant = { Clock.System.now() },
) {
  private val _state = MutableStateFlow(CreateState(now = now()))
  val state: StateFlow<CreateState> = _state.asStateFlow()

  fun setGoal(value: String) = _state.update { it.copy(goal = value, now = now()) }
  fun setDeadline(value: Instant) = _state.update { it.copy(deadline = value, now = now()) }
  fun setWitness(value: Witness) = _state.update { it.copy(witness = value, now = now()) }
  fun setConfession(value: String) = _state.update { it.copy(confession = value, now = now()) }

  fun reset() { _state.value = CreateState(now = now()) }

  @OptIn(ExperimentalUuidApi::class)
  suspend fun commit(): Boolean {
    val s = _state.value
    val deadline = s.deadline
    if (!s.hasGoal || !s.hasWitness || !s.hasConfession) return false
    // Authoritative guard against a past deadline, re-checked against live
    // time in case the user lingered on the summary until it elapsed.
    if (deadline == null || deadline <= now()) return false
    repo.add(
      Commitment(
        id = Uuid.random().toString(),
        goal = s.goal.trim(),
        deadline = deadline,
        witness = s.witness!!,
        confession = s.confession.trim(),
      ),
    )
    reset()
    return true
  }
}

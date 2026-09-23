package com.klt.pactping.notification

import com.klt.pactping.domain.Commitment
import com.klt.pactping.domain.CommitmentRepository
import com.klt.pactping.domain.Record
import kotlinx.coroutines.flow.Flow

/**
 * Repository decorator that fires [DeadlineNotifier] side-effects when a
 * commitment transitions in or out of the Active state. Keeps the UI and
 * the stores oblivious to scheduling.
 *
 * Wrap the SQLDelight repo with this at container construction:
 * `NotifyingCommitmentRepository(SqlDelightCommitmentRepository(db), notifier)`.
 */
class NotifyingCommitmentRepository(
  private val delegate: CommitmentRepository,
  private val notifier: DeadlineNotifier,
) : CommitmentRepository {

  override fun active(): Flow<List<Commitment>> = delegate.active()
  override fun record(): Flow<Record> = delegate.record()
  override fun byId(id: String): Flow<Commitment?> = delegate.byId(id)
  override fun all(): Flow<List<Commitment>> = delegate.all()

  override suspend fun add(commitment: Commitment) {
    delegate.add(commitment)
    notifier.schedule(
      id = commitment.id,
      deadlineEpochMillis = commitment.deadline.toEpochMilliseconds(),
      goal = commitment.goal,
    )
  }

  override suspend fun markKept(id: String) {
    delegate.markKept(id)
    notifier.cancel(id)
  }

  override suspend fun markMissed(id: String) {
    delegate.markMissed(id)
    notifier.cancel(id)
  }
}

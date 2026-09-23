package com.klt.pactping.notification

/**
 * Platform-agnostic scheduler for the single "your deadline is up" local
 * notification. Concrete impls live in `androidMain` and `iosMain`. Common
 * code stays oblivious — the [NotifyingCommitmentRepository] depends only
 * on this interface.
 *
 * Implementations should be best-effort and silent on permission denial —
 * callers don't want to be told about it twice, and the in-app overdue UI
 * is still the source of truth.
 *
 * Identifier semantics: [schedule]ing with an existing [id] replaces the
 * previously scheduled notification for that id (so editing a deadline is
 * a re-schedule, not a duplicate).
 */
interface DeadlineNotifier {
  /**
   * Schedule the deadline-blown notification for [id] at
   * [deadlineEpochMillis]. [goal] is shown as the notification body.
   *
   * If [deadlineEpochMillis] is in the past, implementations should
   * silently skip — we don't want to spam a "you missed" notification
   * when the user creates an already-late commitment.
   */
  fun schedule(id: String, deadlineEpochMillis: Long, goal: String)

  /** Cancel any pending notification scheduled for [id]. No-op if none. */
  fun cancel(id: String)
}

/** Used by tests and any tooling code that wants a wired-up repo without notifications. */
object NoopDeadlineNotifier : DeadlineNotifier {
  override fun schedule(id: String, deadlineEpochMillis: Long, goal: String) = Unit
  override fun cancel(id: String) = Unit
}

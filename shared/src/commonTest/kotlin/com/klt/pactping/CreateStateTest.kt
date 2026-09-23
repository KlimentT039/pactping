package com.klt.pactping

import com.klt.pactping.domain.Witness
import com.klt.pactping.presentation.CreateState
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class CreateStateTest {
  private val now = Instant.fromEpochSeconds(1_000_000)
  private val witness = Witness("w1", "Mike Chen", "+1 (415) 555-0142")

  private fun filled(deadline: Instant?) = CreateState(
    goal = "Finish the deck",
    deadline = deadline,
    witness = witness,
    confession = "I owned it and missed.",
    now = now,
  )

  @Test fun deadline_in_the_future_is_valid() {
    val s = filled(now + 1.seconds)
    assertTrue(s.hasDeadline)
    assertTrue(s.canCommit)
  }

  @Test fun deadline_in_the_past_is_invalid() {
    val s = filled(now - 1.seconds)
    assertFalse(s.hasDeadline)
    assertFalse(s.canCommit)
  }

  @Test fun deadline_exactly_now_is_invalid() {
    val s = filled(now)
    assertFalse(s.hasDeadline)
    assertFalse(s.canCommit)
  }

  @Test fun missing_deadline_is_invalid() {
    val s = filled(null)
    assertFalse(s.hasDeadline)
    assertFalse(s.canCommit)
  }
}

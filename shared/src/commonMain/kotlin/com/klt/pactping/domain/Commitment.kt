package com.klt.pactping.domain

import kotlinx.datetime.Instant

data class Commitment(
  val id: String,
  val goal: String,
  val deadline: Instant,
  val witness: Witness,
  val confession: String,
  val status: CommitmentStatus = CommitmentStatus.Active,
  val resolvedAt: Instant? = null,
)

enum class CommitmentStatus { Active, Kept, Missed }

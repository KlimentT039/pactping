package com.klt.pactping.presentation

import kotlin.time.Duration

data class CountdownParts(
  val days: Long,
  val hours: Long,
  val minutes: Long,
  val seconds: Long,
) {
  val isElapsed: Boolean get() = days == 0L && hours == 0L && minutes == 0L && seconds == 0L
}

fun Duration.toCountdownParts(): CountdownParts {
  val total = inWholeSeconds.coerceAtLeast(0)
  return CountdownParts(
    days = total / 86_400,
    hours = (total % 86_400) / 3_600,
    minutes = (total % 3_600) / 60,
    seconds = total % 60,
  )
}

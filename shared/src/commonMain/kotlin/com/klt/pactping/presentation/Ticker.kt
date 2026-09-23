package com.klt.pactping.presentation

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

fun ticker(interval: Duration = 1.seconds): Flow<Unit> = flow {
  while (true) {
    emit(Unit)
    delay(interval)
  }
}

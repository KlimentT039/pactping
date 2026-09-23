package com.klt.pactping.presentation

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Wraps the suspend funs on the per-screen stores with plain callbacks so
 * Swift can fire-and-forget without bridging Kotlin coroutines directly.
 * Mirrors [IosCreateBridge] for the rest of the app's mutating actions.
 */
object IosActionBridge {
  fun markKept(store: CommitmentDetailStore, onDone: () -> Unit) {
    MainScope().launch {
      store.markKept()
      onDone()
    }
  }

  fun markMissed(store: CommitmentDetailStore, onDone: () -> Unit) {
    MainScope().launch {
      store.markMissed()
      onDone()
    }
  }

  fun confirmMiss(store: ConfessionStore, onDone: () -> Unit) {
    MainScope().launch {
      store.confirmMiss()
      onDone()
    }
  }
}

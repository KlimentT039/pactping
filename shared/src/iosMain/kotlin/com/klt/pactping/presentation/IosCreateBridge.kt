package com.klt.pactping.presentation

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * Wraps the [CreateCommitmentStore.commit] suspend function with a plain
 * callback so Swift can call it without SKIE / async bridging.
 */
object IosCreateBridge {
  fun commit(store: CreateCommitmentStore, onResult: (Boolean) -> Unit) {
    MainScope().launch {
      onResult(store.commit())
    }
  }
}

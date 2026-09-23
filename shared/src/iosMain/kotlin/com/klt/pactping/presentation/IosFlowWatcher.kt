package com.klt.pactping.presentation

import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Token returned by [FlowWatcher.watch]. Swift code calls [cancel] when the
 * observing view disappears.
 */
class FlowSubscription internal constructor(private val job: Job) {
  fun cancel() {
    job.cancel()
  }
}

/**
 * Swift-friendly adapter around a [Flow]. Without SKIE we can't expose
 * Flow directly to Swift comfortably, so [watch] takes a plain closure
 * that fires for each emission on the main dispatcher.
 */
class FlowWatcher<T : Any> internal constructor(private val flow: Flow<T>) {
  private val scope = MainScope()

  fun watch(onEach: (T) -> Unit): FlowSubscription {
    val job = scope.launch {
      flow.collect { onEach(it) }
    }
    return FlowSubscription(job)
  }

  fun close() {
    scope.cancel()
  }
}

/**
 * Per-store factory object so Swift sees stable, named functions instead of
 * inferred Kotlin extensions.
 */
object IosFlowWatchers {
  fun home(store: HomeStore): FlowWatcher<HomeUiState> = FlowWatcher(store.state)
  fun create(store: CreateCommitmentStore): FlowWatcher<CreateState> = FlowWatcher(store.state)
  fun detail(store: CommitmentDetailStore): FlowWatcher<DetailUiState> = FlowWatcher(store.state)
  fun confession(store: ConfessionStore): FlowWatcher<ConfessionUiState> = FlowWatcher(store.state)
  fun record(store: RecordStore): FlowWatcher<RecordUiState> = FlowWatcher(store.state)
}

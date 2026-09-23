package com.klt.pactping

import com.klt.pactping.data.DatabaseDriverFactory
import com.klt.pactping.data.SqlDelightCommitmentRepository
import com.klt.pactping.data.createPactPingDatabase
import com.klt.pactping.db.PactPingDatabase
import com.klt.pactping.domain.CommitmentRepository
import com.klt.pactping.notification.DeadlineNotifier
import com.klt.pactping.notification.IosDeadlineNotifier
import com.klt.pactping.notification.NotifyingCommitmentRepository
import com.klt.pactping.presentation.CommitmentDetailStore
import com.klt.pactping.presentation.ConfessionStore
import com.klt.pactping.presentation.CreateCommitmentStore
import com.klt.pactping.presentation.HomeStore
import com.klt.pactping.presentation.RecordStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Single application-scoped container for the iOS app to grab everything
 * from one place. Mirrors what [com.klt.pactping.ui.PactPingApp] wires up
 * on Android. Keep me alive for the lifetime of the app — owning the
 * SQLite driver here means the DB connection survives across SwiftUI
 * scenes.
 */
class IosAppContainer {
  private val driver = DatabaseDriverFactory().create()
  val database: PactPingDatabase = createPactPingDatabase(driver)

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  private val notifier: DeadlineNotifier = IosDeadlineNotifier()
  val commitments: CommitmentRepository =
    NotifyingCommitmentRepository(SqlDelightCommitmentRepository(database), notifier)

  val homeStore: HomeStore = HomeStore(commitments, scope)
  val createStore: CreateCommitmentStore = CreateCommitmentStore(commitments)
  val recordStore: RecordStore = RecordStore(commitments, scope)

  /** Per-id store for the Commitment Detail / Success screens. */
  fun detailStore(id: String): CommitmentDetailStore =
    CommitmentDetailStore(commitments, id, scope)

  /** Per-id store for the Confession screen. */
  fun confessionStore(id: String): ConfessionStore =
    ConfessionStore(commitments, id, scope)
}

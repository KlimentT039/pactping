package com.klt.pactping.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.klt.pactping.data.DatabaseDriverFactory
import com.klt.pactping.data.SqlDelightCommitmentRepository
import com.klt.pactping.data.createPactPingDatabase
import com.klt.pactping.domain.CommitmentRepository
import com.klt.pactping.notification.AndroidDeadlineNotifier
import com.klt.pactping.notification.NotifyingCommitmentRepository
import com.klt.pactping.presentation.CreateCommitmentStore
import com.klt.pactping.presentation.HomeStore
import com.klt.pactping.ui.confession.ConfessionRoute
import com.klt.pactping.ui.create.CreateFlow
import com.klt.pactping.ui.detail.CommitmentDetailRoute
import com.klt.pactping.ui.home.HomeScreen
import com.klt.pactping.ui.record.RecordRoute
import com.klt.pactping.ui.success.SuccessRoute

object Routes {
  const val Home = "home"
  const val Create = "create"
  const val Detail = "detail/{id}"
  const val Success = "success/{id}"
  const val Confession = "confession/{id}"
  const val Record = "record"

  fun detail(id: String) = "detail/$id"
  fun success(id: String) = "success/$id"
  fun confession(id: String) = "confession/$id"
}

@Composable
fun PactPingApp() {
  val appContext = LocalContext.current.applicationContext
  val database = remember(appContext) {
    createPactPingDatabase(DatabaseDriverFactory(appContext).create())
  }
  val notifier = remember(appContext) { AndroidDeadlineNotifier(appContext) }
  val repo: CommitmentRepository = remember(database, notifier) {
    NotifyingCommitmentRepository(SqlDelightCommitmentRepository(database), notifier)
  }

  val nav = rememberNavController()
  val scope = rememberCoroutineScope()

  val homeStore = remember(repo, scope) { HomeStore(repo, scope) }
  val createStore = remember(repo) { CreateCommitmentStore(repo) }

  NavHost(navController = nav, startDestination = Routes.Home) {
    composable(Routes.Home) {
      val state by homeStore.state.collectAsState()
      HomeScreen(
        state = state,
        onCreateClick = { nav.navigate(Routes.Create) },
        onCommitmentClick = { id, isOverdue ->
          if (isOverdue) nav.navigate(Routes.confession(id))
          else nav.navigate(Routes.detail(id))
        },
        onRecordClick = { nav.navigate(Routes.Record) },
      )
    }
    composable(Routes.Record) {
      RecordRoute(
        repo = repo,
        onBack = { nav.popBackStack() },
      )
    }
    composable(Routes.Create) {
      CreateFlow(
        store = createStore,
        onClose = { nav.popBackStack() },
        onComplete = { nav.popBackStack() },
      )
    }
    composable(
      route = Routes.Detail,
      arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) { entry ->
      val id = entry.arguments?.getString("id") ?: return@composable
      CommitmentDetailRoute(
        repo = repo,
        id = id,
        onBack = { nav.popBackStack() },
        onMarkedKept = {
          nav.popBackStack(Routes.Home, inclusive = false)
          nav.navigate(Routes.success(id))
        },
      )
    }
    composable(
      route = Routes.Success,
      arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) { entry ->
      val id = entry.arguments?.getString("id") ?: return@composable
      SuccessRoute(
        repo = repo,
        id = id,
        onClose = { nav.popBackStack(Routes.Home, inclusive = false) },
        onBrag = { nav.popBackStack(Routes.Home, inclusive = false) },
      )
    }
    composable(
      route = Routes.Confession,
      arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) { entry ->
      val id = entry.arguments?.getString("id") ?: return@composable
      ConfessionRoute(
        repo = repo,
        id = id,
        onLater = { nav.popBackStack(Routes.Home, inclusive = false) },
        onSent = { nav.popBackStack(Routes.Home, inclusive = false) },
      )
    }
  }
}

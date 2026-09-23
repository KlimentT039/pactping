package com.klt.pactping.ui.confession

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klt.pactping.R
import com.klt.pactping.domain.CommitmentRepository
import com.klt.pactping.platform.rememberSmsSender
import com.klt.pactping.presentation.ConfessionStore
import com.klt.pactping.presentation.ConfessionUiState
import com.klt.pactping.presentation.formatDeadline
import com.klt.pactping.ui.theme.PactPingThemeAccess
import kotlinx.coroutines.launch
import kotlin.time.Duration

@Composable
fun ConfessionRoute(
  repo: CommitmentRepository,
  id: String,
  onLater: () -> Unit,
  onSent: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  val store = remember(repo, id, scope) { ConfessionStore(repo, id, scope) }
  val state by store.state.collectAsState()
  val sendSms = rememberSmsSender()

  ConfessionScreen(
    state = state,
    onLater = onLater,
    onSend = {
      val commitment = state.commitment ?: return@ConfessionScreen
      scope.launch {
        store.confirmMiss()
        sendSms(commitment.witness.phoneNumber, commitment.confession)
        onSent()
      }
    },
  )
}

@Composable
private fun ConfessionScreen(
  state: ConfessionUiState,
  onLater: () -> Unit,
  onSend: () -> Unit,
) {
  val c = PactPingThemeAccess.colors
  val commitment = state.commitment

  Surface(color = c.bg, modifier = Modifier.fillMaxSize()) {
    Column(
      Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.systemBars)
        .padding(horizontal = 22.dp, vertical = 12.dp),
    ) {
      TopBar(onLater = onLater)
      Spacer(Modifier.height(20.dp))
      Box(
        Modifier
          .weight(1f)
          .fillMaxWidth(),
      ) {
        if (commitment != null) {
          Content(
            witnessName = commitment.witness.name,
            confession = commitment.confession,
            deadlineLabel = commitment.deadline.formatDeadline(),
            initials = commitment.witness.initials,
            elapsed = state.elapsedSinceDeadline,
          )
        }
      }
      Spacer(Modifier.height(12.dp))
      SendButton(onClick = onSend, enabled = state.isReady)
      Spacer(Modifier.height(4.dp))
      TextButton(onClick = onLater, modifier = Modifier.fillMaxWidth()) {
        Text(
          stringResource(R.string.confession_link_later),
          color = c.muted,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@Composable
private fun TopBar(onLater: () -> Unit) {
  val c = PactPingThemeAccess.colors
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Surface(
      onClick = onLater,
      color = c.surface,
      shape = CircleShape,
      modifier = Modifier.size(36.dp),
    ) {
      Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text("×", color = c.ink, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
      }
    }
    Box(
      Modifier
        .clip(RoundedCornerShape(999.dp))
        .background(c.missSoft)
        .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
      Text(
        stringResource(R.string.confession_pill_blown).uppercase(),
        color = c.miss,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.2.sp,
      )
    }
    Spacer(Modifier.width(36.dp))
  }
}

@Composable
private fun Content(
  witnessName: String,
  confession: String,
  deadlineLabel: String,
  initials: String,
  elapsed: Duration,
) {
  val c = PactPingThemeAccess.colors
  Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
        stringResource(R.string.confession_eyebrow).uppercase(),
        color = c.miss,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.4.sp,
      )
      Text(
        stringResource(R.string.confession_title),
        color = c.ink,
        fontWeight = FontWeight.Black,
        fontSize = 40.sp,
        lineHeight = 42.sp,
        letterSpacing = (-1.2).sp,
      )
      Text(
        stringResource(
          R.string.confession_subtitle,
          deadlineLabel,
          elapsedLabel(elapsed),
        ),
        color = c.muted,
        fontSize = 14.sp,
        lineHeight = 20.sp,
      )
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
          Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(avatarColor(initials)),
          contentAlignment = Alignment.Center,
        ) {
          Text(initials, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
        }
        Text(
          stringResource(R.string.confession_to_label, witnessName),
          color = c.ink,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
        )
      }
      Surface(
        color = c.miss,
        shape = RoundedCornerShape(22.dp, 22.dp, 6.dp, 22.dp),
      ) {
        Text(
          confession,
          color = Color.White,
          fontSize = 14.sp,
          lineHeight = 20.sp,
          fontWeight = FontWeight.Medium,
          modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
        )
      }
      Text(
        stringResource(R.string.confession_meta),
        color = c.muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
      )
    }
  }
}

@Composable
private fun SendButton(onClick: () -> Unit, enabled: Boolean) {
  val c = PactPingThemeAccess.colors
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    contentPadding = PaddingValues(vertical = 24.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = c.miss,
      contentColor = Color.White,
      disabledContainerColor = c.hairline,
      disabledContentColor = c.muted,
    ),
  ) {
    Text(
      stringResource(R.string.confession_cta_send),
      fontWeight = FontWeight.Black,
      fontSize = 17.sp,
      letterSpacing = 0.4.sp,
    )
  }
}

@Composable
private fun elapsedLabel(d: Duration): String {
  val days = d.inWholeDays
  val hours = d.inWholeHours
  val minutes = d.inWholeMinutes.coerceAtLeast(1)
  return when {
    days >= 1 -> stringResource(R.string.confession_elapsed_days, days.toInt())
    hours >= 1 -> stringResource(R.string.confession_elapsed_hours, hours.toInt())
    else -> stringResource(R.string.confession_elapsed_minutes, minutes.toInt())
  }
}

private fun avatarColor(seed: String): Color {
  val palette = listOf(
    Color(0xFF3F6CFF),
    Color(0xFFE0457B),
    Color(0xFF22A06B),
    Color(0xFFB36F00),
    Color(0xFF6E56CF),
  )
  return palette[(seed.hashCode() and 0x7FFFFFFF) % palette.size]
}

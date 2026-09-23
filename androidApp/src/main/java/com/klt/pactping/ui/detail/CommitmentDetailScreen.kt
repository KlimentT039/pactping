package com.klt.pactping.ui.detail

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.klt.pactping.presentation.CommitmentDetailStore
import com.klt.pactping.presentation.CountdownParts
import com.klt.pactping.presentation.DetailUiState
import com.klt.pactping.presentation.formatDeadline
import com.klt.pactping.ui.theme.PactPingThemeAccess
import kotlinx.coroutines.launch

@Composable
fun CommitmentDetailRoute(
  repo: CommitmentRepository,
  id: String,
  onBack: () -> Unit,
  onMarkedKept: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  val store = remember(repo, id, scope) { CommitmentDetailStore(repo, id, scope) }
  val state by store.state.collectAsState()

  CommitmentDetailScreen(
    state = state,
    onBack = onBack,
    onMarkDone = {
      scope.launch {
        store.markKept()
        onMarkedKept()
      }
    },
  )
}

@Composable
private fun CommitmentDetailScreen(
  state: DetailUiState,
  onBack: () -> Unit,
  onMarkDone: () -> Unit,
) {
  val c = PactPingThemeAccess.colors
  Surface(color = c.bg, modifier = Modifier.fillMaxSize()) {
    Column(
      Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.systemBars)
        .padding(horizontal = 22.dp, vertical = 12.dp),
    ) {
      TopBar(onBack = onBack, showPill = state.commitment != null)
      Spacer(Modifier.height(8.dp))
      Box(Modifier.weight(1f)) {
        if (state.commitment == null) {
          NotFound()
        } else {
          Content(state = state)
        }
      }
      if (state.commitment != null) {
        Spacer(Modifier.height(12.dp))
        MarkDoneButton(onClick = onMarkDone)
      }
    }
  }
}

@Composable
private fun TopBar(onBack: () -> Unit, showPill: Boolean) {
  val c = PactPingThemeAccess.colors
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Surface(
      onClick = onBack,
      color = c.surface,
      shape = CircleShape,
      modifier = Modifier.size(36.dp),
    ) {
      Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text("←", color = c.ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
      }
    }
    if (showPill) {
      OnTheLinePill()
    }
    Spacer(Modifier.width(36.dp))
  }
}

@Composable
private fun OnTheLinePill() {
  val c = PactPingThemeAccess.colors
  Box(
    Modifier
      .clip(RoundedCornerShape(999.dp))
      .background(c.surfaceAlt)
      .padding(horizontal = 10.dp, vertical = 5.dp),
  ) {
    Text(
      stringResource(R.string.home_pill_on_the_line).uppercase(),
      color = c.ink,
      fontSize = 10.sp,
      fontWeight = FontWeight.ExtraBold,
      letterSpacing = 1.2.sp,
    )
  }
}

@Composable
private fun Content(state: DetailUiState) {
  val c = PactPingThemeAccess.colors
  val commitment = state.commitment ?: return
  Column(
    Modifier.verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(
        stringResource(R.string.detail_countdown_label).uppercase(),
        color = c.strike,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 2.2.sp,
      )
      HeroCountdown(parts = state.countdown)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        commitment.goal,
        color = c.ink,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.3).sp,
      )
      Text(
        stringResource(R.string.detail_due_prefix, commitment.deadline.formatDeadline()),
        color = c.muted,
        fontSize = 12.sp,
      )
    }
    WitnessCard(name = commitment.witness.name, initials = commitment.witness.initials)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      val firstName = commitment.witness.name.split(" ").firstOrNull()
        ?: stringResource(R.string.create_summary_no_witness)
      Text(
        stringResource(R.string.detail_if_miss_x_gets, firstName).uppercase(),
        color = c.muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 2.sp,
      )
      Surface(color = c.ink, shape = RoundedCornerShape(22.dp, 22.dp, 22.dp, 6.dp)) {
        Text(
          commitment.confession,
          color = c.bg,
          fontSize = 14.sp,
          lineHeight = 20.sp,
          fontWeight = FontWeight.Medium,
          modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
        )
      }
    }
  }
}

@Composable
private fun HeroCountdown(parts: CountdownParts) {
  val c = PactPingThemeAccess.colors
  val dayUnit = stringResource(R.string.unit_days_short)
  val hourUnit = stringResource(R.string.unit_hours_short)
  val minUnit = stringResource(R.string.unit_minutes_short)
  val secUnit = stringResource(R.string.unit_seconds_short)
  Column {
    Row(verticalAlignment = Alignment.Bottom) {
      when {
        parts.days > 0 -> {
          HeroNum(parts.days.toString()); HeroUnit(dayUnit)
          Spacer(Modifier.width(16.dp))
          HeroNum(parts.hours.toString()); HeroUnit(hourUnit)
        }
        parts.hours > 0 -> {
          HeroNum(parts.hours.toString()); HeroUnit(hourUnit)
          Spacer(Modifier.width(16.dp))
          HeroNum(parts.minutes.toString().padStart(2, '0')); HeroUnit(minUnit)
        }
        else -> {
          HeroNum(parts.minutes.toString()); HeroUnit(minUnit)
        }
      }
    }
    Spacer(Modifier.height(2.dp))
    val subText = buildString {
      if (parts.days > 0 || parts.hours > 0) {
        append(parts.minutes.toString().padStart(2, '0'))
        append(minUnit)
        append("  ")
      }
      append(parts.seconds.toString().padStart(2, '0'))
      append(secUnit)
    }
    Text(
      subText,
      color = c.muted,
      fontWeight = FontWeight.Black,
      fontSize = 30.sp,
      letterSpacing = (-1).sp,
    )
  }
}

@Composable
private fun HeroNum(text: String) {
  val c = PactPingThemeAccess.colors
  Text(
    text,
    color = c.ink,
    fontWeight = FontWeight.Black,
    fontSize = 72.sp,
    letterSpacing = (-3).sp,
  )
}

@Composable
private fun HeroUnit(text: String) {
  val c = PactPingThemeAccess.colors
  Text(
    text,
    color = c.muted,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    modifier = Modifier.padding(start = 4.dp, bottom = 12.dp),
  )
}

@Composable
private fun WitnessCard(name: String, initials: String) {
  val c = PactPingThemeAccess.colors
  Surface(
    color = c.surface,
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(1.dp, c.hairline),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
          Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(avatarColor(initials)),
          contentAlignment = Alignment.Center,
        ) {
          Text(initials, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
        }
        Column {
          Text(name, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 15.sp)
          Text(stringResource(R.string.detail_witness_sub), color = c.muted, fontSize = 12.sp)
        }
      }
      Text(
        stringResource(R.string.detail_eyes_on).uppercase(),
        color = c.strike,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.6.sp,
      )
    }
  }
}

@Composable
private fun MarkDoneButton(onClick: () -> Unit) {
  val c = PactPingThemeAccess.colors
  Button(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    contentPadding = PaddingValues(vertical = 20.dp),
    colors = ButtonDefaults.buttonColors(containerColor = c.strike, contentColor = c.onStrike),
  ) {
    Text(stringResource(R.string.detail_mark_done), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
  }
}

@Composable
private fun NotFound() {
  val c = PactPingThemeAccess.colors
  Column(
    Modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Text(
      stringResource(R.string.detail_not_found_title),
      color = c.ink,
      fontWeight = FontWeight.Black,
      fontSize = 28.sp,
    )
    Spacer(Modifier.height(6.dp))
    Text(
      stringResource(R.string.detail_not_found_subtitle),
      color = c.muted,
      fontSize = 14.sp,
    )
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

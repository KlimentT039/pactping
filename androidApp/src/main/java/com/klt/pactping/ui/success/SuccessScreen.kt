package com.klt.pactping.ui.success

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
import com.klt.pactping.domain.Record
import com.klt.pactping.platform.rememberSmsSender
import com.klt.pactping.presentation.CommitmentDetailStore
import com.klt.pactping.presentation.DetailUiState
import com.klt.pactping.ui.theme.PactPingThemeAccess
import kotlin.time.Duration

@Composable
fun SuccessRoute(
  repo: CommitmentRepository,
  id: String,
  onClose: () -> Unit,
  onBrag: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  val store = remember(repo, id, scope) { CommitmentDetailStore(repo, id, scope) }
  val state by store.state.collectAsState()
  val sendSms = rememberSmsSender()
  val bragTemplate = stringResource(R.string.success_brag_template)

  SuccessScreen(
    state = state,
    onClose = onClose,
    onBrag = { witnessName ->
      val commitment = state.commitment ?: return@SuccessScreen
      val firstName = witnessName.split(" ").firstOrNull() ?: witnessName
      val body = bragTemplate.format(firstName, commitment.goal)
      sendSms(commitment.witness.phoneNumber, body)
      onBrag()
    },
  )
}

@Composable
private fun SuccessScreen(
  state: DetailUiState,
  onClose: () -> Unit,
  onBrag: (witnessName: String) -> Unit,
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
      TopBar(onClose = onClose)
      Box(
        Modifier
          .weight(1f)
          .fillMaxWidth(),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
          GreenBurst()
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              stringResource(R.string.success_logged).uppercase(),
              color = c.kept,
              fontSize = 11.sp,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 2.4.sp,
            )
            Text(
              stringResource(R.string.success_title),
              color = c.ink,
              fontWeight = FontWeight.Black,
              fontSize = 36.sp,
              letterSpacing = (-1).sp,
            )
            if (commitment != null) {
              Spacer(Modifier.height(2.dp))
              Text(
                commitment.goal,
                color = c.muted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
              )
              Text(
                spareLabel(state.timeToSpare),
                color = c.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
              )
            }
          }
          RecordCard(record = state.record)
        }
      }

      Spacer(Modifier.height(12.dp))
      if (commitment != null) {
        BragButton(witnessName = commitment.witness.name, onClick = { onBrag(commitment.witness.name) })
        Spacer(Modifier.height(4.dp))
      }
      TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
        Text(
          stringResource(R.string.success_just_close),
          color = c.muted,
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}

@Composable
private fun TopBar(onClose: () -> Unit) {
  val c = PactPingThemeAccess.colors
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Surface(
      onClick = onClose,
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
        .background(c.keptSoft)
        .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
      Text(
        stringResource(R.string.success_pill_kept).uppercase(),
        color = c.kept,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.2.sp,
      )
    }
    Spacer(Modifier.width(36.dp))
  }
}

@Composable
private fun GreenBurst() {
  val c = PactPingThemeAccess.colors
  Box(
    Modifier
      .size(120.dp)
      .clip(CircleShape)
      .background(c.keptSoft),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      Modifier
        .size(96.dp)
        .clip(CircleShape)
        .background(c.kept),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        "✓",
        color = Color.White,
        fontWeight = FontWeight.Black,
        fontSize = 56.sp,
      )
    }
  }
}

@Composable
private fun RecordCard(record: Record) {
  val c = PactPingThemeAccess.colors
  Surface(
    color = c.surface,
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(1.dp, c.hairline),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
      ) {
        Column {
          Text(
            stringResource(R.string.home_label_kept).uppercase(),
            color = c.muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.2.sp,
          )
          Text(
            record.kept.toString(),
            color = c.kept,
            fontWeight = FontWeight.Black,
            fontSize = 28.sp,
            letterSpacing = (-1).sp,
          )
        }
        Column(horizontalAlignment = Alignment.End) {
          Text(
            stringResource(R.string.home_label_broken).uppercase(),
            color = c.muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 2.2.sp,
          )
          Text(
            record.broken.toString(),
            color = c.miss,
            fontWeight = FontWeight.Black,
            fontSize = 28.sp,
            letterSpacing = (-1).sp,
          )
        }
      }
      Box(
        Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(c.miss),
      ) {
        Box(
          Modifier
            .fillMaxWidth(record.keptRatio.coerceIn(0f, 1f))
            .height(6.dp)
            .background(c.kept),
        )
      }
    }
  }
}

@Composable
private fun BragButton(witnessName: String, onClick: () -> Unit) {
  val c = PactPingThemeAccess.colors
  val firstName = witnessName.split(" ").firstOrNull() ?: witnessName
  Button(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    contentPadding = PaddingValues(vertical = 20.dp),
    colors = ButtonDefaults.buttonColors(containerColor = c.kept, contentColor = Color.White),
  ) {
    Text(
      stringResource(R.string.success_send_brag, firstName),
      fontWeight = FontWeight.ExtraBold,
      fontSize = 16.sp,
    )
  }
}

@Composable
private fun spareLabel(timeToSpare: Duration?): String {
  if (timeToSpare == null || timeToSpare.isNegative()) {
    return stringResource(R.string.success_done_in_time)
  }
  val days = timeToSpare.inWholeDays
  val hours = timeToSpare.inWholeHours
  val minutes = timeToSpare.inWholeMinutes
  return when {
    days >= 1 -> stringResource(R.string.success_done_with_spare_days, days.toInt())
    hours >= 1 -> stringResource(R.string.success_done_with_spare_hours, hours.toInt())
    minutes >= 1 -> stringResource(R.string.success_done_with_spare_minutes, minutes.toInt())
    else -> stringResource(R.string.success_done_in_time)
  }
}

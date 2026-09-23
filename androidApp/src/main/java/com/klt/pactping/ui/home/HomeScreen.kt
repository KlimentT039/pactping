package com.klt.pactping.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klt.pactping.R
import com.klt.pactping.domain.Record
import com.klt.pactping.presentation.CommitmentItem
import com.klt.pactping.presentation.CountdownParts
import com.klt.pactping.presentation.HomeUiState
import com.klt.pactping.ui.theme.PactPingTheme
import com.klt.pactping.ui.theme.PactPingThemeAccess

@Composable
fun HomeScreen(
  state: HomeUiState,
  onCreateClick: () -> Unit = {},
  onCommitmentClick: (id: String, isOverdue: Boolean) -> Unit = { _, _ -> },
  onRecordClick: () -> Unit = {},
) {
  val c = PactPingThemeAccess.colors
  Surface(color = c.bg, modifier = Modifier.fillMaxSize()) {
    Box(Modifier.fillMaxSize()) {
      if (state.isEmpty) {
        EmptyState(
          modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
        )
      } else {
        ActiveList(
          state = state,
          onCommitmentClick = onCommitmentClick,
          onRecordClick = onRecordClick,
          modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        )
      }
      Fab(
        onClick = onCreateClick,
        modifier = Modifier
          .align(Alignment.BottomCenter)
          .windowInsetsPadding(WindowInsets.systemBars)
          .padding(horizontal = 22.dp, vertical = 16.dp)
          .fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun ActiveList(
  state: HomeUiState,
  onCommitmentClick: (id: String, isOverdue: Boolean) -> Unit,
  onRecordClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val c = PactPingThemeAccess.colors
  LazyColumn(
    modifier = modifier,
    contentPadding = PaddingValues(
      start = 22.dp, end = 22.dp,
      top = 12.dp, bottom = 120.dp,
    ),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    item { TopBar() }
    item { RecordCard(record = state.record, onClick = onRecordClick) }
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Label(stringResource(R.string.home_active_count, state.active.size))
        Text(
          stringResource(R.string.home_soonest_first),
          color = c.muted,
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp,
        )
      }
    }
    items(state.active, key = { it.id }) { item ->
      CommitmentCard(item = item, onClick = { onCommitmentClick(item.id, item.isOverdue) })
    }
  }
}

@Composable
private fun TopBar() {
  val c = PactPingThemeAccess.colors
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      buildAnnotatedString {
        withStyle(SpanStyle(color = c.ink, fontWeight = FontWeight.Black, letterSpacing = 5.sp)) {
          append(stringResource(R.string.app_name).uppercase())
        }
        withStyle(SpanStyle(color = c.strike, fontWeight = FontWeight.Black)) {
          append(".")
        }
      },
      fontSize = 16.sp,
    )
    Box(
      Modifier
        .size(36.dp)
        .clip(CircleShape)
        .background(c.surface),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        "⋯",
        color = c.ink,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 18.sp,
      )
    }
  }
}

@Composable
private fun Label(text: String) {
  val c = PactPingThemeAccess.colors
  Text(
    text.uppercase(),
    color = c.muted,
    fontWeight = FontWeight.ExtraBold,
    fontSize = 11.sp,
    letterSpacing = 2.2.sp,
  )
}

@Composable
private fun RecordCard(record: Record, onClick: () -> Unit) {
  val c = PactPingThemeAccess.colors
  Surface(
    onClick = onClick,
    color = c.surface,
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(1.dp, c.hairline),
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
          Label(stringResource(R.string.home_label_kept))
          Text(
            record.kept.toString(),
            color = c.kept,
            fontWeight = FontWeight.Black,
            fontSize = 28.sp,
            letterSpacing = (-1).sp,
          )
        }
        Column(horizontalAlignment = Alignment.End) {
          Label(stringResource(R.string.home_label_broken))
          Text(
            record.broken.toString(),
            color = c.miss,
            fontWeight = FontWeight.Black,
            fontSize = 28.sp,
            letterSpacing = (-1).sp,
          )
        }
      }
      KeptRatioBar(ratio = record.keptRatio)
      val pct = (record.keptRatio * 100).toInt()
      Text(
        if (record.total == 0) stringResource(R.string.home_record_empty)
        else stringResource(R.string.home_record_percent_kept, pct),
        color = c.muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
      )
    }
  }
}

@Composable
private fun KeptRatioBar(ratio: Float) {
  val c = PactPingThemeAccess.colors
  Box(
    Modifier
      .fillMaxWidth()
      .height(6.dp)
      .clip(RoundedCornerShape(4.dp))
      .background(c.miss),
  ) {
    Box(
      Modifier
        .fillMaxWidth(ratio.coerceIn(0f, 1f))
        .height(6.dp)
        .background(c.kept),
    )
  }
}

@Composable
private fun CommitmentCard(item: CommitmentItem, onClick: () -> Unit) {
  val c = PactPingThemeAccess.colors
  Surface(
    onClick = onClick,
    color = c.surface,
    shape = RoundedCornerShape(24.dp),
    border = BorderStroke(1.dp, c.hairline),
  ) {
    Column(
      Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
      ) {
        Text(
          item.goal,
          color = c.ink,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 18.sp,
          letterSpacing = (-0.3).sp,
          lineHeight = 22.sp,
          modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(10.dp))
        StatusPill(isOverdue = item.isOverdue, isUrgent = item.isUrgent)
      }
      Countdown(parts = item.countdown, accent = item.isUrgent || item.isOverdue)
      WitnessRow(initials = item.witnessInitials, name = item.witnessName)
    }
  }
}

@Composable
private fun StatusPill(isOverdue: Boolean, isUrgent: Boolean) {
  val c = PactPingThemeAccess.colors
  val (bg, fg, text) = when {
    isOverdue -> Triple(c.missSoft, c.miss, stringResource(R.string.home_pill_overdue))
    isUrgent -> Triple(c.dueSoft, c.dueOn, stringResource(R.string.home_pill_due_soon))
    else -> Triple(c.surfaceAlt, c.ink, stringResource(R.string.home_pill_on_the_line))
  }
  Box(
    Modifier
      .clip(RoundedCornerShape(999.dp))
      .background(bg)
      .padding(horizontal = 10.dp, vertical = 5.dp),
  ) {
    Text(
      text.uppercase(),
      color = fg,
      fontSize = 10.sp,
      fontWeight = FontWeight.ExtraBold,
      letterSpacing = 1.2.sp,
    )
  }
}

@Composable
private fun Countdown(parts: CountdownParts, accent: Boolean) {
  val c = PactPingThemeAccess.colors
  val numberColor = if (accent) c.miss else c.ink
  val unitColor = c.muted
  val dayUnit = stringResource(R.string.unit_days_short)
  val hourUnit = stringResource(R.string.unit_hours_short)
  val minUnit = stringResource(R.string.unit_minutes_short)
  Row(verticalAlignment = Alignment.Bottom) {
    when {
      parts.days > 0 -> {
        BigNum(parts.days.toString(), numberColor)
        UnitLabel(dayUnit, unitColor)
        Spacer(Modifier.width(10.dp))
        BigNum(parts.hours.toString(), numberColor)
        UnitLabel(hourUnit, unitColor)
      }
      parts.hours > 0 -> {
        BigNum(parts.hours.toString(), numberColor)
        UnitLabel(hourUnit, unitColor)
        Spacer(Modifier.width(10.dp))
        BigNum(parts.minutes.toString().padStart(2, '0'), numberColor)
        UnitLabel(minUnit, unitColor)
      }
      else -> {
        BigNum(parts.minutes.toString(), numberColor)
        UnitLabel(minUnit, unitColor)
      }
    }
  }
}

@Composable
private fun BigNum(text: String, color: Color) {
  Text(
    text,
    color = color,
    fontWeight = FontWeight.Black,
    fontSize = 44.sp,
    letterSpacing = (-2).sp,
  )
}

@Composable
private fun UnitLabel(text: String, color: Color) {
  Text(
    text,
    color = color,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
  )
}

@Composable
private fun WitnessRow(initials: String, name: String) {
  val c = PactPingThemeAccess.colors
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
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
      stringResource(R.string.home_witness_watching, name),
      color = c.muted,
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
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

@Composable
private fun Fab(onClick: () -> Unit, modifier: Modifier = Modifier) {
  val c = PactPingThemeAccess.colors
  Button(
    onClick = onClick,
    modifier = modifier,
    shape = RoundedCornerShape(20.dp),
    colors = ButtonDefaults.buttonColors(containerColor = c.strike, contentColor = c.onStrike),
    contentPadding = PaddingValues(vertical = 18.dp),
  ) {
    Text(
      stringResource(R.string.home_fab_make_promise),
      fontWeight = FontWeight.ExtraBold,
      fontSize = 15.sp,
      letterSpacing = 0.2.sp,
    )
  }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
  val c = PactPingThemeAccess.colors
  Column(
    modifier = modifier.padding(horizontal = 32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Box(
      Modifier
        .size(120.dp)
        .clip(CircleShape)
        .background(c.surface)
        .border(1.dp, c.hairline, CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      EyeGlyph(color = c.ink, modifier = Modifier.size(60.dp))
    }
    Spacer(Modifier.height(28.dp))
    Text(
      stringResource(R.string.home_empty_title),
      color = c.ink,
      fontWeight = FontWeight.Black,
      fontSize = 32.sp,
      lineHeight = 34.sp,
      letterSpacing = (-1).sp,
    )
    Spacer(Modifier.height(10.dp))
    Text(
      stringResource(R.string.home_empty_subtitle),
      color = c.muted,
      fontSize = 14.sp,
      lineHeight = 20.sp,
    )
  }
}

@Composable
private fun EyeGlyph(color: Color, modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    val strokePx = (w * 0.06f).coerceAtLeast(2f)
    // Outer almond (eye outline) — two arcs meeting at the corners
    val path = androidx.compose.ui.graphics.Path().apply {
      moveTo(0f, h / 2f)
      quadraticBezierTo(w / 2f, -h * 0.15f, w, h / 2f)
      quadraticBezierTo(w / 2f, h * 1.15f, 0f, h / 2f)
      close()
    }
    drawPath(
      path = path,
      color = color,
      style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokePx),
    )
    // Pupil
    drawCircle(
      color = color,
      radius = w * 0.16f,
      center = androidx.compose.ui.geometry.Offset(w / 2f, h / 2f),
    )
  }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun HomePreviewActive() {
  PactPingTheme {
    HomeScreen(state = sampleState())
  }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun HomePreviewEmpty() {
  PactPingTheme {
    HomeScreen(
      state = HomeUiState(
        isLoading = false,
        active = emptyList(),
        record = Record(0, 0),
      ),
    )
  }
}

private fun sampleState() = HomeUiState(
  isLoading = false,
  record = Record(kept = 13, broken = 3),
  active = listOf(
    CommitmentItem(
      id = "c1",
      goal = "Finish marketing deck",
      witnessName = "Mike Chen",
      witnessInitials = "MC",
      countdown = CountdownParts(days = 2, hours = 14, minutes = 22, seconds = 0),
      isUrgent = false,
      isOverdue = false,
    ),
    CommitmentItem(
      id = "c2",
      goal = "Pay back Alex (\$180)",
      witnessName = "Alex Rivera",
      witnessInitials = "AR",
      countdown = CountdownParts(days = 0, hours = 0, minutes = 47, seconds = 0),
      isUrgent = true,
      isOverdue = false,
    ),
    CommitmentItem(
      id = "c3",
      goal = "Submit Q1 review",
      witnessName = "Sarah Park",
      witnessInitials = "SP",
      countdown = CountdownParts(days = 0, hours = 1, minutes = 12, seconds = 0),
      isUrgent = false,
      isOverdue = true,
    ),
  ),
)

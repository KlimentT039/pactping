package com.klt.pactping.ui.record

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klt.pactping.R
import com.klt.pactping.domain.CommitmentRepository
import com.klt.pactping.presentation.HistoryEntry
import com.klt.pactping.presentation.HistoryFilter
import com.klt.pactping.presentation.RecordStore
import com.klt.pactping.presentation.RecordUiState
import com.klt.pactping.presentation.WeekBucket
import com.klt.pactping.presentation.formatHistoryDate
import com.klt.pactping.ui.theme.PactPingThemeAccess

@Composable
fun RecordRoute(repo: CommitmentRepository, onBack: () -> Unit) {
  val scope = rememberCoroutineScope()
  val store = remember(repo, scope) { RecordStore(repo, scope) }
  val state by store.state.collectAsState()
  RecordScreen(
    state = state,
    onBack = onBack,
    onFilterChange = store::setFilter,
  )
}

@Composable
private fun RecordScreen(
  state: RecordUiState,
  onBack: () -> Unit,
  onFilterChange: (HistoryFilter) -> Unit,
) {
  val c = PactPingThemeAccess.colors
  Surface(color = c.bg, modifier = Modifier.fillMaxSize()) {
    Column(
      Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.systemBars)
        .padding(horizontal = 22.dp, vertical = 12.dp),
    ) {
      TopBar(onBack = onBack)
      Spacer(Modifier.height(20.dp))
      if (!state.hasHistory) {
        EmptyHistory(modifier = Modifier.weight(1f))
      } else {
        LazyColumn(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
          item { HeroNumbers(state) }
          item { WeeksChart(state) }
          item { FilterSegments(filter = state.filter, onChange = onFilterChange) }
          items(state.history, key = { it.id }) { entry -> HistoryRow(entry) }
          item { Spacer(Modifier.height(8.dp)) }
        }
      }
    }
  }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
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
    Text(
      stringResource(R.string.record_title).uppercase(),
      color = c.muted,
      fontSize = 11.sp,
      fontWeight = FontWeight.ExtraBold,
      letterSpacing = 2.4.sp,
    )
    Spacer(Modifier.width(36.dp))
  }
}

@Composable
private fun HeroNumbers(state: RecordUiState) {
  val c = PactPingThemeAccess.colors
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text(
      stringResource(R.string.record_alltime).uppercase(),
      color = c.muted,
      fontSize = 11.sp,
      fontWeight = FontWeight.ExtraBold,
      letterSpacing = 2.2.sp,
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Bottom,
    ) {
      Column {
        Text(
          state.record.kept.toString(),
          color = c.kept,
          fontWeight = FontWeight.Black,
          fontSize = 56.sp,
          letterSpacing = (-2).sp,
        )
        Text(
          stringResource(R.string.home_label_kept).uppercase(),
          color = c.kept,
          fontSize = 11.sp,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 2.2.sp,
        )
      }
      Text(
        "/",
        color = c.muted,
        fontSize = 32.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-1.5).sp,
        modifier = Modifier.padding(bottom = 14.dp),
      )
      Column(horizontalAlignment = Alignment.End) {
        Text(
          state.record.broken.toString(),
          color = c.miss,
          fontWeight = FontWeight.Black,
          fontSize = 56.sp,
          letterSpacing = (-2).sp,
        )
        Text(
          stringResource(R.string.home_label_broken).uppercase(),
          color = c.miss,
          fontSize = 11.sp,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 2.2.sp,
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
          .fillMaxWidth(state.record.keptRatio.coerceIn(0f, 1f))
          .height(6.dp)
          .background(c.kept),
      )
    }
  }
}

@Composable
private fun WeeksChart(state: RecordUiState) {
  val c = PactPingThemeAccess.colors
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        stringResource(R.string.record_8weeks).uppercase(),
        color = c.muted,
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 2.2.sp,
      )
      if (state.streak > 0) {
        Text(
          stringResource(R.string.record_streak, state.streak),
          color = c.kept,
          fontSize = 12.sp,
          fontWeight = FontWeight.ExtraBold,
        )
      }
    }
    BarChart(weeks = state.weeks, keptColor = c.kept, missColor = c.miss)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      for (i in 1..RecordUiState.WEEKS) {
        Text(
          stringResource(R.string.record_week_short, i),
          color = c.muted,
          fontSize = 10.sp,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 1.sp,
          modifier = Modifier.width(28.dp),
          textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
      }
    }
  }
}

@Composable
private fun BarChart(
  weeks: List<WeekBucket>,
  keptColor: androidx.compose.ui.graphics.Color,
  missColor: androidx.compose.ui.graphics.Color,
) {
  val maxTotal = (weeks.maxOfOrNull { it.total } ?: 1).coerceAtLeast(1)
  Canvas(
    modifier = Modifier
      .fillMaxWidth()
      .height(110.dp),
  ) {
    val n = weeks.size.coerceAtLeast(1)
    val gap = 6.dp.toPx()
    val colWidth = (size.width - gap * (n - 1)) / n
    val cornerRadius = 4.dp.toPx()
    val gapBetween = 2.dp.toPx()

    weeks.forEachIndexed { i, w ->
      if (w.total == 0) return@forEachIndexed
      val x = i * (colWidth + gap)
      val totalHeight = (w.total.toFloat() / maxTotal) * size.height
      val keptHeight = (w.kept.toFloat() / w.total) * totalHeight
      val missHeight = (w.missed.toFloat() / w.total) * totalHeight

      if (keptHeight > 0f) {
        drawRoundRect(
          color = keptColor,
          topLeft = Offset(x, size.height - keptHeight),
          size = Size(colWidth, keptHeight),
          cornerRadius = CornerRadius(cornerRadius, cornerRadius),
        )
      }
      if (missHeight > 0f) {
        val topY = size.height - keptHeight - missHeight - if (keptHeight > 0f) gapBetween else 0f
        drawRoundRect(
          color = missColor,
          topLeft = Offset(x, topY),
          size = Size(colWidth, missHeight),
          cornerRadius = CornerRadius(cornerRadius, cornerRadius),
        )
      }
    }
  }
}

@Composable
private fun FilterSegments(filter: HistoryFilter, onChange: (HistoryFilter) -> Unit) {
  val c = PactPingThemeAccess.colors
  Surface(
    color = c.surfaceAlt,
    shape = RoundedCornerShape(14.dp),
  ) {
    Row(
      modifier = Modifier.padding(4.dp),
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Segment(
        text = stringResource(R.string.record_filter_all),
        active = filter == HistoryFilter.All,
        onClick = { onChange(HistoryFilter.All) },
      )
      Segment(
        text = stringResource(R.string.record_filter_kept),
        active = filter == HistoryFilter.Kept,
        onClick = { onChange(HistoryFilter.Kept) },
      )
      Segment(
        text = stringResource(R.string.record_filter_broken),
        active = filter == HistoryFilter.Broken,
        onClick = { onChange(HistoryFilter.Broken) },
      )
    }
  }
}

@Composable
private fun Segment(text: String, active: Boolean, onClick: () -> Unit) {
  val c = PactPingThemeAccess.colors
  Surface(
    onClick = onClick,
    color = if (active) c.ink else androidx.compose.ui.graphics.Color.Transparent,
    shape = RoundedCornerShape(10.dp),
  ) {
    Box(
      Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text,
        color = if (active) c.bg else c.muted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
      )
    }
  }
}

@Composable
private fun HistoryRow(entry: HistoryEntry) {
  val c = PactPingThemeAccess.colors
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(
      Modifier
        .size(8.dp)
        .clip(CircleShape)
        .background(if (entry.isKept) c.kept else c.miss),
    )
    Text(
      entry.goal,
      color = c.ink,
      fontSize = 14.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier.weight(1f),
    )
    val dateLabel = entry.resolvedAt.formatHistoryDate()
    Text(
      if (entry.wasLate) stringResource(R.string.record_history_late, dateLabel) else dateLabel,
      color = c.muted,
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold,
    )
  }
  Box(
    Modifier
      .fillMaxWidth()
      .height(1.dp)
      .background(c.hairline),
  )
}

@Composable
private fun EmptyHistory(modifier: Modifier = Modifier) {
  val c = PactPingThemeAccess.colors
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    Text(
      stringResource(R.string.record_empty_title),
      color = c.ink,
      fontWeight = FontWeight.Black,
      fontSize = 28.sp,
      letterSpacing = (-0.8).sp,
    )
    Spacer(Modifier.height(8.dp))
    Text(
      stringResource(R.string.record_empty_subtitle),
      color = c.muted,
      fontSize = 14.sp,
      lineHeight = 20.sp,
      textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
  }
}

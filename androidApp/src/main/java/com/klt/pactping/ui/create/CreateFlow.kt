@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.klt.pactping.ui.create

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.klt.pactping.R
import com.klt.pactping.domain.Witness
import com.klt.pactping.platform.rememberContactPicker
import com.klt.pactping.platform.rememberSmsSender
import com.klt.pactping.presentation.CreateCommitmentStore
import com.klt.pactping.presentation.CreateState
import com.klt.pactping.presentation.formatDateShort
import com.klt.pactping.presentation.formatDeadline
import com.klt.pactping.presentation.formatTimeShort
import com.klt.pactping.ui.theme.PactPingThemeAccess
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days

private object CreateRoutes {
  const val Goal = "create/goal"
  const val Deadline = "create/deadline"
  const val Witness = "create/witness"
  const val Confession = "create/confession"
  const val Summary = "create/summary"

  val ordered = listOf(Goal, Deadline, Witness, Confession, Summary)
}

@Composable
fun CreateFlow(
  store: CreateCommitmentStore,
  onClose: () -> Unit,
  onComplete: () -> Unit,
) {
  val state by store.state.collectAsState()
  val nav = rememberNavController()
  val scope = rememberCoroutineScope()
  val sendSms = rememberSmsSender()
  val headsUpTemplate = stringResource(R.string.create_witness_headsup_template)

  NavHost(navController = nav, startDestination = CreateRoutes.Goal) {
    composable(CreateRoutes.Goal) {
      GoalStep(
        state = state,
        onChange = store::setGoal,
        onBack = onClose,
        onNext = { nav.navigate(CreateRoutes.Deadline) },
      )
    }
    composable(CreateRoutes.Deadline) {
      DeadlineStep(
        state = state,
        onChange = store::setDeadline,
        onBack = { nav.popBackStack() },
        onNext = { nav.navigate(CreateRoutes.Witness) },
      )
    }
    composable(CreateRoutes.Witness) {
      WitnessStep(
        state = state,
        onSelect = store::setWitness,
        onBack = { nav.popBackStack() },
        onNext = { nav.navigate(CreateRoutes.Confession) },
      )
    }
    composable(CreateRoutes.Confession) {
      ConfessionStep(
        state = state,
        onChange = store::setConfession,
        onBack = { nav.popBackStack() },
        onNext = { nav.navigate(CreateRoutes.Summary) },
      )
    }
    composable(CreateRoutes.Summary) {
      SummaryStep(
        state = state,
        onBack = { nav.popBackStack() },
        onEdit = { nav.popBackStack(CreateRoutes.Goal, inclusive = false) },
        onCommit = {
          // Snapshot the values now — commit() resets the store right after.
          val snapshot = state
          scope.launch {
            if (store.commit()) {
              val w = snapshot.witness
              val d = snapshot.deadline
              if (w != null && d != null) {
                val body = headsUpTemplate.format(snapshot.goal, d.formatDeadline())
                sendSms(w.phoneNumber, body)
              }
              onComplete()
            }
          }
        },
      )
    }
  }
}

// ---------- Shared chrome ----------

@Composable
private fun StepScaffold(
  stepIndex: Int,
  onBack: () -> Unit,
  cta: @Composable () -> Unit,
  content: @Composable () -> Unit,
) {
  val c = PactPingThemeAccess.colors
  Surface(color = c.bg, modifier = Modifier.fillMaxSize()) {
    Column(
      Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.systemBars)
        .padding(horizontal = 22.dp, vertical = 12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        IconButtonCircle(label = if (stepIndex == 0) "×" else "←", onClick = onBack)
        StepIndicator(stepIndex = stepIndex, total = CreateRoutes.ordered.size)
        Spacer(Modifier.width(36.dp))
      }
      Spacer(Modifier.height(20.dp))
      Box(
        Modifier
          .weight(1f)
          .fillMaxWidth(),
      ) { content() }
      Spacer(Modifier.height(12.dp))
      cta()
    }
  }
}

@Composable
private fun IconButtonCircle(label: String, onClick: () -> Unit) {
  val c = PactPingThemeAccess.colors
  Surface(
    onClick = onClick,
    color = c.surface,
    shape = CircleShape,
    modifier = Modifier.size(36.dp),
  ) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
      Text(label, color = c.ink, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }
  }
}

@Composable
private fun StepIndicator(stepIndex: Int, total: Int) {
  val c = PactPingThemeAccess.colors
  Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    for (i in 0 until total) {
      val active = i == stepIndex
      Box(
        Modifier
          .height(4.dp)
          .width(if (active) 36.dp else 22.dp)
          .clip(RoundedCornerShape(2.dp))
          .background(if (active) c.ink else c.hairline),
      )
    }
  }
}

@Composable
private fun StepHeader(stepIndex: Int, title: String, subtitle: String? = null) {
  val c = PactPingThemeAccess.colors
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      stringResource(
        R.string.create_step_indicator,
        stepIndex + 1,
        CreateRoutes.ordered.size,
      ).uppercase(),
      color = c.muted,
      fontWeight = FontWeight.ExtraBold,
      fontSize = 11.sp,
      letterSpacing = 2.2.sp,
    )
    Text(
      title,
      color = c.ink,
      fontWeight = FontWeight.Black,
      fontSize = 34.sp,
      lineHeight = 36.sp,
      letterSpacing = (-1).sp,
    )
    if (subtitle != null) {
      Text(subtitle, color = c.muted, fontSize = 14.sp, lineHeight = 20.sp)
    }
  }
}

@Composable
private fun PrimaryCta(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  val c = PactPingThemeAccess.colors
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(20.dp),
    contentPadding = PaddingValues(vertical = 20.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = c.strike,
      contentColor = c.onStrike,
      disabledContainerColor = c.hairline,
      disabledContentColor = c.muted,
    ),
  ) {
    Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
  }
}

@Composable
private fun GoalStep(
  state: CreateState,
  onChange: (String) -> Unit,
  onBack: () -> Unit,
  onNext: () -> Unit,
) {
  val c = PactPingThemeAccess.colors
  StepScaffold(
    stepIndex = 0,
    onBack = onBack,
    cta = { PrimaryCta(stringResource(R.string.create_cta_next), onNext, enabled = state.hasGoal) },
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
      StepHeader(stepIndex = 0, title = stringResource(R.string.create_goal_title))
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BasicTextField(
          value = state.goal,
          onValueChange = onChange,
          singleLine = true,
          cursorBrush = SolidColor(c.ink),
          textStyle = TextStyle(
            color = c.ink, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.4).sp,
          ),
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
          decorationBox = { inner ->
            Box {
              if (state.goal.isEmpty()) {
                Text(
                  stringResource(R.string.create_goal_placeholder),
                  color = c.muted,
                  fontSize = 26.sp,
                  fontWeight = FontWeight.ExtraBold,
                  letterSpacing = (-0.4).sp,
                )
              }
              inner()
            }
          },
          modifier = Modifier.fillMaxWidth(),
        )
        Box(
          Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(c.ink),
        )
        Text(
          stringResource(R.string.create_goal_helper),
          color = c.muted,
          fontSize = 14.sp,
          lineHeight = 20.sp,
        )
      }
    }
  }
}

@Composable
private fun DeadlineStep(
  state: CreateState,
  onChange: (Instant) -> Unit,
  onBack: () -> Unit,
  onNext: () -> Unit,
) {
  val c = PactPingThemeAccess.colors
  val zone = remember { TimeZone.currentSystemDefault() }
  val fallback = remember { defaultDeadline(zone) }
  LaunchedEffect(Unit) {
    if (state.deadline == null) onChange(fallback)
  }

  var showDate by remember { mutableStateOf(false) }
  var showTime by remember { mutableStateOf(false) }

  val current = state.deadline ?: fallback
  val countdown = remember(current) { current - Clock.System.now() }

  StepScaffold(
    stepIndex = 1,
    onBack = onBack,
    cta = { PrimaryCta(stringResource(R.string.create_cta_next), onNext, enabled = state.hasDeadline) },
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
      StepHeader(stepIndex = 1, title = stringResource(R.string.create_deadline_title))
      Surface(
        color = c.surface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, c.hairline),
      ) {
        Column(
          Modifier.padding(20.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            current.formatDateShort(zone),
            color = c.ink,
            fontWeight = FontWeight.Black,
            fontSize = 26.sp,
            letterSpacing = (-0.6).sp,
          )
          Text(
            current.formatTimeShort(zone),
            color = c.strike,
            fontWeight = FontWeight.Black,
            fontSize = 34.sp,
            letterSpacing = (-0.8).sp,
          )
          val totalHours = countdown.inWholeHours.coerceAtLeast(0)
          val label = when {
            totalHours >= 48 -> stringResource(
              R.string.create_deadline_countdown_dh,
              countdown.inWholeDays.toInt(),
              (totalHours % 24).toInt(),
            )
            totalHours >= 1 -> stringResource(
              R.string.create_deadline_countdown_h,
              totalHours.toInt(),
            )
            else -> stringResource(
              R.string.create_deadline_countdown_m,
              countdown.inWholeMinutes.coerceAtLeast(0).toInt(),
            )
          }
          Text(label, color = c.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedPill(stringResource(R.string.create_deadline_change_date), Modifier.weight(1f)) { showDate = true }
        OutlinedPill(stringResource(R.string.create_deadline_change_time), Modifier.weight(1f)) { showTime = true }
      }
      Text(
        stringResource(R.string.create_deadline_helper),
        color = c.muted,
        fontSize = 14.sp,
        lineHeight = 20.sp,
      )
    }
  }

  if (showDate) {
    val today = remember { Clock.System.now().toLocalDateTime(zone).date }
    // The Material date picker works in UTC-midnight millis, so compare against
    // today's local date expressed the same way to grey out past days.
    val todayUtcMillis = remember(today) {
      LocalDateTime(today, LocalTime(0, 0)).toInstant(TimeZone.UTC).toEpochMilliseconds()
    }
    val pickerState = rememberDatePickerState(
      initialSelectedDateMillis = LocalDateTime(current.toLocalDateTime(zone).date, LocalTime(0, 0))
        .toInstant(TimeZone.UTC).toEpochMilliseconds(),
      selectableDates = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= todayUtcMillis
        override fun isSelectableYear(year: Int): Boolean = year >= today.year
      },
    )
    DatePickerDialog(
      onDismissRequest = { showDate = false },
      confirmButton = {
        TextButton(onClick = {
          val millis = pickerState.selectedDateMillis
          if (millis != null) {
            val newDate = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
            val currentTime = current.toLocalDateTime(zone).time
            onChange(LocalDateTime(newDate, currentTime).toInstant(zone))
          }
          showDate = false
        }) { Text(stringResource(R.string.dialog_ok), color = c.strike, fontWeight = FontWeight.ExtraBold) }
      },
      dismissButton = {
        TextButton(onClick = { showDate = false }) {
          Text(stringResource(R.string.dialog_cancel), color = c.muted, fontWeight = FontWeight.Bold)
        }
      },
    ) {
      DatePicker(state = pickerState)
    }
  }

  if (showTime) {
    val nowLocal = current.toLocalDateTime(zone)
    val tpState = rememberTimePickerState(
      initialHour = nowLocal.hour,
      initialMinute = nowLocal.minute,
      is24Hour = false,
    )
    AlertDialog(
      onDismissRequest = { showTime = false },
      confirmButton = {
        TextButton(onClick = {
          val date = current.toLocalDateTime(zone).date
          val time = LocalTime(tpState.hour, tpState.minute)
          onChange(LocalDateTime(date, time).toInstant(zone))
          showTime = false
        }) { Text(stringResource(R.string.dialog_ok), color = c.strike, fontWeight = FontWeight.ExtraBold) }
      },
      dismissButton = {
        TextButton(onClick = { showTime = false }) {
          Text(stringResource(R.string.dialog_cancel), color = c.muted, fontWeight = FontWeight.Bold)
        }
      },
      text = { TimePicker(state = tpState) },
    )
  }
}

@Composable
private fun OutlinedPill(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
  val c = PactPingThemeAccess.colors
  OutlinedButton(
    onClick = onClick,
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.5.dp, c.hairline),
    contentPadding = PaddingValues(vertical = 14.dp),
    colors = ButtonDefaults.outlinedButtonColors(contentColor = c.ink),
  ) {
    Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
  }
}

private fun defaultDeadline(zone: TimeZone): Instant {
  val tomorrow = Clock.System.now().plus(1.days).toLocalDateTime(zone).date
  return LocalDateTime(tomorrow, LocalTime(20, 0)).toInstant(zone)
}

// ---------- Step 3: Witness ----------

@Composable
private fun WitnessStep(
  state: CreateState,
  onSelect: (Witness) -> Unit,
  onBack: () -> Unit,
  onNext: () -> Unit,
) {
  val launchPicker = rememberContactPicker { picked ->
    onSelect(picked.toWitness())
  }

  StepScaffold(
    stepIndex = 2,
    onBack = onBack,
    cta = { PrimaryCta(stringResource(R.string.create_cta_next), onNext, enabled = state.hasWitness) },
  ) {
    Column(
      modifier = Modifier.verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
      StepHeader(
        stepIndex = 2,
        title = stringResource(R.string.create_witness_title),
        subtitle = stringResource(R.string.create_witness_subtitle),
      )
      val witness = state.witness
      if (witness == null) {
        PickContactCard(onClick = launchPicker)
      } else {
        SelectedWitnessCard(witness = witness)
        ChangeContactLink(onClick = launchPicker)
        HeadsUpPreview(state = state)
      }
    }
  }
}

@Composable
private fun PickContactCard(onClick: () -> Unit) {
  val c = PactPingThemeAccess.colors
  Surface(
    onClick = onClick,
    color = c.surface,
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(1.dp, c.hairline),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Box(
        Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(c.ink),
        contentAlignment = Alignment.Center,
      ) {
        Text("＋", color = c.bg, fontWeight = FontWeight.Black, fontSize = 22.sp)
      }
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          stringResource(R.string.create_witness_pick_cta),
          color = c.ink,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 17.sp,
          letterSpacing = (-0.2).sp,
        )
        Text(
          stringResource(R.string.create_witness_pick_subtitle),
          color = c.muted,
          fontSize = 13.sp,
          lineHeight = 18.sp,
        )
      }
    }
  }
}

@Composable
private fun SelectedWitnessCard(witness: Witness) {
  val c = PactPingThemeAccess.colors
  Surface(
    color = c.surface,
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(1.dp, c.hairline),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Box(
        Modifier
          .size(44.dp)
          .clip(CircleShape)
          .background(avatarColor(witness.initials)),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          witness.initials,
          color = Color.White,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 15.sp,
        )
      }
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          witness.name,
          color = c.ink,
          fontWeight = FontWeight.ExtraBold,
          fontSize = 17.sp,
          letterSpacing = (-0.2).sp,
        )
        Text(witness.phoneNumber, color = c.muted, fontSize = 13.sp)
      }
      Box(
        Modifier
          .size(26.dp)
          .clip(CircleShape)
          .background(c.ink),
        contentAlignment = Alignment.Center,
      ) {
        Text("✓", color = c.bg, fontWeight = FontWeight.Black, fontSize = 14.sp)
      }
    }
  }
}

@Composable
private fun ChangeContactLink(onClick: () -> Unit) {
  val c = PactPingThemeAccess.colors
  TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
    Text(
      stringResource(R.string.create_witness_change),
      color = c.muted,
      fontWeight = FontWeight.Bold,
      fontSize = 13.sp,
    )
  }
}

@Composable
private fun HeadsUpPreview(state: CreateState) {
  val c = PactPingThemeAccess.colors
  val goal = state.goal.ifBlank { return }
  val deadline = state.deadline ?: return
  val body = stringResource(
    R.string.create_witness_headsup_template,
    goal,
    deadline.formatDeadline(),
  )
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      stringResource(R.string.create_witness_headsup_label).uppercase(),
      color = c.muted,
      fontSize = 10.sp,
      fontWeight = FontWeight.ExtraBold,
      letterSpacing = 2.sp,
    )
    Surface(
      color = c.surface,
      shape = RoundedCornerShape(22.dp),
      border = BorderStroke(1.dp, c.hairline),
    ) {
      Text(
        body,
        color = c.ink,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
      )
    }
  }
}

// ---------- Step 4: Confession ----------

@Composable
private fun ConfessionStep(
  state: CreateState,
  onChange: (String) -> Unit,
  onBack: () -> Unit,
  onNext: () -> Unit,
) {
  val c = PactPingThemeAccess.colors
  val firstName = state.witness?.name?.split(" ")?.firstOrNull()
    ?: stringResource(R.string.create_confession_fallback_witness)

  StepScaffold(
    stepIndex = 3,
    onBack = onBack,
    cta = { PrimaryCta(stringResource(R.string.create_cta_next), onNext, enabled = state.hasConfession) },
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
      StepHeader(
        stepIndex = 3,
        title = stringResource(R.string.create_confession_title),
        subtitle = stringResource(R.string.create_confession_subtitle, firstName),
      )
      Surface(
        color = c.surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, c.hairline),
      ) {
        BasicTextField(
          value = state.confession,
          onValueChange = { if (it.length <= 280) onChange(it) },
          textStyle = TextStyle(
            color = c.ink, fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp,
          ),
          cursorBrush = SolidColor(c.ink),
          keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
          decorationBox = { inner ->
            Box {
              if (state.confession.isEmpty()) {
                Text(
                  stringResource(R.string.create_confession_placeholder, firstName),
                  color = c.muted,
                  fontSize = 15.sp,
                  lineHeight = 22.sp,
                )
              }
              inner()
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp)
            .padding(18.dp),
        )
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          stringResource(R.string.create_confession_helper),
          color = c.muted,
          fontSize = 12.sp,
        )
        Text(
          stringResource(R.string.create_confession_char_count, state.confession.length),
          color = if (state.hasConfession) c.ink else c.muted,
          fontSize = 12.sp,
          fontWeight = FontWeight.ExtraBold,
        )
      }
    }
  }
}

// ---------- Step 5: Summary ----------

@Composable
private fun SummaryStep(
  state: CreateState,
  onBack: () -> Unit,
  onEdit: () -> Unit,
  onCommit: () -> Unit,
) {
  val c = PactPingThemeAccess.colors
  val deadline = state.deadline
  val witness = state.witness
  val dash = stringResource(R.string.create_summary_dash)
  val firstName = witness?.name?.split(" ")?.firstOrNull()
    ?: stringResource(R.string.create_summary_no_witness)

  StepScaffold(
    stepIndex = 4,
    onBack = onBack,
    cta = {
      Column {
        PrimaryCta(stringResource(R.string.create_cta_lock_in), onCommit, enabled = state.canCommit)
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
          Text(
            stringResource(R.string.create_cta_go_back_edit),
            color = c.muted,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    },
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(20.dp),
      modifier = Modifier.verticalScroll(rememberScrollState()),
    ) {
      StepHeader(stepIndex = 4, title = stringResource(R.string.create_summary_title))
      Surface(
        color = c.surface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, c.hairline),
      ) {
        Column(Modifier.padding(horizontal = 18.dp)) {
          SummaryRow(
            label = stringResource(R.string.create_summary_label_goal),
            value = state.goal.ifBlank { dash },
          )
          SummaryRow(
            label = stringResource(R.string.create_summary_label_due),
            value = deadline?.formatDeadline() ?: dash,
          )
          SummaryRow(
            label = stringResource(R.string.create_summary_label_witness),
            value = witness?.name ?: dash,
            sub = witness?.phoneNumber,
          )
        }
      }
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          stringResource(R.string.create_summary_if_miss, firstName).uppercase(),
          color = c.muted,
          fontSize = 11.sp,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 2.sp,
        )
        Surface(color = c.ink, shape = RoundedCornerShape(22.dp, 22.dp, 22.dp, 6.dp)) {
          Text(
            state.confession.ifBlank { dash },
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
}

@Composable
private fun SummaryRow(label: String, value: String, sub: String? = null) {
  val c = PactPingThemeAccess.colors
  Column(
    Modifier
      .fillMaxWidth()
      .padding(vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text(
      label.uppercase(),
      color = c.muted,
      fontSize = 10.sp,
      fontWeight = FontWeight.ExtraBold,
      letterSpacing = 2.sp,
    )
    Text(value, color = c.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    if (sub != null) Text(sub, color = c.muted, fontSize = 13.sp)
  }
}

// ---------- Helpers ----------

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

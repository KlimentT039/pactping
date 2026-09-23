package com.klt.pactping.presentation

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private fun DayOfWeek.short(): String = when (this) {
  DayOfWeek.MONDAY -> "Mon"
  DayOfWeek.TUESDAY -> "Tue"
  DayOfWeek.WEDNESDAY -> "Wed"
  DayOfWeek.THURSDAY -> "Thu"
  DayOfWeek.FRIDAY -> "Fri"
  DayOfWeek.SATURDAY -> "Sat"
  DayOfWeek.SUNDAY -> "Sun"
  else -> ""
}

private fun Month.short(): String = when (this) {
  Month.JANUARY -> "Jan"
  Month.FEBRUARY -> "Feb"
  Month.MARCH -> "Mar"
  Month.APRIL -> "Apr"
  Month.MAY -> "May"
  Month.JUNE -> "Jun"
  Month.JULY -> "Jul"
  Month.AUGUST -> "Aug"
  Month.SEPTEMBER -> "Sep"
  Month.OCTOBER -> "Oct"
  Month.NOVEMBER -> "Nov"
  Month.DECEMBER -> "Dec"
  else -> ""
}

fun Instant.formatDeadline(zone: TimeZone = TimeZone.currentSystemDefault()): String {
  val dt = toLocalDateTime(zone)
  val hour12 = ((dt.hour + 11) % 12) + 1
  val ampm = if (dt.hour < 12) "AM" else "PM"
  val mm = dt.minute.toString().padStart(2, '0')
  return "${dt.dayOfWeek.short()} ${dt.month.short()} ${dt.dayOfMonth} · $hour12:$mm $ampm"
}

fun Instant.formatTimeShort(zone: TimeZone = TimeZone.currentSystemDefault()): String {
  val dt = toLocalDateTime(zone)
  val hour12 = ((dt.hour + 11) % 12) + 1
  val ampm = if (dt.hour < 12) "AM" else "PM"
  val mm = dt.minute.toString().padStart(2, '0')
  return "$hour12:$mm $ampm"
}

fun Instant.formatDateShort(zone: TimeZone = TimeZone.currentSystemDefault()): String {
  val dt = toLocalDateTime(zone)
  return "${dt.dayOfWeek.short()}, ${dt.month.short()} ${dt.dayOfMonth}"
}

fun Instant.formatHistoryDate(zone: TimeZone = TimeZone.currentSystemDefault()): String {
  val dt = toLocalDateTime(zone)
  return "${dt.dayOfWeek.short()} ${dt.month.short()} ${dt.dayOfMonth}"
}

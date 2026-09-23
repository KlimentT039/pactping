package com.klt.pactping.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val PactPingTypography = Typography(
  displayLarge = TextStyle(
    fontWeight = FontWeight.Black, fontSize = 64.sp, lineHeight = 60.sp, letterSpacing = (-2.5).sp,
  ),
  displayMedium = TextStyle(
    fontWeight = FontWeight.Black, fontSize = 44.sp, lineHeight = 44.sp, letterSpacing = (-1.4).sp,
  ),
  displaySmall = TextStyle(
    fontWeight = FontWeight.Black, fontSize = 34.sp, lineHeight = 36.sp, letterSpacing = (-1).sp,
  ),
  headlineLarge = TextStyle(
    fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.6).sp,
  ),
  headlineMedium = TextStyle(
    fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = (-0.3).sp,
  ),
  titleLarge = TextStyle(
    fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 22.sp,
  ),
  titleMedium = TextStyle(
    fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 18.sp,
  ),
  bodyLarge = TextStyle(
    fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp,
  ),
  bodyMedium = TextStyle(
    fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp,
  ),
  bodySmall = TextStyle(
    fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp,
  ),
  labelLarge = TextStyle(
    fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.2.sp,
  ),
  labelMedium = TextStyle(
    fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, lineHeight = 14.sp, letterSpacing = 1.8.sp,
  ),
  labelSmall = TextStyle(
    fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 2.2.sp,
  ),
)

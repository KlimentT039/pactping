package com.klt.pactping.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class PactPingSemantic(
  val ink: Color,
  val inkSoft: Color,
  val muted: Color,
  val bg: Color,
  val surface: Color,
  val surfaceAlt: Color,
  val hairline: Color,
  val strike: Color,
  val kept: Color,
  val miss: Color,
  val keptSoft: Color,
  val missSoft: Color,
  val dueSoft: Color,
  val dueOn: Color,
  val onStrike: Color,
)

val LocalPactPingSemantic = staticCompositionLocalOf<PactPingSemantic> {
  error("PactPingSemantic not provided")
}

private fun lightSemantic() = PactPingSemantic(
  ink = PactPingColors.Ink,
  inkSoft = PactPingColors.InkSoft,
  muted = PactPingColors.Muted,
  bg = PactPingColors.Cream,
  surface = PactPingColors.Surface,
  surfaceAlt = PactPingColors.SurfaceAlt,
  hairline = PactPingColors.Hairline,
  strike = PactPingColors.Strike,
  kept = PactPingColors.Kept,
  miss = PactPingColors.Miss,
  keptSoft = PactPingColors.KeptSoft,
  missSoft = PactPingColors.MissSoft,
  dueSoft = PactPingColors.DueSoft,
  dueOn = PactPingColors.DueOn,
  onStrike = Color.White,
)

private fun darkSemantic() = PactPingSemantic(
  ink = PactPingColors.InkDark,
  inkSoft = PactPingColors.InkSoftDark,
  muted = PactPingColors.MutedDark,
  bg = PactPingColors.CreamDark,
  surface = PactPingColors.SurfaceDark,
  surfaceAlt = PactPingColors.SurfaceAltDark,
  hairline = PactPingColors.HairlineDark,
  strike = PactPingColors.StrikeDark,
  kept = PactPingColors.KeptDark,
  miss = PactPingColors.MissDark,
  keptSoft = PactPingColors.KeptSoftDark,
  missSoft = PactPingColors.MissSoftDark,
  dueSoft = PactPingColors.DueSoftDark,
  dueOn = PactPingColors.DueOnDark,
  onStrike = PactPingColors.CreamDark,
)

private fun materialLight(s: PactPingSemantic) = lightColorScheme(
  primary = s.strike,
  onPrimary = s.onStrike,
  background = s.bg,
  onBackground = s.ink,
  surface = s.surface,
  onSurface = s.ink,
  surfaceVariant = s.surfaceAlt,
  onSurfaceVariant = s.inkSoft,
  error = s.miss,
)

private fun materialDark(s: PactPingSemantic) = darkColorScheme(
  primary = s.strike,
  onPrimary = s.onStrike,
  background = s.bg,
  onBackground = s.ink,
  surface = s.surface,
  onSurface = s.ink,
  surfaceVariant = s.surfaceAlt,
  onSurfaceVariant = s.inkSoft,
  error = s.miss,
)

@Composable
fun PactPingTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val semantic = if (darkTheme) darkSemantic() else lightSemantic()
  val scheme = if (darkTheme) materialDark(semantic) else materialLight(semantic)
  CompositionLocalProvider(LocalPactPingSemantic provides semantic) {
    MaterialTheme(
      colorScheme = scheme,
      typography = PactPingTypography,
      content = content,
    )
  }
}

object PactPingThemeAccess {
  val colors: PactPingSemantic
    @Composable get() = LocalPactPingSemantic.current
}

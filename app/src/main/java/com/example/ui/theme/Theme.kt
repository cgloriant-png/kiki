package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ParamoteurHighDensityColorScheme =
  lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = HighDensitySurface,
    primaryContainer = PrimaryBlueContainer,
    onPrimaryContainer = PrimaryBlueDark,
    secondary = SecondaryText,
    onSecondary = HighDensitySurface,
    background = HighDensityBg,
    onBackground = NeutralText,
    surface = HighDensitySurface,
    onSurface = NeutralText,
    surfaceVariant = HighDensityNavBar,
    onSurfaceVariant = SecondaryText,
    outline = BorderOutline,
    error = RedAlertText,
    onError = HighDensitySurface,
    errorContainer = RedAlertBg,
    onErrorContainer = RedAlertDark
  )

@Composable
fun ParamoteurTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = ParamoteurHighDensityColorScheme,
    typography = Typography,
    content = content
  )
}


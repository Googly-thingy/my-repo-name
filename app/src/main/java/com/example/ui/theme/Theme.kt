package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val MinimalWhiteColorScheme =
  lightColorScheme(
    primary = StudioPrimary,
    onPrimary = Color.White,
    primaryContainer = StudioSurfaceMuted,
    onPrimaryContainer = StudioPrimary,
    secondary = StudioAccent,
    onSecondary = Color.White,
    secondaryContainer = StudioAccentMuted,
    onSecondaryContainer = StudioPrimary,
    tertiary = StudioAmber,
    onTertiary = Color.White,
    background = StudioBackground,
    onBackground = TextPrimary,
    surface = StudioSurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurfaceMuted,
    onSurfaceVariant = TextSecondary,
    outline = StudioBorderSubtle,
    outlineVariant = Color(0xFFF1F5F9)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = MinimalWhiteColorScheme,
    typography = Typography,
    content = content
  )
}


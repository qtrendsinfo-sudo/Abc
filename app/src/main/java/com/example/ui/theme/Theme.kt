package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = TalabatOrange,
    secondary = TalabatGreen,
    tertiary = TalabatOrange,
    background = DarkSlate,
    surface = Color(0xFF1E293B),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
  )

private val LightColorScheme =
  lightColorScheme(
    primary = TalabatOrange,
    secondary = TalabatGreen,
    tertiary = TalabatOrange,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = DarkSlate,
    onSurface = DarkSlate
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Force false for dynamic color to strictly stay on the orange/green theme brand
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

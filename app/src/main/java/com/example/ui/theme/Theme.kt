package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = SynthDark,
    primaryContainer = SlateDarkSecondary,
    onPrimaryContainer = TextCyan,
    secondary = TextCyan,
    onSecondary = SynthDark,
    background = SynthDark,
    onBackground = Color.White,
    surface = HologramSlate,
    onSurface = Color.White,
    surfaceVariant = ObsidianPrimary,
    onSurfaceVariant = TextCyan,
    error = LaserRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00A8CC),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2EBF2),
    onPrimaryContainer = Color(0xFF004D40),
    secondary = Color(0xFF00796B),
    onSecondary = Color.White,
    background = Color(0xFFF4F6F9),
    onBackground = Color(0xFF101B2B),
    surface = Color.White,
    onSurface = Color(0xFF101B2B),
    surfaceVariant = Color(0xFFE0F2F1),
    onSurfaceVariant = Color(0xFF004D40),
    error = LaserRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force cyberpunk dark theme default!
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

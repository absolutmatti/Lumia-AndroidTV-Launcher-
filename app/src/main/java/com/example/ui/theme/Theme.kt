package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = LumiaAccent,
    secondary = LumiaTextSecondary,
    tertiary = LumiaCardDefault,
    background = LumiaBackground,
    surface = LumiaCardDefault,
    onPrimary = LumiaTextPrimary,
    onSecondary = LumiaTextPrimary,
    onTertiary = LumiaTextPrimary,
    onBackground = LumiaTextPrimary,
    onSurface = LumiaTextPrimary,
    error = LumiaDestructive,
    onError = LumiaTextPrimary
)

@Composable
fun LumiaLauncherTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

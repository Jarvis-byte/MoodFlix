package com.arka.moodflix.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = Amber400,
    onPrimary = Ink900,
    primaryContainer = Amber700,
    onPrimaryContainer = Amber300,
    secondary = Violet400,
    onSecondary = Ink900,
    secondaryContainer = Violet700,
    background = Ink900,
    onBackground = Cream100,
    surface = Ink800,
    onSurface = Cream100,
    surfaceVariant = Ink700,
    onSurfaceVariant = Muted,
    outline = Ink600,
    error = Rose400
)

private val LightColors = lightColorScheme(
    primary = Amber700,
    onPrimary = Cream100,
    secondary = Violet700,
    background = Cream100,
    onBackground = Ink900,
    surface = Color_White,
    onSurface = Ink900,
    surfaceVariant = Cream200,
    onSurfaceVariant = Ink700,
    error = Rose400
)

@Composable
fun MoodFlixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = MoodFlixTypography,
        content = content
    )
}

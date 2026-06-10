package com.example.fitnesstracker.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Black,
    background = Black,
    onBackground = LightGray,
    surface = CardGray,
    onSurface = White,
    outline = BorderGray,
    surfaceVariant = DarkGray,
    onSurfaceVariant = MediumGray
)

@Composable
fun FitnessTrackerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

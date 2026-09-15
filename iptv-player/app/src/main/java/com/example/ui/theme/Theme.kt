package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TVDarkColorScheme = darkColorScheme(
    primary = PrimaryElectricCyan,
    onPrimary = BackgroundDark,
    primaryContainer = PrimarySapphire,
    onPrimaryContainer = TextPrimary,
    secondary = PrimaryNeonGlow,
    onSecondary = BackgroundDark,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = BorderFocused
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    // Projectors and TV screens require high-contrast dark theme
    MaterialTheme(
        colorScheme = TVDarkColorScheme,
        typography = Typography,
        content = content
    )
}

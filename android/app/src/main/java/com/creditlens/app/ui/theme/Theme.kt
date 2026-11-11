package com.creditlens.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = PrimaryText,
    primaryContainer = SecondarySurface,
    onPrimaryContainer = PrimaryText,
    secondary = AccentCyan,
    onSecondary = PrimaryText,
    background = MainBackground,
    onBackground = PrimaryText,
    surface = CardBackground,
    onSurface = PrimaryText,
    surfaceVariant = SecondarySurface,
    onSurfaceVariant = SecondaryText,
    outline = BorderColor,
    outlineVariant = BorderColor
)

@Composable
fun CreditLensTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

package com.khalied.cukinggo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = PawBrown,
    onPrimary = Color.White,
    primaryContainer = PeachAccent,
    onPrimaryContainer = InkSoft,
    secondary = MintPop,
    onSecondary = InkSoft,
    secondaryContainer = MintPop,
    onSecondaryContainer = InkSoft,
    tertiary = BlushPink,
    onTertiary = InkSoft,
    background = CreamBg,
    onBackground = InkSoft,
    surface = CreamSurface,
    onSurface = InkSoft,
    surfaceVariant = SandVariant,
    onSurfaceVariant = PawBrown,
    outline = PawBrown,
    error = Color(0xFFB3261E),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = NightPaw,
    onPrimary = InkSoft,
    primaryContainer = PawBrown,
    onPrimaryContainer = CreamBg,
    secondary = NightMint,
    onSecondary = Color(0xFF152B25),
    secondaryContainer = Color(0xFF33473F),
    onSecondaryContainer = NightMint,
    tertiary = BlushPink,
    onTertiary = InkSoft,
    background = NightBg,
    onBackground = NightInk,
    surface = NightSurface,
    onSurface = NightInk,
    surfaceVariant = Color(0xFF463830),
    onSurfaceVariant = Color(0xFFE3D2C4),
    outline = PeachAccent,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun CukingGoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

package com.khalied.cukinggo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Frost UI: latar dingin, permukaan kaca, aksen biru es.
 *
 * Warna di sini adalah "lantai"-nya saja. Permukaan kaca sendiri dibentuk oleh
 * `Modifier.frostSurface()` yang menambahkan transparansi, blur, dan border tipis
 * di atasnya.
 */
private val LightColors = lightColorScheme(
    // primary dipakai sebagai warna teks judul di seluruh app, jadi yang dipakai
    // IceDeep, bukan IceAccent: IceAccent di atas latar dingin cuma 2,3:1, di
    // bawah ambang AA. IceAccent tetap jadi aksen dekoratif.
    primary = IceDeep,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5E9F5),
    onPrimaryContainer = SteelText,
    secondary = MistGray,
    onSecondary = Color.White,
    secondaryContainer = FrostVariant,
    onSecondaryContainer = SteelText,
    tertiary = Color(0xFFB8D4E8),
    onTertiary = SteelText,
    background = FrostBg,
    onBackground = SteelText,
    surface = GlassSurface,
    onSurface = SteelText,
    surfaceVariant = FrostVariant,
    onSurfaceVariant = Color(0xFF4A5D70),
    outline = MistGray,
    error = Color(0xFFC0392B),
    onError = Color.White,
    // Satu-satunya warna hangat yang tersisa, dan hanya untuk status merusak
    // (hapus, matikan kabar dekat), sesuai batas Frost UI.
    errorContainer = Color(0xFFF6DAD6),
    onErrorContainer = Color(0xFF4A110B)
)

private val DarkColors = darkColorScheme(
    primary = FrostNightAccent,
    onPrimary = Color(0xFF082033),
    primaryContainer = Color(0xFF23425A),
    onPrimaryContainer = FrostNightInk,
    secondary = FrostNightMuted,
    onSecondary = Color(0xFF0F1720),
    secondaryContainer = Color(0xFF2A3B4A),
    onSecondaryContainer = FrostNightInk,
    tertiary = Color(0xFF3E5A72),
    onTertiary = FrostNightInk,
    background = FrostNightBg,
    onBackground = FrostNightInk,
    surface = FrostNightSurface,
    onSurface = FrostNightInk,
    surfaceVariant = Color(0xFF243241),
    onSurfaceVariant = FrostNightMuted,
    outline = FrostNightMuted,
    error = Color(0xFFFF8A80),
    onError = Color(0xFF3A0A06),
    errorContainer = Color(0xFF5C1A14),
    onErrorContainer = Color(0xFFFFDAD6)
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

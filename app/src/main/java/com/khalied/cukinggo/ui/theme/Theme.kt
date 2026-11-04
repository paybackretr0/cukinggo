package com.khalied.cukinggo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * NeoBrutalism: latar Paper, permukaan Card, tinta sebagai garis sekaligus teks.
 *
 * Yang diatur di sini cuma "lantai" warnanya. Garis tebal dan bayangan keras
 * dipasang oleh `Modifier.appSurface()` / `Modifier.brutalShadow()`, karena
 * keduanya mengubah gambar dan ukuran, bukan sekadar warna.
 *
 * `primary` diisi Ink, bukan warna aksen: di app ini primary dipakai sebagai
 * warna judul dan isian tombol bawaan Material, dan tinta dengan teks Paper
 * adalah pasangan paling terbaca (18,9:1) sekaligus bahasa neo-brutalism.
 * Aksennya masuk lewat `primaryContainer`/`secondary`/`tertiary`, yang memang
 * hanya dipakai di tempat yang memintanya.
 */
private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Paper,
    primaryContainer = Yellow,
    onPrimaryContainer = Ink,
    secondary = Blue,
    onSecondary = Paper,
    secondaryContainer = Cyan,
    onSecondaryContainer = Ink,
    tertiary = Yellow,
    onTertiary = Ink,
    background = Paper,
    onBackground = Ink,
    surface = Card,
    onSurface = Ink,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = Muted,
    outline = Ink,
    error = ErrorRed,
    onError = Paper,
    // Satu-satunya warna hangat yang tersisa, dan hanya untuk status merusak.
    errorContainer = Color(0xFFFFD9D6),
    onErrorContainer = Color(0xFF410002)
)

/**
 * Tema malam memakai netral yang sama, bukan biru gelap: yang berubah cuma
 * terang-gelapnya, jadi garis tinta menjadi tinta terang dan bayangannya ikut.
 */
private val DarkColors = darkColorScheme(
    primary = NightInk,
    onPrimary = NightCanvas,
    primaryContainer = Yellow,
    // Teks di atas kuning tetap tinta gelap: aksennya terang di kedua tema.
    onPrimaryContainer = Ink,
    secondary = Cyan,
    onSecondary = Ink,
    secondaryContainer = Color(0xFF1F3A44),
    onSecondaryContainer = NightInk,
    tertiary = Yellow,
    onTertiary = Ink,
    background = NightCanvas,
    onBackground = NightInk,
    surface = NightCard,
    onSurface = NightInk,
    surfaceVariant = NightMutedSurface,
    onSurfaceVariant = NightMuted,
    outline = NightInk,
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


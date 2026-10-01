package com.khalied.cukinggo.ui.theme

import androidx.compose.ui.graphics.Color

// --- Frost UI (lihat DESIGN.md, "Frost UI") ---
// Permukaan kaca di atas latar dingin. Token dingin, transparansi, dan blur.
val FrostBg = Color(0xFFEAF2F8)
val GlassSurface = Color(0xFFFFFFFF)
val FrostBorder = Color(0xFFFFFFFF)

/** Aksen dekoratif: batang grafik, marker peta, ilustrasi. Tidak pernah jadi teks. */
val IceAccent = Color(0xFF5FA8D3)

/**
 * Biru es yang lebih pekat, khusus teks dan isian tombol.
 *
 * IceAccent di atas FrostBg cuma 2,3:1, jadi ia tidak boleh dipakai untuk judul
 * atau teks apa pun; IceDeep di latar yang sama 4,9:1 (lolos AA), dan teks putih
 * di atasnya 5,5:1.
 */
val IceDeep = Color(0xFF2F6E96)
val SteelText = Color(0xFF2E3A46)
val MistGray = Color(0xFF8FA6BC)
val IceSoft = Color(0xFFB8D4E8)
val FrostDecor = Color(0xFFA9D4EE)
val FrostSolidsurface = Color(0xFFF2F7FB)
val FrostVariant = Color(0xFFDCE8F2)

// Varian malam Frost: keluarga biru yang sama, bukan hitam pekat.
val FrostNightBg = Color(0xFF0F1720)
val FrostNightSurface = Color(0xFF1A2530)
val FrostNightInk = Color(0xFFE6EEF5)
val FrostNightAccent = Color(0xFF7FC0E8)
val FrostNightMuted = Color(0xFF9FB3C8)

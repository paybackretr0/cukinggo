package com.khalied.cukinggo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.khalied.cukinggo.R

/**
 * Inter untuk judul, dengan bobot **Black** yang diambil dari file variable-nya.
 *
 * `inter.ttf` yang dibundel memang variable font (punya tabel `fvar`), jadi
 * bobot 900 bisa diminta langsung lewat [FontVariation]. Tanpa itu, semua
 * deklarasi di bawah cuma menunjuk instance yang sama dan "Black" jadi tebal
 * sintetis: hurufnya menggemuk tapi bentuknya tidak berubah, dan itu justru yang
 * membuat judul neo-brutalism terlihat murah.
 */
private fun inter(weight: FontWeight, axis: Int): Font = Font(
    resId = R.font.inter,
    weight = weight,
    style = FontStyle.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(axis))
)

val InterFamily = FontFamily(
    inter(FontWeight.Normal, 400),
    inter(FontWeight.Medium, 500),
    inter(FontWeight.SemiBold, 600),
    inter(FontWeight.Bold, 700),
    inter(FontWeight.ExtraBold, 800),
    inter(FontWeight.Black, 900)
)

/** Manrope untuk teks isi: tidak perlu sampai Black, cukup sampai Bold. */
private fun manrope(weight: FontWeight, axis: Int): Font = Font(
    resId = R.font.manrope,
    weight = weight,
    style = FontStyle.Normal,
    variationSettings = FontVariation.Settings(FontVariation.weight(axis))
)

val ManropeFamily = FontFamily(
    manrope(FontWeight.Normal, 400),
    manrope(FontWeight.Medium, 500),
    manrope(FontWeight.SemiBold, 600),
    manrope(FontWeight.Bold, 700)
)

/**
 * Judul dibuat tebal dan rapat, teks isi tetap longgar.
 *
 * Tracking judul negatif: huruf Black yang berjarak normal terbaca renggang,
 * sedangkan di arah ini judul adalah bidang, bukan kalimat. Teks isi tidak ikut
 * dirapatkan karena bagian itulah yang dibaca paling lama di layar.
 */
val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Black,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.9).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Black,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.7).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 23.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.4).sp
    ),
    titleLarge = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.3).sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.2).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = ManropeFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

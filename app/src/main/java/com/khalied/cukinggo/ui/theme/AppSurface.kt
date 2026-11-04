package com.khalied.cukinggo.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Satu permukaan app: isian datar + garis tinta + bayangan keras.
 *
 * Dulu di sini ada tiga lapisan kaca (transparansi, blur, garis putih tipis).
 * Sekarang resepnya cuma tiga hal, dan ketiganya punya tugas sendiri: tanpa
 * isian datar bayangannya hilang di latar, tanpa garis tepi bayangannya cuma
 * muncul di dua sisi, dan tanpa bayangan permukaannya menyatu dengan halaman.
 *
 * [fill] bawaannya `surface`, bukan putih mati: di tema malam `surface` sudah
 * gelap, jadi teks terang di atasnya tetap terbaca tanpa cabang `isDark` di
 * setiap pemanggil.
 *
 * Urutannya penting: bayangan dipasang paling awal supaya ia berada **di
 * belakang** isian, lalu `clip` supaya isian dan garisnya berhenti di bentuk
 * yang sama.
 */
@Composable
fun Modifier.appSurface(
    shape: Shape = MaterialTheme.shapes.medium,
    fill: Color = MaterialTheme.colorScheme.surface,
    border: Color = MaterialTheme.colorScheme.outline,
    shadowOffset: Dp = 4.dp,
    borderWidth: Dp = 3.dp
): Modifier = this
    .brutalShadow(offset = shadowOffset, shape = shape, color = border)
    .clip(shape)
    .background(fill)
    .border(borderWidth, border, shape)

package com.khalied.cukinggo.widget

import androidx.compose.ui.graphics.toArgb
import com.khalied.cukinggo.R
import com.khalied.cukinggo.ui.theme.InkSoft
import com.khalied.cukinggo.ui.theme.NightInk

/**
 * Tiga nilai yang harus ditentukan sendiri oleh widget, karena RemoteViews
 * digambar di proses launcher dan tidak bisa membaca tema Compose:
 * bingkai kartu, warna jejak kaki pengganti foto, dan warna teks catatan.
 *
 * Warna diambil dari token palet app (ui/theme/Color.kt) supaya widget tidak
 * diam-diam punya paletnya sendiri. Tema gelap ditentukan oleh pilihan pengguna
 * di dalam app, bukan hanya setelan HP, jadi kedua varian harus tersedia
 * sekaligus di sini.
 */
data class WidgetPalette(
    val frameRes: Int,
    val fallbackArtRes: Int,
    val captionColor: Int
)

fun widgetPalette(isDark: Boolean): WidgetPalette = if (isDark) {
    WidgetPalette(
        frameRes = R.drawable.widget_polaroid_frame_dark,
        fallbackArtRes = R.drawable.widget_paw_dark,
        captionColor = NightInk.toArgb()
    )
} else {
    WidgetPalette(
        frameRes = R.drawable.widget_polaroid_frame,
        fallbackArtRes = R.drawable.widget_paw_light,
        captionColor = InkSoft.toArgb()
    )
}

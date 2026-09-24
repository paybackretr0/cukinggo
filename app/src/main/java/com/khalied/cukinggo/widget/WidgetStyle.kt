package com.khalied.cukinggo.widget

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.toArgb
import com.khalied.cukinggo.R
import com.khalied.cukinggo.ui.theme.CreamBg
import com.khalied.cukinggo.ui.theme.CreamSurface
import com.khalied.cukinggo.ui.theme.InkSoft
import com.khalied.cukinggo.ui.theme.NightBg
import com.khalied.cukinggo.ui.theme.NightInk
import com.khalied.cukinggo.ui.theme.NightSurface
import java.time.LocalDate

/**
 * Bentuk widget yang bergilir tiap hari. Ketiganya memakai foto dan data yang
 * sama, yang berbeda cuma cara membingkainya, jadi pengguna tidak perlu memasang
 * varian widget baru untuk mendapat tampilan yang berbeda.
 */
enum class WidgetSkin {
    /** Stiker tempel: badan bergaris tepi tebal dengan dua telinga cuking mencuat di atasnya. */
    STICKER,

    /** Balon bicara: badan membulat dengan ekor kecil di kiri bawah. */
    SPEECH,

    /** Jendela: foto dipotong bulat, dengan kumis di kiri dan kanan. */
    WINDOW;

    companion object {
        /**
         * Bentuk untuk satu hari. Dipilih dari tanggal, bukan acak, supaya widget
         * tidak berubah tiap kali digambar ulang, dan supaya dua widget yang
         * terpasang bersamaan menampilkan bentuk yang sama hari itu.
         */
        fun forDate(date: LocalDate): WidgetSkin =
            entries[Math.floorMod(date.toEpochDay(), entries.size.toLong()).toInt()]
    }
}

/**
 * Pilihan pengguna untuk bentuk widget: biarkan bergilir tiap hari, atau paksa
 * satu bentuk. Disimpan sebagai [storageKey] supaya layer penyimpanan tidak perlu
 * tahu enum ini.
 */
enum class WidgetSkinMode(val storageKey: String) {
    AUTO("auto"),
    STICKER("sticker"),
    SPEECH("speech"),
    WINDOW("window");

    /** Bentuk yang dipakai hari ini: pilihan pengguna, atau rotasi harian. */
    fun skinFor(date: LocalDate): WidgetSkin = when (this) {
        AUTO -> WidgetSkin.forDate(date)
        STICKER -> WidgetSkin.STICKER
        SPEECH -> WidgetSkin.SPEECH
        WINDOW -> WidgetSkin.WINDOW
    }

    companion object {
        /** Key tidak dikenal atau belum pernah disimpan dianggap "otomatis". */
        fun fromKey(key: String?): WidgetSkinMode =
            entries.firstOrNull { it.storageKey == key } ?: AUTO
    }
}

/** Cara foto ditempelkan: dipotong penuh, atau dipotong bulat. */
internal enum class WidgetPhotoShape { CROP, CIRCLE }

/**
 * Satu bentuk widget setelah diterjemahkan jadi aset dan angka yang bisa dibaca
 * RemoteViews. RemoteViews tidak bisa membaca tema Compose maupun mengganti tint
 * saat runtime, jadi tiap bentuk punya asetnya sendiri untuk tema terang dan gelap.
 */
internal data class WidgetStyle(
    @DrawableRes val frameRes: Int,
    @DrawableRes val fallbackArtRes: Int,
    val captionColor: Int,
    val photoShape: WidgetPhotoShape,
    /**
     * Warna badan widget. Dipakai untuk memenuhi sudut luar foto bulat, supaya
     * fotonya tidak butuh saluran alpha dan ukurannya tetap aman dikirim lewat
     * Binder bersama seluruh RemoteViews.
     */
    val bodyColor: Int,
    /** Telinga cuking, hanya dipakai bentuk stiker. */
    @DrawableRes val earsRes: Int = 0,
    /** Ekor balon, hanya dipakai bentuk balon. */
    @DrawableRes val tailRes: Int = 0,
    /**
     * Kumis bentuk jendela, dan warnanya dipakai untuk menggambar kumis itu ke
     * dalam bitmap foto, bukan sebagai hiasan terpisah di layout.
     *
     * Alasannya: hiasan di layout punya jarak tetap dari tepi kartu, sedangkan
     * lingkaran fotonya menyusut di tengah kartu dan ukurannya ikut berubah saat
     * widget ditarik. Hasilnya kumis akan melayang jauh dari lingkarannya, dan
     * itu memang terjadi pada versi pertama bentuk ini. Digambar di bitmap, kumis
     * selalu menempel di tepi lingkaran di ukuran berapa pun.
     */
    val whiskerLineColor: Int = 0,
    /** Garis luar kumis, supaya tetap terbaca di atas foto yang gelap atau terang. */
    val whiskerOutlineColor: Int = 0,
    /** Ruang di atas foto, dipakai bentuk stiker supaya telinganya tidak menutupinya. */
    val photoTopPaddingDp: Int = 0,
    /** Ruang di bawah foto, dipakai bentuk balon supaya ekornya tidak menutupinya. */
    val photoBottomPaddingDp: Int = 0
)

internal fun widgetStyle(skin: WidgetSkin, isDark: Boolean): WidgetStyle = when (skin) {
    WidgetSkin.STICKER -> WidgetStyle(
        frameRes = pick(isDark, R.drawable.widget_frame_sticker, R.drawable.widget_frame_sticker_dark),
        fallbackArtRes = fallbackArt(isDark),
        captionColor = captionColor(isDark),
        photoShape = WidgetPhotoShape.CROP,
        bodyColor = bodyColor(isDark),
        earsRes = pick(isDark, R.drawable.widget_ears, R.drawable.widget_ears_dark),
        // 18dp = tinggi pita di atas badan stiker, jadi fotonya mulai tepat di
        // bawah telinganya, bukan tertutup hiasan.
        photoTopPaddingDp = 18
    )

    WidgetSkin.SPEECH -> WidgetStyle(
        frameRes = pick(isDark, R.drawable.widget_frame_speech, R.drawable.widget_frame_speech_dark),
        fallbackArtRes = fallbackArt(isDark),
        captionColor = captionColor(isDark),
        photoShape = WidgetPhotoShape.CROP,
        bodyColor = bodyColor(isDark),
        tailRes = pick(isDark, R.drawable.widget_tail, R.drawable.widget_tail_dark),
        photoBottomPaddingDp = 12
    )

    WidgetSkin.WINDOW -> WidgetStyle(
        frameRes = pick(isDark, R.drawable.widget_frame_window, R.drawable.widget_frame_window_dark),
        fallbackArtRes = fallbackArt(isDark),
        captionColor = captionColor(isDark),
        photoShape = WidgetPhotoShape.CIRCLE,
        // Bentuk jendela sengaja memakai warna latar, bukan warna permukaan, supaya
        // terasa seperti kaca jendela dan tidak kembar dengan dua bentuk lain.
        bodyColor = windowBodyColor(isDark),
        whiskerLineColor = whiskerLineColor(isDark),
        whiskerOutlineColor = whiskerOutlineColor(isDark)
    )
}

private fun pick(isDark: Boolean, light: Int, dark: Int): Int = if (isDark) dark else light

private fun fallbackArt(isDark: Boolean): Int =
    pick(isDark, R.drawable.widget_paw_light, R.drawable.widget_paw_dark)

private fun captionColor(isDark: Boolean): Int = (if (isDark) NightInk else InkSoft).toArgb()

private fun bodyColor(isDark: Boolean): Int = (if (isDark) NightSurface else CreamSurface).toArgb()

private fun windowBodyColor(isDark: Boolean): Int = (if (isDark) NightBg else CreamBg).toArgb()

/** Kumis digambar di atas foto, jadi warnanya diambil dari warna badan dan tepi. */
private fun whiskerLineColor(isDark: Boolean): Int =
    (if (isDark) NightInk else CreamSurface).toArgb()

private fun whiskerOutlineColor(isDark: Boolean): Int = (if (isDark) NightBg else InkSoft).toArgb()

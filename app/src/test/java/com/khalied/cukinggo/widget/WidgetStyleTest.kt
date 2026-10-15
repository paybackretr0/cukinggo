package com.khalied.cukinggo.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetStyleTest {

    /**
     * Kalau aset dan warnanya tidak ikut berganti, mode gelap widget akan memakai
     * aset terang dan teksnya bisa tidak terbaca di layar utama.
     */
    @Test
    fun `tiap bentuk memakai aset dan warna berbeda antara mode terang dan gelap`() {
        WidgetSkin.entries.forEach { skin ->
            val terang = widgetStyle(skin, isDark = false)
            val gelap = widgetStyle(skin, isDark = true)

            assertNotEquals("bingkai $skin", terang.frameRes, gelap.frameRes)
            assertNotEquals("jejak kaki $skin", terang.fallbackArtRes, gelap.fallbackArtRes)
            assertNotEquals("warna catatan $skin", terang.captionColor, gelap.captionColor)
            assertNotEquals("warna badan $skin", terang.bodyColor, gelap.bodyColor)
        }
    }

    @Test
    fun `setiap bentuk punya bingkai sendiri`() {
        val frames = WidgetSkin.entries
            .map { skin -> widgetStyle(skin, isDark = false).frameRes }

        assertEquals(WidgetSkin.entries.size, frames.toSet().size)
    }

    @Test
    fun `hanya bentuk jendela yang memotong foto bulat dan memakai kumis`() {
        val stiker = widgetStyle(WidgetSkin.STICKER, isDark = false)
        val balon = widgetStyle(WidgetSkin.SPEECH, isDark = false)
        val jendela = widgetStyle(WidgetSkin.WINDOW, isDark = false)

        assertEquals(WidgetPhotoShape.CROP, stiker.photoShape)
        assertEquals(WidgetPhotoShape.CROP, balon.photoShape)
        assertEquals(WidgetPhotoShape.CIRCLE, jendela.photoShape)
        assertTrue("warna kumis bentuk jendela", jendela.whiskerLineColor != 0)
        assertTrue("garis luar kumis bentuk jendela", jendela.whiskerOutlineColor != 0)
        assertEquals(0, stiker.whiskerLineColor)
        assertEquals(0, balon.whiskerLineColor)
    }

    /**
     * Kumis digambar dua kali di atas foto: garis tipis terang di atas garis tebal
     * gelap. Kalau keduanya berwarna sama, kumisnya hilang di foto yang sewarna.
     */
    @Test
    fun `kumis memakai warna garis dan warna tepi yang berbeda di kedua tema`() {
        listOf(false, true).forEach { isDark ->
            val jendela = widgetStyle(WidgetSkin.WINDOW, isDark)

            assertNotEquals(
                "kumis berbeda dari garis luarnya (gelap=$isDark)",
                jendela.whiskerLineColor,
                jendela.whiskerOutlineColor
            )
        }
    }

    @Test
    fun `telinga hanya dipakai bentuk stiker, ekor hanya bentuk balon`() {
        val stiker = widgetStyle(WidgetSkin.STICKER, isDark = false)
        val balon = widgetStyle(WidgetSkin.SPEECH, isDark = false)
        val jendela = widgetStyle(WidgetSkin.WINDOW, isDark = false)

        assertTrue("telinga bentuk stiker", stiker.earsRes != 0)
        assertEquals(0, stiker.tailRes)
        assertTrue("ekor bentuk balon", balon.tailRes != 0)
        assertEquals(0, balon.earsRes)
        assertEquals(0, jendela.earsRes)
        assertEquals(0, jendela.tailRes)
    }

    /**
     * Sudut kotak foto tercetak di luar lengkung badan balon (radius 26dp) karena
     * foto hanya diberi jarak 6dp dari tepi kartu, jadi cuma bentuk itu yang perlu
     * penutup sudut. Bentuk stiker (badan 20dp) dan bentuk jendela (foto bulat)
     * sudah aman, dan menutup sudutnya justru akan mengubah tampilannya.
     */
    @Test
    fun `hanya bentuk balon yang menutup sudut atas fotonya`() {
        val stiker = widgetStyle(WidgetSkin.STICKER, isDark = false)
        val balon = widgetStyle(WidgetSkin.SPEECH, isDark = false)
        val jendela = widgetStyle(WidgetSkin.WINDOW, isDark = false)

        assertTrue("penutup sudut bentuk balon", balon.cornerMaskRes != 0)
        assertEquals(0, stiker.cornerMaskRes)
        assertEquals(0, jendela.cornerMaskRes)
    }

    /**
     * Penutup sudut bentuk balon berisi warna badan dan garis tepi bingkainya,
     * jadi ia punya versi terang dan gelap seperti aset bentuk lain. Kalau satu
     * aset dipakai di kedua tema, penutupnya akan terlihat sebagai tambalan.
     */
    @Test
    fun `penutup sudut bentuk balon ikut tema`() {
        val terang = widgetStyle(WidgetSkin.SPEECH, isDark = false)
        val gelap = widgetStyle(WidgetSkin.SPEECH, isDark = true)

        assertNotEquals(terang.cornerMaskRes, gelap.cornerMaskRes)
    }

    /**
     * Badan bentuk jendela dipakai mengisi sudut luar foto bulat. Kalau warnanya
     * sama dengan dua bentuk lain, sudut itu tidak akan terlihat sebagai bagian
     * dari bentuk jendela.
     */
    @Test
    fun `badan bentuk jendela berbeda dari bentuk lain`() {
        val stiker = widgetStyle(WidgetSkin.STICKER, isDark = false)
        val jendela = widgetStyle(WidgetSkin.WINDOW, isDark = false)

        assertNotEquals(stiker.bodyColor, jendela.bodyColor)
    }
}

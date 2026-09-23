package com.khalied.cukinggo.widget

import org.junit.Assert.assertNotEquals
import org.junit.Test

class WidgetPaletteTest {

    /**
     * Kalau ketiganya tidak ikut berganti, mode gelap widget akan memakai aset
     * terang dan teksnya bisa tidak terbaca di layar utama.
     */
    @Test
    fun `mode gelap memakai aset dan warna yang berbeda dari mode terang`() {
        val terang = widgetPalette(isDark = false)
        val gelap = widgetPalette(isDark = true)

        assertNotEquals(terang.frameRes, gelap.frameRes)
        assertNotEquals(terang.fallbackArtRes, gelap.fallbackArtRes)
        assertNotEquals(terang.captionColor, gelap.captionColor)
    }
}

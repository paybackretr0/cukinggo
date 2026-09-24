package com.khalied.cukinggo.widget

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetSkinModeTest {

    @Test
    fun `mode otomatis mengikuti rotasi harian`() {
        val tanggal = LocalDate.of(2026, 9, 25)

        assertEquals(WidgetSkin.forDate(tanggal), WidgetSkinMode.AUTO.skinFor(tanggal))
    }

    /**
     * Kalau mode yang dipaksa ikut berubah bersama tanggal, pilihan pengguna tidak
     * ada artinya.
     */
    @Test
    fun `bentuk yang dipaksa pengguna tidak berubah walau tanggalnya berganti`() {
        val kemarin = LocalDate.of(2026, 9, 24)
        val besok = LocalDate.of(2026, 9, 26)

        listOf(kemarin, besok).forEach { tanggal ->
            assertEquals(WidgetSkin.STICKER, WidgetSkinMode.STICKER.skinFor(tanggal))
            assertEquals(WidgetSkin.SPEECH, WidgetSkinMode.SPEECH.skinFor(tanggal))
            assertEquals(WidgetSkin.WINDOW, WidgetSkinMode.WINDOW.skinFor(tanggal))
        }
    }

    @Test
    fun `key tidak dikenal atau belum pernah disimpan dianggap otomatis`() {
        assertEquals(WidgetSkinMode.AUTO, WidgetSkinMode.fromKey(null))
        assertEquals(WidgetSkinMode.AUTO, WidgetSkinMode.fromKey(""))
        assertEquals(WidgetSkinMode.AUTO, WidgetSkinMode.fromKey("stiker"))
        assertEquals(WidgetSkinMode.WINDOW, WidgetSkinMode.fromKey("window"))
    }

    @Test
    fun `setiap mode bisa disimpan lalu dibaca balik`() {
        WidgetSkinMode.entries.forEach { mode ->
            assertEquals(mode, WidgetSkinMode.fromKey(mode.storageKey))
        }
    }
}

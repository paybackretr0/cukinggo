package com.khalied.cukinggo.widget

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetSkinTest {

    @Test
    fun `tanggal yang sama selalu memberi bentuk yang sama`() {
        val tanggal = LocalDate.of(2026, 9, 24)

        assertEquals(WidgetSkin.forDate(tanggal), WidgetSkin.forDate(tanggal))
    }

    @Test
    fun `bentuknya berganti tiap hari`() {
        val hariIni = WidgetSkin.forDate(LocalDate.of(2026, 9, 24))
        val besok = WidgetSkin.forDate(LocalDate.of(2026, 9, 25))

        assertNotEquals("bentuk dua hari berurutan tidak boleh sama", hariIni, besok)
    }

    @Test
    fun `tiga hari berturut-turut menampilkan ketiga bentuk`() {
        val awal = LocalDate.of(2026, 9, 24)

        val tigaHari = (0L..2L).map { WidgetSkin.forDate(awal.plusDays(it)) }

        assertEquals(WidgetSkin.entries.size, tigaHari.toSet().size)
    }

    @Test
    fun `tanggal sebelum 1970 tetap memberi bentuk yang sah`() {
        assertTrue(WidgetSkin.forDate(LocalDate.of(1969, 12, 31)) in WidgetSkin.entries)
    }
}

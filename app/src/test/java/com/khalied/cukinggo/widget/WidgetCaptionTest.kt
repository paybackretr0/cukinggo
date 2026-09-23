package com.khalied.cukinggo.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetCaptionTest {

    private val funnyLines = listOf("Baris satu.", "Baris dua.", "Baris tiga.")

    @Test
    fun `catatan pengguna dipakai apa adanya`() {
        val caption = widgetCaption("Lagi tidur di kursi.", catId = 7, funnyLines = funnyLines)

        assertEquals("Lagi tidur di kursi.", caption)
    }

    @Test
    fun `catatan dirapikan dari spasi berlebih`() {
        val caption = widgetCaption("   Minta makan.  ", catId = 7, funnyLines = funnyLines)

        assertEquals("Minta makan.", caption)
    }

    @Test
    fun `tanpa catatan, widget memakai baris lucu`() {
        val caption = widgetCaption(null, catId = 4, funnyLines = funnyLines)

        assertTrue(caption in funnyLines)
    }

    @Test
    fun `catatan berisi spasi saja dianggap belum ada catatan`() {
        val caption = widgetCaption("    ", catId = 4, funnyLines = funnyLines)

        assertTrue(caption in funnyLines)
    }

    @Test
    fun `kucing yang sama selalu dapat baris yang sama`() {
        val pertama = widgetCaption(null, catId = 42, funnyLines = funnyLines)
        val kedua = widgetCaption(null, catId = 42, funnyLines = funnyLines)

        assertEquals(pertama, kedua)
    }

    @Test
    fun `baris lucu bergilir untuk kucing yang berbeda`() {
        val semuaTeks = (1L..12L).map { id -> widgetCaption(null, id, funnyLines) }

        assertEquals(funnyLines.size, semuaTeks.toSet().size)
    }

    @Test
    fun `id nol dan negatif tetap memilih baris yang sah`() {
        assertTrue(widgetCaption(null, catId = 0, funnyLines = funnyLines) in funnyLines)
        assertTrue(widgetCaption(null, catId = -5, funnyLines = funnyLines) in funnyLines)
    }

    @Test
    fun `daftar baris lucu kosong menghasilkan teks kosong, bukan crash`() {
        val caption = widgetCaption(null, catId = 3, funnyLines = emptyList())

        assertEquals("", caption)
    }
}

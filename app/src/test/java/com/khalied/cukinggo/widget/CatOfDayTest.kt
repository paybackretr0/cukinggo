package com.khalied.cukinggo.widget

import com.khalied.cukinggo.domain.model.Cat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatOfDayTest {

    private val cats = listOf(
        cat(1, "Kucing satu"),
        cat(2, "Kucing dua"),
        cat(3, "Kucing tiga"),
        cat(4, "Kucing empat")
    )

    @Test
    fun `koleksi kosong tidak memilih apa pun`() {
        assertNull(catOfDay(emptyList(), epochDay = 20_000))
    }

    @Test
    fun `satu kucing saja selalu jadi kucing hari ini`() {
        val satu = listOf(cat(1, "Kucing satu"))

        assertEquals(satu[0], catOfDay(satu, epochDay = 0))
        assertEquals(satu[0], catOfDay(satu, epochDay = 1))
        assertEquals(satu[0], catOfDay(satu, epochDay = 20_000))
    }

    @Test
    fun `hari yang sama selalu memberi kucing yang sama`() {
        val pagi = catOfDay(cats, epochDay = 20_000)
        val malam = catOfDay(cats, epochDay = 20_000)

        assertEquals(pagi, malam)
    }

    @Test
    fun `hari berikutnya bergeser satu langkah`() {
        val hariIni = catOfDay(cats, epochDay = 20_000)
        val besok = catOfDay(cats, epochDay = 20_001)

        val urutanSekarang = cats.indexOf(hariIni)
        val urutanBesok = cats.indexOf(besok)
        assertEquals((urutanSekarang + 1) % cats.size, urutanBesok)
    }

    @Test
    fun `satu putaran penuh menampilkan setiap kucing sekali`() {
        val satuPutaran = (0L until cats.size.toLong()).map { hari -> catOfDay(cats, hari) }

        assertEquals(cats.size, satuPutaran.toSet().size)
    }

    @Test
    fun `tidak ada kucing yang muncul dua hari berturut-turut`() {
        val berurutan = (0L until 20L).map { hari -> catOfDay(cats, hari) }

        berurutan.zipWithNext().forEach { (hariIni, besok) ->
            assertTrue("kucing yang sama muncul dua hari berturut-turut", hariIni != besok)
        }
    }

    @Test
    fun `tanggal sebelum 1970 tetap memilih kucing yang sah`() {
        val hasil = catOfDay(cats, epochDay = -1)

        assertTrue(hasil in cats)
    }

    private fun cat(id: Long, description: String): Cat = Cat(
        id = id,
        photoPath = "cat_$id.jpg",
        description = description,
        latitude = -6.2,
        longitude = 106.8,
        timestamp = id * 1_000L
    )
}

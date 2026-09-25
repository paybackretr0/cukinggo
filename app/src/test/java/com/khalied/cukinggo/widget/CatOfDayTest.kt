package com.khalied.cukinggo.widget

import com.khalied.cukinggo.domain.model.CatSighting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatOfDayTest {

    private val sightings = listOf(
        sighting(1, "Kucing satu"),
        sighting(2, "Kucing dua"),
        sighting(3, "Kucing tiga"),
        sighting(4, "Kucing empat")
    )

    @Test
    fun `koleksi kosong tidak memilih apa pun`() {
        assertNull(catOfDay(emptyList(), epochDay = 20_000))
    }

    @Test
    fun `satu catatan saja selalu jadi cuking hari ini`() {
        val satu = listOf(sighting(1, "Kucing satu"))

        assertEquals(satu[0], catOfDay(satu, epochDay = 0))
        assertEquals(satu[0], catOfDay(satu, epochDay = 1))
        assertEquals(satu[0], catOfDay(satu, epochDay = 20_000))
    }

    @Test
    fun `hari yang sama selalu memberi catatan yang sama`() {
        val pagi = catOfDay(sightings, epochDay = 20_000)
        val malam = catOfDay(sightings, epochDay = 20_000)

        assertEquals(pagi, malam)
    }

    @Test
    fun `hari berikutnya bergeser satu langkah`() {
        val hariIni = catOfDay(sightings, epochDay = 20_000)
        val besok = catOfDay(sightings, epochDay = 20_001)

        val urutanSekarang = sightings.indexOf(hariIni)
        val urutanBesok = sightings.indexOf(besok)
        assertEquals((urutanSekarang + 1) % sightings.size, urutanBesok)
    }

    @Test
    fun `satu putaran penuh menampilkan setiap catatan sekali`() {
        val satuPutaran = (0L until sightings.size.toLong()).map { hari -> catOfDay(sightings, hari) }

        assertEquals(sightings.size, satuPutaran.toSet().size)
    }

    @Test
    fun `tidak ada catatan yang muncul dua hari berturut-turut`() {
        val berurutan = (0L until 20L).map { hari -> catOfDay(sightings, hari) }

        berurutan.zipWithNext().forEach { (hariIni, besok) ->
            assertTrue("catatan yang sama muncul dua hari berturut-turut", hariIni != besok)
        }
    }

    @Test
    fun `tanggal sebelum 1970 tetap memilih catatan yang sah`() {
        val hasil = catOfDay(sightings, epochDay = -1)

        assertTrue(hasil in sightings)
    }

    private fun sighting(id: Long, description: String): CatSighting = CatSighting(
        id = id,
        catId = id,
        catName = null,
        photoPath = "cat_$id.jpg",
        description = description,
        latitude = -6.2,
        longitude = 106.8,
        timestamp = id * 1_000L
    )
}

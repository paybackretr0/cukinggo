package com.khalied.cukinggo.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pemetaan dari tabel ke model domain. Dua bentuk yang diuji: satu penemuan
 * (dengan namanya menyusul dari tabel profil), dan satu profil beserta seluruh
 * penemuannya (lihat `toDomainOrNull`).
 */
class CatMappingTest {

    @Test
    fun `penemuan dipetakan ke domain tanpa kehilangan data`() {
        val entity = CatSightingEntity(
            id = 7L,
            catId = 3L,
            photoPath = "/data/files/cat_7.jpg",
            description = "Cuking oren di warung",
            latitude = -6.2,
            longitude = 106.816666,
            timestamp = 1_700_000_000_000L
        )

        val sighting = entity.toDomain(catName = "si Kumis")

        assertEquals(7L, sighting.id)
        assertEquals(3L, sighting.catId)
        assertEquals("si Kumis", sighting.catName)
        assertEquals("/data/files/cat_7.jpg", sighting.photoPath)
        assertEquals("Cuking oren di warung", sighting.description)
        assertEquals(-6.2, sighting.latitude, 0.0)
        assertEquals(106.816666, sighting.longitude, 0.0)
        assertEquals(1_700_000_000_000L, sighting.timestamp)
    }

    @Test
    fun `catatan kosong tetap null saat dipetakan`() {
        val row = CatSightingRow(
            id = 9L,
            catId = 4L,
            catName = "si Belang",
            photoPath = "/data/files/cat_9.jpg",
            description = null,
            latitude = 3.0,
            longitude = 4.0,
            timestamp = 99L
        )

        val sighting = row.toDomain()

        assertNull(sighting.description)
        assertEquals("si Belang", sighting.catName)
        assertEquals(9L, sighting.id)
    }

    @Test
    fun `profil tanpa penemuan tidak dianggap ada`() {
        val kosong = CatWithSightings(
            cat = CatEntity(id = 1L, name = null, createdAt = 10L),
            sightings = emptyList()
        )

        assertNull(kosong.toDomainOrNull())
    }

    @Test
    fun `penemuan diurutkan terbaru dulu, dan id jadi pemutus serinya`() {
        val profil = CatWithSightings(
            cat = CatEntity(id = 1L, name = "si Kumis", createdAt = 10L),
            sightings = listOf(
                sighting(id = 2L, timestamp = 200L),
                sighting(id = 5L, timestamp = 300L),
                sighting(id = 3L, timestamp = 300L)
            )
        )

        val cat = profil.toDomainOrNull()

        // Dua catatan berwaktu sama: yang terbesar idnya dianggap lebih baru.
        assertEquals(listOf(5L, 3L, 2L), cat?.sightings?.map { it.id })
        assertEquals(5L, cat?.latest?.id)
    }

    @Test
    fun `nama profil ikut ke setiap penemuannya`() {
        val profil = CatWithSightings(
            cat = CatEntity(id = 1L, name = "si Kumis", createdAt = 10L),
            sightings = listOf(
                sighting(id = 1L, timestamp = 100L),
                sighting(id = 2L, timestamp = 200L)
            )
        )

        val cat = profil.toDomainOrNull()

        // Satu cuking cuma punya satu nama, dan itu tinggal di profilnya.
        assertTrue(cat?.sightings?.all { it.catName == "si Kumis" } == true)
    }

    private fun sighting(id: Long, timestamp: Long): CatSightingEntity = CatSightingEntity(
        id = id,
        catId = 1L,
        photoPath = "/data/files/cat_$id.jpg",
        description = null,
        latitude = -6.2,
        longitude = 106.8,
        timestamp = timestamp
    )
}

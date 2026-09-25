package com.khalied.cukinggo.ui.components

import com.khalied.cukinggo.domain.model.CatSighting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapTrailsTest {

    private fun sighting(
        id: Long,
        catId: Long,
        latitude: Double,
        longitude: Double,
        timestamp: Long
    ) = CatSighting(
        id = id,
        catId = catId,
        catName = null,
        photoPath = "/tmp/cat_$id.jpg",
        description = null,
        latitude = latitude,
        longitude = longitude,
        timestamp = timestamp
    )

    @Test
    fun `dua tempat berbeda dari satu cuking jadi satu jejak`() {
        // Masukannya terbaru dulu, sama seperti yang datang dari repository.
        val sightings = listOf(
            sighting(id = 2, catId = 1, latitude = -6.19, longitude = 106.82, timestamp = 200L),
            sighting(id = 1, catId = 1, latitude = -6.20, longitude = 106.80, timestamp = 100L)
        )

        val trails = buildTrails(sightings)

        assertEquals(1, trails.size)
        assertEquals(1L, trails.single().catId)
        assertEquals(2, trails.single().points.size)
    }

    @Test
    fun `jejak diurutkan dari tempat yang paling lama`() {
        val sightings = listOf(
            sighting(id = 3, catId = 1, latitude = -6.18, longitude = 106.84, timestamp = 300L),
            sighting(id = 1, catId = 1, latitude = -6.20, longitude = 106.80, timestamp = 100L),
            sighting(id = 2, catId = 1, latitude = -6.19, longitude = 106.82, timestamp = 200L)
        )

        val points = buildTrails(sightings).single().points

        assertEquals(listOf(1L, 2L, 3L), points.map { it.sightingId })
    }

    @Test
    fun `waktu yang sama dipecah oleh id supaya urutannya pasti`() {
        val sightings = listOf(
            sighting(id = 9, catId = 1, latitude = -6.18, longitude = 106.84, timestamp = 100L),
            sighting(id = 2, catId = 1, latitude = -6.20, longitude = 106.80, timestamp = 100L)
        )

        val points = buildTrails(sightings).single().points

        assertEquals(listOf(2L, 9L), points.map { it.sightingId })
    }

    @Test
    fun `ketemu lagi di tempat yang sama tidak menambah titik`() {
        val sightings = listOf(
            sighting(id = 3, catId = 1, latitude = -6.19, longitude = 106.82, timestamp = 300L),
            sighting(id = 2, catId = 1, latitude = -6.20, longitude = 106.80, timestamp = 200L),
            sighting(id = 1, catId = 1, latitude = -6.20, longitude = 106.80, timestamp = 100L)
        )

        val points = buildTrails(sightings).single().points

        // Tiga catatan, dua tempat: yang berulang dipakai versi terbarunya.
        assertEquals(listOf(2L, 3L), points.map { it.sightingId })
    }

    @Test
    fun `selisih koordinat sekecil satu meter masih dianggap tempat yang sama`() {
        val sightings = listOf(
            sighting(id = 2, catId = 1, latitude = -6.2000001, longitude = 106.8, timestamp = 200L),
            sighting(id = 1, catId = 1, latitude = -6.2000000, longitude = 106.8, timestamp = 100L)
        )

        // Dua pembacaan GPS di titik yang sama tidak persis sama angkanya, dan itu
        // tidak boleh terbaca sebagai cuking yang berpindah tempat.
        assertTrue(buildTrails(sightings).isEmpty())
    }

    @Test
    fun `kembali ke tempat lama tetap dihitung sebagai gerakan`() {
        val sightings = listOf(
            sighting(id = 1, catId = 1, latitude = -6.20, longitude = 106.80, timestamp = 100L),
            sighting(id = 2, catId = 1, latitude = -6.19, longitude = 106.82, timestamp = 200L),
            sighting(id = 3, catId = 1, latitude = -6.20, longitude = 106.80, timestamp = 300L)
        )

        val points = buildTrails(sightings).single().points

        // Pulang ke tempat pertama bukan pengulangan: urutannya A, B, A.
        assertEquals(listOf(1L, 2L, 3L), points.map { it.sightingId })
    }

    @Test
    fun `satu tempat saja tidak punya jejak`() {
        val sightings = listOf(
            sighting(id = 1, catId = 1, latitude = -6.20, longitude = 106.80, timestamp = 100L),
            sighting(id = 2, catId = 1, latitude = -6.20, longitude = 106.80, timestamp = 200L)
        )

        assertTrue(buildTrails(sightings).isEmpty())
    }

    @Test
    fun `tiap cuking punya jejaknya sendiri`() {
        val sightings = listOf(
            sighting(id = 1, catId = 1, latitude = -6.20, longitude = 106.80, timestamp = 100L),
            sighting(id = 2, catId = 1, latitude = -6.19, longitude = 106.82, timestamp = 200L),
            sighting(id = 3, catId = 2, latitude = -6.30, longitude = 106.90, timestamp = 100L),
            sighting(id = 4, catId = 2, latitude = -6.31, longitude = 106.91, timestamp = 200L)
        )

        val trails = buildTrails(sightings)

        assertEquals(listOf(1L, 2L), trails.map { it.catId })
        assertTrue(trails.all { trail -> trail.points.size == 2 })
    }

    @Test
    fun `daftar kosong tidak menghasilkan jejak`() {
        assertTrue(buildTrails(emptyList()).isEmpty())
    }

    @Test
    fun `warna jejak tetap sama untuk cuking yang sama`() {
        assertEquals(trailColorFor(3L), trailColorFor(3L))
    }

    @Test
    fun `cuking yang berbeda tidak selalu dapat warna yang sama`() {
        // Empat warna dipakai bergiliran, jadi cuking yang berurutan idnya berbeda.
        assertNotEquals(trailColorFor(1L), trailColorFor(2L))
    }

    @Test
    fun `daftar kosong tidak punya titik pandang`() {
        assertNull(trailFocus(emptyList()))
    }

    @Test
    fun `titik pandang ada di tengah bentang jejak`() {
        val sightings = listOf(
            sighting(id = 1, catId = 1, latitude = -6.10, longitude = 106.70, timestamp = 100L),
            sighting(id = 2, catId = 1, latitude = -6.30, longitude = 106.90, timestamp = 200L)
        )

        val focus = trailFocus(sightings)

        assertEquals(-6.20, focus?.latitude ?: 0.0, 0.000001)
        assertEquals(106.80, focus?.longitude ?: 0.0, 0.000001)
    }

    @Test
    fun `jejak yang lebih panjang dipaskan dari zoom yang lebih jauh`() {
        val dekat = listOf(
            sighting(id = 1, catId = 1, latitude = -6.2000, longitude = 106.8000, timestamp = 100L),
            sighting(id = 2, catId = 1, latitude = -6.2005, longitude = 106.8005, timestamp = 200L)
        )
        val jauh = listOf(
            sighting(id = 1, catId = 1, latitude = -6.20, longitude = 106.80, timestamp = 100L),
            sighting(id = 2, catId = 1, latitude = -7.80, longitude = 110.40, timestamp = 200L)
        )

        val zoomDekat = trailFocus(dekat)?.zoom ?: 0.0
        val zoomJauh = trailFocus(jauh)?.zoom ?: 0.0

        assertTrue("jejak panjang harus terlihat dari lebih jauh", zoomJauh < zoomDekat)
    }

    @Test
    fun `jejak yang pendek tidak dipaskan lebih dekat dari zoom marker foto`() {
        val sightings = listOf(
            sighting(id = 1, catId = 1, latitude = -6.2000, longitude = 106.8000, timestamp = 100L),
            sighting(id = 2, catId = 1, latitude = -6.2001, longitude = 106.8001, timestamp = 200L)
        )

        // Tanpa batas ini, jejak beberapa meter akan di-zoom sampai peta tidak
        // terbaca lagi.
        assertEquals(16.0, trailFocus(sightings)?.zoom ?: 0.0, 0.000001)
    }

    @Test
    fun `jejak satu tempat tetap punya titik pandang yang wajar`() {
        val sightings = listOf(
            sighting(id = 1, catId = 1, latitude = -6.2, longitude = 106.8, timestamp = 100L),
            sighting(id = 2, catId = 1, latitude = -6.2, longitude = 106.8, timestamp = 200L)
        )

        val focus = trailFocus(sightings)

        assertNotNull(focus)
        assertEquals(-6.2, focus?.latitude ?: 0.0, 0.000001)
        assertTrue((focus?.zoom ?: 0.0) in 12.0..16.0)
    }
}

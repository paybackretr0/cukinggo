package com.khalied.cukinggo.location

import com.khalied.cukinggo.domain.model.CatSighting
import com.khalied.cukinggo.util.distanceMeters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NearestCatTest {

    private val akuLatitude = -6.2
    private val akuLongitude = 106.8

    @Test
    fun `daftar kosong tidak memilih apa pun`() {
        assertNull(nearestCat(emptyList(), akuLatitude, akuLongitude))
    }

    @Test
    fun `yang paling dekat dari beberapa cuking yang masuk radius`() {
        val sightings = listOf(
            sighting(catId = 1, latitude = -6.204, longitude = 106.8),
            sighting(catId = 2, latitude = -6.201, longitude = 106.8),
            sighting(catId = 3, latitude = -6.203, longitude = 106.8)
        )

        val hasil = nearestCat(sightings, akuLatitude, akuLongitude)

        assertNotNull(hasil)
        assertEquals(2L, hasil?.sighting?.catId)
    }

    @Test
    fun `jarak yang dilaporkan sama dengan jarak sebenarnya`() {
        val sightings = listOf(sighting(catId = 1, latitude = -6.201, longitude = 106.8))

        val hasil = nearestCat(sightings, akuLatitude, akuLongitude)

        assertNotNull(hasil)
        assertEquals(
            distanceMeters(akuLatitude, akuLongitude, -6.201, 106.8),
            hasil?.distanceMeters ?: 0.0,
            0.000001
        )
    }

    @Test
    fun `semua yang di luar radius tidak dipilih`() {
        val sightings = listOf(
            sighting(catId = 1, latitude = -6.26, longitude = 106.8),
            sighting(catId = 2, latitude = -6.27, longitude = 106.8)
        )

        assertNull(nearestCat(sightings, akuLatitude, akuLongitude, maxMeters = 1_000.0))
    }

    @Test
    fun `cuking di ujung radius masih terhitung, yang lebih jauh dari itu tidak`() {
        val sightings = listOf(sighting(catId = 1, latitude = -6.203, longitude = 106.8))
        val jarak = distanceMeters(akuLatitude, akuLongitude, -6.203, 106.8)

        assertNotNull(
            "jarak yang persis sama dengan radius masih masuk",
            nearestCat(sightings, akuLatitude, akuLongitude, maxMeters = jarak)
        )
        assertNull(
            "sedikit lebih jauh dari radius sudah tidak masuk",
            nearestCat(sightings, akuLatitude, akuLongitude, maxMeters = jarak - 1.0)
        )
    }

    @Test
    fun `dua cuking berjarak sama dimenangkan yang lebih baru di daftar`() {
        // Daftar dari Room urutannya terbaru dulu, jadi yang di depan adalah yang
        // lebih baru ditandai.
        val sightings = listOf(
            sighting(catId = 7, latitude = -6.201, longitude = 106.8),
            sighting(catId = 9, latitude = -6.199, longitude = 106.8)
        )

        val hasil = nearestCat(sightings, akuLatitude, akuLongitude)

        assertEquals(7L, hasil?.sighting?.catId)
    }

    @Test
    fun `radius bawaannya lima ratus meter`() {
        val dekat = listOf(sighting(catId = 1, latitude = -6.203, longitude = 106.8))
        val jauh = listOf(sighting(catId = 2, latitude = -6.207, longitude = 106.8))

        assertNotNull(
            "sekitar 330 m masih masuk radius bawaan",
            nearestCat(dekat, akuLatitude, akuLongitude)
        )
        assertNull(
            "sekitar 780 m sudah di luar radius bawaan",
            nearestCat(jauh, akuLatitude, akuLongitude)
        )
        assertEquals(500.0, NEARBY_CAT_RADIUS_METERS, 0.0)
    }

    private fun sighting(catId: Long, latitude: Double, longitude: Double): CatSighting =
        CatSighting(
            id = catId,
            catId = catId,
            catName = null,
            photoPath = "cat_$catId.jpg",
            description = null,
            latitude = latitude,
            longitude = longitude,
            timestamp = catId * 1_000L
        )
}

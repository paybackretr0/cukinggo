package com.khalied.cukinggo.ui.components

import com.khalied.cukinggo.domain.model.CatSighting
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapClusteringTest {

    private fun sighting(id: Long, latitude: Double, longitude: Double) = CatSighting(
        id = id,
        catId = id,
        catName = null,
        photoPath = "/tmp/cat_$id.jpg",
        description = null,
        latitude = latitude,
        longitude = longitude,
        timestamp = id
    )

    @Test
    fun `penemuan yang berdekatan digabung jadi satu cluster`() {
        val sightings = listOf(
            sighting(1, -6.20000, 106.80000),
            sighting(2, -6.20010, 106.80010),
            sighting(3, -6.20020, 106.80005)
        )

        val clusters = clusterSightings(sightings, zoomLevel = 10)

        assertEquals(1, clusters.size)
        assertEquals(3, clusters.single().count)
    }

    @Test
    fun `penemuan yang berjauhan tetap terpisah`() {
        val sightings = listOf(
            sighting(1, -6.2, 106.8), // Jakarta
            sighting(2, -7.8, 110.4), // Yogyakarta
            sighting(3, -8.65, 115.2) // Bali
        )

        val clusters = clusterSightings(sightings, zoomLevel = 6)

        assertEquals(3, clusters.size)
        assertEquals(3, clusters.sumOf { it.count })
    }

    @Test
    fun `titik cluster memakai rata-rata anggotanya`() {
        val sightings = listOf(
            sighting(1, -6.0, 106.0),
            sighting(2, -6.2, 106.2)
        )

        // Pada zoom 6 ukuran sel ~4.2°, kedua titik pasti jatuh di sel yang sama.
        val cluster = clusterSightings(sightings, zoomLevel = 6).single()

        assertTrue(abs(cluster.latitude - (-6.1)) < 0.0001)
        assertTrue(abs(cluster.longitude - 106.1) < 0.0001)
    }

    @Test
    fun `zoom lebih dekat menghasilkan sel yang lebih kecil`() {
        assertTrue(clusterCellSizeDegrees(14) < clusterCellSizeDegrees(10))
        assertTrue(clusterCellSizeDegrees(18) < clusterCellSizeDegrees(14))
    }

    @Test
    fun `daftar kosong tidak menghasilkan cluster`() {
        assertTrue(clusterSightings(emptyList(), zoomLevel = 10).isEmpty())
    }
}

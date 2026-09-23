package com.khalied.cukinggo.ui.components

import com.khalied.cukinggo.domain.model.Cat
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapClusteringTest {

    private fun cat(id: Long, latitude: Double, longitude: Double) = Cat(
        id = id,
        photoPath = "/tmp/cat_$id.jpg",
        description = null,
        latitude = latitude,
        longitude = longitude,
        timestamp = id
    )

    @Test
    fun `kucing yang berdekatan digabung jadi satu cluster`() {
        val cats = listOf(
            cat(1, -6.20000, 106.80000),
            cat(2, -6.20010, 106.80010),
            cat(3, -6.20020, 106.80005)
        )

        val clusters = clusterCats(cats, zoomLevel = 10)

        assertEquals(1, clusters.size)
        assertEquals(3, clusters.single().count)
    }

    @Test
    fun `kucing yang berjauhan tetap terpisah`() {
        val cats = listOf(
            cat(1, -6.2, 106.8),   // Jakarta
            cat(2, -7.8, 110.4),   // Yogyakarta
            cat(3, -8.65, 115.2)   // Bali
        )

        val clusters = clusterCats(cats, zoomLevel = 6)

        assertEquals(3, clusters.size)
        assertEquals(3, clusters.sumOf { it.count })
    }

    @Test
    fun `titik cluster memakai rata-rata anggotanya`() {
        val cats = listOf(
            cat(1, -6.0, 106.0),
            cat(2, -6.2, 106.2)
        )

        // Pada zoom 6 ukuran sel ~4.2°, kedua titik pasti jatuh di sel yang sama.
        val cluster = clusterCats(cats, zoomLevel = 6).single()

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
        assertTrue(clusterCats(emptyList(), zoomLevel = 10).isEmpty())
    }
}

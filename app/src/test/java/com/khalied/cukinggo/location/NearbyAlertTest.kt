package com.khalied.cukinggo.location

import com.khalied.cukinggo.domain.model.Cat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbyAlertTest {

    private val cats = listOf(
        cat(id = 3, latitude = -6.2, longitude = 106.8),
        cat(id = 2, latitude = -6.3, longitude = 106.9),
        cat(id = 1, latitude = -6.4, longitude = 107.0)
    )

    @Test
    fun `semua kucing jadi area pantauan`() {
        val areas = catWatchAreas(cats)

        assertEquals(listOf(3L, 2L, 1L), areas.map { it.catId })
        assertEquals(-6.3, areas[1].latitude, 0.000001)
        assertEquals(106.9, areas[1].longitude, 0.000001)
    }

    @Test
    fun `area pantauan dibatasi jumlahnya`() {
        val banyak = (1L..150L).map { id -> cat(id = id, latitude = -6.2, longitude = 106.8) }

        val areas = catWatchAreas(banyak)

        assertEquals(MAX_WATCH_AREAS, areas.size)
        // Yang diambil adalah yang terbaru, yaitu urutan paling depan.
        assertEquals(1L, areas.first().catId)
    }

    @Test
    fun `belum pernah dikabarkan berarti boleh dikabarkan`() {
        assertTrue(shouldAlert(lastAlertedAt = null, now = 1_000L))
    }

    @Test
    fun `kabar berikutnya menunggu sampai masa jeda lewat`() {
        val sekarang = 100_000L

        assertFalse(
            "kabar kedua masih dalam masa jeda",
            shouldAlert(lastAlertedAt = sekarang - NEARBY_ALERT_COOLDOWN_MILLIS + 1, now = sekarang)
        )
        assertTrue(
            "kabar kedua sudah boleh setelah masa jeda penuh",
            shouldAlert(lastAlertedAt = sekarang - NEARBY_ALERT_COOLDOWN_MILLIS, now = sekarang)
        )
    }

    @Test
    fun `fitur yang dimatikan tidak butuh izin apa pun`() {
        val state = nearbyAlertState(
            enabled = false,
            canPostNotifications = false,
            hasForegroundLocation = false,
            hasBackgroundLocation = false
        )

        assertEquals(NearbyAlertState.OFF, state)
    }

    @Test
    fun `langkah izinnya berurutan, notifikasi dulu`() {
        assertEquals(
            NearbyAlertState.NEED_NOTIFICATION_PERMISSION,
            nearbyAlertState(true, canPostNotifications = false, hasForegroundLocation = false, hasBackgroundLocation = false)
        )
        assertEquals(
            NearbyAlertState.NEED_FOREGROUND_LOCATION,
            nearbyAlertState(true, canPostNotifications = true, hasForegroundLocation = false, hasBackgroundLocation = false)
        )
        assertEquals(
            NearbyAlertState.NEED_BACKGROUND_LOCATION,
            nearbyAlertState(true, canPostNotifications = true, hasForegroundLocation = true, hasBackgroundLocation = false)
        )
        assertEquals(
            NearbyAlertState.ACTIVE,
            nearbyAlertState(true, canPostNotifications = true, hasForegroundLocation = true, hasBackgroundLocation = true)
        )
    }

    private fun cat(id: Long, latitude: Double, longitude: Double): Cat = Cat(
        id = id,
        photoPath = "cat_$id.jpg",
        name = null,
        description = null,
        latitude = latitude,
        longitude = longitude,
        timestamp = id * 1_000L
    )
}

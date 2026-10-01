package com.khalied.cukinggo.util

import com.khalied.cukinggo.domain.model.CatProfile
import com.khalied.cukinggo.domain.model.CatSighting
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeeklyRecapTest {

    private val zone = ZoneId.of("Asia/Jakarta")
    private val today = LocalDate.of(2026, 9, 24)

    private fun timestampAt(daysAgo: Long, hour: Int = 12): Long =
        today.minusDays(daysAgo).atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    private fun sighting(id: Long, catId: Long, daysAgo: Long, hour: Int = 12) = CatSighting(
        id = id,
        catId = catId,
        catName = null,
        photoPath = "/tmp/cat_$id.jpg",
        description = null,
        latitude = 0.0,
        longitude = 0.0,
        timestamp = timestampAt(daysAgo, hour)
    )

    private fun profile(id: Long, daysAgo: Long) = CatProfile(
        id = id,
        name = null,
        createdAt = timestampAt(daysAgo)
    )

    @Test
    fun `selalu tujuh hari dan urut dari yang paling lama`() {
        val recap = weeklyRecap(emptyList(), emptyList(), today, zone)

        assertEquals(7, recap.days.size)
        assertEquals(today.minusDays(6), recap.days.first().date)
        assertEquals(today, recap.days.last().date)
    }

    @Test
    fun `catatan lebih tua dari tujuh hari tidak ikut dihitung`() {
        val recap = weeklyRecap(
            sightings = listOf(
                sighting(1, catId = 1, daysAgo = 0),
                sighting(2, catId = 2, daysAgo = 6),
                sighting(3, catId = 3, daysAgo = 7)
            ),
            catProfiles = emptyList(),
            today = today,
            zone = zone
        )

        assertEquals(2, recap.sightingCount)
        assertEquals(2, recap.activeDayCount)
    }

    @Test
    fun `hari kosong tetap muncul sebagai batang nol`() {
        val recap = weeklyRecap(
            sightings = listOf(sighting(1, catId = 1, daysAgo = 2)),
            catProfiles = emptyList(),
            today = today,
            zone = zone
        )

        assertEquals(1, recap.activeDayCount)
        assertEquals(0, recap.days.first().sightingCount)
        assertEquals(1, recap.days.last { day -> day.sightingCount == 1 }.sightingCount)
    }

    @Test
    fun `cuking baru dihitung dari tanggal profilnya dibuat`() {
        val recap = weeklyRecap(
            sightings = emptyList(),
            catProfiles = listOf(
                profile(id = 1, daysAgo = 1),
                profile(id = 2, daysAgo = 8)
            ),
            today = today,
            zone = zone
        )

        assertEquals(1, recap.newCatCount)
        assertTrue(recap.hasAnything)
    }

    @Test
    fun `satu cuking yang difoto beberapa kali cuma muncul sekali di daftar`() {
        val recap = weeklyRecap(
            sightings = listOf(
                sighting(1, catId = 7, daysAgo = 0),
                sighting(2, catId = 7, daysAgo = 1),
                sighting(3, catId = 9, daysAgo = 1)
            ),
            catProfiles = emptyList(),
            today = today,
            zone = zone
        )

        assertEquals(3, recap.sightingCount)
        assertEquals(2, recap.catsSeen.size)
        // Yang tersisa adalah penemuan terbaru tiap cuking, dan masukannya
        // terbaru dulu, jadi yang teratas adalah penemuan paling baru.
        assertEquals(1L, recap.catsSeen.first().id)
    }

    @Test
    fun `catatan bertanggal masa depan diabaikan`() {
        val recap = weeklyRecap(
            sightings = listOf(sighting(1, catId = 1, daysAgo = -1)),
            catProfiles = emptyList(),
            today = today,
            zone = zone
        )

        assertFalse(recap.hasAnything)
        assertEquals(0, recap.sightingCount)
    }
}

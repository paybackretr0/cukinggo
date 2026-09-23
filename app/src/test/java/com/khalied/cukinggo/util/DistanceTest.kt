package com.khalied.cukinggo.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DistanceTest {

    @Test
    fun `titik yang sama menghasilkan jarak nol`() {
        val distance = distanceMeters(-6.2, 106.816666, -6.2, 106.816666)
        assertTrue(distance < 0.001)
    }

    @Test
    fun `selisih 0,001 derajat lintang sekitar 111 meter`() {
        val distance = distanceMeters(-6.2000, 106.8000, -6.2010, 106.8000)
        assertTrue("jarak tak terduga: $distance", distance in 110.0..112.5)
    }

    @Test
    fun `jakarta ke bandung sekitar 120 kilometer`() {
        val distance = distanceMeters(-6.2, 106.816666, -6.9175, 107.6191)
        assertTrue("jarak tak terduga: $distance", distance in 115_000.0..125_000.0)
    }

    @Test
    fun `format jarak dekat dalam meter`() {
        assertEquals("180 m", formatDistance(180.4))
        assertEquals("0 m", formatDistance(0.2))
    }

    @Test
    fun `format jarak menengah dalam kilometer dengan satu desimal`() {
        assertEquals("1,2 km", formatDistance(1_240.0))
        assertEquals("9,9 km", formatDistance(9_940.0))
    }

    @Test
    fun `format jarak jauh tanpa desimal`() {
        assertEquals("15 km", formatDistance(15_400.0))
        assertEquals("120 km", formatDistance(119_600.0))
    }
}

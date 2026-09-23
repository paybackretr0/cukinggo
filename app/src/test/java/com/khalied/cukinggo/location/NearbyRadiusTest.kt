package com.khalied.cukinggo.location

import org.junit.Assert.assertEquals
import org.junit.Test

class NearbyRadiusTest {

    @Test
    fun `pilihan radiusnya 100, 200, dan 500 meter`() {
        assertEquals(listOf(100, 200, 500), NearbyRadius.entries.map { it.meters })
    }

    @Test
    fun `radius bawaan 200 meter`() {
        assertEquals(NearbyRadius.NORMAL, NearbyRadius.DEFAULT)
        assertEquals(200, NearbyRadius.DEFAULT.meters)
    }

    @Test
    fun `angka tersimpan dibaca kembali sebagai pilihan yang sama`() {
        NearbyRadius.entries.forEach { radius ->
            assertEquals(radius, NearbyRadius.fromMeters(radius.meters))
        }
    }

    @Test
    fun `angka asing jatuh ke radius bawaan`() {
        assertEquals(NearbyRadius.DEFAULT, NearbyRadius.fromMeters(50))
        assertEquals(NearbyRadius.DEFAULT, NearbyRadius.fromMeters(0))
        assertEquals(NearbyRadius.DEFAULT, NearbyRadius.fromMeters(null))
    }
}

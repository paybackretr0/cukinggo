package com.khalied.cukinggo.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CatShareTest {

    /**
     * Bentuk link ini bukan selera: dokumentasi Maps URLs meminta `api=1`,
     * parameter `query`, dan koma pemisah koordinat di-encode jadi `%2C`.
     * Kalau salah, link-nya tidak membuka titik apa pun.
     */
    @Test
    fun `link maps memakai format resmi Maps URLs`() {
        val link = mapsLinkFor(-6.2, 106.816666)

        assertEquals(
            "https://www.google.com/maps/search/?api=1&query=-6.20000%2C106.81667",
            link
        )
    }

    @Test
    fun `koordinat selalu lima angka desimal dan tanpa spasi`() {
        val link = mapsLinkFor(-6.917464, 107.619123)

        assertTrue(link.contains("-6.91746%2C107.61912"))
        assertTrue("tidak boleh ada spasi di dalam URL", !link.contains(" "))
    }

    @Test
    fun `koordinat positif juga benar`() {
        val link = mapsLinkFor(47.5951518, -122.3316393)

        assertEquals(
            "https://www.google.com/maps/search/?api=1&query=47.59515%2C-122.33164",
            link
        )
    }
}

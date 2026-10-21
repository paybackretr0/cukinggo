package com.khalied.cukinggo.detect

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CatPresenceTest {

    @Test
    fun `satu frame belum cukup untuk menyalakan isyarat`() {
        val presence = CatPresence(framesToShow = 2, framesToHide = 3)

        assertFalse(presence.onResult(true))
        assertFalse(presence.visible)
    }

    @Test
    fun `dua frame berturut menyalakan isyarat`() {
        val presence = CatPresence(framesToShow = 2, framesToHide = 3)

        presence.onResult(true)
        assertTrue(presence.onResult(true))
    }

    @Test
    fun `satu frame meleset belum memadamkan isyarat`() {
        val presence = CatPresence(framesToShow = 2, framesToHide = 3)
        presence.onResult(true)
        presence.onResult(true)

        assertTrue(presence.onResult(false))
    }

    @Test
    fun `tiga frame meleset berturut memadamkan isyarat`() {
        val presence = CatPresence(framesToShow = 2, framesToHide = 3)
        presence.onResult(true)
        presence.onResult(true)

        presence.onResult(false)
        presence.onResult(false)
        assertTrue("Dua meleset masih dianggap terlihat", presence.visible)
        assertFalse("Tiga meleset harus memadamkan", presence.onResult(false))
    }

    @Test
    fun `frame yang menemukan cuking membatalkan hitungan meleset`() {
        val presence = CatPresence(framesToShow = 2, framesToHide = 3)
        presence.onResult(true)
        presence.onResult(true)

        presence.onResult(false)
        presence.onResult(false)
        presence.onResult(true)
        presence.onResult(false)
        presence.onResult(false)

        assertTrue("Hitungan meleset seharusnya diulang dari nol", presence.visible)
    }

    @Test
    fun `reset memadamkan isyarat dan hitungannya`() {
        val presence = CatPresence(framesToShow = 2, framesToHide = 3)
        presence.onResult(true)
        presence.onResult(true)

        presence.reset()

        assertFalse(presence.visible)
        // Satu frame setelah reset belum cukup, jadi hitungannya memang ikut nol.
        assertFalse(presence.onResult(true))
    }
}

package com.khalied.cukinggo.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BlankTextTest {

    @Test
    fun `null tetap null`() {
        assertNull(blankToNull(null))
    }

    @Test
    fun `teks kosong jadi null`() {
        assertNull(blankToNull(""))
    }

    @Test
    fun `teks berisi spasi saja jadi null`() {
        assertNull(blankToNull("   \t\n "))
    }

    @Test
    fun `spasi di ujung dibuang`() {
        assertEquals("si Kumis", blankToNull("  si Kumis  "))
    }

    @Test
    fun `spasi di tengah nama tidak diubah`() {
        assertEquals("si Kumis Belang", blankToNull("si Kumis Belang"))
    }
}

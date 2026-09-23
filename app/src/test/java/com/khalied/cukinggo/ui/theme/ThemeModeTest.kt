package com.khalied.cukinggo.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {

    @Test
    fun `ikut sistem meneruskan kondisi hp`() {
        assertTrue(ThemeMode.SYSTEM.isDark(systemInDarkTheme = true))
        assertFalse(ThemeMode.SYSTEM.isDark(systemInDarkTheme = false))
    }

    @Test
    fun `terang selalu terang dan gelap selalu gelap`() {
        assertFalse(ThemeMode.LIGHT.isDark(systemInDarkTheme = true))
        assertTrue(ThemeMode.DARK.isDark(systemInDarkTheme = false))
    }

    @Test
    fun `key yang disimpan terbaca kembali sebagai mode yang sama`() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.fromKey(mode.storageKey))
        }
    }

    @Test
    fun `key kosong atau tidak dikenal kembali ke ikut sistem`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromKey(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromKey("neon"))
    }
}

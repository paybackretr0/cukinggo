package com.khalied.cukinggo.whatsapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StickerPackFormatTest {

    private fun manifest(stickers: List<PackSticker>) = StickerPackManifest(
        identifier = StickerPackManifest.IDENTIFIER,
        name = StickerPackManifest.NAME,
        publisher = StickerPackManifest.PUBLISHER,
        trayFileName = StickerPackManifest.TRAY_FILE_NAME,
        imageDataVersion = "42",
        stickers = stickers
    )

    @Test
    fun `tulisan yang disimpan bisa dibaca kembali`() {
        val source = manifest(
            listOf(
                PackSticker("sticker_001.webp", "🐱", "Stiker cuking bernama si Kumis"),
                PackSticker("sticker_002.webp", "🐱", null)
            )
        )

        val parsed = StickerPackManifest.parse(source.serialize())

        assertEquals(source, parsed)
    }

    @Test
    fun `teks untuk pembaca layar yang kosong tetap kosong`() {
        val parsed = StickerPackManifest.parse(
            manifest(listOf(PackSticker("sticker_001.webp", "🐱", ""))).serialize()
        )

        assertNull(parsed?.stickers?.first()?.accessibilityText)
    }

    @Test
    fun `tab dan baris baru di teks tidak memecah barisnya`() {
        val parsed = StickerPackManifest.parse(
            manifest(
                listOf(PackSticker("sticker_001.webp", "🐱", "cuking\nbertab\there"))
            ).serialize()
        )

        assertEquals(1, parsed?.stickers?.size)
        assertEquals("cuking bertab here", parsed?.stickers?.first()?.accessibilityText)
    }

    @Test
    fun `manifest yang tidak lengkap dibaca sebagai kosong`() {
        assertNull(StickerPackManifest.parse(""))
        assertNull(StickerPackManifest.parse("cukinggo\tCukingGo"))
        assertNull(StickerPackManifest.parse("a\tb\tc\td\te\n"))
    }

    @Test
    fun `baris stiker yang rusak dilewati, bukan menggagalkan semuanya`() {
        val text = "cukinggo\tCukingGo\tCukingGo\ttray.webp\t42\n" +
            "sticker_001.webp\t🐱\t\n" +
            "rusak\n"

        val parsed = StickerPackManifest.parse(text)

        assertEquals(1, parsed?.stickers?.size)
        assertEquals("sticker_001.webp", parsed?.stickers?.first()?.fileName)
    }

    @Test
    fun `paket butuh tiga sampai tiga puluh stiker`() {
        assertFalse(StickerPackRules.canExport(0))
        assertFalse(StickerPackRules.canExport(2))
        assertTrue(StickerPackRules.canExport(3))
        assertTrue(StickerPackRules.canExport(30))
        assertFalse(StickerPackRules.canExport(31))
    }

    @Test
    fun `yang ikut hanya tiga puluh stiker pertama`() {
        val paths = (1..35).map { "sticker_$it.png" }

        val ikut = StickerPackRules.takeForPack(paths)

        assertEquals(30, ikut.size)
        assertEquals("sticker_1.png", ikut.first())
        assertEquals("sticker_30.png", ikut.last())
    }

    @Test
    fun `nama file berurutan dan tetap tiga angka`() {
        assertEquals("sticker_001.webp", StickerPackRules.stickerFileName(0))
        assertEquals("sticker_010.webp", StickerPackRules.stickerFileName(9))
        assertEquals("sticker_030.webp", StickerPackRules.stickerFileName(29))
    }

    @Test
    fun `batas ukuran mengikuti aturan WhatsApp`() {
        assertEquals(512, StickerPackRules.STICKER_PX)
        assertEquals(96, StickerPackRules.TRAY_PX)
        assertEquals(100 * 1024, StickerPackRules.MAX_STICKER_BYTES)
        assertEquals(50 * 1024, StickerPackRules.MAX_TRAY_BYTES)
        assertTrue(StickerPackRules.QUALITY_STEPS.first() > StickerPackRules.QUALITY_STEPS.last())
    }
}

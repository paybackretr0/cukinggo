package com.khalied.cukinggo.whatsapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StickerPackSelectionTest {

    private val available = (1..5).map { "sticker_$it.png" }

    @Test
    fun `pilihan awal mengambil yang terbaru sampai tiga puluh`() {
        val banyak = (1..40).map { "sticker_$it.png" }

        val awal = StickerPackSelection.defaultSelection(banyak)

        assertEquals(30, awal.size)
        assertEquals("sticker_1.png", awal.first())
        assertEquals("sticker_30.png", awal.last())
    }

    @Test
    fun `pilihan awal mengikuti urutan yang diberikan layar`() {
        assertEquals(available, StickerPackSelection.defaultSelection(available))
    }

    @Test
    fun `mengetuk stiker yang belum dipilih memasukkannya`() {
        val hasil = StickerPackSelection.toggle(setOf("sticker_1.png"), "sticker_2.png")

        assertEquals(setOf("sticker_1.png", "sticker_2.png"), hasil)
    }

    @Test
    fun `mengetuk stiker yang sudah dipilih mengeluarkannya`() {
        val hasil = StickerPackSelection.toggle(setOf("sticker_1.png", "sticker_2.png"), "sticker_1.png")

        assertEquals(setOf("sticker_2.png"), hasil)
    }

    @Test
    fun `boleh turun di bawah tiga supaya pilihan bisa dikosongkan`() {
        val hasil = StickerPackSelection.toggle(setOf("sticker_1.png", "sticker_2.png"), "sticker_2.png")

        assertEquals(setOf("sticker_1.png"), hasil)
        assertFalse(StickerPackRules.canExport(hasil.size))
    }

    @Test
    fun `menambah saat sudah tiga puluh tidak mengubah apa pun`() {
        val penuh = (1..30).map { "sticker_$it.png" }.toSet()

        val hasil = StickerPackSelection.toggle(penuh, "sticker_31.png")

        assertEquals(30, hasil.size)
        assertFalse("sticker_31.png" in hasil)
    }

    @Test
    fun `mengeluarkan dari pilihan penuh tetap boleh`() {
        val penuh = (1..30).map { "sticker_$it.png" }.toSet()

        val hasil = StickerPackSelection.toggle(penuh, "sticker_5.png")

        assertEquals(29, hasil.size)
        assertFalse("sticker_5.png" in hasil)
    }

    @Test
    fun `stiker yang sudah tidak ada dibuang dari paket`() {
        val dipilih = setOf("sticker_1.png", "sticker_3.png", "hilang.png")

        val hasil = StickerPackSelection.ordered(dipilih, available)

        assertEquals(listOf("sticker_1.png", "sticker_3.png"), hasil)
    }

    @Test
    fun `urutan paket mengikuti urutan tampil, bukan urutan diketuk`() {
        val dipilih = setOf("sticker_4.png", "sticker_2.png")

        val hasil = StickerPackSelection.ordered(dipilih, available)

        assertEquals(listOf("sticker_2.png", "sticker_4.png"), hasil)
    }

    @Test
    fun `belum pernah memilih memakai pilihan bawaan`() {
        val hasil = StickerPackSelection.resolve(stored = null, available = available)

        assertEquals(available.toSet(), hasil)
    }

    @Test
    fun `pilihan yang sengaja dikosongkan dibiarkan kosong`() {
        val hasil = StickerPackSelection.resolve(stored = emptySet(), available = available)

        assertTrue(hasil.isEmpty())
    }

    @Test
    fun `stiker yang hilang dibuang dari pilihan`() {
        val hasil = StickerPackSelection.resolve(
            stored = setOf("sticker_1.png", "hilang.png"),
            available = available
        )

        assertEquals(setOf("sticker_1.png"), hasil)
    }

    @Test
    fun `pilihan yang semua stikernya sudah hilang kembali ke bawaan`() {
        val hasil = StickerPackSelection.resolve(
            stored = setOf("hilang_1.png", "hilang_2.png"),
            available = available
        )

        assertEquals(available.toSet(), hasil)
    }

    @Test
    fun `semua stiker bisa dipilih kalau jumlahnya di bawah batas`() {
        val semua = available.toSet()

        assertTrue(StickerPackRules.canExport(StickerPackSelection.ordered(semua, available).size))
    }
}

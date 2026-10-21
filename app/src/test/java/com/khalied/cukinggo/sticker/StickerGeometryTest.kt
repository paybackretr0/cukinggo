package com.khalied.cukinggo.sticker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StickerGeometryTest {

    /** Gambar 5 x 4 dengan satu kotak buram di dalamnya. */
    private fun picture(): IntArray {
        val pixels = IntArray(5 * 4)
        for (y in 1..2) {
            for (x in 1..3) {
                pixels[y * 5 + x] = 0xFF000000.toInt()
            }
        }
        return pixels
    }

    @Test
    fun `tepi potongan mengabaikan latar transparan`() {
        val bounds = alphaBounds(picture(), width = 5, height = 4)

        assertEquals(StickerBounds(left = 1, top = 1, right = 4, bottom = 3), bounds)
    }

    @Test
    fun `gambar yang seluruhnya transparan tidak punya tepi`() {
        val bounds = alphaBounds(IntArray(5 * 4), width = 5, height = 4)

        assertNull(bounds)
    }

    @Test
    fun `piksel yang nyaris transparan tidak dihitung`() {
        val pixels = IntArray(5 * 4)
        // Alpha 8: sisa latar yang tipis, bukan cuking.
        pixels[0] = 0x08FFFFFF
        pixels[1 * 5 + 1] = 0xFF000000.toInt()

        val bounds = alphaBounds(pixels, width = 5, height = 4)

        assertEquals(StickerBounds(left = 1, top = 1, right = 2, bottom = 2), bounds)
    }

    @Test
    fun `ambang alpha bisa dinaikkan`() {
        val pixels = IntArray(5 * 4)
        pixels[0] = 0x20FFFFFF
        pixels[1 * 5 + 1] = 0x80FFFFFF.toInt()

        val longgar = alphaBounds(pixels, width = 5, height = 4, minAlpha = 16)
        val ketat = alphaBounds(pixels, width = 5, height = 4, minAlpha = 64)

        assertEquals(StickerBounds(0, 0, 2, 2), longgar)
        assertEquals(StickerBounds(1, 1, 2, 2), ketat)
    }

    @Test
    fun `skala mengikuti sisi terpanjang supaya seluruh gambar muat`() {
        // Sisi 200 dipaskan ke 512 (2.56x), dan sisi 100 ikut skala yang sama,
        // jadi tingginya 256 dan tidak ada bagian yang terpotong.
        assertEquals(2.56f, fitScale(sourceWidth = 100, sourceHeight = 200, targetSize = 512), 0.001f)
        assertEquals(2.56f, fitScale(sourceWidth = 200, sourceHeight = 100, targetSize = 512), 0.001f)
        assertEquals(1.28f, fitScale(sourceWidth = 400, sourceHeight = 100, targetSize = 512), 0.001f)
    }

    @Test
    fun `gambar persegi memakai skala penuh`() {
        assertEquals(1f, fitScale(sourceWidth = 512, sourceHeight = 512, targetSize = 512), 0.001f)
        assertEquals(0.5f, fitScale(sourceWidth = 1024, sourceHeight = 1024, targetSize = 512), 0.001f)
    }
}

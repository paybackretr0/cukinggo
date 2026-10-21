package com.khalied.cukinggo.whatsapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Alasan penolakan dari WhatsApp adalah teks bebas dari app lain, jadi yang
 * diuji di sini adalah cara membacanya, bukan kalimat yang dihasilkan.
 */
class StickerPackRejectionTest {

    private val authority = "com.khalied.cukinggo.stickercontentprovider"

    @Test
    fun `provider yang tidak ditemukan di ruang klon dikenali`() {
        val alasan = "cannot find the provider for authority: $authority"

        assertEquals(
            StickerPackRejection.ProviderNotVisible,
            StickerPackRejection.from(alasan, authority)
        )
    }

    @Test
    fun `huruf besar kecil dan spasi di tepi tidak mengubah hasil`() {
        val alasan = "  Cannot find the provider for authority: $authority  "

        assertEquals(
            StickerPackRejection.ProviderNotVisible,
            StickerPackRejection.from(alasan, authority)
        )
    }

    @Test
    fun `kalimat lain yang menyebut provider tapi bukan authority kita dibiarkan apa adanya`() {
        val alasan = "cannot find the provider for authority: com.contoh.lain.stickercontentprovider"

        assertEquals(
            StickerPackRejection.Other(alasan),
            StickerPackRejection.from(alasan, authority)
        )
    }

    @Test
    fun `alasan yang tidak menyebut provider tetap ditampilkan apa adanya`() {
        val alasan = "sticker pack contains too few stickers"

        assertEquals(
            StickerPackRejection.Other(alasan),
            StickerPackRejection.from(alasan, authority)
        )
    }

    @Test
    fun `dialog yang ditutup tanpa alasan bukan penolakan`() {
        assertNull(StickerPackRejection.from(null, authority))
        assertNull(StickerPackRejection.from("", authority))
        assertNull(StickerPackRejection.from("   ", authority))
    }
}

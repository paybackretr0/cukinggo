package com.khalied.cukinggo.whatsapp

/**
 * Aturan paket stiker WhatsApp, dikumpulkan di satu tempat.
 *
 * Angka-angkanya bukan pilihan gaya: WhatsApp menolak paket yang melanggar
 * salah satunya, dan penolakannya terjadi di app lain, jadi pengguna cuma akan
 * melihat paket yang tidak muncul. Karena itu batasnya juga dipakai untuk
 * memutuskan apakah tombol "Tambahkan ke WhatsApp" pantas ditekan.
 */
object StickerPackRules {

    /** Sisi gambar stiker. Harus tepat, bukan maksimal. */
    const val STICKER_PX = 512

    /** Sisi ikon tray yang mewakili paket di pemilih stiker WhatsApp. */
    const val TRAY_PX = 96

    /** Batas ukuran satu stiker statis menurut WhatsApp. */
    const val MAX_STICKER_BYTES = 100 * 1024

    /** Batas ukuran ikon tray. */
    const val MAX_TRAY_BYTES = 50 * 1024

    /** Minimal stiker dalam satu paket. Di bawah ini WhatsApp tidak mau. */
    const val MIN_STICKERS = 3

    /** Maksimal stiker dalam satu paket. */
    const val MAX_STICKERS = 30

    /**
     * Tingkat kualitas WebP yang dicoba berurutan sampai ukurannya muat.
     *
     * Mulai dari 90 karena alpha ikut dikompresi lossy, dan di bawah itu tepi
     * potongan mulai terlihat bergerigi di latar gelap. Turun satu tingkat cuma
     * kalau hasil sebelumnya masih di atas 100 KB.
     */
    val QUALITY_STEPS = listOf(90, 80, 70, 60, 50, 40)

    /** Nama file stiker ke-`index`, satu berdasarkan urutan di paket. */
    fun stickerFileName(index: Int): String =
        "sticker_${(index + 1).toString().padStart(3, '0')}.webp"

    /**
     * Apakah jumlah stiker sekarang cukup untuk sebuah paket.
     *
     * Dipakai layar sebelum menawarkan tombolnya, jadi pengguna tidak menekan
     * tombol yang pasti ditolak WhatsApp.
     */
    fun canExport(stickerCount: Int): Boolean = stickerCount in MIN_STICKERS..MAX_STICKERS

    /**
     * Stiker yang benar-benar ikut, terbaru dulu.
     *
     * WhatsApp membatasi 30 per paket; yang dipotong adalah yang paling lama,
     * karena yang baru saja dibuat biasanya yang ingin dipakai.
     */
    fun takeForPack(stickerPaths: List<String>): List<String> =
        stickerPaths.take(MAX_STICKERS)
}

/**
 * Satu stiker di dalam paket: nama filenya, emoji, dan teks untuk pembaca layar.
 *
 * [accessibilityText] null berarti WhatsApp memakai nama filenya; itu lebih baik
 * daripada kalimat generik yang tidak menjelaskan apa pun.
 */
data class PackSticker(
    val fileName: String,
    val emojis: String,
    val accessibilityText: String?
)

/**
 * Isi satu paket, ditulis ke disk oleh eksportir dan dibaca lagi oleh
 * [StickerContentProvider].
 *
 * Disimpan sebagai teks sederhana, bukan JSON, karena pembacanya cuma satu
 * provider di app ini dan format teks tidak butuh parser tambahan. Yang penting
 * justru sebaliknya: bentuknya bisa diuji di JVM tanpa Android sama sekali.
 *
 * Baris pertama berisi metadata paket, sisanya satu baris per stiker, dipisah
 * tab. Semua tab dan baris baru di dalam teks dibuang saat menulis, jadi tidak
 * ada field yang bisa memecah barisnya sendiri.
 */
data class StickerPackManifest(
    val identifier: String,
    val name: String,
    val publisher: String,
    val trayFileName: String,
    /** Naik setiap ekspor supaya WhatsApp tahu isinya berubah. */
    val imageDataVersion: String,
    val stickers: List<PackSticker>
) {

    fun serialize(): String = buildString {
        append(sanitize(identifier)).append('\t')
        append(sanitize(name)).append('\t')
        append(sanitize(publisher)).append('\t')
        append(sanitize(trayFileName)).append('\t')
        append(sanitize(imageDataVersion)).append('\n')
        stickers.forEach { sticker ->
            append(sanitize(sticker.fileName)).append('\t')
            append(sanitize(sticker.emojis)).append('\t')
            append(sanitize(sticker.accessibilityText.orEmpty())).append('\n')
        }
    }

    companion object {
        /** Nama paket di pemilih stiker WhatsApp. */
        const val IDENTIFIER = "cukinggo"
        const val NAME = "CukingGo"
        const val PUBLISHER = "CukingGo"
        const val TRAY_FILE_NAME = "tray.webp"

        /**
         * Membaca kembali teks dari [serialize].
         *
         * Baris yang tidak lengkap dilewati, bukan membuat semuanya gagal:
         * provider dijalankan oleh app lain, dan satu file manifest yang
         * setengah tertulis (misalnya app dimatikan saat mengekspor) tidak
         * boleh membuat WhatsApp crash karena provider melempar exception.
         */
        fun parse(text: String): StickerPackManifest? {
            val lines = text.lineSequence().filter { it.isNotBlank() }.toList()
            val header = lines.firstOrNull()?.split('\t') ?: return null
            if (header.size < 5) return null

            val stickers = lines.drop(1).mapNotNull { line ->
                val parts = line.split('\t')
                if (parts.size < 2 || parts[0].isBlank()) return@mapNotNull null
                PackSticker(
                    fileName = parts[0],
                    emojis = parts[1],
                    accessibilityText = parts.getOrNull(2)?.takeIf { it.isNotBlank() }
                )
            }
            if (stickers.isEmpty()) return null

            return StickerPackManifest(
                identifier = header[0],
                name = header[1],
                publisher = header[2],
                trayFileName = header[3],
                imageDataVersion = header[4],
                stickers = stickers
            )
        }

        private fun sanitize(value: String): String =
            value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ')
    }
}

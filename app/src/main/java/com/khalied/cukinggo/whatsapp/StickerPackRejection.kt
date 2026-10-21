package com.khalied.cukinggo.whatsapp

/**
 * Arti penolakan WhatsApp, dipisah dari teksnya supaya layar bisa menjelaskan
 * penyebab yang paling sering terjadi dengan kalimatnya sendiri.
 *
 * Teksnya datang dari app lain dan berbahasa Inggris, jadi menampilkannya apa
 * adanya untuk semua kasus berarti pengguna membaca pesan sistem, bukan
 * penjelasan.
 */
sealed interface StickerPackRejection {

    /**
     * WhatsApp tidak menemukan provider stiker app ini.
     *
     * Di HP ini penyebabnya bukan manifest: provider-nya ada dan bisa dibaca
     * dari ruang utama app. Yang tidak ada adalah salinannya di ruang klon,
     * karena WhatsApp duplikat berjalan sebagai user Android sendiri, dan
     * provider hanya terpasang di user tempat app-nya dipasang.
     */
    data object ProviderNotVisible : StickerPackRejection

    /** Alasan lain, ditampilkan apa adanya karena itu satu-satunya petunjuk. */
    data class Other(val reason: String) : StickerPackRejection

    companion object {

        /**
         * Membaca extra `validation_error` dari WhatsApp.
         *
         * Null berarti tidak ada alasan yang bisa dibaca, biasanya karena
         * dialognya memang cuma ditutup pengguna. Kasus itu tidak perlu
         * ditampilkan sebagai penolakan.
         */
        fun from(reason: String?, authority: String): StickerPackRejection? {
            val text = reason?.trim().orEmpty()
            if (text.isEmpty()) return null
            return if (mentionsMissingProvider(text, authority)) {
                ProviderNotVisible
            } else {
                Other(text)
            }
        }

        /**
         * Dicocokkan dari kata "provider" + nama authority, bukan dari satu
         * kalimat utuh: WhatsApp mengganti kalimatnya antar versi, dan yang
         * tidak berubah adalah authority yang dicarinya.
         */
        private fun mentionsMissingProvider(reason: String, authority: String): Boolean {
            val text = reason.lowercase()
            if (!text.contains(authority.lowercase())) return false
            return MISSING_PROVIDER_PHRASES.any { phrase -> text.contains(phrase) }
        }

        private val MISSING_PROVIDER_PHRASES = listOf(
            "cannot find",
            "can not find",
            "could not find",
            "couldn't find",
            "failed to find"
        )
    }
}

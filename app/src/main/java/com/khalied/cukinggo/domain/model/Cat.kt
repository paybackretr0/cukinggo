package com.khalied.cukinggo.domain.model

/**
 * Profil cuking: satu identitas yang menampung semua penemuannya.
 *
 * Nama panggilan tinggal di sini, bukan di penemuan, karena satu cuking cuma
 * punya satu nama walau ketemu berkali-kali. Yang bertambah tiap kali ketemu
 * lagi adalah [sightings].
 */
data class Cat(
    val id: Long,
    /** Nama panggilan dari pemilik catatan, opsional. Null artinya belum diisi. */
    val name: String?,
    /** Kapan cuking ini pertama ditandai. */
    val createdAt: Long,
    /** Semua penemuannya, urut terbaru dulu. Selalu berisi minimal satu. */
    val sightings: List<CatSighting>
) {
    /** Penemuan terbaru, yaitu yang paling akhir dicatat. */
    val latest: CatSighting get() = sightings.first()
}

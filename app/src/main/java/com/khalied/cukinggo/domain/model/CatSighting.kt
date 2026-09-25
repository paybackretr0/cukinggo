package com.khalied.cukinggo.domain.model

/**
 * Satu penemuan: satu foto, satu kegiatan, satu lokasi, dan satu waktu.
 *
 * Ini satu-satunya hal yang bisa ditambah, dan tidak pernah diubah setelah
 * tersimpan. Cukingnya sendiri diwakili [Cat], yang menampung semua penemuan
 * dari cuking yang sama.
 */
data class CatSighting(
    val id: Long,
    /** Cuking yang punya penemuan ini. */
    val catId: Long,
    /** Nama panggilan cukingnya, dibaca dari profilnya. Null artinya belum diberi nama. */
    val catName: String?,
    val photoPath: String,
    /** Kegiatan cuking waktu ditemukan, ditulis apa adanya oleh pengguna. */
    val description: String?,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

/**
 * Satu penemuan terbaru per cuking, urutannya tetap terbaru dulu.
 *
 * Dipakai tempat yang memang menanyakan cuking, bukan penemuan: kabar "dekat
 * cuking" dan widget "cuking terdekat". Yang dipertahankan adalah kemunculan
 * pertama tiap cuking, jadi daftar masukannya harus sudah urut terbaru dulu.
 *
 * Alasannya memakai lokasi terbaru, bukan seluruh penemuan: yang ditanya di dua
 * tempat itu "cuking ini sekarang di mana", sedangkan seluruh penemuannya sudah
 * terjawab di layar detailnya sendiri.
 */
fun List<CatSighting>.latestPerCat(): List<CatSighting> = distinctBy { it.catId }

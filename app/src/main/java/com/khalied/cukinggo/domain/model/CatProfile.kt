package com.khalied.cukinggo.domain.model

/**
 * Profil cuking tanpa daftar penemuannya.
 *
 * Dipakai tempat yang cuma bertanya soal identitasnya, misalnya rekap mingguan
 * yang mau tahu cuking mana yang baru ditandai minggu ini. Bentuk ini sengaja
 * dipisah dari [Cat] supaya tempat seperti itu tidak ikut menarik seluruh foto
 * penemuannya.
 */
data class CatProfile(
    val id: Long,
    val name: String?,
    /** Kapan cuking ini pertama ditandai; inilah yang dipakai sebagai tanggalnya. */
    val createdAt: Long
)

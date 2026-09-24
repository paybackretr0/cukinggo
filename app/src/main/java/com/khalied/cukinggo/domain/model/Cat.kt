package com.khalied.cukinggo.domain.model

/**
 * Model domain kucing yang dipakai layer UI.
 */
data class Cat(
    val id: Long = 0,
    val photoPath: String,
    /** Nama panggilan dari pemilik catatan, opsional. Null artinya belum diisi. */
    val name: String?,
    val description: String?,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

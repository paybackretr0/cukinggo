package com.khalied.cukinggo.domain.model

/**
 * Model domain kucing yang dipakai layer UI.
 */
data class Cat(
    val id: Long = 0,
    val photoPath: String,
    val description: String?,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

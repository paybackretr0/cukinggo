package com.khalied.cukinggo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Profil cuking di database: satu identitas yang menampung semua penemuannya.
 *
 * Foto, kegiatan, dan koordinatnya tidak ada di sini, tapi di [CatSightingEntity]:
 * satu cuking bisa ditemukan berkali-kali, dan tiap penemuan menyimpan buktinya
 * sendiri.
 */
@Entity(tableName = "cats")
data class CatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String?,
    /** Waktu penemuan pertamanya, dipakai sebagai tanggal cuking ini ditandai. */
    val createdAt: Long
)

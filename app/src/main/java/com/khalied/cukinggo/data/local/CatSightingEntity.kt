package com.khalied.cukinggo.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.khalied.cukinggo.domain.model.CatSighting

/**
 * Satu penemuan cuking. Baris di tabel ini hanya ditambah dan dihapus, tidak
 * pernah diubah isinya, karena catatan yang lama memang tidak diedit: yang baru
 * selalu jadi baris baru.
 *
 * [ForeignKey] dengan CASCADE dipasang supaya profil yang terhapus tidak
 * meninggalkan penemuan yatim. Repositori tetap menghapus penemuannya lebih dulu
 * secara eksplisit, jadi urutannya benar walau penegakan foreign key di SQLite
 * sedang tidak aktif.
 */
@Entity(
    tableName = "cat_sightings",
    foreignKeys = [
        ForeignKey(
            entity = CatEntity::class,
            parentColumns = ["id"],
            childColumns = ["catId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("catId")]
)
data class CatSightingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val catId: Long,
    val photoPath: String,
    val description: String?,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

/**
 * Hasil gabungan penemuan dengan nama cukingnya.
 *
 * Bentuknya dipakai langsung oleh daftar dan peta, jadi satu query cukup dan
 * tidak ada nama yang perlu diambil menyusul per baris.
 */
data class CatSightingRow(
    val id: Long,
    val catId: Long,
    val catName: String?,
    val photoPath: String,
    val description: String?,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

fun CatSightingRow.toDomain(): CatSighting = CatSighting(
    id = id,
    catId = catId,
    catName = catName,
    photoPath = photoPath,
    description = description,
    latitude = latitude,
    longitude = longitude,
    timestamp = timestamp
)

/** [catName] dibawa terpisah karena tabel penemuan sendiri tidak menyimpan nama. */
fun CatSightingEntity.toDomain(catName: String?): CatSighting = CatSighting(
    id = id,
    catId = catId,
    catName = catName,
    photoPath = photoPath,
    description = description,
    latitude = latitude,
    longitude = longitude,
    timestamp = timestamp
)

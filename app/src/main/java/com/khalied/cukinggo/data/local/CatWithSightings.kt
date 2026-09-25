package com.khalied.cukinggo.data.local

import androidx.room.Embedded
import androidx.room.Relation
import com.khalied.cukinggo.domain.model.Cat
import com.khalied.cukinggo.domain.model.CatSighting

/**
 * Profil cuking beserta seluruh penemuannya, dibaca Room lewat relasi.
 *
 * Urutan penemuannya diatur di sini, bukan di query: hasil [Relation] datang
 * dalam urutan tabelnya sendiri, sedangkan yang dibutuhkan UI selalu terbaru dulu.
 */
data class CatWithSightings(
    @Embedded val cat: CatEntity,
    @Relation(parentColumn = "id", entityColumn = "catId")
    val sightings: List<CatSightingEntity>
)

/**
 * Null kalau profilnya tidak punya satu pun penemuan.
 *
 * Keadaan itu tidak seharusnya terjadi, tapi bisa tersisa kalau prosesnya mati
 * tepat di tengah penghapusan. Cuking tanpa penemuan tidak punya foto, waktu, dan
 * lokasi untuk ditampilkan, jadi lebih jujur diperlakukan sebagai "sudah tidak
 * ada" daripada ditampilkan setengah jadi.
 */
fun CatWithSightings.toDomainOrNull(): Cat? {
    val ordered: List<CatSightingEntity> = sightings.sortedWith(
        compareByDescending<CatSightingEntity> { it.timestamp }
            .thenByDescending { it.id }
    )
    if (ordered.isEmpty()) return null

    val catName = cat.name
    return Cat(
        id = cat.id,
        name = catName,
        createdAt = cat.createdAt,
        sightings = ordered.map { entity -> entity.toDomain(catName) }
    )
}

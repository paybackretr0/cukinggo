package com.khalied.cukinggo.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Profil cuking. Penemuannya ada di [CatSightingDao], dan pembacaan yang
 * membutuhkan keduanya (profil beserta seluruh penemuannya) ada di sini karena
 * yang ditanyakan hasilnya adalah cukingnya.
 */
@Dao
interface CatDao {

    @Insert
    suspend fun insertCat(cat: CatEntity): Long

    /** Profil satu cuking beserta seluruh penemuannya, untuk layar detail. */
    @Transaction
    @Query("SELECT * FROM cats WHERE id = :id")
    fun observeCatWithSightings(id: Long): Flow<CatWithSightings?>

    /** Versi sekali baca dari query di atas. */
    @Transaction
    @Query("SELECT * FROM cats WHERE id = :id")
    suspend fun getCatWithSightings(id: Long): CatWithSightings?

    @Query("DELETE FROM cats WHERE id = :id")
    suspend fun deleteCatById(id: Long)

    /**
     * Mengganti nama panggilan satu cuking. Null berarti namanya dikosongkan,
     * dan itu memang boleh: nama sifatnya opsional.
     */
    @Query("UPDATE cats SET name = :name WHERE id = :id")
    suspend fun updateName(id: Long, name: String?)

    /**
     * Profil semua cuking tanpa penemuannya.
     *
     * Dipakai rekap mingguan, yang cuma butuh "cuking ini baru ditandai kapan".
     * Membaca seluruh penemuannya untuk itu akan menarik ratusan baris foto yang
     * tidak dipakai.
     */
    @Query("SELECT * FROM cats")
    fun observeAllCats(): Flow<List<CatEntity>>
}

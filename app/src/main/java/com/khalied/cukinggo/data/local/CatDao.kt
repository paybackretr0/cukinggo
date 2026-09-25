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
}

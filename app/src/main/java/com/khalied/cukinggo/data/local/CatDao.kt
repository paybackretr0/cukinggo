package com.khalied.cukinggo.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CatDao {

    @Insert
    suspend fun insertCat(cat: CatEntity): Long

    @Query("SELECT * FROM cats ORDER BY timestamp DESC")
    fun getAllCats(): Flow<List<CatEntity>>

    /** Dipakai widget "Kucing terakhir", yang cuma butuh satu kucing terbaru. */
    @Query("SELECT * FROM cats ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestCat(): CatEntity?

    /**
     * Dipakai widget "Kucing hari ini", yang perlu seluruh koleksi untuk memilih
     * kucing harinya. Sekali baca, bukan flow, karena widget digambar per permintaan.
     */
    @Query("SELECT * FROM cats ORDER BY timestamp DESC")
    suspend fun getCatsOnce(): List<CatEntity>

    /**
     * Hanya waktu catatan, dipakai menghitung rentetan harian. Kolomnya dipilih
     * satu saja karena yang dibutuhkan cuma tanggalnya, bukan isi catatannya.
     */
    @Query("SELECT timestamp FROM cats")
    suspend fun getAllTimestamps(): List<Long>

    /** Dipakai kabar "dekat kucing", yang cuma tahu id dari request ID geofence. */
    @Query("SELECT * FROM cats WHERE id IN (:ids)")
    suspend fun getCatsByIds(ids: List<Long>): List<CatEntity>

    @Query("SELECT * FROM cats WHERE id = :id")
    suspend fun getCatById(id: Long): CatEntity?

    @Query("SELECT * FROM cats WHERE id = :id")
    fun observeCatById(id: Long): Flow<CatEntity?>

    @Delete
    suspend fun deleteCat(cat: CatEntity)

    @Query("DELETE FROM cats WHERE id = :id")
    suspend fun deleteCatById(id: Long)
}

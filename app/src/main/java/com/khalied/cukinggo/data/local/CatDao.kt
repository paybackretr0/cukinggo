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

    @Query("SELECT * FROM cats WHERE id = :id")
    suspend fun getCatById(id: Long): CatEntity?

    @Query("SELECT * FROM cats WHERE id = :id")
    fun observeCatById(id: Long): Flow<CatEntity?>

    @Delete
    suspend fun deleteCat(cat: CatEntity)

    @Query("DELETE FROM cats WHERE id = :id")
    suspend fun deleteCatById(id: Long)
}

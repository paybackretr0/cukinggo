package com.khalied.cukinggo.data.repository

import com.khalied.cukinggo.data.local.CatDao
import com.khalied.cukinggo.data.local.CatEntity
import com.khalied.cukinggo.data.local.toDomain
import com.khalied.cukinggo.domain.model.Cat
import com.khalied.cukinggo.util.ImageStorageHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Satu-satunya pintu masuk ke data kucing: Room DB + file foto lokal.
 */
class CatRepository(
    private val catDao: CatDao,
    private val imageStorageHelper: ImageStorageHelper,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    /**
     * Dijalankan setiap daftar kucing berubah. Repository sendiri tidak tahu
     * siapa yang mendengarkan, jadi urusan widget tetap di luar lapisan data.
     */
    private val onCatsChanged: () -> Unit = {}
) {

    fun getAllCats(): Flow<List<Cat>> =
        catDao.getAllCats().map { entities -> entities.map(CatEntity::toDomain) }

    /** Kucing terbaru, dipakai widget "Kucing terakhir". */
    suspend fun latestCat(): Cat? = withContext(ioDispatcher) {
        catDao.getLatestCat()?.toDomain()
    }

    /** Seluruh kucing sekali baca, dipakai widget "Kucing hari ini" dan area pantauan. */
    suspend fun getAllCatsOnce(): List<Cat> = withContext(ioDispatcher) {
        catDao.getCatsOnce().map(CatEntity::toDomain)
    }

    /** Waktu semua catatan, dipakai menghitung rentetan harian di widget. */
    suspend fun getAllTimestamps(): List<Long> = withContext(ioDispatcher) {
        catDao.getAllTimestamps()
    }

    /** Sekumpulan kucing berdasarkan id, dipakai kabar "dekat kucing". */
    suspend fun getCatsByIds(ids: List<Long>): List<Cat> = withContext(ioDispatcher) {
        catDao.getCatsByIds(ids).map(CatEntity::toDomain)
    }

    fun observeCat(id: Long): Flow<Cat?> =
        catDao.observeCatById(id).map { it?.toDomain() }

    suspend fun getCat(id: Long): Cat? = withContext(ioDispatcher) {
        catDao.getCatById(id)?.toDomain()
    }

    suspend fun addCat(
        photoPath: String,
        description: String?,
        latitude: Double,
        longitude: Double,
        timestamp: Long = System.currentTimeMillis()
    ): Long = withContext(ioDispatcher) {
        val id = catDao.insertCat(
            CatEntity(
                photoPath = photoPath,
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                latitude = latitude,
                longitude = longitude,
                timestamp = timestamp
            )
        )
        onCatsChanged()
        id
    }

    suspend fun deleteCat(cat: Cat) = withContext(ioDispatcher) {
        catDao.deleteCatById(cat.id)
        imageStorageHelper.deletePhoto(cat.photoPath)
        onCatsChanged()
    }
}

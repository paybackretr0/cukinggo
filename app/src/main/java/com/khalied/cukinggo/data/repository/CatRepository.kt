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
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    fun getAllCats(): Flow<List<Cat>> =
        catDao.getAllCats().map { entities -> entities.map(CatEntity::toDomain) }

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
        catDao.insertCat(
            CatEntity(
                photoPath = photoPath,
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                latitude = latitude,
                longitude = longitude,
                timestamp = timestamp
            )
        )
    }

    suspend fun deleteCat(cat: Cat) = withContext(ioDispatcher) {
        catDao.deleteCatById(cat.id)
        imageStorageHelper.deletePhoto(cat.photoPath)
    }
}

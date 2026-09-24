package com.khalied.cukinggo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.khalied.cukinggo.domain.model.Cat

@Entity(tableName = "cats")
data class CatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val photoPath: String,
    val name: String?,
    val description: String?,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long
)

fun CatEntity.toDomain(): Cat = Cat(
    id = id,
    photoPath = photoPath,
    name = name,
    description = description,
    latitude = latitude,
    longitude = longitude,
    timestamp = timestamp
)

fun Cat.toEntity(): CatEntity = CatEntity(
    id = id,
    photoPath = photoPath,
    name = name,
    description = description,
    latitude = latitude,
    longitude = longitude,
    timestamp = timestamp
)

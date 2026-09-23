package com.khalied.cukinggo.ui.components

import com.khalied.cukinggo.domain.model.Cat
import kotlin.math.floor
import kotlin.math.pow

/**
 * Pengelompokan marker untuk mode zoom jauh: kucing yang berdekatan digabung jadi
 * satu gelembung angka supaya marker tidak saling menumpuk.
 */
internal data class CatCluster(
    val cellLatitude: Int,
    val cellLongitude: Int,
    val latitude: Double,
    val longitude: Double,
    val count: Int
)

/** Ukuran sel grid (derajat) yang kira-kira setara beberapa lebar marker di layar. */
internal fun clusterCellSizeDegrees(zoomLevel: Int): Double {
    val tileDegrees = 360.0 / 2.0.pow(zoomLevel.coerceIn(1, 20))
    return (tileDegrees * 0.75).coerceAtLeast(0.0005)
}

internal fun clusterCats(cats: List<Cat>, zoomLevel: Int): List<CatCluster> {
    if (cats.isEmpty()) return emptyList()

    val cellSize = clusterCellSizeDegrees(zoomLevel)
    val bins = LinkedHashMap<Pair<Int, Int>, MutableList<Cat>>()
    cats.forEach { cat ->
        val binKey = floor(cat.latitude / cellSize).toInt() to
            floor(cat.longitude / cellSize).toInt()
        bins.getOrPut(binKey) { mutableListOf() }.add(cat)
    }

    return bins.map { (binKey, members) ->
        CatCluster(
            cellLatitude = binKey.first,
            cellLongitude = binKey.second,
            latitude = members.sumOf { it.latitude } / members.size,
            longitude = members.sumOf { it.longitude } / members.size,
            count = members.size
        )
    }
}

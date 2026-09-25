package com.khalied.cukinggo.ui.components

import com.khalied.cukinggo.domain.model.CatSighting
import kotlin.math.floor
import kotlin.math.pow

/**
 * Pengelompokan marker untuk mode zoom jauh: penemuan yang berdekatan digabung
 * jadi satu gelembung angka supaya marker tidak saling menumpuk.
 *
 * Yang dikelompokkan adalah penemuannya, bukan cukingnya, karena peta di Home
 * menampilkan satu titik per penemuan: cuking yang sudah ketemu di tiga tempat
 * memang layak terlihat di tiga tempat.
 */
internal data class SightingCluster(
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

internal fun clusterSightings(
    sightings: List<CatSighting>,
    zoomLevel: Int
): List<SightingCluster> {
    if (sightings.isEmpty()) return emptyList()

    val cellSize = clusterCellSizeDegrees(zoomLevel)
    val bins = LinkedHashMap<Pair<Int, Int>, MutableList<CatSighting>>()
    sightings.forEach { sighting ->
        val binKey = floor(sighting.latitude / cellSize).toInt() to
            floor(sighting.longitude / cellSize).toInt()
        bins.getOrPut(binKey) { mutableListOf() }.add(sighting)
    }

    return bins.map { (binKey, members) ->
        SightingCluster(
            cellLatitude = binKey.first,
            cellLongitude = binKey.second,
            latitude = members.sumOf { it.latitude } / members.size,
            longitude = members.sumOf { it.longitude } / members.size,
            count = members.size
        )
    }
}

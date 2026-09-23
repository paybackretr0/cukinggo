package com.khalied.cukinggo.util

import java.util.Locale
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0

private val indonesian = Locale("id", "ID")

/**
 * Jarak lingkaran besar (haversine) antara dua koordinat, dalam meter.
 * Murni matematika, tidak butuh Android API.
 */
fun distanceMeters(
    latitude1: Double,
    longitude1: Double,
    latitude2: Double,
    longitude2: Double
): Double {
    val lat1 = Math.toRadians(latitude1)
    val lat2 = Math.toRadians(latitude2)
    val deltaLat = Math.toRadians(latitude2 - latitude1)
    val deltaLng = Math.toRadians(longitude2 - longitude1)

    val a = sin(deltaLat / 2).pow(2) +
        cos(lat1) * cos(lat2) * sin(deltaLng / 2).pow(2)

    return 2 * EARTH_RADIUS_METERS * asin(sqrt(a.coerceIn(0.0, 1.0)))
}

/** Format ramah dibaca: "180 m", "1,2 km", "15 km". */
fun formatDistance(meters: Double): String = when {
    meters < 1_000 -> "${meters.roundToInt()} m"
    meters < 10_000 -> String.format(indonesian, "%.1f km", meters / 1_000)
    else -> "${(meters / 1_000).roundToInt()} km"
}

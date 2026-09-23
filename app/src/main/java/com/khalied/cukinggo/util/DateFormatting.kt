package com.khalied.cukinggo.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val indonesian = Locale("id", "ID")
private val dayFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", indonesian)
private val dayLongFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", indonesian)
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", indonesian)

private fun localDate(timestamp: Long): LocalDate =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()

fun formatTime(timestamp: Long): String =
    timeFormatter.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()))

/** "Hari ini", "Kemarin", atau tanggalnya. Label hari dikirim dari string resource. */
fun formatDayLabel(timestamp: Long, today: String = "Hari ini", yesterday: String = "Kemarin"): String {
    val date = localDate(timestamp)
    val now = LocalDate.now()
    return when (date) {
        now -> today
        now.minusDays(1) -> yesterday
        else -> dayFormatter.format(date)
    }
}

fun formatFullDateTime(timestamp: Long): String {
    val day = dayLongFormatter.format(localDate(timestamp))
    return "$day, ${formatTime(timestamp)}"
}

fun formatCoordinates(latitude: Double, longitude: Double): String =
    // Koordinat selalu pakai titik desimal supaya tidak rancu dengan pemisah lat/lng.
    String.format(Locale.US, "%.5f, %.5f", latitude, longitude)

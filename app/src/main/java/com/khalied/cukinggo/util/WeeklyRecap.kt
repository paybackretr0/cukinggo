package com.khalied.cukinggo.util

import com.khalied.cukinggo.domain.model.CatProfile
import com.khalied.cukinggo.domain.model.CatSighting
import com.khalied.cukinggo.domain.model.latestPerCat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Jumlah hari yang dicakup rekap: hari ini dan enam hari sebelumnya. */
const val WEEKLY_RECAP_DAYS = 7

/**
 * Satu batang di grafik rekap: satu hari dan berapa kali ketemu cuking hari itu.
 *
 * [date] disimpan sebagai tanggal utuhnya, bukan cuma labelnya, supaya layar
 * bisa memilih label dan penanda \"hari ini\" sendiri tanpa menghitung ulang.
 */
data class WeeklyRecapDay(
    val date: LocalDate,
    val sightingCount: Int
)

/**
 * Ringkasan tujuh hari terakhir.
 *
 * [days] selalu tujuh batang dan urut dari yang paling lama, supaya grafiknya
 * punya lebar yang sama walau datanya sedikit; hari tanpa catatan tetap muncul
 * sebagai batang kosong, karena yang ingin dilihat justru hari yang bolong.
 */
data class WeeklyRecap(
    val days: List<WeeklyRecapDay>,
    val sightingCount: Int,
    val newCatCount: Int,
    val activeDayCount: Int,
    /** Satu penemuan terbaru tiap cuking yang ketemu minggu ini, terbaru dulu. */
    val catsSeen: List<CatSighting>
) {
    val hasAnything: Boolean get() = sightingCount > 0 || newCatCount > 0
}

/**
 * Menghitung rekap dari catatan yang sudah ada, bukan dari angka yang disimpan
 * dan ditambah tiap hari.
 *
 * Alasannya sama dengan rentetan harian: catatan yang dihapus atau jam HP yang
 * bergeser langsung ikut benar, dan tidak ada angka tersimpan yang bisa
 * berbeda dari isi database.
 *
 * Batas harinya mengikuti zona waktu setempat, bukan UTC, karena pukul 06.00 di
 * Jakarta sudah hari berikutnya menurut UTC.
 *
 * Cuking dihitung \"baru\" dari [CatProfile.createdAt], yaitu kapan profile-nya
 * pertama dibuat; itu satu-satunya tanggal yang menjawab \"cuking ini baru kenal\".
 */
fun weeklyRecap(
    sightings: List<CatSighting>,
    catProfiles: List<CatProfile>,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault()
): WeeklyRecap {
    val dayOffset = (WEEKLY_RECAP_DAYS - 1).toLong()
    val firstDay = today.minusDays(dayOffset)

    fun localDay(timestamp: Long): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()

    // Catatan bertanggal masa depan (jam HP yang bergeser) tidak masuk hitungan
    // mana pun, sama seperti di rentetan harian.
    val inWindow = sightings.filter { sighting ->
        val day = localDay(sighting.timestamp)
        day >= firstDay && day <= today
    }

    val countPerDay = inWindow.groupingBy { localDay(it.timestamp) }.eachCount()
    val days = (0 until WEEKLY_RECAP_DAYS).map { offset ->
        val date = firstDay.plusDays(offset.toLong())
        WeeklyRecapDay(date = date, sightingCount = countPerDay[date] ?: 0)
    }

    // Terbaru dulu sudah urutan aslinya; latestPerCat mempertahankan kemunculan
    // pertama tiap cuking, jadi yang tertinggal adalah penemuan terbarunya.
    val catsSeen = inWindow.latestPerCat()

    val newCatCount = catProfiles.count { profile ->
        val day = localDay(profile.createdAt)
        day >= firstDay && day <= today
    }

    return WeeklyRecap(
        days = days,
        sightingCount = inWindow.size,
        newCatCount = newCatCount,
        activeDayCount = days.count { day -> day.sightingCount > 0 },
        catsSeen = catsSeen
    )
}

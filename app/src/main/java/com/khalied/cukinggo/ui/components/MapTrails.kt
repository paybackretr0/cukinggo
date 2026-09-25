package com.khalied.cukinggo.ui.components

import com.khalied.cukinggo.domain.model.CatSighting
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.round

/**
 * Satu titik di jejak: satu penemuan yang lokasinya digambar.
 */
internal data class TrailPoint(
    val sightingId: Long,
    val latitude: Double,
    val longitude: Double
)

/**
 * Jejak perjalanan satu cuking: tempat-tempat yang pernah tercatat, urut dari yang
 * paling lama ke yang paling baru.
 *
 * Urutannya terbalik dari daftar di layar lain (yang selalu terbaru dulu), dan itu
 * disengaja: yang digambar di sini perjalanannya, jadi garisnya harus mulai dari
 * tempat dia pertama ketemu.
 */
internal data class CatTrail(
    val catId: Long,
    val points: List<TrailPoint>
)

/**
 * Lima angka di belakang koma, kira-kira satu meter.
 *
 * Dipakai membandingkan dua titik: ketemu lagi di tempat yang sama persis tidak
 * menambah gerakan apa pun ke jejaknya.
 */
private const val PLACE_SCALE = 100_000.0

private fun samePlace(first: TrailPoint, second: TrailPoint): Boolean =
    round(first.latitude * PLACE_SCALE) == round(second.latitude * PLACE_SCALE) &&
        round(first.longitude * PLACE_SCALE) == round(second.longitude * PLACE_SCALE)

/**
 * Jejak semua cuking yang punya perjalanan.
 *
 * Satu cuking hanya punya jejak kalau dia pernah tercatat di minimal dua tempat
 * berbeda: dengan satu tempat, tidak ada garis yang bisa digambar, dan titiknya
 * sudah diwakili marker fotonya sendiri.
 */
internal fun buildTrails(sightings: List<CatSighting>): List<CatTrail> =
    sightings
        .groupBy { sighting -> sighting.catId }
        .mapNotNull { (catId, perCat) -> trailFor(catId, perCat) }

private fun trailFor(catId: Long, sightings: List<CatSighting>): CatTrail? {
    // Id dipakai sebagai pemutus seri, sama seperti urutan di tempat lain, supaya
    // jejaknya tidak berubah-ubah saat dua catatan punya waktu yang sama.
    val ordered = sightings.sortedWith(
        compareBy<CatSighting> { sighting -> sighting.timestamp }
            .thenBy { sighting -> sighting.id }
    )

    val points = mutableListOf<TrailPoint>()
    ordered.forEach { sighting ->
        val point = TrailPoint(sighting.id, sighting.latitude, sighting.longitude)
        val last = points.lastOrNull()
        if (last != null && samePlace(last, point)) {
            // Ketemu lagi di tempat yang sama: yang disimpan versi terbarunya,
            // supaya titik itu tetap mewakili catatan yang paling akhir.
            points[points.lastIndex] = point
        } else {
            points.add(point)
        }
    }

    return if (points.size < 2) null else CatTrail(catId = catId, points = points)
}

/**
 * Warna garis jejak, satu warna tetap per cuking.
 *
 * Dipilih dari keluarga palet app, tapi dalam versi pekat. Alasannya kontras:
 * garis ini digambar di atas tile peta, dan tile osmdroid selalu terang (tidak ikut
 * mode gelap app), jadi pastel mentahnya seperti PeachAccent dan MintPop terlalu
 * pudar di sana. Karena warnanya berfungsi sebagai penanda data, yaitu "garis ini
 * milik cuking yang mana", ia tidak dipakai di permukaan UI yang lain.
 */
private val TRAIL_COLORS = intArrayOf(
    0xFF8B5E3C.toInt(), // PawBrown, sewarna dengan garis luar marker yang sudah ada
    0xFF2F8C74.toInt(), // mint pekat
    0xFFC4722F.toInt(), // peach pekat
    0xFFC4506A.toInt() // pink pekat
)

/**
 * Warna jejak milik satu cuking, dan selalu sama.
 *
 * Diambil dari id cuking, bukan dari urutan di daftar: daftarnya berubah tiap kali
 * ada catatan baru, dan warna yang berpindah-pindah akan membuat jejak yang sama
 * terlihat seperti jejak cuking lain.
 */
internal fun trailColorFor(catId: Long): Int {
    val count = TRAIL_COLORS.size
    val index = ((catId % count).toInt() + count) % count
    return TRAIL_COLORS[index]
}

/** Titik tengah jejak dan tingkat zoom yang membuat seluruh jejaknya masuk layar. */
internal data class TrailFocus(
    val latitude: Double,
    val longitude: Double,
    val zoom: Double
)

/**
 * Rentang terkecil yang dianggap punya bentang.
 *
 * Sekitar dua puluh meter. Di bawah itu jejaknya toh terlihat seperti satu titik,
 * dan rentang nol akan menghasilkan zoom tak terhingga.
 */
private const val MIN_FOCUS_SPAN = 0.0002

/**
 * Batas zoom saat dipaskan. Paling jauh 12 dan paling dekat 16, sama dengan zoom
 * yang dipakai marker foto: cukup untuk melihat seluruh jejak kota, tapi tidak
 * sampai menampilkan satu halaman buku peta.
 */
private const val FOCUS_MIN_ZOOM = 12.0
private const val FOCUS_MAX_ZOOM = 16.0

/**
 * Posisi dan zoom buat menampilkan seluruh jejak sekaligus.
 *
 * Kalau yang dipakai cuma titik terbaru, jejak panjang justru terpotong di tepi
 * kartu, padahal yang mau dilihat di situ perjalanannya, bukan tempat terakhirnya.
 * Karena itu titik tengahnya diambil dari bentang jejaknya, dan zoomnya dihitung
 * dari bentang itu dengan rumus tile peta: satu tingkat di atas "jejaknya selebar
 * satu tile" supaya jejaknya terbaca tapi ujung terjauhnya tetap di dalam layar.
 */
internal fun trailFocus(sightings: List<CatSighting>): TrailFocus? {
    if (sightings.isEmpty()) return null

    val minLatitude = sightings.minOf { it.latitude }
    val maxLatitude = sightings.maxOf { it.latitude }
    val minLongitude = sightings.minOf { it.longitude }
    val maxLongitude = sightings.maxOf { it.longitude }

    val span = maxOf(maxLatitude - minLatitude, maxLongitude - minLongitude)
        .coerceAtLeast(MIN_FOCUS_SPAN)
    val zoom = floor(log2(360.0 / span)) + 1.0

    return TrailFocus(
        latitude = (minLatitude + maxLatitude) / 2.0,
        longitude = (minLongitude + maxLongitude) / 2.0,
        zoom = zoom.coerceIn(FOCUS_MIN_ZOOM, FOCUS_MAX_ZOOM)
    )
}

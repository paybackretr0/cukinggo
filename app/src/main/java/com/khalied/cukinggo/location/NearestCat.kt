package com.khalied.cukinggo.location

import android.annotation.SuppressLint
import com.khalied.cukinggo.domain.model.Cat
import com.khalied.cukinggo.util.distanceMeters

/**
 * Batas "dekat" untuk cuking terdekat: 500 m.
 *
 * Angkanya sengaja disamakan dengan radius paling longgar di fitur kabar dekat
 * (lihat [NearbyRadius]) dan bukan diambil dari pilihan radius pengguna: pilihan
 * itu mengatur kapan sebuah kabar layak muncul, sedangkan di sini yang ditanya
 * cuma "cuking mana yang paling dekat dari sini". Jadi radius kabar tetap jadi
 * urusan notifikasi, dan "dekat" di widget serta kartu Home punya satu arti yang
 * sama di semua pengaturan.
 */
const val NEARBY_CAT_RADIUS_METERS = 500.0

/** Menunggu posisi baru saat perangkat belum pernah mencatat posisi apa pun. */
private const val FRESH_FIX_TIMEOUT_MILLIS = 5_000L

/** Hasil pencarian cuking terdekat. Dipisah begini karena "tidak ada cuking" dan
 * "posisimu belum diketahui" itu dua hal berbeda yang perlu dikatakan berbeda. */
sealed interface NearestCatResult {

    data class Found(val cat: Cat, val distanceMeters: Double) : NearestCatResult

    /** Izin lokasi belum ada, atau perangkat tidak punya posisi yang bisa dipakai. */
    data object NoLocation : NearestCatResult

    /** Posisinya diketahui, tapi tidak ada cuking di dalam radiusnya. */
    data object NoneNearby : NearestCatResult
}

/**
 * Cuking terdekat di dalam [maxMeters], atau null kalau tidak ada yang masuk radius.
 *
 * Murni matematika, jadi aturannya bisa diuji tanpa perangkat. Kalau dua cuking
 * jaraknya sama persis, yang menang adalah yang lebih dulu ada di [cats], dan
 * daftar dari Room urutannya terbaru dulu, jadi pada seri yang menang adalah
 * catatan yang paling baru.
 */
fun nearestCat(
    cats: List<Cat>,
    latitude: Double,
    longitude: Double,
    maxMeters: Double = NEARBY_CAT_RADIUS_METERS
): NearestCatResult.Found? {
    var best: NearestCatResult.Found? = null
    var bestDistance = Double.MAX_VALUE

    cats.forEach { cat ->
        val distance = distanceMeters(latitude, longitude, cat.latitude, cat.longitude)
        if (distance <= maxMeters && distance < bestDistance) {
            best = NearestCatResult.Found(cat, distance)
            bestDistance = distance
        }
    }

    return best
}

/**
 * Posisi perangkat dipakai untuk mencari cuking terdekat.
 *
 * Asalnya posisi terakhir yang sudah diketahui perangkat, karena itu instan dan
 * tidak menyalakan GPS. [allowFreshFix] dipakai layar Home, yang memang boleh
 * menunggu sebentar kalau perangkat belum pernah mencatat posisi sama sekali;
 * widget membiarkannya false, karena menggambar kartu di layar utama tidak boleh
 * menyalakan GPS diam-diam.
 */
@SuppressLint("MissingPermission")
suspend fun findNearestCat(
    locationHelper: LocationHelper,
    cats: List<Cat>,
    maxMeters: Double = NEARBY_CAT_RADIUS_METERS,
    allowFreshFix: Boolean = false
): NearestCatResult {
    // Lint tidak bisa melihat penjagaan izin yang ada persis di atas pemanggilan
    // di bawah, jadi penjagaannya ditulis di sini dan dijelaskan di baris ini.
    if (!locationHelper.hasLocationPermission()) return NearestCatResult.NoLocation

    var location = locationHelper.lastKnownLocation()
    if (location == null && allowFreshFix) {
        location = locationHelper.getCurrentLocation(FRESH_FIX_TIMEOUT_MILLIS)
    }
    if (location == null) return NearestCatResult.NoLocation

    val found = nearestCat(cats, location.latitude, location.longitude, maxMeters)
    return found
        ?.let { NearestCatResult.Found(it.cat, it.distanceMeters) }
        ?: NearestCatResult.NoneNearby
}

package com.khalied.cukinggo.location

import com.khalied.cukinggo.domain.model.Cat

/**
 * Pilihan radius "dekat".
 *
 * Angkanya bukan karangan: dokumentasi Android menyarankan minimal 100 m supaya
 * akurasi lokasi lewat Wi-Fi tidak bikin kabar palsu, jadi 100 m adalah pilihan
 * paling ketat, bukan lebih kecil dari itu.
 *
 * Disimpan sebagai angka meter supaya layer penyimpanan tidak perlu tahu tipe UI-nya.
 */
enum class NearbyRadius(val meters: Int) {
    NEAR(100),
    NORMAL(200),
    FAR(500);

    companion object {
        val DEFAULT = NORMAL

        /** Angka yang tidak dikenal (atau sisa versi lama) jatuh ke radius bawaan. */
        fun fromMeters(meters: Int?): NearbyRadius =
            entries.firstOrNull { it.meters == meters } ?: DEFAULT
    }
}

/** Batas dari Play Services: satu app cuma boleh memasang 100 geofence sekaligus. */
const val MAX_WATCH_AREAS = 100

/**
 * Jeda kabar per kucing.
 *
 * Tanpa jeda ini, kucing yang tempatnya kamu lewati tiap hari (misalnya di dekat
 * rumah) akan mengabarkan setiap kali kamu masuk radius, dan itu jadi berisik.
 * Dua belas jam berarti paling banyak dua kabar per hari untuk kucing yang sama.
 */
const val NEARBY_ALERT_COOLDOWN_MILLIS = 12L * 60L * 60L * 1000L

/** Satu lingkaran pantauan di sekitar kucing yang pernah ditandai. */
data class CatWatchArea(
    val catId: Long,
    val latitude: Double,
    val longitude: Double
)

/**
 * Memilih kucing mana yang dipantau.
 *
 * Daftar dari Room sudah terbaru dulu, dan yang diambil adalah [limit] terbaru
 * karena Play Services membatasi 100 geofence per app. Dipisah jadi fungsi murni
 * supaya aturan ini bisa diuji tanpa perangkat.
 */
fun catWatchAreas(cats: List<Cat>, limit: Int = MAX_WATCH_AREAS): List<CatWatchArea> =
    cats.take(limit).map { cat ->
        CatWatchArea(catId = cat.id, latitude = cat.latitude, longitude = cat.longitude)
    }

/** Apakah kucing ini boleh dikabarkan lagi, atau masih dalam masa jeda? */
fun shouldAlert(
    lastAlertedAt: Long?,
    now: Long,
    cooldownMillis: Long = NEARBY_ALERT_COOLDOWN_MILLIS
): Boolean = lastAlertedAt == null || now - lastAlertedAt >= cooldownMillis

/**
 * Langkah berikutnya yang dibutuhkan fitur "kabar dekat kucing".
 *
 * Urutannya mengikuti cara Android bekerja: izin notifikasi dulu (kalau tidak ada,
 * kabarnya tidak akan tampil), lalu izin lokasi biasa, baru izin lokasi latar
 * belakang. Izin latar belakang tidak bisa diberikan sebelum izin lokasi biasa
 * diberikan, jadi urutan ini tidak bisa ditukar.
 */
enum class NearbyAlertState {
    OFF,
    NEED_NOTIFICATION_PERMISSION,
    NEED_FOREGROUND_LOCATION,
    NEED_BACKGROUND_LOCATION,
    ACTIVE
}

fun nearbyAlertState(
    enabled: Boolean,
    canPostNotifications: Boolean,
    hasForegroundLocation: Boolean,
    hasBackgroundLocation: Boolean
): NearbyAlertState = when {
    !enabled -> NearbyAlertState.OFF
    !canPostNotifications -> NearbyAlertState.NEED_NOTIFICATION_PERMISSION
    !hasForegroundLocation -> NearbyAlertState.NEED_FOREGROUND_LOCATION
    !hasBackgroundLocation -> NearbyAlertState.NEED_BACKGROUND_LOCATION
    else -> NearbyAlertState.ACTIVE
}

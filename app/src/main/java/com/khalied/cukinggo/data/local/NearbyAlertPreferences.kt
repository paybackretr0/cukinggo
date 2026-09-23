package com.khalied.cukinggo.data.local

import android.content.Context
import com.khalied.cukinggo.location.NearbyRadius

/**
 * Setelan fitur "kabar dekat kucing", plus catatan waktu kabar terakhir per kucing.
 *
 * Ada dua radius yang disimpan, dan bedanya penting:
 *
 * - [radiusMeters] adalah pilihan pengguna.
 * - [watchedRadiusMeters] adalah radius yang benar-benar sedang terpasang di
 *   Play Services.
 *
 * Pemisahan itu yang membuat penggantian radius bisa dikenali: kalau nilainya
 * berbeda, area pantauan dipasang ulang; kalau sama, tidak ada yang disentuh.
 */
class NearbyAlertPreferences(context: Context) {

    private val preferences = context.applicationContext
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = preferences.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /** Radius pilihan pengguna. */
    fun radiusMeters(): Int = preferences.getInt(KEY_RADIUS, NearbyRadius.DEFAULT.meters)

    fun setRadiusMeters(meters: Int) {
        preferences.edit().putInt(KEY_RADIUS, meters).apply()
    }

    fun lastAlertedAt(catId: Long): Long? =
        preferences.getLong(keyLastAlert(catId), NO_TIMESTAMP).takeIf { it != NO_TIMESTAMP }

    fun setLastAlertedAt(catId: Long, timestamp: Long) {
        preferences.edit().putLong(keyLastAlert(catId), timestamp).apply()
    }

    /** Id kucing yang area pantauannya sedang terpasang. */
    fun watchedCatIds(): Set<String> =
        preferences.getStringSet(KEY_WATCHED_IDS, emptySet())?.toSet().orEmpty()

    fun setWatchedCatIds(ids: Set<String>) {
        preferences.edit().putStringSet(KEY_WATCHED_IDS, ids).apply()
    }

    /** Radius yang sedang terpasang; 0 berarti tidak ada area pantauan. */
    fun watchedRadiusMeters(): Int = preferences.getInt(KEY_WATCHED_RADIUS, 0)

    fun setWatchedRadiusMeters(meters: Int) {
        preferences.edit().putInt(KEY_WATCHED_RADIUS, meters).apply()
    }

    private fun keyLastAlert(catId: Long) = "$KEY_LAST_ALERT$catId"

    private companion object {
        const val FILE_NAME = "cukinggo_nearby_alert"
        const val KEY_ENABLED = "enabled"
        const val KEY_RADIUS = "radius_meters"
        const val KEY_LAST_ALERT = "last_alert_"
        const val KEY_WATCHED_IDS = "watched_ids"
        const val KEY_WATCHED_RADIUS = "watched_radius_meters"
        const val NO_TIMESTAMP = -1L
    }
}

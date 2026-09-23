package com.khalied.cukinggo.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.khalied.cukinggo.appContainer
import com.khalied.cukinggo.data.local.NearbyAlertPreferences
import com.khalied.cukinggo.util.hasBackgroundLocationPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Pengelola area pantauan "kabar dekat kucing".
 *
 * Seluruh daftar dipasang ulang saat berubah, bukan satu per satu, karena
 * daftarnya memang kecil (maksimal 100) dan perubahan daftarnya jarang: hanya
 * saat kamu menambah atau menghapus kucing, atau menyalakan fiturnya.
 *
 * Pasang ulang dilewati kalau daftarnya identik dengan yang sudah terpasang.
 * Tanpa penjagaan itu, setiap kali app dibuka kami akan mengganggu Play Services
 * tanpa ada yang berubah.
 */
internal object NearbyAlerts {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Dipakai UI saat pengguna menyalakan atau mematikan fiturnya. */
    fun setEnabled(context: Context, enabled: Boolean) {
        val appContext = context.applicationContext
        NearbyAlertPreferences(appContext).setEnabled(enabled)
        if (enabled) NearbyAlertNotifier.ensureChannel(appContext)
        syncAsync(appContext)
    }

    /**
     * Mengganti radius pantauan. Area yang sudah terpasang dipasang ulang dengan
     * radius baru, karena radius adalah bagian dari geofence-nya, bukan nilai yang
     * bisa diubah di tempat.
     */
    fun setRadius(context: Context, radius: NearbyRadius) {
        val appContext = context.applicationContext
        NearbyAlertPreferences(appContext).setRadiusMeters(radius.meters)
        syncAsync(appContext)
    }

    /** Dipanggil saat app dibuka, saat data kucing berubah, dan setelah izin dilihat ulang. */
    fun syncAsync(context: Context) {
        val appContext = context.applicationContext
        scope.launch { sync(appContext) }
    }

    /**
     * Anotasi ini mengikuti pola yang sama dengan LocationHelper: lint tidak bisa
     * melihat penjagaan izin yang dibungkus fungsi, dan penjagaannya ada tepat di
     * bawah ini ([hasBackgroundLocationPermission]). Kalau izinnya belum ada,
     * fungsinya berhenti sebelum menyentuh Play Services sama sekali.
     */
    @SuppressLint("MissingPermission")
    suspend fun sync(context: Context) {
        val preferences = NearbyAlertPreferences(context)
        val client = LocationServices.getGeofencingClient(context)
        val pendingIntent = geofencePendingIntent(context)

        // Tanpa izin lokasi latar belakang, geofence tidak akan pernah terpicu
        // saat app tertutup, jadi tidak ada gunanya dipasang.
        val areas = if (preferences.isEnabled() && context.hasBackgroundLocationPermission()) {
            catWatchAreas(context.appContainer.catRepository.getAllCatsOnce())
        } else {
            emptyList()
        }

        val radiusMeters = NearbyRadius.fromMeters(preferences.radiusMeters()).meters
        val desiredIds = areas.map { it.catId.toString() }.toSet()
        // 0 berarti tidak ada area pantauan yang terpasang.
        val desiredRadius = if (areas.isEmpty()) 0 else radiusMeters
        if (desiredIds == preferences.watchedCatIds() &&
            desiredRadius == preferences.watchedRadiusMeters()
        ) {
            return
        }

        if (areas.isEmpty()) {
            // Fitur dimatikan atau izinnya dicabut: pantauan lama harus dilepas,
            // kalau tidak kabarnya tetap muncul walau pengguna sudah mematikannya.
            client.removeGeofences(pendingIntent)
            preferences.setWatchedCatIds(emptySet())
            preferences.setWatchedRadiusMeters(0)
            return
        }

        // Kucing yang sudah dihapus perlu dilepas sendiri: memasang ulang hanya
        // menimpa geofence dengan request ID yang sama, tidak menghapus yang lain.
        val removedIds = preferences.watchedCatIds() - desiredIds
        if (removedIds.isNotEmpty()) client.removeGeofences(removedIds.toList())

        client.addGeofences(geofenceRequest(areas, radiusMeters), pendingIntent)
            .addOnSuccessListener {
                preferences.setWatchedCatIds(desiredIds)
                preferences.setWatchedRadiusMeters(radiusMeters)
            }
    }

    private fun geofenceRequest(areas: List<CatWatchArea>, radiusMeters: Int): GeofencingRequest {
        val geofences = areas.map { area ->
            Geofence.Builder()
                // Request ID memakai id kucing, supaya penerima kabar tahu kucing mana
                // yang terpicu tanpa perlu tabel penghubung.
                .setRequestId(area.catId.toString())
                .setCircularRegion(area.latitude, area.longitude, radiusMeters.toFloat())
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
        }

        return GeofencingRequest.Builder()
            // Kalau saat fitur dinyalakan kamu sedang berada di dalam radiusnya,
            // kabarnya langsung muncul sekali. Itu jadi tanda fiturnya memang jalan.
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()
    }

    private fun geofencePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, NearbyAlertReceiver::class.java)
        // Sejak Android 12, geofencing meminta PendingIntent yang mutable.
        // Di bawah itu flag-nya tidak ada, jadi jangan ikut dikirim.
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0

        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }
}

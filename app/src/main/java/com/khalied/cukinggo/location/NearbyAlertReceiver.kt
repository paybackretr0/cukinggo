package com.khalied.cukinggo.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.khalied.cukinggo.appContainer
import com.khalied.cukinggo.data.local.NearbyAlertPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Dijalankan Play Services saat kamu masuk radius kucing yang ditandai.
 *
 * Membaca database di sini butuh waktu, sedangkan receiver dibatasi sepuluh
 * detik, jadi prosesnya ditahan dengan `goAsync` dan datanya dibaca di coroutine.
 */
class NearbyAlertReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return

        val catIds = event.triggeringGeofences
            .orEmpty()
            .mapNotNull { geofence -> geofence.requestId.toLongOrNull() }
        if (catIds.isEmpty()) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        scope.launch {
            try {
                alertFor(appContext, catIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun alertFor(context: Context, catIds: List<Long>) {
        val preferences = NearbyAlertPreferences(context)
        if (!preferences.isEnabled()) return

        val cats = context.appContainer.catRepository.getCatsByIds(catIds)
        val now = System.currentTimeMillis()

        // Satu kabar per kejadian. Kalau beberapa radius terpicu bersamaan,
        // yang dikabarkan cukup satu, yaitu yang sedang tidak dalam masa jeda.
        val cat = cats.firstOrNull { shouldAlert(preferences.lastAlertedAt(it.id), now) } ?: return

        NearbyAlertNotifier.post(context, cat)
        preferences.setLastAlertedAt(cat.id, now)
    }

    private companion object {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}

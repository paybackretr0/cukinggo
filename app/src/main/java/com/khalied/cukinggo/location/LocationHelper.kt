package com.khalied.cukinggo.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Pembungkus tipis FusedLocationProviderClient: ambil satu posisi terkini tanpa
 * input manual dari user.
 */
class LocationHelper(private val context: Context) {

    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Coba ambil lokasi terkini dulu; kalau dalam [timeoutMillis] belum dapat,
     * jatuh ke lokasi terakhir yang diketahui perangkat.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(timeoutMillis: Long = 20_000L): Location? {
        if (!hasLocationPermission()) return null

        val fresh = withTimeoutOrNull(timeoutMillis) { requestCurrentLocation() }
        if (fresh != null) return fresh

        return withTimeoutOrNull(5_000L) { requestLastKnownLocation() }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestCurrentLocation(): Location? =
        suspendCancellableCoroutine { continuation ->
            val tokenSource = CancellationTokenSource()
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
                .addOnSuccessListener { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null)
                }
            continuation.invokeOnCancellation { tokenSource.cancel() }
        }

    /** Lokasi terakhir yang sudah diketahui perangkat, instan, cocok untuk tombol "ke lokasi saya". */
    suspend fun lastKnownLocation(): Location? =
        withTimeoutOrNull(5_000L) { requestLastKnownLocation() }

    @SuppressLint("MissingPermission")
    private suspend fun requestLastKnownLocation(): Location? =
        suspendCancellableCoroutine { continuation ->
            client.lastLocation
                .addOnSuccessListener { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
                .addOnFailureListener {
                    if (continuation.isActive) continuation.resume(null)
                }
        }
}

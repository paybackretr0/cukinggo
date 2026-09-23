package com.khalied.cukinggo.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Cek singkat "perangkat sedang punya internet yang benar-benar jalan".
 *
 * Dipakai hanya untuk memilih kalimat yang jujur saat peta gagal memuat tile,
 * bukan untuk mengunci fitur: catatan kucing tetap bisa dipakai tanpa internet.
 */
fun Context.isOnline(): Boolean {
    val manager = getSystemService(ConnectivityManager::class.java) ?: return false
    val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

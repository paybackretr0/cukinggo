package com.khalied.cukinggo

import android.app.Application
import android.content.Context
import com.khalied.cukinggo.di.AppContainer
import com.khalied.cukinggo.location.NearbyAlertNotifier
import com.khalied.cukinggo.location.NearbyAlerts

class CukingGoApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Channel notifikasi dibuat sejak app dibuka, bukan menunggu dialog kabar
        // dekat dibuka dulu: pengaturannya jadi sudah terlihat di Pengaturan HP
        // sejak awal, dan kabar bisa muncul walau dialognya belum pernah disentuh.
        NearbyAlertNotifier.ensureChannel(this)
        // Area pantauan geofence disamakan dengan koleksi begitu app dibuka. Tanpa
        // ini, sinkronisasinya cuma terjadi saat Home dibuka atau saat datanya
        // berubah, dan area yang belum tersinkron tidak akan pernah berbunyi.
        NearbyAlerts.syncAsync(this)
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as CukingGoApp).container

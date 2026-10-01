package com.khalied.cukinggo

import android.app.Application
import android.content.Context
import com.khalied.cukinggo.di.AppContainer
import com.khalied.cukinggo.location.NearbyAlertNotifier
import com.khalied.cukinggo.location.NearbyAlerts
import org.osmdroid.config.Configuration
import java.io.File

class CukingGoApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        configureOsmdroid()

        // Channel notifikasi dibuat sejak app dibuka, bukan menunggu dialog kabar
        // dekat dibuka dulu: pengaturannya jadi sudah terlihat di Pengaturan HP
        // sejak awal, dan kabar bisa muncul walau dialognya belum pernah disentuh.
        NearbyAlertNotifier.ensureChannel(this)
        // Area pantauan geofence disamakan dengan koleksi begitu app dibuka. Tanpa
        // ini, sinkronisasinya cuma terjadi saat Home dibuka atau saat datanya
        // berubah, dan area yang belum tersinkron tidak akan pernah berbunyi.
        NearbyAlerts.syncAsync(this)
    }

    /**
     * Cache tile peta disimpan di storage internal aplikasi supaya osmdroid tidak
     * butuh permission storage sama sekali.
     */
    private fun configureOsmdroid() {
        val config = Configuration.getInstance()
        config.load(this, getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        config.userAgentValue = packageName
        config.osmdroidBasePath = File(cacheDir, "osmdroid")
        config.osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
    }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as CukingGoApp).container

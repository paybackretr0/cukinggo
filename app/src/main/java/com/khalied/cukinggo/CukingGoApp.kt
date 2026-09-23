package com.khalied.cukinggo

import android.app.Application
import android.content.Context
import com.khalied.cukinggo.di.AppContainer
import org.osmdroid.config.Configuration
import java.io.File

class CukingGoApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        configureOsmdroid()
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

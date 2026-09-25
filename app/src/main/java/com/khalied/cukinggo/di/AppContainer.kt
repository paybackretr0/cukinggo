package com.khalied.cukinggo.di

import android.content.Context
import com.khalied.cukinggo.data.local.CatDatabase
import com.khalied.cukinggo.data.local.DisplayPreferences
import com.khalied.cukinggo.data.repository.CatRepository
import com.khalied.cukinggo.location.LocationHelper
import com.khalied.cukinggo.util.ImageStorageHelper
import com.khalied.cukinggo.location.NearbyAlerts
import com.khalied.cukinggo.widget.CatWidgets

/**
 * Service locator sederhana: cukup untuk app satu modul tanpa backend.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val database = CatDatabase.getInstance(appContext)

    val displayPreferences = DisplayPreferences(appContext)
    val imageStorageHelper = ImageStorageHelper(appContext)
    val locationHelper = LocationHelper(appContext)
    val catRepository = CatRepository(
        catDao = database.catDao(),
        catSightingDao = database.catSightingDao(),
        imageStorageHelper = imageStorageHelper,
        onCatsChanged = {
            // Dua hal yang harus ikut menyesuaikan saat daftar kucing berubah:
            // isi widget, dan daftar area pantauan kabar "dekat kucing".
            CatWidgets.refreshAll(appContext)
            NearbyAlerts.syncAsync(appContext)
        }
    )
}

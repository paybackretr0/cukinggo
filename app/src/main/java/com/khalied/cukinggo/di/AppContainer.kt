package com.khalied.cukinggo.di

import android.content.Context
import com.khalied.cukinggo.data.local.CatDatabase
import com.khalied.cukinggo.data.local.ThemePreferences
import com.khalied.cukinggo.data.repository.CatRepository
import com.khalied.cukinggo.location.LocationHelper
import com.khalied.cukinggo.util.ImageStorageHelper

/**
 * Service locator sederhana: cukup untuk app satu modul tanpa backend.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val database = CatDatabase.getInstance(appContext)

    val themePreferences = ThemePreferences(appContext)
    val imageStorageHelper = ImageStorageHelper(appContext)
    val locationHelper = LocationHelper(appContext)
    val catRepository = CatRepository(database.catDao(), imageStorageHelper)
}

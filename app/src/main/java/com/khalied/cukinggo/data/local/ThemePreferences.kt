package com.khalied.cukinggo.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Preferensi app. Baru satu nilai (mode tema), jadi SharedPreferences sudah cukup
 * dan tidak perlu menambah dependency baru.
 */
class ThemePreferences(context: Context) {

    private val preferences = context.applicationContext
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    private val _themeModeKey = MutableStateFlow(preferences.getString(KEY_THEME_MODE, null))
    val themeModeKey: StateFlow<String?> = _themeModeKey.asStateFlow()

    fun setThemeModeKey(key: String) {
        preferences.edit().putString(KEY_THEME_MODE, key).apply()
        _themeModeKey.value = key
    }

    private companion object {
        const val FILE_NAME = "cukinggo_prefs"
        const val KEY_THEME_MODE = "theme_mode"
    }
}

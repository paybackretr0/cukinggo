package com.khalied.cukinggo.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Preferensi tampilan: mode tema dan bentuk widget. Dua-duanya pilihan tampilan
 * yang dibaca app dan widget, jadi ditaruh di satu tempat; jumlah nilainya masih
 * sedikit, jadi SharedPreferences sudah cukup dan tidak perlu dependency baru.
 *
 * Nilainya disimpan sebagai key mentah, bukan tipe UI, supaya layer penyimpanan
 * ini tidak perlu tahu enum milik layar atau milik widget.
 */
class DisplayPreferences(context: Context) {

    private val preferences = context.applicationContext
        .getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    private val _themeModeKey = MutableStateFlow(preferences.getString(KEY_THEME_MODE, null))
    val themeModeKey: StateFlow<String?> = _themeModeKey.asStateFlow()

    private val _widgetSkinKey = MutableStateFlow(preferences.getString(KEY_WIDGET_SKIN, null))
    val widgetSkinKey: StateFlow<String?> = _widgetSkinKey.asStateFlow()

    /**
     * Bacaan sinkron untuk komponen yang tidak punya alur Compose, yaitu widget
     * di layar utama.
     */
    fun currentThemeModeKey(): String? = _themeModeKey.value

    fun currentWidgetSkinKey(): String? = _widgetSkinKey.value

    fun setThemeModeKey(key: String) {
        preferences.edit().putString(KEY_THEME_MODE, key).apply()
        _themeModeKey.value = key
    }

    fun setWidgetSkinKey(key: String) {
        preferences.edit().putString(KEY_WIDGET_SKIN, key).apply()
        _widgetSkinKey.value = key
    }

    private companion object {
        const val FILE_NAME = "cukinggo_prefs"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_WIDGET_SKIN = "widget_skin"
    }
}

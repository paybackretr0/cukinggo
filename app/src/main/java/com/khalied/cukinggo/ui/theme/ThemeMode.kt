package com.khalied.cukinggo.ui.theme

/**
 * Pilihan tema dari pengguna. Disimpan sebagai [storageKey] supaya layer
 * penyimpanan tidak perlu tahu tipe UI-nya.
 */
enum class ThemeMode(val storageKey: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    /** Ubah pilihan pengguna + kondisi HP jadi satu keputusan tema gelap. */
    fun isDark(systemInDarkTheme: Boolean): Boolean = when (this) {
        SYSTEM -> systemInDarkTheme
        LIGHT -> false
        DARK -> true
    }

    companion object {
        /** Key tidak dikenal atau belum pernah disimpan dianggap "ikut sistem". */
        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.storageKey == key } ?: SYSTEM
    }
}

package com.khalied.cukinggo.data.local

import android.content.Context

/**
 * Stiker mana yang dipilih untuk paket WhatsApp.
 *
 * Disimpan di file preferensi yang sama dengan pilihan tampilan, dan sebagai
 * path file, bukan nomor urut: nomor urut bergeser setiap ada stiker baru,
 * sedangkan path menunjuk stiker yang sama.
 *
 * `null` dari [load] berarti pengguna belum pernah memilih apa pun, dan itu
 * berbeda artinya dengan "sengaja tidak memilih satu pun". Bedanya dipakai layar
 * untuk memutuskan apakah perlu mengisi pilihan bawaan.
 */
class StickerPackSelectionStore(context: Context) {

    private val preferences = context.applicationContext
        .getSharedPreferences(DisplayPreferences.FILE_NAME, Context.MODE_PRIVATE)

    fun load(): Set<String>? {
        if (!preferences.contains(KEY_SELECTION)) return null
        return preferences.getStringSet(KEY_SELECTION, emptySet()).orEmpty().toSet()
    }

    fun save(paths: Set<String>) {
        // Salinan baru, bukan objek dari load(): SharedPreferences tidak boleh
        // menyimpan himpunan yang masih dipegang pemanggilnya.
        preferences.edit().putStringSet(KEY_SELECTION, paths.toSet()).apply()
    }

    private companion object {
        const val KEY_SELECTION = "sticker_pack_selection"
    }
}

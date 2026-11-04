package com.khalied.cukinggo.di

import android.content.Context
import com.khalied.cukinggo.data.local.CatDatabase
import com.khalied.cukinggo.data.local.DisplayPreferences
import com.khalied.cukinggo.data.repository.CatRepository
import com.khalied.cukinggo.location.LocationHelper
import com.khalied.cukinggo.util.ImageStorageHelper
import com.khalied.cukinggo.location.NearbyAlerts
import com.khalied.cukinggo.sticker.MlKitSubjectSegmenter
import com.khalied.cukinggo.sticker.StickerMaker
import com.khalied.cukinggo.whatsapp.StickerPackExporter
import com.khalied.cukinggo.widget.CatWidgets

/**
 * Service locator sederhana: cukup untuk app satu modul tanpa backend.
 */
class AppContainer(context: Context) {

    /**
     * Konteks app, dipakai layar yang perlu memeriksa WhatsApp dan menyiapkan
     * paket stikernya. Bukan activity, jadi tidak menyimpan apa pun milik layar.
     */
    val appContext: Context = context.applicationContext

    private val database = CatDatabase.getInstance(appContext)

    val displayPreferences = DisplayPreferences(appContext)
    val imageStorageHelper = ImageStorageHelper(appContext)
    val locationHelper = LocationHelper(appContext)

    // Satu pemisah latar dipakai ulang: membuat klien ML Kit per jepretan berarti
    // memuat model berkali-kali untuk pekerjaan yang sama.
    //
    // Dibuat saat pertama dipakai, bukan saat app dibuka: klien ML Kit memanggil
    // Play Services, dan apa pun yang gagal di sana tidak boleh membuat app tidak
    // bisa dibuka sama sekali. Kalau pembuatan stiker yang gagal, yang hilang
    // cuma fiturnya, bukan seluruh catatan cuking.
    val stickerMaker: StickerMaker by lazy { StickerMaker(MlKitSubjectSegmenter()) }
    // Paket stiker WhatsApp ditulis dari master PNG yang sudah ada, jadi tidak
    // butuh pemisah latar lagi.
    val stickerPackExporter = StickerPackExporter(appContext)
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

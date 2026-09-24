package com.khalied.cukinggo.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.khalied.cukinggo.appContainer

/**
 * Widget "Cuking terakhir": satu kartu berisi foto cuking yang paling baru ditandai,
 * dengan catatannya (atau satu baris lucu kalau catatannya kosong). Bingkainya
 * bergilir tiap hari, lihat [WidgetSkin].
 *
 * Isi fotonya hanya berubah kalau app menambah atau menghapus kucing, dan saat itu
 * [CatWidgets.refreshAll] dipanggil dari repository. Yang tetap dipasang adalah
 * alarm tengah malam, karena lencana rentetan di sudut foto harus dihitung ulang
 * tiap hari supaya angkanya tidak memutih saat rentetannya putus.
 */
class CatWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        CatWidgets.scheduleMidnightRefresh(context)

        // onUpdate dibatasi 10 detik dan berjalan di main thread, sedangkan
        // datanya dibaca dari Room. goAsync menahan proses tetap hidup sampai
        // gambarnya selesai dibuat.
        val pendingResult = goAsync()
        CatWidgets.redrawAsync(context, CatWidgetProvider::class.java, appWidgetIds, pickCat) {
            pendingResult.finish()
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Kalau varian "Kucing hari ini" masih terpasang, alarmnya masih dibutuhkan.
        CatWidgets.cancelMidnightRefreshIfUnused(context)
    }

    companion object {
        /** Cuking yang paling baru ditandai. */
        internal val pickCat: suspend (Context) -> WidgetPick = { context ->
            WidgetPick(context.appContainer.catRepository.latestCat())
        }
    }
}

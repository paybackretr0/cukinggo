package com.khalied.cukinggo.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.khalied.cukinggo.appContainer
import java.time.LocalDate

/**
 * Widget "Cuking hari ini": satu kartu yang isinya berganti sendiri tiap hari,
 * dengan bingkai yang juga bergilir tiap hari (lihat [WidgetSkin]).
 *
 * Beda dari widget "Kucing terakhir", widget ini perlu tahu kapan hari berganti,
 * jadi ia memasang alarm tengah malam lewat [CatWidgets.scheduleMidnightRefresh].
 * Alarm itu dipasang ulang setiap kali widget digambar (termasuk setelah HP
 * dinyalakan ulang, karena host mengirim update ke widget begitu sistem siap).
 *
 * Sejak ada lencana rentetan, alarm yang sama juga dipakai widget "Kucing
 * terakhir", karena lencananya perlu dihitung ulang tiap hari di ketiga varian.
 */
class CatOfDayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        CatWidgets.scheduleMidnightRefresh(context)

        val pendingResult = goAsync()
        CatWidgets.redrawAsync(context, CatOfDayWidgetProvider::class.java, appWidgetIds, pickCat) {
            pendingResult.finish()
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Instance terakhir varian ini dilepas. Alarmnya baru dipadamkan kalau
        // tidak ada widget lain yang masih butuh pembaruan harian.
        CatWidgets.cancelMidnightRefreshIfUnused(context)
    }

    companion object {

        /** Satu catatan dipilih dari seluruh koleksi, bergilir satu langkah per hari. */
        internal val pickCat: suspend (Context) -> WidgetPick = { context ->
            WidgetPick(
                catOfDay(
                    sightings = context.appContainer.catRepository.getAllSightingsOnce(),
                    epochDay = LocalDate.now().toEpochDay()
                )
            )
        }
    }
}

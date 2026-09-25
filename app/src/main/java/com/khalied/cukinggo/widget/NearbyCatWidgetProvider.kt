package com.khalied.cukinggo.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.khalied.cukinggo.R
import com.khalied.cukinggo.appContainer
import com.khalied.cukinggo.location.NEARBY_CAT_RADIUS_METERS
import com.khalied.cukinggo.location.NearestCatResult
import com.khalied.cukinggo.location.findNearestCat
import kotlin.math.roundToInt

/**
 * Widget "Cuking terdekat": satu kartu berisi cuking yang paling dekat dari posisimu,
 * dengan bingkai yang bergilir tiap hari (lihat [WidgetSkin]) dan tombol jepret yang
 * sama dengan dua widget lain.
 *
 * Bedanya dari dua widget lain: isinya ikut posisimu, bukan cuma datanya. Posisinya
 * diambil dari posisi terakhir yang sudah diketahui perangkat, dan itu memang
 * kompromi yang disengaja: menggambar kartu di layar utama tidak boleh menyalakan
 * GPS. Ongkosnya, kalau kamu berjalan tanpa pernah membuka app, kartunya bisa
 * menampilkan cuking yang sudah tidak terdekat lagi. Karena itu widget ini digambar
 * ulang setiap kali app dibuka, di samping alarm tengah malam yang dipakai bersama.
 */
class NearbyCatWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        CatWidgets.scheduleMidnightRefresh(context)

        val pendingResult = goAsync()
        CatWidgets.redrawAsync(
            context,
            NearbyCatWidgetProvider::class.java,
            appWidgetIds,
            pickCat
        ) {
            pendingResult.finish()
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        CatWidgets.cancelMidnightRefreshIfUnused(context)
    }

    companion object {

        /**
         * Cuking paling dekat dari posisi perangkat.
         *
         * Yang dibaca cuma penemuan terbaru tiap cuking, karena yang ditanya di
         * sini tempat cukingnya sekarang, bukan semua tempat dia pernah terlihat.
         *
         * Yang kosong dibedakan: izin/posisi yang belum ada dan radius yang kosong
         * adalah dua hal berbeda, dan kalimat kartunya harus mengatakan yang benar.
         */
        internal val pickCat: suspend (Context) -> WidgetPick = { context ->
            when (
                val result = findNearestCat(
                    locationHelper = context.appContainer.locationHelper,
                    sightings = context.appContainer.catRepository.latestSightingsPerCat()
                )
            ) {
                is NearestCatResult.Found -> WidgetPick(result.sighting)

                NearestCatResult.NoLocation -> WidgetPick(
                    sighting = null,
                    emptyCaption = context.getString(R.string.widget_nearby_need_location)
                )

                NearestCatResult.NoneNearby -> WidgetPick(
                    sighting = null,
                    emptyCaption = context.getString(
                        R.string.widget_nearby_empty,
                        NEARBY_CAT_RADIUS_METERS.roundToInt()
                    )
                )
            }
        }
    }
}

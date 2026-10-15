package com.khalied.cukinggo.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.view.View
import android.widget.RemoteViews
import com.khalied.cukinggo.MainActivity
import com.khalied.cukinggo.R
import com.khalied.cukinggo.appContainer
import com.khalied.cukinggo.domain.model.CatSighting
import com.khalied.cukinggo.ui.theme.ThemeMode
import com.khalied.cukinggo.util.catStreak
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Bagian yang dipakai bersama oleh tiga widget di layar utama:
 *
 * - "Kucing terakhir" (kucing yang paling baru ditandai)
 * - "Kucing hari ini" (satu kucing yang berganti tiap hari)
 * - "Kucing terdekat" (kucing paling dekat dari posisimu)
 *
 * Ketiganya memakai layout, warna, dan cara menempelkan foto yang sama persis.
 * Yang berbeda hanya kucing mana yang dipilih, dan itu ditentukan oleh masing-masing
 * provider lewat [pickCat]. Jadi tidak ada satu pun bagian tampilan yang ditulis
 * dua kali.
 *
 * Bentuk bingkainya bergilir tiap hari (lihat [WidgetSkin]), dan bentuknya dipilih
 * dari tanggal, bukan dari provider-provider ini, jadi semua widget yang terpasang
 * bersamaan menampilkan bentuk yang sama hari itu.
 */
internal object CatWidgets {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val providers: List<Class<out AppWidgetProvider>> = listOf(
        CatWidgetProvider::class.java,
        CatOfDayWidgetProvider::class.java,
        NearbyCatWidgetProvider::class.java
    )

    /** Dipanggil setiap daftar kucing berubah: ketiga widget ikut menyesuaikan. */
    fun refreshAll(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            draw(appContext, CatWidgetProvider::class.java, null, CatWidgetProvider.pickCat)
            draw(appContext, CatOfDayWidgetProvider::class.java, null, CatOfDayWidgetProvider.pickCat)
            draw(
                appContext,
                NearbyCatWidgetProvider::class.java,
                null,
                NearbyCatWidgetProvider.pickCat
            )
        }
    }

    /**
     * Dipakai dari `onUpdate`. [onFinished] dipanggil di thread yang sama setelah
     * gambarnya selesai, supaya pemanggilnya bisa menutup `goAsync()`.
     */
    fun redrawAsync(
        context: Context,
        provider: Class<out AppWidgetProvider>,
        ids: IntArray?,
        pickCat: suspend (Context) -> WidgetPick,
        onFinished: () -> Unit = {}
    ) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                draw(appContext, provider, ids, pickCat)
            } finally {
                onFinished()
            }
        }
    }

    /**
     * Alarm tengah malam, dipasang ulang setiap kali widget digambar.
     *
     * Kenapa ada, walau cuma satu varian yang isinya berganti tiap hari: lencana
     * rentetan (lihat [showStreak]) dihitung saat widget digambar, jadi angkanya
     * bisa memutih kalau tidak pernah dihitung ulang. Kalau rentetannya putus,
     * widget yang terakhir digambar dua hari lalu akan masih menampilkan angka
     * lama. Karena itu ketiga varian widget mendaftar ke alarm yang sama.
     *
     * Ketepatan, biar tidak diklaim lebih dari kenyataannya: yang dipakai adalah
     * alarm *inexact* jenis RTC, jadi (1) tidak butuh izin "Alarms & reminders",
     * (2) tidak membangunkan HP tengah malam, dan (3) Android boleh menggeser
     * pengirimannya sampai sekitar satu jam, atau lebih lama saat HP hemat baterai
     * atau Doze. Untuk widget di layar utama, itu justru pas: angkanya diperbarui
     * saat HP memang sedang dipakai.
     */
    fun scheduleMidnightRefresh(context: Context) {
        if (!hasWidgets(context)) return
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val nextMidnight = LocalDate.now()
            .plusDays(1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC,
            nextMidnight,
            midnightIntent(context)
        )
    }

    /**
     * Dipakai `onDisabled`: satu varian widget dilepas, tapi alarm baru benar-benar
     * dimatikan kalau tidak ada satu pun widget yang tersisa. Kalau tidak begitu,
     * melepas widget "Kucing hari ini" sambil menyimpan "Kucing terakhir" akan
     * mematikan pembaruan lencana rentetan milik widget yang masih terpasang.
     */
    fun cancelMidnightRefreshIfUnused(context: Context) {
        if (hasWidgets(context)) return
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        alarmManager.cancel(midnightIntent(context))
    }

    /** Ada tidaknya widget dinilai langsung dari host, bukan dari catatan sendiri. */
    private fun hasWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        return providers.any { provider ->
            manager.getAppWidgetIds(ComponentName(context, provider)).isNotEmpty()
        }
    }

    private fun midnightIntent(context: Context): PendingIntent {
        val intent = Intent(context, WidgetMidnightReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private suspend fun draw(
        context: Context,
        provider: Class<out AppWidgetProvider>,
        ids: IntArray?,
        pickCat: suspend (Context) -> WidgetPick
    ) {
        val manager = AppWidgetManager.getInstance(context)
        val widgetIds = ids
            ?: manager.getAppWidgetIds(ComponentName(context, provider))
        if (widgetIds.isEmpty()) return

        // Membaca data bisa gagal (misalnya DB-nya sedang sibuk), dan kartu kosong
        // masih lebih baik daripada widget yang tidak pernah digambar.
        val pick = runCatching { pickCat(context) }.getOrNull() ?: WidgetPick(sighting = null)

        // Rentetan harian itu milik seluruh catatan, bukan milik satu kucing, jadi
        // dihitung sekali untuk semua widget dan ketiga varian widget menampilkan
        // angka yang sama.
        val streakDays = runCatching {
            catStreak(
                timestamps = context.appContainer.catRepository.getAllTimestamps(),
                today = LocalDate.now()
            )
        }.getOrDefault(0)

        // RemoteViews disusun per instance, bukan sekali untuk semua, karena tap
        // di setiap widget harus membuka kucing yang sedang tampil di widget itu.
        // Dua widget ini bisa menampilkan kucing yang berbeda.
        widgetIds.forEach { widgetId ->
            manager.updateAppWidget(widgetId, buildViews(context, pick, widgetId, streakDays))
        }
    }

    private suspend fun buildViews(
        context: Context,
        pick: WidgetPick,
        widgetId: Int,
        streakDays: Int
    ): RemoteViews {
        val sighting = pick.sighting
        val views = RemoteViews(context.packageName, R.layout.widget_cat_photo)

        val isDark = ThemeMode.fromKey(context.appContainer.displayPreferences.currentThemeModeKey())
            .isDark(isSystemInDark(context))
        val skinMode = WidgetSkinMode.fromKey(
            context.appContainer.displayPreferences.currentWidgetSkinKey()
        )
        val style = widgetStyle(skinMode.skinFor(LocalDate.now()), isDark)

        // setBackgroundResource: satu-satunya cara mengganti isian + garis tepi
        // yang membulat sekaligus. setBackgroundColor akan menghapus radius sudutnya.
        views.setInt(R.id.widget_root, "setBackgroundResource", style.frameRes)
        views.setImageViewResource(R.id.widget_art, style.fallbackArtRes)
        views.setTextColor(R.id.widget_caption, style.captionColor)
        showDecorations(views, style)

        val caption = if (sighting == null) {
            // Kalimat kosongnya boleh dibawa pemilihnya sendiri, karena cuma dia
            // yang tahu kenapa kosong. Bawaannya kalimat "belum ada cuking".
            pick.emptyCaption ?: context.getString(R.string.widget_empty)
        } else {
            widgetCaption(
                description = sighting.description,
                catId = sighting.id,
                funnyLines = context.resources.getStringArray(R.array.widget_funny_lines).toList()
            )
        }
        views.setTextViewText(R.id.widget_caption, caption)

        val hasPhoto = sighting != null &&
            WidgetPhoto(context).applyTo(views, sighting.photoPath, style)
        val circlePhoto = hasPhoto && style.photoShape == WidgetPhotoShape.CIRCLE
        views.setViewVisibility(R.id.widget_photo, if (hasPhoto && !circlePhoto) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_photo_window, if (circlePhoto) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_art, if (hasPhoto) View.GONE else View.VISIBLE)

        showStreak(views, context, streakDays)

        views.setOnClickPendingIntent(
            R.id.widget_root,
            openSightingIntent(context, sighting?.id, widgetId)
        )
        // Tombol jepret punya PendingIntent sendiri, dan yang tersentuh duluan
        // tetap tombolnya karena ia view anak di atas badan widget.
        views.setOnClickPendingIntent(R.id.widget_capture, captureIntent(context, widgetId))
        return views
    }

    /**
     * Hiasan yang dimiliki satu bentuk saja. Bentuk lain menyembunyikan pitanya,
     * jadi satu layout tetap cukup untuk ketiga bentuk.
     *
     * Yang disembunyikan pitanya, bukan cuma gambarnya: pitanya sendiri yang
     * memakan tinggi di layout, jadi kalau ia masih VISIBLE walau gambarnya
     * kosong, bentuk yang tidak memakai hiasan itu tetap menyisakan ruang kosong.
     */
    private fun showDecorations(views: RemoteViews, style: WidgetStyle) {
        showDecoration(views, R.id.widget_ears_band, R.id.widget_ears, style.earsRes)
        showDecoration(views, R.id.widget_tail_band, R.id.widget_tail, style.tailRes)
        showCorners(views, style.cornerMaskRes)
    }

    /**
     * Penutup sudut atas foto, dipakai bentuk yang sudut fotonya tercetak di luar
     * lengkung badannya. Kedua sudut memakai aset yang sama; yang kanan cuma
     * dicerminkan dari layout, jadi tidak perlu file aset kedua.
     */
    private fun showCorners(views: RemoteViews, maskRes: Int) {
        val visibility = if (maskRes == 0) View.GONE else View.VISIBLE
        if (maskRes != 0) {
            views.setImageViewResource(R.id.widget_corner_start, maskRes)
            views.setImageViewResource(R.id.widget_corner_end, maskRes)
        }
        views.setViewVisibility(R.id.widget_corner_start, visibility)
        views.setViewVisibility(R.id.widget_corner_end, visibility)
    }

    private fun showDecoration(views: RemoteViews, bandId: Int, artId: Int, artRes: Int) {
        if (artRes == 0) {
            views.setViewVisibility(bandId, View.GONE)
            return
        }
        views.setImageViewResource(artId, artRes)
        views.setViewVisibility(bandId, View.VISIBLE)
    }

    /** Badge api di sudut foto. Rentetan nol berarti badge-nya tidak ditampilkan. */
    private fun showStreak(views: RemoteViews, context: Context, streakDays: Int) {
        if (streakDays <= 0) {
            views.setViewVisibility(R.id.widget_streak, View.GONE)
            return
        }

        views.setViewVisibility(R.id.widget_streak, View.VISIBLE)
        views.setTextViewText(R.id.widget_streak_text, streakDays.toString())
        // Angka sendirian di sudut foto tidak berarti apa-apa buat TalkBack, jadi
        // keseluruhan badge dibacakan sebagai satu kalimat.
        views.setContentDescription(
            R.id.widget_streak,
            context.getString(R.string.streak_badge_description, streakDays)
        )
    }

    /**
     * Tap widget membuka detail penemuan yang sedang tampil.
     *
     * Request code-nya id widget, bukan satu angka untuk semua. PendingIntent
     * dibedakan tanpa melihat isi extra, jadi kalau request code-nya sama, widget
     * yang dibangun belakangan akan menimpa extra milik widget lain, dan tap-nya
     * mendarat di penemuan yang salah.
     */
    private fun openSightingIntent(
        context: Context,
        sightingId: Long?,
        widgetId: Int
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(
                MainActivity.EXTRA_SIGHTING_ID,
                sightingId ?: MainActivity.NO_SIGHTING_ID
            )

        return PendingIntent.getActivity(
            context,
            widgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Tombol jepret di widget: membuka app langsung di layar kamera, dan kamera itu
     * menjepret sendiri begitu siap (lihat `AddCatScreen`).
     *
     * Request code-nya digeser jauh dari request code tap widget. PendingIntent
     * dibedakan tanpa melihat isi extra, jadi kalau keduanya memakai angka yang
     * sama, tombol jepret dan tap badan widget akan mendarat di PendingIntent yang
     * sama, dan salah satunya diam-diam hilang. Action yang berbeda dipasang juga
     * supaya keduanya tidak pernah bertabrakan.
     */
    private fun captureIntent(context: Context, widgetId: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(ACTION_CAPTURE)
            .putExtra(MainActivity.EXTRA_CAPTURE_NOW, true)

        return PendingIntent.getActivity(
            context,
            CAPTURE_REQUEST_CODE_BASE + widgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun isSystemInDark(context: Context): Boolean {
        val uiMode = context.resources.configuration.uiMode
        return (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
}

/**
 * Isi satu kartu widget: penemuan yang ditampilkan, plus kalimat penggantinya
 * kalau tidak ada.
 *
 * Yang ditampilkan hanyalah satu penemuan, karena widget cuma punya satu foto
 * dan satu baris caption. Kalimat kosongnya dibawa bersama pilihannya karena cuma
 * pemilihnya yang tahu kenapa kosong: widget "Cuking terdekat" bisa kosong karena
 * posisinya belum diketahui atau karena memang tidak ada cuking di radiusnya,
 * sedangkan dua widget lain hanya kosong kalau belum ada catatan sama sekali.
 */
internal data class WidgetPick(
    val sighting: CatSighting?,
    val emptyCaption: String? = null
)

/** Action tombol jepret, dipakai hanya supaya PendingIntent-nya tidak bentrok. */
private const val ACTION_CAPTURE = "com.khalied.cukinggo.action.CAPTURE_CAT"

/** Lihat penjelasan di [CatWidgets.captureIntent] soal kenapa angka ini digeser. */
private const val CAPTURE_REQUEST_CODE_BASE = 10_000

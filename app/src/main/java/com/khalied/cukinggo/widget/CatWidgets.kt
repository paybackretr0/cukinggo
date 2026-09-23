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
import com.khalied.cukinggo.domain.model.Cat
import com.khalied.cukinggo.ui.theme.ThemeMode
import com.khalied.cukinggo.util.catStreak
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Bagian yang dipakai bersama oleh dua widget di layar utama:
 *
 * - "Kucing terakhir" (kucing yang paling baru ditandai)
 * - "Kucing hari ini" (satu kucing yang berganti tiap hari)
 *
 * Keduanya memakai layout, warna, dan cara menempelkan foto yang sama persis.
 * Yang berbeda hanya kucing mana yang dipilih, dan itu ditentukan oleh masing-masing
 * provider lewat [pickCat]. Jadi tidak ada satu pun bagian tampilan yang ditulis
 * dua kali.
 */
internal object CatWidgets {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val providers: List<Class<out AppWidgetProvider>> =
        listOf(CatWidgetProvider::class.java, CatOfDayWidgetProvider::class.java)

    /** Dipanggil setiap daftar kucing berubah: kedua widget ikut menyesuaikan. */
    fun refreshAll(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            draw(appContext, CatWidgetProvider::class.java, null, CatWidgetProvider.pickCat)
            draw(appContext, CatOfDayWidgetProvider::class.java, null, CatOfDayWidgetProvider.pickCat)
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
        pickCat: suspend (Context) -> Cat?,
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
     * lama. Karena itu kedua varian widget mendaftar ke alarm yang sama.
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
        pickCat: suspend (Context) -> Cat?
    ) {
        val manager = AppWidgetManager.getInstance(context)
        val widgetIds = ids
            ?: manager.getAppWidgetIds(ComponentName(context, provider))
        if (widgetIds.isEmpty()) return

        val cat = runCatching { pickCat(context) }.getOrNull()

        // Rentetan harian itu milik seluruh catatan, bukan milik satu kucing, jadi
        // dihitung sekali untuk semua widget dan kedua varian widget menampilkan
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
            manager.updateAppWidget(widgetId, buildViews(context, cat, widgetId, streakDays))
        }
    }

    private suspend fun buildViews(
        context: Context,
        cat: Cat?,
        widgetId: Int,
        streakDays: Int
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_cat_photo)

        val palette = widgetPalette(
            ThemeMode.fromKey(context.appContainer.themePreferences.currentThemeModeKey())
                .isDark(isSystemInDark(context))
        )
        // setBackgroundResource: satu-satunya cara mengganti isian + garis tepi
        // yang membulat sekaligus. setBackgroundColor akan menghapus radius sudutnya.
        views.setInt(R.id.widget_root, "setBackgroundResource", palette.frameRes)
        views.setImageViewResource(R.id.widget_art, palette.fallbackArtRes)
        views.setTextColor(R.id.widget_caption, palette.captionColor)

        val caption = if (cat == null) {
            context.getString(R.string.widget_empty)
        } else {
            widgetCaption(
                description = cat.description,
                catId = cat.id,
                funnyLines = context.resources.getStringArray(R.array.widget_funny_lines).toList()
            )
        }
        views.setTextViewText(R.id.widget_caption, caption)

        val hasPhoto = cat != null && WidgetPhoto(context).applyTo(views, cat.photoPath)
        views.setViewVisibility(R.id.widget_photo, if (hasPhoto) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_art, if (hasPhoto) View.GONE else View.VISIBLE)

        showStreak(views, context, streakDays)

        views.setOnClickPendingIntent(
            R.id.widget_root,
            openCatIntent(context, cat?.id, widgetId)
        )
        return views
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
     * Tap widget membuka detail kucing yang sedang tampil.
     *
     * Request code-nya id widget, bukan satu angka untuk semua. PendingIntent
     * dibedakan tanpa melihat isi extra, jadi kalau request code-nya sama, widget
     * yang dibangun belakangan akan menimpa extra milik widget lain, dan tap-nya
     * mendarat di kucing yang salah.
     */
    private fun openCatIntent(context: Context, catId: Long?, widgetId: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_CAT_ID, catId ?: MainActivity.NO_CAT_ID)

        return PendingIntent.getActivity(
            context,
            widgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun isSystemInDark(context: Context): Boolean {
        val uiMode = context.resources.configuration.uiMode
        return (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }
}

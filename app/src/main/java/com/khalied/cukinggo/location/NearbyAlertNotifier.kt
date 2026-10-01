package com.khalied.cukinggo.location

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import android.content.pm.PackageManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.khalied.cukinggo.MainActivity
import com.khalied.cukinggo.R
import com.khalied.cukinggo.domain.model.CatSighting
import com.khalied.cukinggo.util.blankToNull

/**
 * Kabar di bilah notifikasi saat kamu masuk radius kucing yang pernah ditandai.
 *
 * Tap-nya memakai jalur yang sama dengan widget: id penemuannya dikirim ke
 * [MainActivity], lalu langsung mendarat di halaman detailnya.
 */
internal object NearbyAlertNotifier {

    private const val CHANNEL_ID = "kucing_dekat"

    /**
     * Id tetap untuk kabar contoh. Dipisah dari id cukingnya supaya kabar uji coba
     * tidak menimpa kabar sungguhan untuk cuking yang sama.
     */
    private const val TEST_NOTIFICATION_ID = 2_000_000_001

    /** Dibuat lebih awal supaya channel-nya sudah terlihat di pengaturan notifikasi HP. */
    fun ensureChannel(context: Context) {
        val channel = NotificationChannelCompat
            .Builder(CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT)
            .setName(context.getString(R.string.nearby_channel_name))
            .setDescription(context.getString(R.string.nearby_channel_description))
            .build()

        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    fun post(context: Context, sighting: CatSighting) {
        if (!canPost(context)) return

        ensureChannel(context)

        val name = blankToNull(sighting.catName)
        val note = blankToNull(sighting.description)
        val body = note ?: context.getString(R.string.nearby_alert_body)
        // Nama cuking dipakai sebagai judul kalau ada, supaya kabarnya bisa dikenali
        // dari bilah notifikasi tanpa membuka app dulu.
        val title = if (name != null) {
            context.getString(R.string.nearby_alert_title_named, name)
        } else {
            context.getString(R.string.nearby_alert_title)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_paw)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openCatIntent(context, sighting.id))
            .setAutoCancel(true)
            .build()

        // Id notifikasi memakai id cukingnya: kabar untuk cuking yang sama menimpa
        // kabar sebelumnya, jadi tidak menumpuk di bilah notifikasi.
        NotificationManagerCompat.from(context).notify(sighting.catId.toInt(), notification)
    }

    /**
     * Kabar contoh, dipakai tombol "coba kirim" di dialog kabar dekat.
     *
     * Fitur ini bisa gagal di banyak tempat tanpa terlihat (izin notifikasi, channel
     * belum diizinkan, penghemat baterai yang menahan geofence), dan geofencenya
     * baru berbunyi saat pengguna kebetulan lewat. Satu kabar yang diminta langsung
     * oleh pengguna itulah cara membedakan "izinnya yang bermasalah" dari
     * "memang belum lewat", tanpa menunggu kejadian yang tidak bisa dipaksa.
     *
     * Kalau ada catatan, isinya memakai cuking terakhir supaya bentuknya sama
     * persis dengan kabar sungguhan; kalau koleksinya kosong, dipakai kalimat
     * contoh yang menjelaskan bahwa ini cuma uji coba.
     */
    fun postTest(context: Context, sighting: CatSighting?) {
        if (!canPost(context)) return

        ensureChannel(context)

        val name = sighting?.let { cat -> blankToNull(cat.catName) }
        val note = sighting?.let { cat -> blankToNull(cat.description) }
        val title = if (name != null) {
            context.getString(R.string.nearby_alert_title_named, name)
        } else {
            context.getString(R.string.nearby_test_title)
        }
        val body = note ?: context.getString(R.string.nearby_test_body)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_paw)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(TEST_NOTIFICATION_ID, notification)
    }

    private fun canPost(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun openCatIntent(context: Context, sightingId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_SIGHTING_ID, sightingId)

        return PendingIntent.getActivity(
            context,
            sightingId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

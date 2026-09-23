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
import com.khalied.cukinggo.domain.model.Cat

/**
 * Kabar di bilah notifikasi saat kamu masuk radius kucing yang pernah ditandai.
 *
 * Tap-nya memakai jalur yang sama dengan widget: id kucing dikirim ke
 * [MainActivity], lalu langsung mendarat di halaman detailnya.
 */
internal object NearbyAlertNotifier {

    private const val CHANNEL_ID = "kucing_dekat"

    /** Dibuat lebih awal supaya channel-nya sudah terlihat di pengaturan notifikasi HP. */
    fun ensureChannel(context: Context) {
        val channel = NotificationChannelCompat
            .Builder(CHANNEL_ID, NotificationManager.IMPORTANCE_DEFAULT)
            .setName(context.getString(R.string.nearby_channel_name))
            .setDescription(context.getString(R.string.nearby_channel_description))
            .build()

        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    fun post(context: Context, cat: Cat) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ensureChannel(context)

        val note = cat.description?.trim()?.takeIf { it.isNotEmpty() }
        val body = note ?: context.getString(R.string.nearby_alert_body)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_paw)
            .setContentTitle(context.getString(R.string.nearby_alert_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openCatIntent(context, cat.id))
            .setAutoCancel(true)
            .build()

        // Id notifikasi memakai id kucing: kabar untuk kucing yang sama menimpa
        // kabar sebelumnya, jadi tidak menumpuk di bilah notifikasi.
        NotificationManagerCompat.from(context).notify(cat.id.toInt(), notification)
    }

    private fun openCatIntent(context: Context, catId: Long): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_CAT_ID, catId)

        return PendingIntent.getActivity(
            context,
            catId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

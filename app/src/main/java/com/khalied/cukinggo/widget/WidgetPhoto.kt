package com.khalied.cukinggo.widget

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.RemoteViews
import androidx.core.content.FileProvider
import com.khalied.cukinggo.R
import java.io.File
import kotlin.math.max

/**
 * Menempelkan foto kucing terakhir ke widget.
 *
 * Ini bagian paling berliku dari widget, jadi alurnya ditulis jelas:
 *
 * 1. Foto ada di `filesDir`, yaitu storage privat app, sedangkan widget digambar
 *    oleh proses launcher. Launcher tidak punya izin baca ke sana.
 * 2. Jadi file-nya dibungkus [FileProvider] jadi URI `content://`, lalu izin baca
 *    diberikan eksplisit ke paket host (launcher, dan SystemUI untuk widget di
 *    layar kunci). Tanpa langkah ini, foto akan tampil kosong di layar utama.
 * 3. Kalau tidak ada paket host yang bisa ditemukan, foto dikirim sebagai bitmap
 *    kecil. Ukurannya dijaga di bawah batas transaksi Binder (1 MB), karena
 *    bitmap ikut dikirim bersama seluruh RemoteViews.
 *
 * Framework sendiri menolak URI `file://` di dalam RemoteViews (lihat
 * `checkRemoteViewsUris` di AppWidgetServiceImpl), jadi jalur 2 memang satu-satunya
 * cara mengirim foto resolusi penuh.
 */
internal class WidgetPhoto(private val context: Context) {

    private val authority: String = "${context.packageName}.fileprovider"

    private val hostPackages: List<String> by lazy { queryHostPackages() }

    /** Mengembalikan true kalau foto berhasil ditempelkan. */
    fun applyTo(remoteViews: RemoteViews, photoPath: String): Boolean {
        val file = File(photoPath)
        if (!isCatPhoto(file)) return false

        uriFor(file)?.let { uri ->
            remoteViews.setImageViewUri(R.id.widget_photo, uri)
            return true
        }

        val bitmap = decodeForWidget(file) ?: return false
        remoteViews.setImageViewBitmap(R.id.widget_photo, bitmap)
        return true
    }

    /**
     * Hanya menerima foto kucing yang disimpan app sendiri. Ini penjaga supaya
     * provider tidak pernah dimintai file lain yang kebetulan ada di folder itu.
     */
    private fun isCatPhoto(file: File): Boolean =
        file.isFile && file.parentFile == context.filesDir && file.name.startsWith("cat_")

    private fun uriFor(file: File): Uri? {
        val uri = runCatching {
            FileProvider.getUriForFile(context, authority, file)
        }.getOrNull() ?: return null

        if (hostPackages.isEmpty()) return null

        val grantedCount = hostPackages.count { packageName ->
            runCatching {
                context.grantUriPermission(
                    packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }.isSuccess
        }

        return uri.takeIf { grantedCount > 0 }
    }

    private fun queryHostPackages(): List<String> {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        @Suppress("DEPRECATION")
        val launchers = runCatching {
            context.packageManager.queryIntentActivities(homeIntent, PackageManager.MATCH_ALL)
        }.getOrNull().orEmpty().mapNotNull { it.activityInfo?.packageName }

        // Widget di layar kunci digambar SystemUI, dan itu tidak pernah muncul
        // di query HOME, jadi harus ditambahkan sendiri.
        return (launchers + SYSTEM_UI_PACKAGE).distinct()
    }

    /**
     * Jalur cadangan: bitmap kecil dengan sample size dipilih supaya sisi
     * terpanjangnya tidak melebihi [MAX_FALLBACK_PX]. RGB_565 dipakai supaya
     * ukurannya separuh, dan foto JPEG tidak butuh alpha.
     */
    private fun decodeForWidget(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / sampleSize > MAX_FALLBACK_PX) {
            sampleSize *= 2
        }

        return runCatching {
            BitmapFactory.decodeFile(
                file.absolutePath,
                BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
            )
        }.getOrNull()
    }

    private companion object {
        const val SYSTEM_UI_PACKAGE = "com.android.systemui"

        /** 384 px cukup untuk sel 2x2, dan aman dari batas transaksi Binder. */
        const val MAX_FALLBACK_PX = 384
    }
}

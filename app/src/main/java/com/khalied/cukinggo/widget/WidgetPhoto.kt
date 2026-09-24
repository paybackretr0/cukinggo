package com.khalied.cukinggo.widget

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.net.Uri
import android.widget.RemoteViews
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import com.khalied.cukinggo.R
import java.io.File
import kotlin.math.max
import kotlin.math.min

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
 *
 * Satu bentuk widget memotong fotonya bulat, dan itu selalu lewat jalur 3:
 * pemotongan tidak bisa dikerjakan RemoteViews pada URI, sedangkan foto yang
 * dikirim sebagai bitmap bisa diolah dulu. Ongkosnya foto di bentuk itu lebih
 * lembut, jadi batas ukurannya sengaja dibuat lebih besar dari jalur cadangan
 * biasa, tapi tetap jauh di bawah batas transaksi Binder.
 */
internal class WidgetPhoto(private val context: Context) {

    private val authority: String = "${context.packageName}.fileprovider"

    private val hostPackages: List<String> by lazy { queryHostPackages() }

    /** Mengembalikan true kalau foto berhasil ditempelkan. */
    fun applyTo(remoteViews: RemoteViews, photoPath: String, style: WidgetStyle): Boolean {
        val file = File(photoPath)
        if (!isCatPhoto(file)) return false

        if (style.photoShape == WidgetPhotoShape.CIRCLE) {
            val bitmap = circleBitmap(file, style) ?: return false
            remoteViews.setImageViewBitmap(R.id.widget_photo_window, bitmap)
            return true
        }

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
     * Foto persegi yang dipotong bulat, dengan bagian di luar lingkaran diisi
     * warna badan widget. Pengisian itu yang membuat bitmap-nya tidak butuh
     * saluran alpha: isi di luar lingkaran memang harus sama dengan badan widget,
     * jadi satu bitmap RGB sudah cukup dan ukurannya separuh.
     *
     * Sisi bitmap-nya dihitung dari sisi terpendek foto, jadi foto lanskap
     * maupun tegak sama-sama jadi lingkaran penuh, bukan elips.
     *
     * Kumis bentuk jendela digambar di sini, di dalam lingkaran, bukan sebagai
     * hiasan di layout. Hiasan di layout punya jarak tetap dari tepi kartu,
     * sedangkan lingkaran ini menyusut di tengah dan ukurannya ikut berubah saat
     * widget ditarik, jadi kumis akan melayang menjauh dari lingkarannya.
     */
    private fun circleBitmap(file: File, style: WidgetStyle): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        while (min(bounds.outWidth, bounds.outHeight) / sampleSize > MAX_CIRCLE_PX) {
            sampleSize *= 2
        }

        val source = runCatching {
            BitmapFactory.decodeFile(
                file.absolutePath,
                BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
            )
        }.getOrNull() ?: return null

        val side = min(source.width, source.height)
        if (side <= 0) return null

        return runCatching {
            val output = createBitmap(side, side, Bitmap.Config.RGB_565)
            val canvas = Canvas(output)
            canvas.drawColor(style.bodyColor)
            canvas.clipPath(
                Path().apply { addCircle(side / 2f, side / 2f, side / 2f, Path.Direction.CW) }
            )
            val left = (source.width - side) / 2
            val top = (source.height - side) / 2
            canvas.drawBitmap(
                source,
                Rect(left, top, left + side, top + side),
                Rect(0, 0, side, side),
                Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            )
            drawWhiskers(canvas, side, style)
            output
        }.getOrNull()
    }

    /**
     * Kumis bentuk jendela: dua segitiga di tiap sisi, yaitu bentuk yang sama
     * dengan telinga cuking tapi diputar 90 derajat sehingga ujungnya mengarah ke
     * dalam foto. Digambar di dalam bitmap, bukan sebagai hiasan di layout, supaya
     * selalu menempel di tepi lingkaran di ukuran widget berapa pun: hiasan layout
     * punya jarak tetap dari tepi kartu, sedangkan lingkarannya menyusut di tengah.
     *
     * Tiap segitiga digambar dua kali, berisi warna badan dengan garis tepi gelap,
     * supaya tetap terbaca di atas foto gelap maupun terang tanpa perlu tahu isi
     * fotonya lebih dulu.
     */
    private fun drawWhiskers(canvas: Canvas, side: Int, widgetStyle: WidgetStyle) {
        val centerY = side / 2f
        val length = side * LENGTH_RATIO
        val halfBase = side * HALF_BASE_RATIO
        val gap = side * GAP_RATIO

        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = widgetStyle.whiskerLineColor
            style = Paint.Style.FILL
        }
        val outline = Paint(fill).apply {
            color = widgetStyle.whiskerOutlineColor
            style = Paint.Style.STROKE
            strokeWidth = side * OUTLINE_RATIO
            strokeJoin = Paint.Join.ROUND
        }

        // direction +1 berarti sisi kiri: pangkalnya di tepi kiri, ujungnya ke kanan.
        listOf(1f, -1f).forEach { direction ->
            val baseX = if (direction > 0) 0f else side.toFloat()
            listOf(-1f, 1f).forEach { offset ->
                val baseY = centerY + (offset * gap)
                val whisker = Path().apply {
                    moveTo(baseX, baseY - halfBase)
                    lineTo(baseX, baseY + halfBase)
                    lineTo(baseX + (direction * length), baseY)
                    close()
                }
                canvas.drawPath(whisker, fill)
                canvas.drawPath(whisker, outline)
            }
        }
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

        /** 384 px cukup untuk bingkai biasa, dan aman dari batas transaksi Binder. */
        const val MAX_FALLBACK_PX = 384

        /** 448 px dipilih supaya foto bulat tetap tajam saat widget ditarik besar,
         *  sementara ukuran bitmap-nya (sekitar 400 KB) masih jauh di bawah batas
         *  transaksi Binder, karena fotonya RGB tanpa saluran alpha. */
        const val MAX_CIRCLE_PX = 448

        /** Semua angka kumis dihitung dari sisi lingkaran, jadi ikut ukuran widget. */
        const val LENGTH_RATIO = 0.26f
        const val HALF_BASE_RATIO = 0.05f
        const val GAP_RATIO = 0.075f
        const val OUTLINE_RATIO = 0.022f
    }
}

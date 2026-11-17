package com.khalied.cukinggo.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Membaca foto dalam keadaan tegak, sudah dikecilkan ke [maxSide].
 *
 * Kamera menyimpan hasilnya dengan penanda orientasi, bukan dengan piksel yang
 * sudah diputar. Tanpa langkah EXIF ini foto bisa terbaca miring 90 derajat,
 * padahal di galeri terlihat tegak.
 *
 * Dua pemakai fungsi ini punya batas yang berbeda: pemisah latar stiker butuh
 * detail bulu, sedangkan pemeriksa "ada cuking atau tidak" cukup butuh bentuk
 * besarnya. Karena itu batasnya jadi parameter, bukan angka yang ditulis di sini.
 *
 * `null` berarti file tidak terbaca sebagai gambar. Itu kegagalan teknis, bukan
 * jawaban "tidak ada apa-apa di foto".
 */
fun decodeUprightPhoto(source: File, maxSide: Int): Bitmap? {
    val boundsOnly = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(source.absolutePath, boundsOnly)
    if (boundsOnly.outWidth <= 0 || boundsOnly.outHeight <= 0) return null

    // Sampel dilipat dua sampai muat: dekode penuh dulu lalu diperkecil berarti
    // foto 12 MP sempat menempati puluhan MB sebelum dikecilkan.
    var sampleSize = 1
    while (max(boundsOnly.outWidth, boundsOnly.outHeight) / sampleSize > maxSide) {
        sampleSize *= 2
    }

    val bitmap = BitmapFactory.decodeFile(
        source.absolutePath,
        BitmapFactory.Options().apply { inSampleSize = sampleSize }
    ) ?: return null

    val upright = applyExifRotation(source, bitmap)
    val longestSide = max(upright.width, upright.height)
    if (longestSide <= maxSide) return upright

    // Sampel cuma bisa pangkat dua, jadi hasilnya masih bisa lebih besar dari
    // batas. Skala terakhir ini yang memastikan angkanya benar-benar [maxSide].
    val scale = maxSide.toFloat() / longestSide
    val scaled = Bitmap.createScaledBitmap(
        upright,
        (upright.width * scale).roundToInt(),
        (upright.height * scale).roundToInt(),
        true
    )
    if (scaled !== upright) upright.recycle()
    return scaled
}

private fun applyExifRotation(source: File, bitmap: Bitmap): Bitmap {
    val orientation = runCatching {
        ExifInterface(source).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.postScale(-1f, 1f)
        }
        else -> return bitmap
    }

    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

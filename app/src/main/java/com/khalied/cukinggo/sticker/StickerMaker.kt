package com.khalied.cukinggo.sticker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sisi terpanjang foto yang diserahkan ke pemisah latar.
 *
 * Seribu lima ratus tiga puluh enam piksel: cukup untuk mempertahankan detail
 * bulu, tapi tidak membawa foto 12 MP utuh ke model. Modelnya sendiri bekerja di
 * resolusi jauh lebih kecil, jadi angka yang lebih besar cuma menambah memori
 * tanpa menambah ketajaman.
 */
private const val MAX_SEGMENT_SIDE = 1536

/**
 * Sisi stiker yang disimpan. Angkanya bukan pilihan gaya: 512 x 512 adalah
 * ukuran yang diminta WhatsApp untuk stiker, jadi membuat master yang lebih
 * besar cuma akan diperkecil lagi nanti.
 */
const val STICKER_PIXEL_SIZE = 512

/**
 * Bagian kanvas yang boleh dipakai cuking, sisanya jadi tepi transparan.
 *
 * Sembilan puluh persen: lima persen kosong di tiap sisi, cukup supaya cuking
 * tidak menempel di tepi stiker. Ruangnya diambil dari kanvas, bukan dari sisa
 * foto, karena foto close-up sering membuat cuking sudah menyentuh tepi gambar
 * dan bantalan di dalam foto akan habis dipotong tepi itu.
 */
private const val STICKER_CONTENT_FRACTION = 0.90f

/** Hasil satu kali pembuatan stiker. */
sealed interface StickerResult {
    data class Ready(val file: File) : StickerResult

    /** Tidak ada cuking yang bisa dipotong dari foto ini. */
    data object NothingDetected : StickerResult

    /** Gagal karena hal teknis: file tidak terbaca, model belum siap, dan sejenisnya. */
    data class Failed(val cause: Throwable? = null) : StickerResult
}

/**
 * Membuat stiker transparan dari satu foto.
 *
 * Urutannya: foto dibaca tegak sesuai EXIF, dikecilkan, dipotong latarnya,
 * dipangkas ke tepi cuking, lalu dipaskan ke kanvas persegi 512 px dengan tepi
 * transparan. Semua langkah berat dikerjakan di [Dispatchers.Default], bukan di
 * thread utama.
 */
class StickerMaker(private val segmenter: SubjectSegmenter) {

    suspend fun makeSticker(source: File, target: File): StickerResult =
        withContext(Dispatchers.Default) {
            val decoded = try {
                decodeUpright(source)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                return@withContext StickerResult.Failed(error)
            } ?: return@withContext StickerResult.Failed()

            var cutout: Bitmap? = null
            var crop: Bitmap? = null
            var sticker: Bitmap? = null
            try {
                cutout = segmenter.cutout(decoded) ?: return@withContext StickerResult.NothingDetected

                val pixels = IntArray(cutout.width * cutout.height)
                cutout.getPixels(pixels, 0, cutout.width, 0, 0, cutout.width, cutout.height)
                val bounds = alphaBounds(pixels, cutout.width, cutout.height)
                    ?: return@withContext StickerResult.NothingDetected

                crop = Bitmap.createBitmap(cutout, bounds.left, bounds.top, bounds.width, bounds.height)
                sticker = fitIntoSingleCanvas(crop)
                writePng(sticker, target)
                StickerResult.Ready(target)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                StickerResult.Failed(error)
            } finally {
                // Yang dipakai lebih dulu dilepas lebih dulu. `createBitmap` bisa
                // mengembalikan bitmap yang sama kalau tidak ada yang diubah,
                // jadi tiap bitmap diperiksa dulu sebelum didaur ulang.
                if (sticker != null && sticker !== crop) sticker.recycle()
                if (crop != null && crop !== cutout) crop.recycle()
                cutout?.recycle()
                decoded.recycle()
            }
        }

    /**
     * Foto dibaca dengan sampel yang cukup kecil, lalu diputar sesuai EXIF.
     *
     * Kamera menyimpan hasilnya dengan penanda orientasi, bukan dengan piksel
     * yang sudah diputar. Tanpa langkah ini stiker bisa keluar miring 90 derajat
     * walau di galeri fotonya terlihat tegak.
     */
    private fun decodeUpright(source: File): Bitmap? {
        val boundsOnly = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, boundsOnly)
        if (boundsOnly.outWidth <= 0 || boundsOnly.outHeight <= 0) return null

        var sampleSize = 1
        while (max(boundsOnly.outWidth, boundsOnly.outHeight) / sampleSize > MAX_SEGMENT_SIDE) {
            sampleSize *= 2
        }

        val bitmap = BitmapFactory.decodeFile(
            source.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sampleSize }
        ) ?: return null

        val upright = applyExifRotation(source, bitmap)
        val longestSide = max(upright.width, upright.height)
        if (longestSide <= MAX_SEGMENT_SIDE) return upright

        val scale = MAX_SEGMENT_SIDE.toFloat() / longestSide
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

    /**
     * Potongan diletakkan di tengah kanvas persegi, tanpa memotong apa pun.
     *
     * Yang dipaskan ke [STICKER_CONTENT_FRACTION] adalah potongannya, bukan
     * kanvasnya, jadi tepi transparannya selalu ada berapa pun rapatnya foto
     * aslinya.
     */
    private fun fitIntoSingleCanvas(crop: Bitmap): Bitmap {
        val canvasBitmap = Bitmap.createBitmap(
            STICKER_PIXEL_SIZE,
            STICKER_PIXEL_SIZE,
            Bitmap.Config.ARGB_8888
        )
        val maxContent = (STICKER_PIXEL_SIZE * STICKER_CONTENT_FRACTION).roundToInt()
        val scale = fitScale(crop.width, crop.height, maxContent)
        val targetWidth = (crop.width * scale).roundToInt().coerceAtMost(maxContent)
        val targetHeight = (crop.height * scale).roundToInt().coerceAtMost(maxContent)
        val left = (STICKER_PIXEL_SIZE - targetWidth) / 2
        val top = (STICKER_PIXEL_SIZE - targetHeight) / 2

        Canvas(canvasBitmap).drawBitmap(
            crop,
            null,
            Rect(left, top, left + targetWidth, top + targetHeight),
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        )
        return canvasBitmap
    }

    /**
     * PNG, bukan WebP: alpha-nya tanpa kompromi, dan batas 100 KB milik WhatsApp
     * baru relevan saat stiker diekspor ke paketnya nanti. Menyimpan master yang
     * sudah dimampatkan berarti kehilangan mutu dua kali.
     */
    private fun writePng(bitmap: Bitmap, target: File) {
        FileOutputStream(target).use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }
}

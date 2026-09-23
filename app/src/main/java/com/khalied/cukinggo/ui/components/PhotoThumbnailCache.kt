package com.khalied.cukinggo.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Thumbnail persegi kecil untuk marker peta. Decode dilakukan di IO thread dan
 * hasilnya di-cache supaya tidak berulang saat peta di-zoom.
 */
internal class PhotoThumbnailCache(private val targetSizePx: Int) {

    private val cache = mutableMapOf<String, Bitmap>()
    private val mutex = Mutex()

    suspend fun thumbnail(photoPath: String): Bitmap? {
        cache[photoPath]?.let { return it }
        return mutex.withLock {
            cache[photoPath]?.let { return@withLock it }
            val decoded = withContext(Dispatchers.IO) { decodeSquare(photoPath, targetSizePx) }
            decoded?.also { cache[photoPath] = it }
        }
    }

    private fun decodeSquare(photoPath: String, targetSizePx: Int): Bitmap? {
        val file = File(photoPath)
        if (!file.exists()) return null

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(photoPath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sampleSize = 1
        val shortestSide = min(bounds.outWidth, bounds.outHeight)
        while (shortestSide / (sampleSize * 2) >= targetSizePx * 2) {
            sampleSize *= 2
        }

        val decoded = BitmapFactory.decodeFile(
            photoPath,
            BitmapFactory.Options().apply { inSampleSize = sampleSize }
        ) ?: return null

        val side = min(decoded.width, decoded.height)
        val left = (decoded.width - side) / 2
        val top = (decoded.height - side) / 2
        val square = Bitmap.createBitmap(decoded, left, top, side, side)
        return Bitmap.createScaledBitmap(square, targetSizePx, targetSizePx, true)
    }
}

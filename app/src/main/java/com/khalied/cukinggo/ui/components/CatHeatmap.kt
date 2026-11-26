package com.khalied.cukinggo.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import com.khalied.cukinggo.domain.model.CatSighting
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.tan

/**
 * Satu lapisan panas siap tempel: bitmap plus kotak koordinat tempat ia dipasang.
 *
 * Kotaknya dipakai apa adanya oleh `GroundOverlay`, jadi gambarnya menempel di
 * bumi, bukan di layar: menggeser atau memperbesar peta tidak mengubah arti
 * warnanya.
 */
internal data class HeatLayer(
    val bitmap: Bitmap,
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double
)

/**
 * Mengubah daftar penemuan jadi gambar kepadatan.
 *
 * Warnanya dihitung di grid kecil (48x48) lalu diperbesar dengan penyaringan
 * bilinear, bukan digambar per titik: dua penemuan yang berdekatan harus
 * menyatu jadi satu bercak yang lebih pekat, bukan dua lingkaran yang saling
 * menimpa dengan tepi keras.
 *
 * Proyeksinya Mercator, sama seperti yang dipakai tile peta, supaya bercaknya
 * tidak melar ke arah utara-selatan di luar daerah tropis.
 */
internal object CatHeatmap {

    private const val GRID = 48
    private const val OUTPUT_PX = 512

    /**
     * Kepadatan yang dianggap "penuh".
     *
     * Dipatok sedikit di atas satu supaya satu penemuan sendirian tetap terlihat
     * sebagai bercak kecil, bukan langsung merah pekat seperti kerumunan.
     */
    private const val FULL_DENSITY = 2.4f

    /** Kernel penghalus 5 titik, dipisah mendatar dan menegak. */
    private val KERNEL = floatArrayOf(1f, 4f, 6f, 4f, 1f)
    private const val KERNEL_SUM = 16f

    fun render(sightings: List<CatSighting>): HeatLayer? {
        if (sightings.isEmpty()) return null

        val latitudes = sightings.map { sighting -> sighting.latitude }
        val longitudes = sightings.map { sighting -> sighting.longitude }
        var south = latitudes.min()
        var north = latitudes.max()
        var west = longitudes.min()
        var east = longitudes.max()

        // Padding 15% ditambah lantai 0,004 derajat: satu catatan saja tidak boleh
        // menghasilkan kotak nol derajat, karena overlay sekecil itu tidak terlihat.
        val latPad = max((north - south) * 0.15, 0.004)
        val lngPad = max((east - west) * 0.15, 0.004)
        south -= latPad
        north += latPad
        west -= lngPad
        east += lngPad

        val northY = mercator(north)
        val southY = mercator(south)
        val spanY = northY - southY
        val spanX = east - west
        if (spanX <= 0.0 || spanY <= 0.0) return null

        val cells = FloatArray(GRID * GRID)
        sightings.forEach { sighting ->
            val x = (((sighting.longitude - west) / spanX) * (GRID - 1))
                .roundToInt()
                .coerceIn(0, GRID - 1)
            val y = (((northY - mercator(sighting.latitude)) / spanY) * (GRID - 1))
                .roundToInt()
                .coerceIn(0, GRID - 1)
            cells[y * GRID + x] += 1f
        }

        val density = blur(blur(cells))
        val peak = max(density.max(), FULL_DENSITY)
        val pixels = IntArray(GRID * GRID) { index ->
            colorFor((density[index] / peak).coerceIn(0f, 1f))
        }

        val small = Bitmap.createBitmap(GRID, GRID, Bitmap.Config.ARGB_8888)
        small.setPixels(pixels, 0, GRID, 0, 0, GRID, GRID)
        val output = Bitmap.createBitmap(OUTPUT_PX, OUTPUT_PX, Bitmap.Config.ARGB_8888)
        Canvas(output).drawBitmap(
            small,
            null,
            Rect(0, 0, OUTPUT_PX, OUTPUT_PX),
            Paint(Paint.FILTER_BITMAP_FLAG)
        )
        small.recycle()

        return HeatLayer(output, south, west, north, east)
    }

    private fun mercator(latitude: Double): Double {
        val clamped = latitude.coerceIn(-85.0, 85.0)
        return ln(tan(PI / 4 + Math.toRadians(clamped) / 2))
    }

    private fun blur(source: FloatArray): FloatArray {
        val horizontal = FloatArray(source.size)
        for (y in 0 until GRID) {
            for (x in 0 until GRID) {
                var sum = 0f
                for (k in -2..2) {
                    val sx = (x + k).coerceIn(0, GRID - 1)
                    sum += source[y * GRID + sx] * KERNEL[k + 2]
                }
                horizontal[y * GRID + x] = sum / KERNEL_SUM
            }
        }
        val vertical = FloatArray(source.size)
        for (y in 0 until GRID) {
            for (x in 0 until GRID) {
                var sum = 0f
                for (k in -2..2) {
                    val sy = (y + k).coerceIn(0, GRID - 1)
                    sum += horizontal[sy * GRID + x] * KERNEL[k + 2]
                }
                vertical[y * GRID + x] = sum / KERNEL_SUM
            }
        }
        return vertical
    }

    /**
     * Kuning ke oranye ke merah, dengan alfa naik mengikuti kepadatan.
     *
     * Ambang bawah 0,05 sengaja ada: sel yang nyaris kosong tidak boleh
     * meninggalkan kabut tipis di seluruh kotak overlay, karena kabut itu terbaca
     * seperti peta yang keruh, bukan seperti sebaran.
     */
    private fun colorFor(intensity: Float): Int {
        val t = ((intensity - 0.05f) / 0.95f).coerceIn(0f, 1f)
        val alpha = (t * 200f).toInt()
        if (alpha == 0) return 0

        val red: Int
        val green: Int
        val blue: Int
        if (t < 0.5f) {
            val f = t / 0.5f
            red = 255
            green = lerp(214, 143, f)
            blue = lerp(59, 64, f)
        } else {
            val f = (t - 0.5f) / 0.5f
            red = lerp(255, 214, f)
            green = lerp(143, 48, f)
            blue = lerp(64, 49, f)
        }
        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }

    private fun lerp(from: Int, to: Int, fraction: Float): Int =
        (from + (to - from) * fraction).toInt().coerceIn(0, 255)
}

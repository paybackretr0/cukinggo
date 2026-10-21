package com.khalied.cukinggo.sticker

import kotlin.math.min

/**
 * Piksel di bawah nilai alpha ini dianggap latar, bukan cuking.
 *
 * Dipakai sebagai ambang saat mencari tepi potongan. Enam belas dari 255 cukup
 * rendah untuk mempertahankan bulu yang tipis, tapi masih membuang sisa latar
 * yang nyaris tembus pandang.
 */
const val ALPHA_CUTOFF = 16

/**
 * Kotak piksel, dengan tepi kanan dan bawah eksklusif.
 *
 * Bentuknya sengaja bukan `Rect` Android supaya perhitungan tepi potongan bisa
 * diuji tanpa device, dan supaya file ini tidak menyeret `android.graphics` ke
 * unit test.
 */
data class StickerBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    init {
        require(right >= left) { "Tepi kanan tidak boleh di kiri tepi kiri" }
        require(bottom >= top) { "Tepi bawah tidak boleh di atas tepi atas" }
    }

    val width: Int get() = right - left

    val height: Int get() = bottom - top
}

/**
 * Kotak terkecil yang memuat semua piksel dengan alpha minimal [minAlpha].
 *
 * Null kalau tidak ada satu pun piksel yang lolos, dan itu artinya potongannya
 * kosong: lebih jujur dilaporkan gagal daripada menyimpan kotak transparan.
 */
fun alphaBounds(
    argb: IntArray,
    width: Int,
    height: Int,
    minAlpha: Int = ALPHA_CUTOFF
): StickerBounds? {
    require(width > 0 && height > 0) { "Ukuran gambar harus positif" }
    require(argb.size >= width * height) { "Panjang piksel kurang dari ukuran gambar" }

    var left = width
    var top = height
    var right = -1
    var bottom = -1

    for (y in 0 until height) {
        val rowStart = y * width
        for (x in 0 until width) {
            val alpha = argb[rowStart + x] ushr 24
            if (alpha < minAlpha) continue
            if (x < left) left = x
            if (x > right) right = x
            if (y < top) top = y
            if (y > bottom) bottom = y
        }
    }

    if (right < left || bottom < top) return null
    return StickerBounds(left = left, top = top, right = right + 1, bottom = bottom + 1)
}

/**
 * Skala supaya gambar [sourceWidth] x [sourceHeight] muat seluruhnya di kanvas
 * persegi [targetSize].
 *
 * Yang dipakai sisi yang paling sempit, jadi tidak ada bagian gambar yang
 * terpotong. Gambar kecil memang ikut diperbesar, karena potongan cuking sering
 * cuma sepertiga frame dan stiker 512 px harus tetap terisi.
 */
fun fitScale(sourceWidth: Int, sourceHeight: Int, targetSize: Int): Float {
    require(sourceWidth > 0 && sourceHeight > 0) { "Ukuran sumber harus positif" }
    require(targetSize > 0) { "Ukuran kanvas harus positif" }
    return min(
        targetSize.toFloat() / sourceWidth,
        targetSize.toFloat() / sourceHeight
    )
}

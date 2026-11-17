package com.khalied.cukinggo.detect

import com.google.mlkit.vision.common.InputImage
import com.khalied.cukinggo.util.decodeUprightPhoto
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sisi terpanjang foto yang diserahkan ke pemeriksa.
 *
 * Seribu dua puluh empat piksel: model label bekerja di resolusi yang jauh lebih
 * kecil, jadi foto 12 MP utuh cuma menambah memori tanpa menambah jawaban. Angka
 * ini lebih kecil dari batas pemisah latar stiker karena yang dicari di sini
 * cuma bentuk besar cuking, bukan detail bulu.
 */
const val MAX_CHECK_SIDE = 1024

/**
 * Keadaan pemeriksaan satu foto.
 *
 * [Idle] dan [Checking] bukan hasil model, tapi tetap tinggal di sini supaya
 * layar dan ViewModel memakai satu tipe saja, dan aturan "boleh disimpan atau
 * tidak" cuma ditulis sekali di [blocksSave].
 */
sealed interface PhotoCheck {
    /** Belum ada foto yang perlu diperiksa. */
    data object Idle : PhotoCheck

    /** Foto sedang dibaca model. */
    data object Checking : PhotoCheck

    /** Model menemukan cuking dengan keyakinan di atas ambang. */
    data object HasCat : PhotoCheck

    /** Model tidak menemukan cuking. Ini satu-satunya keadaan yang menahan simpan. */
    data object NoCat : PhotoCheck

    /**
     * Pemeriksaan tidak bisa diselesaikan: file tidak terbaca, model belum siap,
     * atau perangkatnya menolak. Bukan kesalahan pengguna, jadi tidak menahan.
     */
    data class Failed(val cause: Throwable? = null) : PhotoCheck
}

/**
 * Apakah keadaan ini menahan tombol Simpan.
 *
 * [PhotoCheck.NoCat] menahan karena itulah permintaan pemilik: catatan cuking
 * harus berisi cuking. [PhotoCheck.Checking] juga menahan supaya tombolnya tidak
 * bisa ditekan sebelum jawabannya ada, bukan karena fotonya sudah terbukti salah.
 *
 * [PhotoCheck.Failed], [PhotoCheck.Idle], dan [PhotoCheck.HasCat] tidak menahan.
 * Model yang gagal bukan bukti foto ini tidak berisi cuking, dan memblokir simpan
 * di situ berarti satu bug Play Services membuat orang kehilangan catatannya.
 */
fun PhotoCheck.blocksSave(): Boolean = when (this) {
    PhotoCheck.NoCat, PhotoCheck.Checking -> true
    PhotoCheck.Idle, PhotoCheck.HasCat, is PhotoCheck.Failed -> false
}

/**
 * Memeriksa foto hasil jepretan, bukan frame pratinjau.
 *
 * Sinyal pratinjau datang dari aliran yang berbeda: jeda 200 ms antar frame,
 * histeresis dua frame untuk menyala, dan frame yang datang saat model sibuk
 * dilewati. Foto yang disimpan tidak melewati jalur itu, jadi jawabannya harus
 * dicari ulang dari file yang benar-benar akan disimpan.
 */
class PhotoCatChecker(private val detector: CatDetector) {

    suspend fun check(file: File): PhotoCheck = withContext(Dispatchers.Default) {
        val bitmap = decodeUprightPhoto(file, MAX_CHECK_SIDE)
            ?: return@withContext PhotoCheck.Failed()

        try {
            val confidence = detector.confidence(InputImage.fromBitmap(bitmap, 0))
                ?: return@withContext PhotoCheck.Failed()

            if (confidence >= MIN_CAT_CONFIDENCE) PhotoCheck.HasCat else PhotoCheck.NoCat
        } catch (error: Throwable) {
            PhotoCheck.Failed(error)
        } finally {
            // Bitmap-nya besar dan cuma dipakai sekali, jadi dilepas di sini dan
            // bukan menunggu pengumpul sampah.
            bitmap.recycle()
        }
    }
}

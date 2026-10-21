package com.khalied.cukinggo.sticker

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
// Nama antarmuka ML Kit sama dengan antarmuka app ini, jadi yang diimpor diberi
// nama lain. Tanpa itu kelas di bawah mengira dirinya turunan tipe ML Kit.
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter as MlKitSubjectSegmenterClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Pemotong latar: dari foto utuh jadi gambar cuking dengan latar transparan.
 *
 * Antarmukanya dipisah supaya pengujian dan penggantian model tidak menyentuh
 * alur penyimpanan. Yang dipakai sekarang ML Kit; kalau nanti model yang
 * dibundel dipakai sebagai cadangan, ia masuk lewat pintu yang sama.
 */
interface SubjectSegmenter {

    /**
     * Null kalau tidak ada subjek yang bisa dipotong. Itu bukan kegagalan
     * teknis, tapi jawaban yang sah: fotonya mungkin kosong atau cukingnya
     * terlalu kecil.
     */
    suspend fun cutout(bitmap: Bitmap): Bitmap?

    fun close()
}

/**
 * Pemisah latar dari ML Kit.
 *
 * Modelnya tidak ikut di APK, tapi diunduh lewat Play Services. Itu yang membuat
 * tambahan ukuran app-nya kecil, dan itu juga sebabnya permintaan pertama bisa
 * belum menghasilkan apa-apa selagi modelnya disiapkan.
 */
class MlKitSubjectSegmenter : SubjectSegmenter {

    private val segmenter: MlKitSubjectSegmenterClient = SubjectSegmentation.getClient(
        SubjectSegmenterOptions.Builder()
            // Bitmap hasil potong sudah membawa alpha, jadi tidak perlu menyusun
            // topeng sendiri. Yang diminta cuma bentuk akhirnya.
            .enableForegroundBitmap()
            .build()
    )

    override suspend fun cutout(bitmap: Bitmap): Bitmap? = suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        segmenter.process(image)
            .addOnSuccessListener { result ->
                if (cont.isActive) cont.resume(result.foregroundBitmap)
            }
            .addOnFailureListener { error ->
                if (cont.isActive) cont.resumeWithException(error)
            }
    }

    override fun close() {
        segmenter.close()
    }
}

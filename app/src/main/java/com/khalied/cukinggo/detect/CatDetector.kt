package com.khalied.cukinggo.detect

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Ambang keyakinan label cuking.
 *
 * Empat puluh persen: cukup longgar untuk cuking yang setengah terhalang, tapi
 * label "Cat" yang muncul dari foto poster atau gambar di layar tetap harus
 * cukup yakin. Angka ini juga dipakai sebagai ambang model, jadi label yang
 * lebih rendah dari ini tidak pernah sampai ke [catConfidence].
 */
const val MIN_CAT_CONFIDENCE = 0.4f

/**
 * Pemberi label bawaan ML Kit yang dibungkus jadi satu pertanyaan: ada cuking
 * atau tidak di frame ini.
 *
 * Modelnya ikut di APK, jadi deteksi tetap jalan tanpa internet dan tanpa
 * menunggu unduhan. Satu instance dipakai ulang selama layar kamera terbuka;
 * membuat klien baru per frame berarti memuat model berkali-kali.
 *
 * Frame yang datang saat frame sebelumnya belum selesai dilewati, bukan
 * diantrekan. Kalau diantrekan, hasilnya justru makin tertinggal dari kenyataan.
 */
class CatDetector : Closeable {

    private val labeler: ImageLabeler = ImageLabeling.getClient(
        ImageLabelerOptions.Builder()
            .setConfidenceThreshold(MIN_CAT_CONFIDENCE)
            .build()
    )

    private val busy = AtomicBoolean(false)

    /**
     * Menganalisis satu frame.
     *
     * Mengembalikan false kalau frame ini dilewati karena masih ada frame lain
     * yang diproses; pemanggil yang menutup frame itu. Kalau true, [onResult]
     * yang akan dipanggil, dan pemanggil menutup framenya di dalam callback itu.
     */
    fun analyze(image: InputImage, onResult: (Boolean) -> Unit): Boolean {
        if (!busy.compareAndSet(false, true)) return false

        try {
            labeler.process(image)
                .addOnSuccessListener { labels ->
                    val confidence = catConfidence(
                        labels.map { CatLabel(text = it.text, confidence = it.confidence) }
                    )
                    onResult(confidence >= MIN_CAT_CONFIDENCE)
                }
                .addOnFailureListener { onResult(false) }
                .addOnCompleteListener { busy.set(false) }
        } catch (error: Throwable) {
            // Kalau pemrosesan gagal sebelum Task-nya terbentuk, callback tidak
            // akan pernah jalan. Frame-nya tetap harus ditutup, dan isyaratnya
            // diperlakukan sebagai "tidak terlihat", bukan dibiarkan macet.
            busy.set(false)
            onResult(false)
        }
        return true
    }

    /**
     * Membaca satu gambar sekali jalan, lalu berhenti.
     *
     * Dipakai untuk memeriksa foto yang sudah dijepret, bukan frame pratinjau.
     * Beda dengan [analyze], di sini tidak ada frame yang boleh dilewati: satu
     * foto berarti satu jawaban, jadi permintaannya selalu dijalankan.
     *
     * `null` berarti model gagal menjawab. Itu sengaja dipisah dari skor rendah,
     * supaya pemanggil tidak menghukum pengguna karena perangkatnya yang gagal.
     */
    suspend fun confidence(image: InputImage): Float? =
        suspendCancellableCoroutine { continuation ->
            try {
                labeler.process(image)
                    .addOnSuccessListener { labels ->
                        if (continuation.isActive) {
                            continuation.resume(
                                catConfidence(
                                    labels.map {
                                        CatLabel(text = it.text, confidence = it.confidence)
                                    }
                                )
                            )
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }
            } catch (error: Throwable) {
                // Klien yang sudah ditutup melempar dari sini, bukan lewat
                // listener. Task-nya tidak pernah terbentuk, jadi callback tidak
                // akan datang dan tanpa cabang ini penantiannya menggantung.
                if (continuation.isActive) continuation.resume(null)
            }
        }

    override fun close() {
        labeler.close()
    }
}

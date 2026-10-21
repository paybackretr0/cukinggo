package com.khalied.cukinggo.detect

import android.os.SystemClock
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage

/**
 * Jarak minimum antar frame yang dianalisis.
 *
 * Dua ratus milidetik berarti paling banyak lima kali per detik. Deteksi ini
 * cuma isyarat, dan pratinjau kamera tetap harus mulus; menganalisis tiap frame
 * hanya menambah panas dan baterai tanpa membuat isyaratnya terasa lebih cepat.
 *
 * Diukur di HP uji dengan pengaturan ini: 44 analisis dalam 9,5 detik (sekitar
 * 4,6 per detik) dari 206 frame yang datang, dan pratinjau tidak tersendat.
 */
private const val MIN_ANALYSIS_INTERVAL_MILLIS = 200L

/**
 * Menyambungkan frame kamera ke [CatDetector], lalu ke [CatPresence].
 *
 * Pemilikannya penting di sini: `ImageProxy` hanya boleh ditutup setelah ML Kit
 * selesai membacanya. Karena itu frame ditutup di dalam callback, bukan tepat
 * setelah `process` dipanggil. Frame yang dilewati ditutup segera, karena tidak
 * ada yang akan membacanya.
 */
class CatFrameAnalyzer(
    private val detector: CatDetector,
    private val presence: CatPresence,
    private val onVisibilityChanged: (Boolean) -> Unit
) : ImageAnalysis.Analyzer {

    private var lastSubmittedAt = 0L

    // `imageProxy.image` masih ditandai eksperimental oleh CameraX, padahal
    // jalan satu-satunya untuk memberi MediaImage ke ML Kit. Anotasi opt-in-nya
    // dipakai versi `androidx.annotation`, bukan `kotlin.OptIn`: yang kedua
    // dibuang saat compile sehingga lint tetap membacanya sebagai pelanggaran.
    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        val now = SystemClock.elapsedRealtime()
        if (mediaImage == null || now - lastSubmittedAt < MIN_ANALYSIS_INTERVAL_MILLIS) {
            imageProxy.close()
            return
        }
        lastSubmittedAt = now

        val image = runCatching {
            InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        }.getOrNull()
        if (image == null) {
            imageProxy.close()
            return
        }

        val accepted = detector.analyze(image) { found ->
            onVisibilityChanged(presence.onResult(found))
            imageProxy.close()
        }
        if (!accepted) imageProxy.close()
    }
}

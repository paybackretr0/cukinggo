package com.khalied.cukinggo.ui.components

import androidx.camera.view.LifecycleCameraController
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.khalied.cukinggo.detect.CatDetector
import com.khalied.cukinggo.detect.CatFrameAnalyzer
import com.khalied.cukinggo.detect.CatPresence
import java.util.concurrent.Executors

/**
 * Menyalakan deteksi cuking selama [enabled], lalu mengembalikan isyarat
 * "cuking terlihat" untuk overlay kamera.
 *
 * Deteksinya dipasang sebagai analyzer CameraX, bukan loop sendiri: frame yang
 * dipakai adalah frame yang memang sudah mengalir untuk kamera, jadi tidak ada
 * pembacaan kamera kedua yang berebut dengan pratinjau.
 *
 * Analyzer dilepas begitu [enabled] jadi false, misalnya saat pengguna sedang
 * mereview foto. Kamera tetap hidup untuk pratinjau, tapi tidak ada frame yang
 * dianalisis, jadi tidak ada baterai yang terbuang untuk sesuatu yang tidak
 * dilihat siapa pun.
 */
@Composable
fun rememberCatDetection(
    controller: LifecycleCameraController,
    enabled: Boolean
): State<Boolean> {
    val visible = remember { mutableStateOf(false) }
    // Model dimuat saat frame pertama dibutuhkan, bukan saat layar dibuka: layar
    // ganti foto juga memakai komponen ini, dan di sana kamera belum tentu
    // dipanggil sama sekali.
    val detector = remember { lazy(LazyThreadSafetyMode.NONE) { CatDetector() } }
    val presence = remember { CatPresence() }
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(controller, enabled) {
        if (enabled) {
            controller.setImageAnalysisAnalyzer(
                executor,
                CatFrameAnalyzer(
                    detector = detector.value,
                    presence = presence,
                    onVisibilityChanged = { visible.value = it }
                )
            )
        }
        onDispose {
            controller.clearImageAnalysisAnalyzer()
            presence.reset()
            visible.value = false
        }
    }

    // Ditutup saat layar ini benar-benar ditinggalkan, bukan saat deteksi
    // dimatikan sementara: kembali dari review foto tidak perlu memuat model
    // dari awal lagi.
    DisposableEffect(Unit) {
        onDispose {
            controller.clearImageAnalysisAnalyzer()
            if (detector.isInitialized()) detector.value.close()
            executor.shutdown()
        }
    }

    return visible
}

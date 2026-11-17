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
 *
 * [detector] dipinjam dari `AppContainer`, bukan dibuat di sini. Layar yang sama
 * juga memakainya untuk memeriksa foto hasil jepretan, dan model label tidak
 * boleh dimuat dua kali hanya karena dua pemanggilnya berbeda tempat.
 */
@Composable
fun rememberCatDetection(
    controller: LifecycleCameraController,
    enabled: Boolean,
    detector: CatDetector
): State<Boolean> {
    val visible = remember { mutableStateOf(false) }
    val presence = remember { CatPresence() }
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(controller, enabled) {
        if (enabled) {
            controller.setImageAnalysisAnalyzer(
                executor,
                CatFrameAnalyzer(
                    detector = detector,
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

    // Executor-nya milik layar ini, jadi tetap ditutup saat layar benar-benar
    // ditinggalkan. Detektornya tidak ikut ditutup karena bukan milik layar.
    DisposableEffect(Unit) {
        onDispose {
            controller.clearImageAnalysisAnalyzer()
            executor.shutdown()
        }
    }

    return visible
}

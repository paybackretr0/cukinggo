package com.khalied.cukinggo.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * Tandai konten yang ada di belakang sebagai sumber blur.
 *
 * Dipasang di wadah latar (biasanya root layar), bukan di permukaan kacanya:
 * yang diblur adalah isi layar di belakang kaca, jadi sumbernya harus ditandai
 * lebih dulu supaya Haze tahu apa yang perlu dibaca.
 */
fun Modifier.frostBackdrop(state: HazeState): Modifier = hazeSource(state = state)

/**
 * Satu permukaan kaca: transparansi + blur latar + border tipis.
 *
 * Digabung jadi satu modifier supaya semua permukaan di app memakai resep yang
 * sama, dan angkanya cuma diubah di satu tempat. [state] null berarti permukaan
 * ini tidak punya sumber blur (misalnya muncul di luar layar ber-latar Frost);
 * kaca tetap tampil sebagai bidang tembus pandang tanpa blur, bukan gagal.
 *
 * Border-nya tipis dan hampir putih: yang menegaskan tepinya adalah perbedaan
 * opacity, bukan garis gelap, jadi permukaannya tetap terasa ringan.
 *
 * Urutannya penting: `clip` dipasang sebelum `hazeEffect`. Lapisan blur Haze
 * digambar oleh node-nya sendiri, jadi kalau `clip` ada di dalamnya, blur tetap
 * keluar sebagai kotak di sudut-sudut yang seharusnya sudah terpotong.
 */
@OptIn(dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi::class)
@Composable
fun Modifier.frostSurface(
    state: HazeState? = null,
    shape: Shape = MaterialTheme.shapes.large,
    tint: Color = GlassSurface,
    opacity: Float = 0.66f,
    borderAlpha: Float = 0.28f
): Modifier {
    val blurred = if (state != null) {
        Modifier.hazeEffect(state = state, style = HazeMaterials.ultraThin())
    } else {
        Modifier
    }
    return this
        .clip(shape)
        .then(blurred)
        .background(tint.copy(alpha = opacity))
        .border(1.dp, FrostBorder.copy(alpha = borderAlpha), shape)
}

package com.khalied.cukinggo.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape

/**
 * Cakupan animasi bersama untuk satu layar di dalam NavHost.
 *
 * Disimpan sebagai composition local, bukan diteruskan sebagai parameter ke tiap
 * layar: yang dipakai layar cuma satu modifier ([sharedCatPhoto]), sedangkan
 * meneruskan dua cakupan animasi berarti dua janji tambahan yang harus ikut dijaga
 * di setiap tanda tangan fungsi layar. Nilai null berarti layar itu tidak sedang
 * berada di dalam NavHost yang menyediakannya, dan [sharedCatPhoto] cukup tidak
 * melakukan apa-apa.
 *
 * Dipakai `compositionLocalOf`, bukan `staticCompositionLocalOf`: nilainya berganti
 * tiap kali tujuan navigasinya berganti, sedangkan pembacanya cuma elemen foto.
 * Versi statis akan menyusun ulang seluruh isi layar (termasuk peta) setiap kali
 * cakupannya berganti.
 */
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Lama foto terbang dari kartu ke layar detail.
 *
 * Sedikit lebih lama dari geseran layarnya, karena yang dilihat di sini
 * perpindahan satu benda, bukan pergantian halaman.
 */
private const val SHARED_PHOTO_MILLIS = 320

/**
 * Foto satu penemuan yang menyambung antara kartu di daftar dan fotonya di layar
 * detail: yang berpindah cuma satu benda, jadi mata tidak perlu mencari lagi
 * penemuan mana yang barusan dibuka.
 *
 * Kuncinya id penemuan, bukan id cuking, karena kartu di daftar mewakili satu
 * penemuan. Cuking yang punya beberapa penemuan punya beberapa kartu, dan yang
 * menyambung adalah foto kartu yang benar-benar disentuh.
 *
 * Dipasang menggantikan `clip` yang sebelumnya ada di posisi itu, supaya bentuk
 * membulatnya tetap ikut saat fotonya terbang: potongan yang dipasang di luar
 * elemen bersama tidak berlaku di perjalanannya, dan yang terlihat justru foto
 * bersudut tegak.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedCatPhoto(sightingId: Long, clip: Shape): Modifier {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    val shared = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(key = "foto-penemuan-$sightingId"),
                animatedVisibilityScope = animatedVisibilityScope,
                boundsTransform = { _, _ ->
                    tween(durationMillis = SHARED_PHOTO_MILLIS, easing = FastOutSlowInEasing)
                }
            )
        }
    } else {
        Modifier
    }

    return this.then(shared).clip(clip)
}

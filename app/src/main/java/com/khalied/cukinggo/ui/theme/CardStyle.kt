package com.khalied.cukinggo.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Garis tepi kartu dan panel.
 *
 * Tiga dp, bukan satu: di arah ini garis inilah yang menggantikan bayangan blur
 * sebagai penanda batas permukaan, jadi ia harus terbaca dari jauh.
 */
@Composable
fun appCardOutline(): BorderStroke =
    BorderStroke(3.dp, MaterialTheme.colorScheme.outline)

/**
 * Garis tepi chip dan lencana.
 *
 * Dua dp, bukan tiga: bidangnya kecil, dan garis 3dp di kotak setinggi 32dp
 * membuat isinya terasa sesak.
 */
@Composable
fun appChipOutline(): BorderStroke =
    BorderStroke(2.dp, MaterialTheme.colorScheme.outline)

/**
 * Bayangan keras: siluet solid yang digeser ke kanan bawah, tanpa blur.
 *
 * Digambar sendiri, bukan `Modifier.shadow()`: modifier bawaan selalu memberi
 * blur, sedangkan di arah ini bayangan adalah bidang datar yang tepinya harus
 * setajam garis di atasnya. Karena digambar di `drawBehind`, ia tidak menambah
 * ukuran layout dan tidak butuh elevation.
 *
 * Dipasang **sebelum** background, supaya siluetnya berada di belakang isian.
 */
@Composable
fun Modifier.brutalShadow(
    offset: Dp = 4.dp,
    shape: Shape = MaterialTheme.shapes.medium,
    color: Color = MaterialTheme.colorScheme.outline
): Modifier = drawBehind {
    // Siluet bentuk permukaannya jadi satu Path, lalu digambar sekali dengan
    // geseran. Cara ini benar untuk kotak maupun bentuk lain, tanpa harus
    // menangani tiap jenis Outline satu per satu.
    val silhouette = Path().apply {
        addOutline(shape.createOutline(size, layoutDirection, this@drawBehind))
    }
    translate(left = offset.toPx(), top = offset.toPx()) {
        drawPath(silhouette, color)
    }
}


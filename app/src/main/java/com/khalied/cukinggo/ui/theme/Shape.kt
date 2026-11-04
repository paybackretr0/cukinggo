package com.khalied.cukinggo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Semua sudut 0dp.
 *
 * Arah NeoBrutalism memakai sudut tajam sebagai bahasa bentuk. Satu-satunya
 * lengkung yang tersisa adalah lingkaran foto dan tombol jepret, dan itu dibuat
 * langsung di komponennya. Kalau di sini masih ada 12-24dp, tiap komponen baru
 * akan diam-diam kembali membulat dan identitasnya jadi setengah jadi.
 */
private val Sharp = RoundedCornerShape(0.dp)

val AppShapes = Shapes(
    extraSmall = Sharp,
    small = Sharp,
    medium = Sharp,
    large = Sharp,
    extraLarge = Sharp
)

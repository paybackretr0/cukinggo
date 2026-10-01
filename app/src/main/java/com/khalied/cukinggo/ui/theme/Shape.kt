package com.khalied.cukinggo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Sudut medium-large untuk permukaan kaca: cukup membulat supaya tepinya lembut,
 * tapi tidak sampai pill. Frost UI meminta 16-24dp, jadi langit-langitnya 24dp.
 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

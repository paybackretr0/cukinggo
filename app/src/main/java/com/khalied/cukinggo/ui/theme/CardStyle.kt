package com.khalied.cukinggo.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Garis tipis untuk kartu yang duduk langsung di atas background halaman.
 *
 * Alasan: shadow dikhususkan untuk elemen yang benar-benar melayang di atas
 * permukaan lain (peta, pill pesan di atas peta, FAB). Kartu biasa cukup
 * dibatasi garis tipis supaya tepinya tetap jelas tanpa ikut terangkat,
 * jadi shadow tetap jadi penanda hierarki, bukan dekorasi di semua tempat.
 */
@Composable
fun appCardOutline(): BorderStroke =
    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))

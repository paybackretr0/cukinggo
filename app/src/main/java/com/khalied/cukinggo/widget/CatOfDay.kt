package com.khalied.cukinggo.widget

import com.khalied.cukinggo.domain.model.CatSighting

/**
 * Memilih "cuking hari ini" dari seluruh koleksi.
 *
 * Urutannya berjalan satu langkah per hari, bukan acak. Alasannya: dengan
 * langkah harian, setiap catatan kebagian tampil satu kali dalam satu putaran,
 * dan tidak ada catatan yang muncul dua hari berturut-turut (selama koleksinya
 * lebih dari satu). Kalau acak, catatan yang sama bisa muncul tiga hari beruntun,
 * dan foto di layar utama terasa seperti tidak pernah berganti.
 *
 * [epochDay] adalah jumlah hari sejak 1970 menurut zona waktu perangkat, jadi
 * pergantian catatannya ikut tengah malam waktu setempat.
 */
fun catOfDay(sightings: List<CatSighting>, epochDay: Long): CatSighting? {
    if (sightings.isEmpty()) return null
    // floorMod, bukan %, supaya tanggal sebelum 1970 tetap jatuh di rentang yang sah.
    val index = Math.floorMod(epochDay, sightings.size.toLong()).toInt()
    return sightings[index]
}

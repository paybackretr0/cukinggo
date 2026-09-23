package com.khalied.cukinggo.widget

import com.khalied.cukinggo.domain.model.Cat

/**
 * Memilih "kucing hari ini" dari seluruh koleksi.
 *
 * Urutannya berjalan satu langkah per hari, bukan acak. Alasannya: dengan
 * langkah harian, setiap kucing kebagian tampil satu kali dalam satu putaran,
 * dan tidak ada kucing yang muncul dua hari berturut-turut (selama koleksinya
 * lebih dari satu). Kalau acak, kucing yang sama bisa muncul tiga hari beruntun,
 * dan foto di layar utama terasa seperti tidak pernah berganti.
 *
 * [epochDay] adalah jumlah hari sejak 1970 menurut zona waktu perangkat, jadi
 * pergantian kucingnya ikut tengah malam waktu setempat.
 */
fun catOfDay(cats: List<Cat>, epochDay: Long): Cat? {
    if (cats.isEmpty()) return null
    // floorMod, bukan %, supaya tanggal sebelum 1970 tetap jatuh di rentang yang sah.
    val index = Math.floorMod(epochDay, cats.size.toLong()).toInt()
    return cats[index]
}

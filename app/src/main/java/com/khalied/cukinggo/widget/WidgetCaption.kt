package com.khalied.cukinggo.widget

/**
 * Teks yang tampil di bawah foto pada widget "Kucing terakhir".
 *
 * Aturan sederhana dan sengaja tidak pintar-pintar: kalau pengguna menulis
 * catatan, catatannya yang tampil apa adanya. Kalau belum ada catatan, widget
 * memakai satu baris lucu dari [funnyLines].
 *
 * Baris lucunya dipilih dari id kucing, bukan acak, supaya kucing yang sama
 * selalu mendapat baris yang sama. Kalau acak, teks di layar utama akan berubah
 * setiap kali widget di-refresh, dan itu justru terasa seperti iklan berkedip,
 * bukan seperti catatan.
 */
fun widgetCaption(description: String?, catId: Long, funnyLines: List<String>): String {
    val note = description?.trim().orEmpty()
    if (note.isNotEmpty()) return note
    if (funnyLines.isEmpty()) return ""
    // floorMod, bukan %, supaya id 0 atau id aneh tetap jatuh di rentang yang sah.
    return funnyLines[Math.floorMod(catId, funnyLines.size.toLong()).toInt()]
}

package com.khalied.cukinggo.whatsapp

/**
 * Aturan stiker mana yang ikut ke paket WhatsApp.
 *
 * Terpisah dari layar dan dari penyimpanan supaya bisa diuji tanpa Android:
 * yang diurus di sini cuma himpunan path, bukan tampilan atau file.
 *
 * Pilihan pengguna disimpan sebagai path file, bukan nomor urut: nomor urut
 * berubah begitu ada stiker baru, sedangkan path tetap menunjuk stiker yang
 * sama.
 */
object StickerPackSelection {

    /**
     * Pilihan awal saat pengguna belum pernah memilih apa pun.
     *
     * Yang terbaru lebih dulu, dipotong ke batas WhatsApp. Ini juga yang dulu
     * selalu dipakai sebelum pilihannya bisa diatur.
     */
    fun defaultSelection(available: List<String>): List<String> =
        StickerPackRules.takeForPack(available)

    /**
     * Hasil mengetuk satu stiker.
     *
     * Mengetuk yang sudah dipilih mengeluarkannya, walau jumlahnya jadi di bawah
     * tiga: menyisakan tiga stiker yang tidak diinginkan dan menolak
     * mengeluarkannya justru membuat pilihan ini tidak berguna. Pemberitahuan
     * "butuh 3 stiker" yang mengingatkan.
     *
     * Mengetuk yang belum dipilih tidak melakukan apa-apa kalau batas 30 sudah
     * tercapai, supaya angka di layar tidak pernah melewati batas WhatsApp.
     */
    fun toggle(current: Set<String>, path: String): Set<String> = when {
        path in current -> current - path
        current.size >= StickerPackRules.MAX_STICKERS -> current
        else -> current + path
    }

    /**
     * Stiker terpilih menurut urutan tampil (terbaru dulu).
     *
     * Path yang tidak ada lagi di koleksi otomatis hilang, jadi stiker yang
     * catatannya dihapus tidak bisa tertinggal di dalam paket.
     */
    fun ordered(selected: Set<String>, available: List<String>): List<String> =
        available.filter { path -> path in selected }

    /**
     * Pilihan yang dipakai saat daftar stiker baru selesai dimuat.
     *
     * Tiga keadaan dibedakan: belum pernah memilih (pakai bawaan), sengaja
     * mengosongkan (biarkan kosong), dan pernah memilih tapi semua stiker yang
     * dipilih sudah dihapus (pakai bawaan lagi). Tanpa pembedaan terakhir,
     * menghapus semua stiker lama membuat layar terbuka dengan nol pilihan
     * padahal pengguna tidak pernah berniat mengosongkannya.
     */
    fun resolve(stored: Set<String>?, available: List<String>): Set<String> = when {
        stored == null -> defaultSelection(available).toSet()
        stored.isEmpty() -> emptySet()
        else -> {
            val masihAda = stored.intersect(available.toSet())
            if (masihAda.isEmpty()) defaultSelection(available).toSet() else masihAda
        }
    }
}

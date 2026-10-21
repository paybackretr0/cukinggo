package com.khalied.cukinggo.detect

/**
 * Menjaga isyarat "cuking terlihat" supaya tidak berkedip.
 *
 * Detektor bekerja per frame, dan satu frame bisa meleset walau cukingnya masih
 * di depan kamera. Kalau isyaratnya mengikuti frame apa adanya, cincin di layar
 * akan menyala-mati tiap beberapa ratus milidetik, dan itu terbaca seperti
 * kamera yang rusak. Jadi isyarat baru menyala setelah beberapa frame berturut
 * menemukan cuking, dan baru padam setelah beberapa frame berturut tidak
 * menemukannya.
 *
 * Kelas ini murni: tidak menyentuh kamera, waktu, atau Android, jadi ambangnya
 * bisa diuji tanpa device.
 */
class CatPresence(
    private val framesToShow: Int = 2,
    private val framesToHide: Int = 3
) {
    init {
        require(framesToShow > 0) { "Butuh minimal satu frame untuk menyalakan" }
        require(framesToHide > 0) { "Butuh minimal satu frame untuk memadamkan" }
    }

    private var hits = 0
    private var misses = 0

    var visible: Boolean = false
        private set

    /** Mengembalikan keadaan setelah frame ini diproses. */
    fun onResult(found: Boolean): Boolean {
        if (found) {
            misses = 0
            if (!visible) {
                hits += 1
                if (hits >= framesToShow) {
                    visible = true
                    hits = 0
                }
            }
        } else {
            hits = 0
            if (visible) {
                misses += 1
                if (misses >= framesToHide) {
                    visible = false
                    misses = 0
                }
            }
        }
        return visible
    }

    /**
     * Mengosongkan hitungan, dipakai saat analisis dilepas.
     *
     * Isyaratnya ikut padam: kalau layarnya ditinggalkan, tidak ada yang perlu
     * diberi tahu bahwa cuking tadi terlihat.
     */
    fun reset() {
        hits = 0
        misses = 0
        visible = false
    }
}

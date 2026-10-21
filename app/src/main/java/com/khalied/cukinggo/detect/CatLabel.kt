package com.khalied.cukinggo.detect

/** Satu label hasil model, dipisah dari tipe ML Kit supaya bisa diuji di JVM. */
data class CatLabel(val text: String, val confidence: Float)

/**
 * Teks label yang dipakai model bawaan ML Kit untuk cuking.
 *
 * Dicocokkan dari namanya, bukan dari nomor kelas. Model label punya ratusan
 * kategori, dan nomor kelasnya bukan urutan yang dijanjikan ke pemanggil;
 * sedangkan nama "Cat" memang bagian dari kontrak modelnya.
 */
private const val CAT_TEXT = "cat"

/**
 * Skor tertinggi untuk label cuking, 0 kalau tidak ada.
 *
 * Beberapa label bisa muncul sekaligus ("Cat", "Mammal", "Whiskers"), dan yang
 * dipakai adalah yang paling yakin di antara yang memang menyebut cuking.
 */
fun catConfidence(labels: List<CatLabel>): Float =
    labels.asSequence()
        .filter { it.text.equals(CAT_TEXT, ignoreCase = true) }
        .maxOfOrNull { it.confidence }
        ?: 0f

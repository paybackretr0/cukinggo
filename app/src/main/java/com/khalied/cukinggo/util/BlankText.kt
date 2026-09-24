package com.khalied.cukinggo.util

/**
 * Menyeragamkan isian yang boleh dikosongkan: spasi di ujung dibuang, dan isian
 * yang cuma berisi spasi dianggap belum diisi (null), bukan string kosong.
 *
 * Dipakai nama dan catatan cuking, supaya "kosong" hanya punya satu arti di
 * seluruh app, jadi pemakaian di UI tinggal memeriksa null.
 */
fun blankToNull(raw: String?): String? = raw?.trim()?.takeIf { it.isNotEmpty() }

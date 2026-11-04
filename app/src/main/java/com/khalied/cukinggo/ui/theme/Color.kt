package com.khalied.cukinggo.ui.theme

import androidx.compose.ui.graphics.Color

// --- NeoBrutalism (lihat DESIGN.md, "NeoBrutalism") ---
// Blok warna datar, garis tinta tebal, bayangan keras. Tidak ada transparansi:
// yang memisahkan dua permukaan adalah garis, bukan opacity.
// Setiap pasangan teks/latar di bawah sudah dihitung; angkanya ada di DESIGN.md.

/** Tinta: garis tepi, teks utama, dan bayangan keras. */
val Ink = Color(0xFF111111)

/**
 * Latar halaman. Off-white hangat, bukan putih: kartu putih harus tetap terbaca
 * sebagai bidang yang terpisah dari halamannya.
 */
val Paper = Color(0xFFFFF8E7)

/** Permukaan kartu dan dialog. */
val Card = Color(0xFFFFFFFF)

/** Permukaan sekunder: kolom isian dan area yang harus mundur satu langkah. */
val SurfaceMuted = Color(0xFFF3ECDC)

/** Teks sekunder, 8,4:1 di atas [Paper]. */
val Muted = Color(0xFF4A4A4A)

/**
 * Aksen utama: FAB, lencana rentetan, batang "hari ini" di rekap, badan ilustrasi,
 * telinga marker peta. Teks [Ink] di atasnya 13,3:1.
 *
 * Satu warna untuk dua peran (aksi utama dan chip saringan) supaya tidak ada dua
 * kuning yang mirip tapi beda: dua bidang yang warnanya hampir sama justru
 * terbaca sebagai kesalahan produksi, bukan sebagai dua tingkat hierarki.
 */
val Yellow = Color(0xFFFFD43B)

/** Aksen informasi: koordinat, jarak, gelembung peta. Teks [Ink] 12,7:1. */
val Cyan = Color(0xFF7FE3F0)

/** Aksen lembut: hati yang berhamburan, penanda hapus. Teks [Ink] 8,9:1. */
val Pink = Color(0xFFFF8FBF)

/**
 * Isian tombol sekunder dan warna tautan.
 *
 * Satu-satunya aksen yang memakai teks putih: [Paper] di atasnya 4,9:1,
 * sedangkan [Ink] di atas biru ini cuma 3,7:1 dan gagal untuk teks kecil.
 */
val Blue = Color(0xFF2F5BFF)

/** Status gagal. 6,2:1 di atas [Paper]. */
val ErrorRed = Color(0xFFB3261E)

// Varian malam: keluarga netral yang sama, bukan hitam pekat dengan abu-abu.
val NightCanvas = Color(0xFF121212)
val NightCard = Color(0xFF1C1C1C)
val NightMutedSurface = Color(0xFF262626)
val NightInk = Color(0xFFF5F5F5)
val NightMuted = Color(0xFFB5B5B5)


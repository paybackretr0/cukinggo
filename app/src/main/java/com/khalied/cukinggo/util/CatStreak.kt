package com.khalied.cukinggo.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Rentetan hari berturut-turut yang punya minimal satu catatan kucing.
 *
 * Dihitung dari waktu catatannya, bukan dari angka yang disimpan dan ditambah
 * satu tiap hari. Dengan cara itu tidak ada keadaan yang bisa jadi tidak sinkron:
 * kalau catatan kucing dihapus, atau tanggal HP berubah, angkanya langsung ikut
 * benar tanpa perlu ada yang memperbaiki.
 *
 * Aturannya:
 * - Dua catatan di hari yang sama tetap dihitung satu hari.
 * - Rentetan masih hidup kalau hari terakhir yang ada catatannya adalah hari ini
 *   atau kemarin. Kemarin ikut dihitung supaya rentetannya tidak hilang cuma
 *   karena kamu belum sempat keluar pagi-pagi.
 * - Lebih lama dari itu berarti rentetannya sudah putus, dan hasilnya 0.
 * - Catatan bertanggal di masa depan (jam HP yang bergeser) tidak memperpanjang
 *   rentetan, karena itu bukan hari yang benar-benar terjadi.
 */
fun catStreak(
    timestamps: List<Long>,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault()
): Int {
    if (timestamps.isEmpty()) return 0

    val todayEpochDay = today.toEpochDay()
    val recordedDays = timestamps
        .map { timestamp ->
            Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate().toEpochDay()
        }
        .filter { day -> day <= todayEpochDay }
        .toSortedSet()

    val lastDay = recordedDays.lastOrNull() ?: return 0
    if (todayEpochDay - lastDay > 1) return 0

    var streak = 1
    var day = lastDay
    while (recordedDays.contains(day - 1)) {
        streak += 1
        day -= 1
    }
    return streak
}

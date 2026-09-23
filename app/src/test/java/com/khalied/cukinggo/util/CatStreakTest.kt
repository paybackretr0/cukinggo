package com.khalied.cukinggo.util

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class CatStreakTest {

    private val zone = ZoneId.of("Asia/Jakarta")
    private val today = LocalDate.of(2026, 9, 23)

    private fun at(date: LocalDate, hour: Int = 12): Long =
        ZonedDateTime.of(date, java.time.LocalTime.of(hour, 0), zone).toInstant().toEpochMilli()

    private fun streakOf(vararg dates: LocalDate): Int =
        catStreak(dates.map { at(it) }, today, zone)

    @Test
    fun `belum ada catatan berarti belum ada rentetan`() {
        assertEquals(0, catStreak(emptyList(), today, zone))
    }

    @Test
    fun `kucing hari ini memulai rentetan satu hari`() {
        assertEquals(1, streakOf(today))
    }

    @Test
    fun `tiga hari berturut-turut dihitung tiga`() {
        assertEquals(3, streakOf(today, today.minusDays(1), today.minusDays(2)))
    }

    @Test
    fun `beberapa kucing di hari yang sama tetap dihitung satu hari`() {
        val timestamps = listOf(
            at(today, 8),
            at(today, 12),
            at(today, 19),
            at(today.minusDays(1), 9)
        )

        assertEquals(2, catStreak(timestamps, today, zone))
    }

    @Test
    fun `urutan catatan tidak memengaruhi hasil`() {
        val berurutan = listOf(at(today), at(today.minusDays(1)), at(today.minusDays(2)))
        val acak = listOf(at(today.minusDays(2)), at(today), at(today.minusDays(1)))

        assertEquals(
            catStreak(berurutan, today, zone),
            catStreak(acak, today, zone)
        )
    }

    @Test
    fun `rentetan tetap hidup kalau hari ini belum sempat jalan`() {
        assertEquals(2, streakOf(today.minusDays(1), today.minusDays(2)))
    }

    @Test
    fun `rentetan putus kalau terakhir kemarin lusa`() {
        assertEquals(0, streakOf(today.minusDays(2), today.minusDays(3)))
    }

    @Test
    fun `catatan bertanggal besok tidak memperpanjang rentetan`() {
        val timestamps = listOf(at(today.plusDays(1)), at(today), at(today.minusDays(1)))

        assertEquals(2, catStreak(timestamps, today, zone))
    }

    @Test
    fun `rentetan yang cuma berisi masa depan diabaikan`() {
        assertEquals(0, streakOf(today.plusDays(1), today.plusDays(2)))
    }

    @Test
    fun `pergantian hari ikut zona waktu setempat`() {
        // 23.30 di Jakarta sudah hari berikutnya di UTC, jadi batas harinya harus
        // mengikuti waktu setempat, bukan UTC.
        val malamIni = ZonedDateTime.of(today, java.time.LocalTime.of(23, 30), zone)
            .toInstant().toEpochMilli()
        val kemarinMalam = ZonedDateTime.of(today.minusDays(1), java.time.LocalTime.of(23, 30), zone)
            .toInstant().toEpochMilli()

        assertEquals(2, catStreak(listOf(malamIni, kemarinMalam), today, zone))
    }
}

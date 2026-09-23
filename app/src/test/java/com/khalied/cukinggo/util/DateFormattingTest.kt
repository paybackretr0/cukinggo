package com.khalied.cukinggo.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DateFormattingTest {

    private fun timestampOf(date: LocalDate, hour: Int, minute: Int): Long =
        date.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun `formatTime menghasilkan jam 24 jam`() {
        val timestamp = timestampOf(LocalDate.now(), 14, 5)
        assertEquals("14:05", formatTime(timestamp))
    }

    @Test
    fun `formatDayLabel memakai label hari ini untuk timestamp hari ini`() {
        val today = timestampOf(LocalDate.now(), 9, 0)
        assertEquals("Hari ini", formatDayLabel(today, "Hari ini", "Kemarin"))
    }

    @Test
    fun `formatDayLabel memakai label kemarin untuk timestamp kemarin`() {
        val yesterday = timestampOf(LocalDate.now().minusDays(1), 21, 30)
        assertEquals("Kemarin", formatDayLabel(yesterday, "Hari ini", "Kemarin"))
    }

    @Test
    fun `formatDayLabel memakai tanggal untuk hari yang lebih lama`() {
        val old = timestampOf(LocalDate.of(2024, 3, 7), 12, 0)
        assertEquals("7 Mar 2024", formatDayLabel(old, "Hari ini", "Kemarin"))
    }

    @Test
    fun `formatCoordinates selalu pakai titik desimal`() {
        assertEquals("-6.20000, 106.81667", formatCoordinates(-6.2, 106.816666))
    }

    @Test
    fun `formatFullDateTime berisi tanggal lokal dan jam`() {
        val timestamp = Instant.parse("2026-09-23T07:30:00Z").toEpochMilli()
        val localDate = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()

        val formatted = formatFullDateTime(timestamp)

        assertTrue(formatted.contains(localDate.dayOfMonth.toString()))
        assertTrue(formatted.contains(localDate.year.toString()))
        assertTrue(formatted.contains(":"))
    }
}

package com.khalied.cukinggo.detect

import org.junit.Assert.assertEquals
import org.junit.Test

class CatLabelTest {

    @Test
    fun `label Cat dipakai apa adanya`() {
        val labels = listOf(CatLabel("Cat", 0.8f), CatLabel("Mammal", 0.9f))

        assertEquals(0.8f, catConfidence(labels), 0.001f)
    }

    @Test
    fun `huruf besar kecil tidak membedakan`() {
        assertEquals(0.7f, catConfidence(listOf(CatLabel("CAT", 0.7f))), 0.001f)
        assertEquals(0.6f, catConfidence(listOf(CatLabel("cat", 0.6f))), 0.001f)
    }

    @Test
    fun `skor tertinggi yang dipakai kalau labelnya muncul lebih dari sekali`() {
        val labels = listOf(CatLabel("Cat", 0.4f), CatLabel("Cat", 0.9f))

        assertEquals(0.9f, catConfidence(labels), 0.001f)
    }

    @Test
    fun `kata lain yang mengandung cat tidak dihitung`() {
        val labels = listOf(CatLabel("Cattle", 0.99f), CatLabel("Caterpillar", 0.9f))

        assertEquals(0f, catConfidence(labels), 0.001f)
    }

    @Test
    fun `tanpa label cuking skornya nol`() {
        assertEquals(0f, catConfidence(emptyList()), 0.001f)
    }
}

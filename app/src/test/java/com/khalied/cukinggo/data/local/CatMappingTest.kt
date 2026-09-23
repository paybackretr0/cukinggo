package com.khalied.cukinggo.data.local

import com.khalied.cukinggo.domain.model.Cat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CatMappingTest {

    @Test
    fun `entity dipetakan ke domain tanpa kehilangan data`() {
        val entity = CatEntity(
            id = 7L,
            photoPath = "/data/files/cat_7.jpg",
            description = "Kucing oren di warung",
            latitude = -6.2,
            longitude = 106.816666,
            timestamp = 1_700_000_000_000L
        )

        val cat = entity.toDomain()

        assertEquals(7L, cat.id)
        assertEquals("/data/files/cat_7.jpg", cat.photoPath)
        assertEquals("Kucing oren di warung", cat.description)
        assertEquals(-6.2, cat.latitude, 0.0)
        assertEquals(106.816666, cat.longitude, 0.0)
        assertEquals(1_700_000_000_000L, cat.timestamp)
    }

    @Test
    fun `deskripsi kosong tetap nullable saat dipetakan balik ke entity`() {
        val cat = Cat(
            id = 0,
            photoPath = "/data/files/cat_8.jpg",
            description = null,
            latitude = 1.0,
            longitude = 2.0,
            timestamp = 42L
        )

        val entity = cat.toEntity()

        assertNull(entity.description)
        assertEquals("/data/files/cat_8.jpg", entity.photoPath)
    }
}

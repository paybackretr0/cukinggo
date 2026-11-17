package com.khalied.cukinggo.detect

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoCheckTest {

    @Test
    fun `foto tanpa cuking menahan simpan`() {
        assertTrue(PhotoCheck.NoCat.blocksSave())
    }

    @Test
    fun `foto yang belum selesai dicek juga menahan`() {
        assertTrue(PhotoCheck.Checking.blocksSave())
    }

    @Test
    fun `foto yang ada cukingnya boleh disimpan`() {
        assertFalse(PhotoCheck.HasCat.blocksSave())
    }

    @Test
    fun `pemeriksaan yang gagal tidak menghukum pengguna`() {
        assertFalse(PhotoCheck.Failed().blocksSave())
    }

    @Test
    fun `belum ada foto berarti belum ada yang ditahan`() {
        assertFalse(PhotoCheck.Idle.blocksSave())
    }
}

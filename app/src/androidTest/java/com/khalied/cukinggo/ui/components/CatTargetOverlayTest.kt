package com.khalied.cukinggo.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.khalied.cukinggo.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Menguji bagian yang benar-benar dilihat pengguna saat detektor menyala.
 *
 * Yang diperiksa cuma satu: baris "cuking terlihat" muncul saat isyaratnya
 * menyala dan hilang saat padam. Sudut-sudut bingkainya selalu ada, jadi tidak
 * ada yang bisa dibedakan dari situ.
 *
 * Isyaratnya sendiri diuji terpisah: [com.khalied.cukinggo.detect.CatPresenceTest]
 * untuk jeda nyala-padam, dan [com.khalied.cukinggo.detect.CatLabelerTest] untuk
 * model yang menilai foto cuking sungguhan.
 */
@RunWith(AndroidJUnit4::class)
class CatTargetOverlayTest {

    @get:Rule
    val compose = createComposeRule()

    private fun label(): String =
        InstrumentationRegistry.getInstrumentation()
            .targetContext
            .getString(R.string.add_camera_cat_seen)

    @Test
    fun isyaratMenyalaMenampilkanBarisCukingTerlihat() {
        compose.setContent { CatTargetOverlay(detected = true) }

        compose.onNodeWithText(label()).assertIsDisplayed()
    }

    @Test
    fun tanpaIsyaratBarisnyaTidakAda() {
        compose.setContent { CatTargetOverlay(detected = false) }

        compose.onNodeWithText(label()).assertDoesNotExist()
    }
}

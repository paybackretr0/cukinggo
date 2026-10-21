package com.khalied.cukinggo.detect

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Membuktikan dua hal yang tidak bisa diuji di JVM: model label bawaan ML Kit
 * benar-benar dimuat dari APK, dan foto cuking sungguhan mendapat label "Cat".
 *
 * Foto ujinya bukan aset app, hanya aset test, jadi tidak ikut ke APK rilis.
 * Sumber: "Cat03.jpg" oleh Alvesgaspar, Wikimedia Commons, CC BY-SA 3.0,
 * https://commons.wikimedia.org/wiki/File:Cat03.jpg (diperkecil ke 1280 px).
 */
@RunWith(AndroidJUnit4::class)
class CatLabelerTest {

    @Test
    fun fotoCukingMendapatLabelCat() {
        val testContext = InstrumentationRegistry.getInstrumentation().context
        val bitmap: Bitmap? = testContext.assets.open("test-cat.jpg").use { stream ->
            android.graphics.BitmapFactory.decodeStream(stream)
        }
        assertNotNull("Foto uji tidak terbaca", bitmap)

        val labeler = ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.3f)
                .build()
        )
        try {
            val labels = Tasks.await(
                labeler.process(InputImage.fromBitmap(requireNotNull(bitmap), 0)),
                30,
                TimeUnit.SECONDS
            )
            val confidence = catConfidence(
                labels.map { CatLabel(text = it.text, confidence = it.confidence) }
            )
            assertTrue(
                "Cat tidak terdeteksi. Label yang muncul: " +
                    labels.joinToString { "${it.text}=${it.confidence}" },
                confidence >= MIN_CAT_CONFIDENCE
            )
        } finally {
            labeler.close()
        }
    }
}

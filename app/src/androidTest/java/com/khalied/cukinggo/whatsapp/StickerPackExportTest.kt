package com.khalied.cukinggo.whatsapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Membuktikan dua hal yang cuma bisa diuji di HP: file yang dihasilkan benar-benar
 * WebP 512 px di bawah 100 KB, dan provider menjawab dengan bentuk yang dipakai
 * WhatsApp.
 *
 * Fotonya tidak dipakai di sini; yang dibuat adalah bitmap sederhana, karena yang
 * diuji adalah ukuran dan format keluarannya, bukan potongan latarnya.
 */
@RunWith(AndroidJUnit4::class)
class StickerPackExportTest {

    private val context: Context =
        InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var sourceFiles: List<File>

    @Before
    fun siapkanStiker() {
        sourceFiles = (1..3).map { index -> writeCircleSticker(index) }
    }

    @After
    fun bersihkan() {
        sourceFiles.forEach { it.delete() }
        StickerPackExporter.packDirectory(context).deleteRecursively()
    }

    @Test
    fun paketTigaStikerDitulisSesuaiAturanWhatsApp() = runBlocking {
        val exporter = StickerPackExporter(context)

        val result = exporter.export(sourceFiles.map { StickerSource(it.absolutePath, "si Kumis") })

        assertEquals(StickerPackExport.Ready(3), result)
        val pack = StickerPackExporter.packDirectory(context)
        val manifest = requireNotNull(
            StickerPackManifest.parse(
                File(pack, StickerPackExporter.MANIFEST_FILE_NAME).readText()
            )
        )
        assertEquals(3, manifest.stickers.size)
        assertEquals(StickerPackManifest.TRAY_FILE_NAME, manifest.trayFileName)

        manifest.stickers.forEach { sticker ->
            val file = File(pack, sticker.fileName)
            assertTrue("${sticker.fileName} tidak ada", file.isFile)
            assertTrue(
                "${sticker.fileName} ${file.length()} byte, batasnya ${StickerPackRules.MAX_STICKER_BYTES}",
                file.length() <= StickerPackRules.MAX_STICKER_BYTES
            )
            val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
            assertNotNull("${sticker.fileName} bukan gambar yang terbaca", bitmap)
            assertEquals(StickerPackRules.STICKER_PX, bitmap!!.width)
            assertEquals(StickerPackRules.STICKER_PX, bitmap.height)
            bitmap.recycle()
        }

        val tray = File(pack, manifest.trayFileName)
        assertTrue(tray.isFile)
        assertTrue(tray.length() <= StickerPackRules.MAX_TRAY_BYTES)
        val trayBitmap = android.graphics.BitmapFactory.decodeFile(tray.absolutePath)
        assertEquals(StickerPackRules.TRAY_PX, trayBitmap!!.width)
        assertEquals(StickerPackRules.TRAY_PX, trayBitmap.height)
        trayBitmap.recycle()
    }

    @Test
    fun duaStikerBelumCukup() = runBlocking {
        val result = StickerPackExporter(context)
            .export(sourceFiles.take(2).map { StickerSource(it.absolutePath, null) })

        assertEquals(StickerPackExport.TooFew(2), result)
    }

    @Test
    fun providerMenjawabSepertiYangDimintaWhatsApp() = runBlocking {
        StickerPackExporter(context)
            .export(sourceFiles.map { StickerSource(it.absolutePath, null) })

        val authority = WhatsAppStickers.authority(context)

        val packs = context.contentResolver.query(
            Uri.parse("content://$authority/metadata"),
            null,
            null,
            null,
            null
        )!!.use { cursor ->
            assertTrue(cursor.moveToFirst())
            val identifier = cursor.getString(cursor.getColumnIndexOrThrow("sticker_pack_identifier"))
            val name = cursor.getString(cursor.getColumnIndexOrThrow("sticker_pack_name"))
            val tray = cursor.getString(cursor.getColumnIndexOrThrow("sticker_pack_icon"))
            listOf(identifier, name, tray)
        }
        assertEquals(
            listOf(
                StickerPackManifest.IDENTIFIER,
                StickerPackManifest.NAME,
                StickerPackManifest.TRAY_FILE_NAME
            ),
            packs
        )

        val stickers = context.contentResolver.query(
            Uri.parse("content://$authority/stickers/${StickerPackManifest.IDENTIFIER}"),
            null,
            null,
            null,
            null
        )!!.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(cursor.getString(cursor.getColumnIndexOrThrow("sticker_file_name")))
                }
            }
        }
        assertEquals(
            listOf("sticker_001.webp", "sticker_002.webp", "sticker_003.webp"),
            stickers
        )

        val asset = context.contentResolver.openAssetFileDescriptor(
            Uri.parse(
                "content://$authority/stickers_asset/${StickerPackManifest.IDENTIFIER}/sticker_001.webp"
            ),
            "r"
        )
        assertNotNull("Stiker pertama tidak bisa dibaca lewat provider", asset)
        asset!!.use { descriptor ->
            assertTrue(descriptor.length > 0)
        }
    }

    @Test
    fun providerMenolakFileYangTidakAdaDiPaket() = runBlocking {
        StickerPackExporter(context)
            .export(sourceFiles.map { StickerSource(it.absolutePath, null) })

        val authority = WhatsAppStickers.authority(context)
        // ContentResolver boleh mengembalikan null atau melempar, tergantung
        // versi Android; dua-duanya berarti file itu tidak bisa dibuka.
        val asing = runCatching {
            context.contentResolver.openAssetFileDescriptor(
                Uri.parse(
                    "content://$authority/stickers_asset/" +
                        "${StickerPackManifest.IDENTIFIER}/cat_999.jpg"
                ),
                "r"
            )
        }.getOrNull()

        assertNull("File di luar daftar paket seharusnya tidak bisa dibuka", asing)
    }

    /** Bitmap 512 x 512 dengan lingkaran berwarna dan latar transparan. */
    private fun writeCircleSticker(index: Int): File {
        val side = StickerPackRules.STICKER_PX
        val bitmap = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.HSVToColor(floatArrayOf(index * 90f, 0.5f, 0.9f))
        }
        canvas.drawOval(RectF(96f, 96f, side - 96f, side - 96f), paint)

        val file = File(context.cacheDir, "test_sticker_$index.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        return file
    }
}

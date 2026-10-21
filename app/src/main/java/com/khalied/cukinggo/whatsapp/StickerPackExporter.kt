package com.khalied.cukinggo.whatsapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import com.khalied.cukinggo.R
import com.khalied.cukinggo.ui.theme.IceAccent
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Satu stiker yang mau dimasukkan ke paket. */
data class StickerSource(
    val stickerPath: String,
    /** Nama cukingnya, dipakai menyusun kalimat untuk pembaca layar. */
    val catName: String?
)

/** Hasil satu percobaan mengekspor paket. */
sealed interface StickerPackExport {
    /** Paket tertulis di disk dan siap dibuka WhatsApp. */
    data class Ready(val stickerCount: Int) : StickerPackExport

    /** Stiker yang ada di HP belum cukup untuk paket WhatsApp. */
    data class TooFew(val available: Int) : StickerPackExport
}

/**
 * Menyiapkan paket stiker WhatsApp dari stiker yang sudah tersimpan.
 *
 * Yang ditulis bukan ulang dari fotonya, tapi dari master PNG 512 px yang sudah
 * ada: potongannya sudah benar, dan mengulang pemotongan hanya akan memakan
 * waktu serta memberi hasil yang bisa berbeda.
 *
 * Semua file ditulis ke folder sementara lebih dulu, lalu folder lama diganti.
 * Kalau ekspornya gagal di tengah (misalnya app dimatikan), paket lama tetap
 * utuh dan tidak ada paket setengah jadi yang dibaca WhatsApp.
 */
class StickerPackExporter(private val context: Context) {

    suspend fun export(stickers: List<StickerSource>): StickerPackExport =
        withContext(Dispatchers.IO) {
            val usable = stickers
                .filter { File(it.stickerPath).isFile }
                .take(StickerPackRules.MAX_STICKERS)

            if (usable.size < StickerPackRules.MIN_STICKERS) {
                return@withContext StickerPackExport.TooFew(usable.size)
            }

            val target = packDirectory(context)
            val staging = File(target.parentFile, ".staging")
            staging.deleteRecursively()
            if (!staging.mkdirs()) throw IOException("Folder stiker tidak bisa dibuat")

            try {
                val packStickers = usable.mapIndexed { index, source ->
                    val bitmap = decodeSquare(source.stickerPath)
                    val bytes = encodeWebp(bitmap, StickerPackRules.MAX_STICKER_BYTES)
                    bitmap.recycle()

                    val fileName = StickerPackRules.stickerFileName(index)
                    File(staging, fileName).writeBytes(bytes)
                    PackSticker(
                        fileName = fileName,
                        emojis = CAT_EMOJI,
                        accessibilityText = accessibilityTextFor(source.catName)
                    )
                }

                val tray = renderTrayIcon()
                val trayBytes = encodeWebp(tray, StickerPackRules.MAX_TRAY_BYTES)
                tray.recycle()
                File(staging, StickerPackManifest.TRAY_FILE_NAME).writeBytes(trayBytes)

                val manifest = StickerPackManifest(
                    identifier = StickerPackManifest.IDENTIFIER,
                    name = StickerPackManifest.NAME,
                    publisher = StickerPackManifest.PUBLISHER,
                    trayFileName = StickerPackManifest.TRAY_FILE_NAME,
                    // Momen ekspor dipakai sebagai versi: WhatsApp membandingkan
                    // string ini untuk tahu paketnya berubah.
                    imageDataVersion = System.currentTimeMillis().toString(),
                    stickers = packStickers
                )
                File(staging, MANIFEST_FILE_NAME).writeText(manifest.serialize())

                target.deleteRecursively()
                if (!staging.renameTo(target)) {
                    throw IOException("Paket stiker gagal dipasang")
                }
                StickerPackExport.Ready(packStickers.size)
            } finally {
                staging.deleteRecursively()
            }
        }

    /**
     * Membaca master PNG lalu memasukkannya ke kanvas 512 x 512.
     *
     * Stiker dari [com.khalied.cukinggo.sticker.StickerMaker] sudah 512 px, jadi
     * jalur ini biasanya tidak menyentuh gambarnya sama sekali.
     */
    private fun decodeSquare(path: String): Bitmap {
        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val source = BitmapFactory.decodeFile(path, options)
            ?: throw IOException("Stiker tidak terbaca: $path")

        val side = StickerPackRules.STICKER_PX
        if (source.width == side && source.height == side) return source

        val square = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val scale = min(side.toFloat() / source.width, side.toFloat() / source.height)
        val width = (source.width * scale).roundToInt()
        val height = (source.height * scale).roundToInt()
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        Canvas(square).drawBitmap(
            source,
            null,
            RectF((side - width) / 2f, (side - height) / 2f, (side + width) / 2f, (side + height) / 2f),
            paint
        )
        source.recycle()
        return square
    }

    /**
     * WebP lossy dengan alpha, dicoba dari kualitas tertinggi yang muat.
     *
     * WhatsApp tidak menerima stiker di atas 100 KB, sedangkan PNG 512 px dengan
     * latar transparan gampang melewatinya. Kalau semua tingkat kualitas masih
     * terlalu besar, ekspornya digagalkan dengan pesan, bukan diam-diam
     * mengirim stiker yang akan ditolak.
     */
    private fun encodeWebp(bitmap: Bitmap, maxBytes: Int): ByteArray {
        @Suppress("DEPRECATION")
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            Bitmap.CompressFormat.WEBP
        }

        StickerPackRules.QUALITY_STEPS.forEach { quality ->
            val out = ByteArrayOutputStream()
            bitmap.compress(format, quality, out)
            if (out.size() <= maxBytes) return out.toByteArray()
        }
        throw IOException("Gambarnya masih lebih dari ${maxBytes / 1024} KB setelah dikompres")
    }

    /**
     * Kalimat yang dibacakan pembaca layar di WhatsApp.
     *
     * Ditulis dalam bahasa Indonesia karena app ini berbahasa Indonesia; nama
     * cukingnya ikut supaya stiker yang berbeda tidak terdengar sama semua.
     */
    private fun accessibilityTextFor(catName: String?): String =
        if (catName.isNullOrBlank()) {
            context.getString(R.string.sticker_pack_accessibility)
        } else {
            context.getString(R.string.sticker_pack_accessibility_named, catName)
        }

    /**
     * Ikon tray 96 x 96: cakar putih di atas lingkaran warna aksen app.
     *
     * Latarnya diberi lingkaran, bukan dibiarkan transparan, karena tray WhatsApp
     * bisa terang atau gelap; cakar putih di latar terang akan hilang.
     */
    private fun renderTrayIcon(): Bitmap {
        val side = StickerPackRules.TRAY_PX
        val icon = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(icon)
        val circle = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = IceAccent.toArgb() }
        canvas.drawCircle(side / 2f, side / 2f, side / 2f, circle)

        val paw = ContextCompat.getDrawable(context, R.drawable.ic_paw)
            ?: throw IOException("Ikon cakar tidak terbaca")
        val inset = (side * TRAY_ICON_INSET).roundToInt()
        paw.setBounds(inset, inset, side - inset, side - inset)
        paw.draw(canvas)
        return icon
    }

    companion object {
        const val MANIFEST_FILE_NAME = "pack.tsv"

        /** Satu emoji yang jujur: isinya memang cuking semua. */
        private const val CAT_EMOJI = "🐱"

        /** Bagian sisi ikon tray yang dipakai cakar, sisanya jadi bantalan. */
        private const val TRAY_ICON_INSET = 0.22f

        /** Folder kerja paket, satu-satunya yang dibaca provider. */
        fun packDirectory(context: Context): File =
            File(File(context.filesDir, "sticker_packs"), StickerPackManifest.IDENTIFIER)
    }
}

package com.khalied.cukinggo.whatsapp

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

/**
 * Membuka isi paket stiker untuk WhatsApp.
 *
 * WhatsApp tidak membaca file kita langsung; ia bertanya lewat empat alamat
 * tetap: daftar paket, satu paket, daftar stiker, lalu file stikernya satu per
 * satu. Nama kolom di cursor juga bagian dari kontraknya, jadi jangan diganti.
 *
 * Provider ini `exported` dan wajib punya izin baca `com.whatsapp.sticker.READ`:
 * hanya WhatsApp yang memegang izin itu, dan app lain tidak bisa membaca isinya
 * walau tahu alamatnya. File yang boleh diambil pun cuma yang namanya tercatat
 * di manifest paket, jadi tidak ada jalan untuk meminta file lain di HP.
 */
class StickerContentProvider : ContentProvider() {

    private var matcher: UriMatcher? = null

    override fun onCreate(): Boolean {
        val context = context ?: return false
        matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(WhatsAppStickers.authority(context), METADATA, CODE_METADATA)
            addURI(WhatsAppStickers.authority(context), "$METADATA/*", CODE_METADATA_SINGLE)
            addURI(WhatsAppStickers.authority(context), "$STICKERS/*", CODE_STICKERS)
            addURI(WhatsAppStickers.authority(context), "$ASSETS/*/*", CODE_ASSET)
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val manifest = readManifest()
        return when (matcher?.match(uri)) {
            CODE_METADATA -> packCursor(listOfNotNull(manifest))
            CODE_METADATA_SINGLE -> {
                val wanted = uri.lastPathSegment
                packCursor(listOfNotNull(manifest).filter { it.identifier == wanted })
            }

            CODE_STICKERS -> {
                val wanted = uri.lastPathSegment
                val stickers = manifest?.takeIf { it.identifier == wanted }?.stickers.orEmpty()
                MatrixCursor(
                    arrayOf(
                        COLUMN_FILE_NAME,
                        COLUMN_EMOJI,
                        COLUMN_ACCESSIBILITY_TEXT
                    )
                ).apply {
                    stickers.forEach { sticker ->
                        addRow(arrayOf(sticker.fileName, sticker.emojis, sticker.accessibilityText))
                    }
                }
            }

            else -> throw IllegalArgumentException("Alamat tidak dikenal: $uri")
        }
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        if (matcher?.match(uri) != CODE_ASSET) return null

        val segments = uri.pathSegments
        if (segments.size != 3) return null
        val identifier = segments[1]
        val fileName = segments[2]

        val manifest = readManifest() ?: return null
        if (manifest.identifier != identifier) return null

        val allowed = fileName == manifest.trayFileName ||
            manifest.stickers.any { sticker -> sticker.fileName == fileName }
        if (!allowed) return null

        val file = File(StickerPackExporter.packDirectory(requireNotNull(context)), fileName)
        if (!file.isFile) return null

        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return AssetFileDescriptor(descriptor, 0, file.length())
    }

    override fun getType(uri: Uri): String? = when (matcher?.match(uri)) {
        CODE_METADATA -> "vnd.android.cursor.dir/vnd.${requireNotNull(context).packageName}.$METADATA"
        CODE_METADATA_SINGLE -> "vnd.android.cursor.item/vnd.${requireNotNull(context).packageName}.$METADATA"
        CODE_STICKERS -> "vnd.android.cursor.dir/vnd.${requireNotNull(context).packageName}.$STICKERS"
        CODE_ASSET -> "image/webp"
        else -> null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("Provider ini hanya untuk dibaca")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("Provider ini hanya untuk dibaca")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = throw UnsupportedOperationException("Provider ini hanya untuk dibaca")

    /**
     * Dibaca ulang tiap permintaan, bukan disimpan di memori.
     *
     * Paketnya bisa diperbarui kapan saja dari layar stiker, dan WhatsApp
     * menanyakan daftar ini lagi setelah dialognya ditutup. Membaca file kecil
     * ini jauh lebih murah daripada menampilkan paket lama sampai app ditutup.
     */
    private fun readManifest(): StickerPackManifest? = runCatching {
        val file = File(
            StickerPackExporter.packDirectory(requireNotNull(context)),
            StickerPackExporter.MANIFEST_FILE_NAME
        )
        if (!file.isFile) return@runCatching null
        StickerPackManifest.parse(file.readText())
    }.getOrNull()

    private fun packCursor(packs: List<StickerPackManifest>): Cursor =
        MatrixCursor(
            arrayOf(
                COLUMN_PACK_IDENTIFIER,
                COLUMN_PACK_NAME,
                COLUMN_PACK_PUBLISHER,
                COLUMN_PACK_ICON,
                COLUMN_ANDROID_LINK,
                COLUMN_IOS_LINK,
                COLUMN_PUBLISHER_EMAIL,
                COLUMN_PUBLISHER_WEBSITE,
                COLUMN_PRIVACY_POLICY,
                COLUMN_LICENSE,
                COLUMN_IMAGE_DATA_VERSION,
                COLUMN_AVOID_CACHE,
                COLUMN_ANIMATED
            )
        ).apply {
            packs.forEach { pack ->
                addRow(
                    arrayOf<Any?>(
                        pack.identifier,
                        pack.name,
                        pack.publisher,
                        pack.trayFileName,
                        // Tautan toko dan situs memang tidak ada: app ini belum
                        // dirilis, dan kolom kosong lebih jujur daripada tautan
                        // yang buntu.
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        pack.imageDataVersion,
                        0,
                        0
                    )
                )
            }
        }

    companion object {
        private const val METADATA = "metadata"
        private const val STICKERS = "stickers"
        private const val ASSETS = "stickers_asset"

        private const val CODE_METADATA = 1
        private const val CODE_METADATA_SINGLE = 2
        private const val CODE_STICKERS = 3
        private const val CODE_ASSET = 4

        // Nama kolom milik WhatsApp. Jangan diganti.
        private const val COLUMN_PACK_IDENTIFIER = "sticker_pack_identifier"
        private const val COLUMN_PACK_NAME = "sticker_pack_name"
        private const val COLUMN_PACK_PUBLISHER = "sticker_pack_publisher"
        private const val COLUMN_PACK_ICON = "sticker_pack_icon"
        private const val COLUMN_ANDROID_LINK = "android_play_store_link"
        private const val COLUMN_IOS_LINK = "ios_app_download_link"
        private const val COLUMN_PUBLISHER_EMAIL = "sticker_pack_publisher_email"
        private const val COLUMN_PUBLISHER_WEBSITE = "sticker_pack_publisher_website"
        private const val COLUMN_PRIVACY_POLICY = "sticker_pack_privacy_policy_website"
        private const val COLUMN_LICENSE = "sticker_pack_license_agreement_website"
        private const val COLUMN_IMAGE_DATA_VERSION = "image_data_version"
        private const val COLUMN_AVOID_CACHE = "whatsapp_will_not_cache_stickers"
        private const val COLUMN_ANIMATED = "animated_sticker_pack"

        private const val COLUMN_FILE_NAME = "sticker_file_name"
        private const val COLUMN_EMOJI = "sticker_emoji"
        private const val COLUMN_ACCESSIBILITY_TEXT = "sticker_accessibility_text"
    }
}

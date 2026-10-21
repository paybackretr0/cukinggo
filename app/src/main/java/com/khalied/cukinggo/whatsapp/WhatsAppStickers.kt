package com.khalied.cukinggo.whatsapp

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Sambungan ke WhatsApp: apakah app-nya ada, apakah paket kita sudah
 * ditambahkan, dan intent yang membuka dialog "Tambahkan paket stiker".
 *
 * Semua nama action, extra, dan authority di sini adalah kontrak milik WhatsApp,
 * bukan pilihan kita. Salah satu huruf saja berubah, dialognya tidak akan muncul.
 */
object WhatsAppStickers {

    const val CONSUMER_PACKAGE = "com.whatsapp"
    const val BUSINESS_PACKAGE = "com.whatsapp.w4b"

    /** Izin baca yang dipasang di provider kita, diminta WhatsApp saat mengambil stiker. */
    const val READ_PERMISSION = "com.whatsapp.sticker.READ"

    /** Authority provider stiker app ini. Harus sama dengan yang di AndroidManifest. */
    fun authority(context: Context): String =
        "${context.packageName}.stickercontentprovider"

    /** WhatsApp (atau WhatsApp Business) terpasang dan aktif di HP ini. */
    fun isInstalled(context: Context): Boolean =
        isPackageEnabled(context, CONSUMER_PACKAGE) || isPackageEnabled(context, BUSINESS_PACKAGE)

    /**
     * Paket ini sudah ditambahkan di WhatsApp.
     *
     * Ditanyakan lewat provider WhatsApp sendiri, bukan disimpan sebagai
     * preferensi: pengguna bisa melepas paketnya kapan saja dari WhatsApp, dan
     * app ini tidak akan pernah tahu kalau jawabannya disimpan sendiri.
     */
    fun isPackAdded(context: Context): Boolean {
        val packages = listOf(CONSUMER_PACKAGE, BUSINESS_PACKAGE)
            .filter { isPackageEnabled(context, it) }
        if (packages.isEmpty()) return false

        // Paket dianggap ditambahkan kalau sudah ada di salah satu WhatsApp yang
        // terpasang. Menuntut keduanya, seperti contoh resmi WhatsApp, akan
        // menampilkan "belum ditambahkan" selamanya di HP yang cuma memakai
        // WhatsApp Business.
        return packages.any { pkg -> isWhitelistedIn(context, pkg) }
    }

    /**
     * Intent yang membuka dialog WhatsApp untuk menambahkan paket.
     *
     * Chooser dipakai supaya pengguna bisa memilih WhatsApp biasa atau WhatsApp
     * Business saat keduanya terpasang; kalau cuma satu, chooser itu langsung
     * dilewati sistem.
     *
     * Null berarti tidak ada WhatsApp yang bisa menangani intent ini.
     */
    fun addPackIntent(context: Context, chooserTitle: String): Intent? {
        if (!isInstalled(context)) return null

        val intent = Intent(ACTION_ADD_PACK).apply {
            putExtra(EXTRA_PACK_ID, StickerPackManifest.IDENTIFIER)
            putExtra(EXTRA_PACK_AUTHORITY, authority(context))
            putExtra(EXTRA_PACK_NAME, StickerPackManifest.NAME)
        }
        if (context.packageManager.resolveActivity(intent, 0) == null) return null
        return Intent.createChooser(intent, chooserTitle)
    }

    private fun isWhitelistedIn(context: Context, whatsappPackage: String): Boolean = runCatching {
        val uri: Uri = Uri.Builder()
            .scheme(ContentResolver.SCHEME_CONTENT)
            .authority("$whatsappPackage.provider.sticker_whitelist_check")
            .appendPath("is_whitelisted")
            .appendQueryParameter("authority", authority(context))
            .appendQueryParameter("identifier", StickerPackManifest.IDENTIFIER)
            .build()

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return@use false
            val column = cursor.getColumnIndex("result")
            column >= 0 && cursor.getInt(column) == 1
        } ?: false
    }.getOrDefault(false)

    private fun isPackageEnabled(context: Context, packageName: String): Boolean =
        runCatching {
            context.packageManager.getApplicationInfo(packageName, 0).enabled
        }.getOrDefault(false)

    private const val ACTION_ADD_PACK = "com.whatsapp.intent.action.ENABLE_STICKER_PACK"
    private const val EXTRA_PACK_ID = "sticker_pack_id"
    private const val EXTRA_PACK_AUTHORITY = "sticker_pack_authority"
    private const val EXTRA_PACK_NAME = "sticker_pack_name"
}

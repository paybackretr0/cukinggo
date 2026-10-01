package com.khalied.cukinggo.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.util.Locale

/**
 * Membuat intent "bagikan cuking": foto (kalau filenya masih ada) plus teks
 * template berisi catatan, koordinat, dan link Google Maps.
 *
 * Fotonya dikirim lewat [FileProvider] yang sama dengan yang dipakai widget,
 * dengan izin baca sekali pakai lewat flag intent. Jadi tidak ada permission
 * storage, dan foto tidak pernah disalin ke folder publik.
 */
fun catShareIntent(context: Context, photoPath: String, text: String): Intent {
    val intent = Intent(Intent.ACTION_SEND)
    val photoUri = mediaUriFor(context, photoPath)

    if (photoUri != null) {
        intent.type = "image/jpeg"
        intent.putExtra(Intent.EXTRA_STREAM, photoUri)
        // Tanpa flag ini, app penerima tidak boleh membaca URI-nya.
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } else {
        // Fotonya sudah tidak ada, tapi catatannya masih berguna untuk dibagikan.
        intent.type = "text/plain"
    }

    intent.putExtra(Intent.EXTRA_TEXT, text)
    return intent
}

/**
 * Intent "bagikan video cuking": video pendamping plus teks yang sama dengan
 * bagikan foto.
 *
 * Dipisah dari [catShareIntent] karena media yang dikirim berbeda, dan app
 * penerima memilih jenis penanganan dari MIME type-nya: `video/mp4` membuka
 * pemutar atau editor video, sedangkan `image/jpeg` membuka penampil foto.
 */
fun catVideoShareIntent(context: Context, videoPath: String, text: String): Intent {
    val intent = Intent(Intent.ACTION_SEND)
    val videoUri = mediaUriFor(context, videoPath)

    if (videoUri != null) {
        intent.type = "video/mp4"
        intent.putExtra(Intent.EXTRA_STREAM, videoUri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    } else {
        // Videonya sudah tidak ada, tapi catatannya masih berguna.
        intent.type = "text/plain"
    }

    intent.putExtra(Intent.EXTRA_TEXT, text)
    return intent
}

/**
 * Intent "bagikan teks saja": isi pesannya tanpa foto.
 *
 * Ada karena sebagian app, Instagram di antaranya, mengabaikan teks yang
 * menempel pada gambar dan cuma mengambil fotonya. Lewat jalur teks ini
 * pesannya tetap sampai, dan pengguna bisa memakainya sebagai caption atau
 * mengirimnya sebagai pesan biasa.
 */
fun catTextShareIntent(text: String): Intent =
    Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }

/** URI content:// untuk satu file media app. Null kalau filenya sudah tidak ada. */
private fun mediaUriFor(context: Context, path: String): android.net.Uri? {
    val file = File(path)
    if (!file.isFile) return null
    return runCatching {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }.getOrNull()
}

/**
 * Link titik kucing di Google Maps.
 *
 * Memakai format resmi Maps URLs (`/maps/search/?api=1&query=lat,lng`) supaya
 * terbuka di app Google Maps kalau ada, dan di browser kalau tidak ada. Tidak
 * butuh API key, dan koma pemisah koordinat harus di-encode jadi `%2C` seperti
 * yang diminta dokumentasi Maps URLs.
 */
fun mapsLinkFor(latitude: Double, longitude: Double): String {
    val coordinates = String.format(Locale.US, "%.5f,%.5f", latitude, longitude)
    return "https://www.google.com/maps/search/?api=1&query=" +
        coordinates.replace(",", "%2C")
}

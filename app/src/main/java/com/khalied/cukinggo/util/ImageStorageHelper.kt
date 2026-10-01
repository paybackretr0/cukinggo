package com.khalied.cukinggo.util

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

/**
 * Semua foto dan video cuking disimpan di internal storage aplikasi
 * (context.filesDir), jadi otomatis terhapus saat app di-uninstall dan tidak
 * butuh permission storage.
 */
class ImageStorageHelper(private val context: Context) {

    /** File sementara di cache saat kamera baru selesai memotret. */
    fun createCaptureFile(): File =
        File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")

    /** File sementara di cache saat perekaman video baru selesai. */
    fun createVideoCaptureFile(): File =
        File(context.cacheDir, "capture_${System.currentTimeMillis()}.mp4")

    /** Pindahkan hasil jepretan dari cache ke internal storage permanen. */
    fun moveCaptureToInternalStorage(captureFile: File): String {
        val target = File(context.filesDir, "cat_${System.currentTimeMillis()}.jpg")
        captureFile.copyTo(target, overwrite = true)
        captureFile.delete()
        return target.absolutePath
    }

    /** Pindahkan hasil rekaman dari cache ke internal storage permanen. */
    fun moveVideoToInternalStorage(videoFile: File): String {
        val target = File(context.filesDir, "cat_${System.currentTimeMillis()}.mp4")
        videoFile.copyTo(target, overwrite = true)
        videoFile.delete()
        return target.absolutePath
    }

    fun saveImageToInternalStorage(bitmap: Bitmap): String {
        val filename = "cat_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return file.absolutePath
    }

    /**
     * Menghapus satu file media milik app. Nama generiknya dipakai bersama oleh
     * foto dan video: keduanya tinggal di [Context.getFilesDir], jadi yang
     * membedakan cuma ekstensinya, bukan cara menghapusnya.
     */
    fun deleteStoredFile(path: String?) {
        if (path.isNullOrBlank()) return
        val file = File(path)
        if (file.exists() && file.parentFile == context.filesDir) {
            file.delete()
        }
    }

    fun discardCapture(captureFile: File?) {
        if (captureFile != null && captureFile.exists()) {
            captureFile.delete()
        }
    }
}

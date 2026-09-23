package com.khalied.cukinggo.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.khalied.cukinggo.R
import com.khalied.cukinggo.ui.theme.InkSoft
import com.khalied.cukinggo.ui.theme.MintPop
import com.khalied.cukinggo.ui.theme.PawBrown
import com.khalied.cukinggo.ui.theme.PeachAccent

/**
 * Marker peta digambar sendiri supaya tidak butuh aset eksternal:
 * - zoom dekat  : lingkaran berisi foto kucing (dengan telinga kucing kecil di atasnya)
 * - zoom jauh   : gelembung angka (jumlah kucing di area itu) supaya tidak saling nabrak
 */
object CatMarkerRenderer {

    private const val SHADOW_COLOR = 0x33000000

    /** Marker foto kucing. [photo] harus sudah berupa bitmap persegi. */
    fun photoMarker(photo: Bitmap, sizePx: Int, scale: Float = 1f): Bitmap {
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val size = sizePx.toFloat()
        val center = size / 2f
        canvas.scale(scale, scale, center, center)

        val radius = size * 0.34f
        val cx = center
        // Titik tengah lingkaran = titik lokasi kucing di peta (anchor center).
        val cy = size * 0.5f

        drawEars(canvas, cx, cy, radius, size)

        canvas.drawCircle(
            cx, cy + size * 0.02f, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SHADOW_COLOR }
        )

        canvas.save()
        canvas.clipPath(Path().apply { addCircle(cx, cy, radius, Path.Direction.CW) })
        canvas.drawBitmap(
            photo,
            null,
            RectF(cx - radius, cy - radius, cx + radius, cy + radius),
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        )
        canvas.restore()

        canvas.drawCircle(
            cx, cy, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = size * 0.05f
                color = 0xFFFFFFFF.toInt()
            }
        )
        canvas.drawCircle(
            cx, cy, radius + size * 0.025f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = size * 0.025f
                color = PawBrown.copy(alpha = 0.45f).toArgb()
            }
        )

        return output
    }

    /** Marker angka untuk mode zoom jauh / cluster. */
    fun countMarker(context: Context, count: Int, sizePx: Int, scale: Float = 1f): Bitmap {
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val size = sizePx.toFloat()
        val center = size / 2f
        canvas.scale(scale, scale, center, center)

        val radius = size * 0.34f
        val cx = center
        // Titik tengah lingkaran = titik lokasi kucing di peta (anchor center).
        val cy = size * 0.5f

        drawEars(canvas, cx, cy, radius, size)

        canvas.drawCircle(
            cx, cy + size * 0.02f, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SHADOW_COLOR }
        )
        canvas.drawCircle(
            cx, cy, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = PeachAccent.toArgb()
            }
        )
        canvas.drawCircle(
            cx, cy, radius,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = size * 0.05f
                color = 0xFFFFFFFF.toInt()
            }
        )
        canvas.drawCircle(
            cx, cy, radius + size * 0.025f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = size * 0.025f
                color = PawBrown.copy(alpha = 0.45f).toArgb()
            }
        )

        val label = if (count > 99) "99+" else count.toString()
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(roundedTypeface(context), Typeface.BOLD)
            color = InkSoft.toArgb()
            textSize = when (label.length) {
                1 -> size * 0.42f
                2 -> size * 0.36f
                else -> size * 0.26f
            }
        }
        val metrics = textPaint.fontMetrics
        canvas.drawText(label, cx, cy - (metrics.ascent + metrics.descent) / 2f, textPaint)

        return output
    }

    /** Titik "kamu di sini": halo mint + inti putih, sengaja beda dari marker kucing. */
    fun userDot(sizePx: Int, scale: Float = 1f): Bitmap {
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val size = sizePx.toFloat()
        val center = size / 2f
        canvas.scale(scale, scale, center, center)

        canvas.drawCircle(
            center, center, size * 0.44f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = MintPop.copy(alpha = 0.22f).toArgb() }
        )
        canvas.drawCircle(
            center, center + size * 0.015f, size * 0.24f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SHADOW_COLOR }
        )
        canvas.drawCircle(
            center, center, size * 0.24f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
        )
        canvas.drawCircle(
            center, center, size * 0.155f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = MintPop.toArgb() }
        )
        return output
    }

    private fun drawEars(canvas: Canvas, cx: Float, cy: Float, radius: Float, size: Float) {
        val ears = Path().apply {
            moveTo(cx - radius * 0.95f, cy - radius * 0.30f)
            lineTo(cx - radius * 0.70f, cy - radius * 1.28f)
            lineTo(cx - radius * 0.15f, cy - radius * 0.80f)
            close()
            moveTo(cx + radius * 0.95f, cy - radius * 0.30f)
            lineTo(cx + radius * 0.70f, cy - radius * 1.28f)
            lineTo(cx + radius * 0.15f, cy - radius * 0.80f)
            close()
        }
        canvas.drawPath(
            ears,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                color = PeachAccent.toArgb()
            }
        )
        canvas.drawPath(
            ears,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = size * 0.045f
                strokeJoin = Paint.Join.ROUND
                color = 0xFFFFFFFF.toInt()
            }
        )
    }

    private fun roundedTypeface(context: Context): Typeface =
        runCatching { ResourcesCompat.getFont(context, R.font.fredoka) }.getOrNull()
            ?: Typeface.DEFAULT_BOLD
}

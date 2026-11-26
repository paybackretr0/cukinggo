package com.khalied.cukinggo.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.res.ResourcesCompat
import com.khalied.cukinggo.R
import com.khalied.cukinggo.domain.model.CatSighting
import com.khalied.cukinggo.ui.theme.Cyan
import com.khalied.cukinggo.ui.theme.Ink
import com.khalied.cukinggo.ui.theme.Muted
import com.khalied.cukinggo.ui.theme.NightCanvas
import com.khalied.cukinggo.ui.theme.NightInk
import com.khalied.cukinggo.ui.theme.NightMuted
import com.khalied.cukinggo.ui.theme.Paper
import com.khalied.cukinggo.ui.theme.Pink
import com.khalied.cukinggo.ui.theme.Yellow
import java.io.File
import java.io.FileOutputStream
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ukuran kartu Story: 1080 x 1920, rasio 9:16 yang dipakai Instagram.
 *
 * Instagram tidak membuka cara menaruh bingkai atau teks di composer-nya, jadi
 * semua desainnya harus sudah jadi di gambar ini. Karena itu ukurannya dipatok,
 * bukan mengikuti ukuran layar: hasilnya harus sama di HP mana pun.
 */
private const val STORY_WIDTH = 1080
private const val STORY_HEIGHT = 1920

/** Jarak aman dari tepi kiri dan kanan. */
private const val STORY_MARGIN = 72f

/**
 * Tiga tampilan kartu yang bisa dipilih sebelum dibagikan.
 *
 * Ketiganya memakai susunan keterangan yang sama supaya pengguna tidak perlu
 * menebak lagi mana nama, mana kegiatan, dan mana lokasi; yang berbeda cuma
 * suasana dan hiasannya.
 */
enum class StoryDesign {
    /** Kertas, foto miring dengan telinga cuking dan selotip. */
    PAPER,

    /** Balon bicara di latar biru muda, paling manis. */
    BUBBLE,

    /** Latar gelap dengan garis kuning dan konfeti. */
    NIGHT
}

/**
 * Menggambar kartu Story dari satu penemuan, lalu menyimpannya sebagai PNG.
 *
 * Yang digambar: foto cuking, namanya, kegiatan yang ditulis pengguna, jumlah
 * ketemu, waktu, dan koordinat. Tiap keterangan diberi label kecil supaya tidak
 * ada yang tertukar. `null` berarti fotonya tidak terbaca atau filenya gagal
 * ditulis; pemanggil yang menjelaskan ke pengguna.
 *
 * Fungsi ini menyentuh disk dan bitmap besar, jadi dipanggil di luar main
 * thread.
 */
fun renderStoryCard(
    context: Context,
    sighting: CatSighting,
    seenCount: Int,
    design: StoryDesign
): File? {
    val photo = decodeUprightPhoto(File(sighting.photoPath), 1400) ?: return null
    val card = Bitmap.createBitmap(STORY_WIDTH, STORY_HEIGHT, Bitmap.Config.ARGB_8888)

    try {
        drawStoryCard(Canvas(card), context, sighting, seenCount, design, photo)
        return writeStoryCard(context, sighting.id, design, card)
    } finally {
        photo.recycle()
        card.recycle()
    }
}

/**
 * Pratinjau ketiga tampilan untuk pemilih desain.
 *
 * Digambar oleh kode yang sama dengan kartu aslinya, cuma diperkecil, supaya
 * yang dipilih di dialog benar-benar sama dengan yang nanti terkirim. `emptyMap`
 * kalau fotonya tidak terbaca.
 *
 * Bitmap hasilnya milik pemanggil dan perlu di-recycle setelah dialognya tutup.
 */
fun renderStoryPreviews(
    context: Context,
    sighting: CatSighting,
    seenCount: Int,
    widthPx: Int = 320
): Map<StoryDesign, Bitmap> {
    val photo = decodeUprightPhoto(File(sighting.photoPath), 480) ?: return emptyMap()
    val heightPx = widthPx * STORY_HEIGHT / STORY_WIDTH

    try {
        return StoryDesign.values().associateWith { design ->
            val preview = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(preview)
            canvas.scale(
                widthPx / STORY_WIDTH.toFloat(),
                heightPx / STORY_HEIGHT.toFloat()
            )
            drawStoryCard(canvas, context, sighting, seenCount, design, photo)
            preview
        }
    } finally {
        photo.recycle()
    }
}

/**
 * Semua desain digambar dalam ruang 1080x1920. Pratinjau tinggal mengecilkan
 * kanvasnya, jadi tidak ada dua versi tata letak yang bisa menyimpang.
 */
private fun drawStoryCard(
    canvas: Canvas,
    context: Context,
    sighting: CatSighting,
    seenCount: Int,
    design: StoryDesign,
    photo: Bitmap
) {
    val titleFont = displayTypeface(context)
    val bodyFont = bodyTypeface(context)
    val valueFont = valueTypeface(context)
    // Hiasannya acak tapi tidak berubah-ubah: benihnya dari id penemuan, jadi
    // kartu yang sama selalu punya taburan yang sama.
    val random = Random(sighting.id * 31L + design.ordinal)

    when (design) {
        StoryDesign.PAPER -> drawPaperDesign(
            canvas, context, sighting, seenCount, photo, titleFont, bodyFont, valueFont, random
        )

        StoryDesign.BUBBLE -> drawBubbleDesign(
            canvas, context, sighting, seenCount, photo, titleFont, bodyFont, valueFont, random
        )

        StoryDesign.NIGHT -> drawNightDesign(
            canvas, context, sighting, seenCount, photo, titleFont, bodyFont, valueFont, random
        )
    }
}

/** Kertas hangat, foto miring, telinga cuking, dan dua potong selotip. */
private fun drawPaperDesign(
    canvas: Canvas,
    context: Context,
    sighting: CatSighting,
    seenCount: Int,
    photo: Bitmap,
    titleFont: Typeface,
    bodyFont: Typeface,
    valueFont: Typeface,
    random: Random
) {
    canvas.drawColor(Paper.toArgb())

    // Bidang warna besar yang cuma kelihatan tepinya di balik foto: abstrak,
    // tapi tidak mengambil tempat keterangan.
    canvas.drawCircle(920f, 300f, 200f, fill(Yellow.toArgb()))
    canvas.drawCircle(130f, 1170f, 150f, fill(Cyan.toArgb()))

    val photoRect = RectF(96f, 330f, 984f, 1120f)
    canvas.save()
    canvas.rotate(-3f, photoRect.centerX(), photoRect.centerY())
    drawCatEars(canvas, photoRect, Ink.toArgb(), Pink.toArgb())
    drawFramedPhoto(
        canvas = canvas,
        photo = photo,
        rect = photoRect,
        radius = 28f,
        borderColor = Ink.toArgb(),
        borderWidth = 10f,
        shadowColor = Ink.toArgb(),
        shadowOffset = 16f
    )
    drawTape(canvas, photoRect.left + 120f, photoRect.top + 4f, -26f, Cyan.toArgb())
    drawTape(canvas, photoRect.right - 120f, photoRect.top + 4f, 22f, Pink.toArgb())
    canvas.restore()

    drawInfoBlock(
        canvas = canvas,
        context = context,
        sighting = sighting,
        seenCount = seenCount,
        titleFont = titleFont,
        bodyFont = bodyFont,
        valueFont = valueFont,
        top = 1150f,
        valueColor = Ink.toArgb(),
        mutedColor = Muted.toArgb(),
        brandColor = Muted.toArgb()
    )

    drawConfetti(
        canvas = canvas,
        random = random,
        palette = listOf(Ink.toArgb(), Cyan.toArgb(), Pink.toArgb(), Yellow.toArgb())
    )
}

/** Foto di dalam balon bicara, latar biru muda, plus gelembung dan hati. */
private fun drawBubbleDesign(
    canvas: Canvas,
    context: Context,
    sighting: CatSighting,
    seenCount: Int,
    photo: Bitmap,
    titleFont: Typeface,
    bodyFont: Typeface,
    valueFont: Typeface,
    random: Random
) {
    canvas.drawColor(Cyan.toArgb())

    canvas.drawCircle(90f, 360f, 190f, fill(Pink.toArgb()))
    canvas.drawCircle(1010f, 980f, 150f, fill(Yellow.toArgb()))

    val bubble = RectF(84f, 290f, 996f, 1140f)
    val bubblePath = Path().apply {
        addRoundRect(bubble, 64f, 64f, Path.Direction.CW)
    }
    // Ekor balon di sudut kanan bawah, bukan di tengah bawah: di tengah ada nama
    // dan keterangan yang tidak boleh tertutup apa pun, sedangkan sudut kanan
    // bawah cuma berisi ruang kosong sebelum baris merek.
    val tailPath = Path().apply {
        moveTo(800f, 1096f)
        lineTo(948f, 1112f)
        lineTo(846f, 1246f)
        close()
    }

    canvas.save()
    canvas.translate(16f, 16f)
    canvas.drawPath(bubblePath, fill(Ink.toArgb()))
    canvas.drawPath(tailPath, fill(Ink.toArgb()))
    canvas.restore()
    // Ekor digambar lebih dulu supaya pangkalnya tertutup badan balon, jadi
    // tidak ada garis sambungan yang kelihatan.
    canvas.drawPath(tailPath, fill(Paper.toArgb()))
    canvas.drawPath(tailPath, stroke(Ink.toArgb(), 10f))
    canvas.drawPath(bubblePath, fill(Paper.toArgb()))
    canvas.drawPath(bubblePath, stroke(Ink.toArgb(), 10f))

    drawFramedPhoto(
        canvas = canvas,
        photo = photo,
        rect = RectF(120f, 326f, 960f, 1104f),
        radius = 40f,
        borderColor = Ink.toArgb(),
        borderWidth = 8f
    )

    drawInfoBlock(
        canvas = canvas,
        context = context,
        sighting = sighting,
        seenCount = seenCount,
        titleFont = titleFont,
        bodyFont = bodyFont,
        valueFont = valueFont,
        top = 1164f,
        valueColor = Ink.toArgb(),
        mutedColor = Muted.toArgb(),
        brandColor = Ink.toArgb()
    )

    drawConfetti(
        canvas = canvas,
        random = random,
        palette = listOf(Ink.toArgb(), Paper.toArgb(), Yellow.toArgb(), Pink.toArgb())
    )
}

/** Latar gelap, garis kuning, dan konfeti warna: paling menyala di story malam. */
private fun drawNightDesign(
    canvas: Canvas,
    context: Context,
    sighting: CatSighting,
    seenCount: Int,
    photo: Bitmap,
    titleFont: Typeface,
    bodyFont: Typeface,
    valueFont: Typeface,
    random: Random
) {
    canvas.drawColor(NightCanvas.toArgb())

    // Bulan kuning di balik sudut foto, bukan lingkaran transparan: bidang
    // tembus pandang di atas hitam terbaca seperti warna yang kusam, bukan
    // seperti hiasan.
    canvas.drawCircle(980f, 330f, 180f, fill(Yellow.toArgb()))
    canvas.drawCircle(70f, 1760f, 86f, fill(Cyan.toArgb()))
    canvas.drawCircle(1010f, 1824f, 54f, fill(Pink.toArgb()))

    val photoRect = RectF(96f, 340f, 984f, 1130f)
    canvas.save()
    canvas.rotate(2.5f, photoRect.centerX(), photoRect.centerY())
    drawFramedPhoto(
        canvas = canvas,
        photo = photo,
        rect = photoRect,
        radius = 24f,
        borderColor = Yellow.toArgb(),
        borderWidth = 10f,
        shadowColor = Pink.toArgb(),
        shadowOffset = 16f
    )
    drawTape(canvas, photoRect.left + 120f, photoRect.top + 2f, -20f, Cyan.toArgb())
    canvas.restore()

    drawInfoBlock(
        canvas = canvas,
        context = context,
        sighting = sighting,
        seenCount = seenCount,
        titleFont = titleFont,
        bodyFont = bodyFont,
        valueFont = valueFont,
        top = 1170f,
        valueColor = NightInk.toArgb(),
        mutedColor = NightMuted.toArgb(),
        brandColor = Cyan.toArgb()
    )

    drawConfetti(
        canvas = canvas,
        random = random,
        palette = listOf(Yellow.toArgb(), Pink.toArgb(), Cyan.toArgb(), NightInk.toArgb())
    )
}

/**
 * Keterangan yang selalu ada di ketiga desain.
 *
 * Tiap nilai diberi label kecil di atasnya: NAMA, KEGIATAN, dan LOKASI. Tanpa
 * label, deretan angka dan kalimat di bawah foto gampang tertukar, apalagi
 * karena kegiatan boleh kosong.
 *
 * Mengembalikan baseline baris terakhir, dipakai untuk menaruh cap merek.
 */
private fun drawInfoBlock(
    canvas: Canvas,
    context: Context,
    sighting: CatSighting,
    seenCount: Int,
    titleFont: Typeface,
    bodyFont: Typeface,
    valueFont: Typeface,
    top: Float,
    valueColor: Int,
    mutedColor: Int,
    brandColor: Int
) {
    val left = STORY_MARGIN
    val right = STORY_WIDTH - STORY_MARGIN
    val width = right - left

    val name = blankToNull(sighting.catName)
        ?: context.getString(R.string.home_trail_picker_unnamed)
    val activity = blankToNull(sighting.description)
        ?: context.getString(R.string.story_card_no_activity)
    val day = formatDayLabel(
        sighting.timestamp,
        context.getString(R.string.day_today),
        context.getString(R.string.day_yesterday)
    )
    val coordinates = formatCoordinates(sighting.latitude, sighting.longitude)

    var y = top
    drawPill(canvas, context.getString(R.string.story_card_label_name), left, y, Yellow.toArgb())
    y += PILL_HEIGHT + 12f

    val namePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = titleFont
        textSize = 76f
        color = valueColor
    }
    canvas.drawText(
        TextUtils.ellipsize(name, namePaint, width, TextUtils.TruncateAt.END).toString(),
        left,
        y + 58f,
        namePaint
    )
    y += 58f + 30f

    drawPill(canvas, context.getString(R.string.story_card_label_activity), left, y, Pink.toArgb())
    y += PILL_HEIGHT + 10f

    val activityPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = valueFont
        textSize = 38f
        color = valueColor
    }
    val activityLayout = StaticLayout.Builder
        .obtain(activity, 0, activity.length, activityPaint, width.toInt())
        .setMaxLines(2)
        .setEllipsize(TextUtils.TruncateAt.END)
        .setLineSpacing(4f, 1f)
        .build()
    canvas.save()
    canvas.translate(left, y)
    activityLayout.draw(canvas)
    canvas.restore()
    y += activityLayout.height + 22f

    drawPill(canvas, context.getString(R.string.story_card_label_location), left, y, Cyan.toArgb())
    y += PILL_HEIGHT + 10f

    val coordinatesPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = valueFont
        textSize = 34f
        color = mutedColor
    }
    canvas.drawText(coordinates, left, y + 32f, coordinatesPaint)
    y += 32f + 38f

    val whenPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = valueFont
        textSize = 30f
        color = mutedColor
    }
    val seen = context.getString(R.string.detail_seen_count, seenCount) +
        " · " + day + ", " + formatTime(sighting.timestamp)
    canvas.drawText(
        TextUtils.ellipsize(seen, whenPaint, width - 240f, TextUtils.TruncateAt.END).toString(),
        left,
        y,
        whenPaint
    )
    drawBrand(canvas, right, y, valueFont, brandColor)
}

/** Tinggi label pill, dipakai untuk menghitung jarak antar keterangan. */
private const val PILL_HEIGHT = 44f

/**
 * Label kecil berbentuk stiker.
 *
 * Warnanya selalu sama di ketiga desain: kuning untuk nama, merah muda untuk
 * kegiatan, biru muda untuk lokasi. Jadi begitu pengguna melihat satu kartu, dia
 * sudah tahu warna mana yang menandai apa di kartu berikutnya.
 */
private fun drawPill(canvas: Canvas, text: String, x: Float, top: Float, color: Int) {
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = 25f
        this.color = Ink.toArgb()
        letterSpacing = 0.1f
    }
    val label = text.uppercase()
    val pill = RectF(x, top, x + paint.measureText(label) + 34f, top + PILL_HEIGHT)
    canvas.drawRoundRect(pill, PILL_HEIGHT / 2f, PILL_HEIGHT / 2f, fill(color))
    canvas.drawRoundRect(
        pill,
        PILL_HEIGHT / 2f,
        PILL_HEIGHT / 2f,
        stroke(Ink.toArgb(), 3.5f)
    )
    val baseline = pill.centerY() - (paint.ascent() + paint.descent()) / 2f
    canvas.drawText(label, x + 17f, baseline, paint)
}

/** Cap merek kecil dengan cakar digambar sendiri, bukan emoji yang warnanya ikut sistem. */
private fun drawBrand(
    canvas: Canvas,
    right: Float,
    baseline: Float,
    font: Typeface,
    color: Int
) {
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = font
        textSize = 28f
        this.color = color
        textAlign = Paint.Align.RIGHT
    }
    canvas.drawText("CukingGo", right, baseline, paint)
    val pawX = right - paint.measureText("CukingGo") - 26f
    drawPaw(canvas, pawX, baseline - 9f, 10f, color)
}

/** Foto dengan potongan tengah, garis tepi tebal, dan bayangan keras opsional. */
private fun drawFramedPhoto(
    canvas: Canvas,
    photo: Bitmap,
    rect: RectF,
    radius: Float,
    borderColor: Int,
    borderWidth: Float,
    shadowColor: Int? = null,
    shadowOffset: Float = 0f
) {
    shadowColor?.let { color ->
        canvas.drawRoundRect(
            RectF(rect).apply { offset(shadowOffset, shadowOffset) },
            radius,
            radius,
            fill(color)
        )
    }

    canvas.save()
    canvas.clipPath(
        Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) }
    )
    canvas.drawBitmap(
        photo,
        centerCropSource(photo, rect.width() / rect.height()),
        rect,
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    )
    canvas.restore()

    canvas.drawRoundRect(rect, radius, radius, stroke(borderColor, borderWidth))
}

/** Sepasang telinga cuking di belakang foto, badannya yang menutup bagian bawah. */
private fun drawCatEars(canvas: Canvas, photo: RectF, outer: Int, inner: Int) {
    fun ear(startX: Float, direction: Float) {
        canvas.drawPath(
            Path().apply {
                moveTo(startX, photo.top + 40f)
                lineTo(startX + 84f * direction, photo.top - 96f)
                lineTo(startX + 172f * direction, photo.top + 34f)
                close()
            },
            fill(outer)
        )
        canvas.drawPath(
            Path().apply {
                moveTo(startX + 40f * direction, photo.top + 20f)
                lineTo(startX + 84f * direction, photo.top - 48f)
                lineTo(startX + 132f * direction, photo.top + 16f)
                close()
            },
            fill(inner)
        )
    }

    ear(photo.left + 34f, 1f)
    ear(photo.right - 34f, -1f)
}

/** Selotip kecil yang menempel di sudut foto. */
private fun drawTape(canvas: Canvas, cx: Float, cy: Float, angle: Float, color: Int) {
    canvas.save()
    canvas.rotate(angle, cx, cy)
    val tape = RectF(cx - 82f, cy - 23f, cx + 82f, cy + 23f)
    canvas.drawRect(tape, fill(color, 235))
    canvas.drawRect(tape, stroke(Ink.toArgb(), 4f))
    canvas.restore()
}

/**
 * Hiasan acak di dua jalur sempit yang tidak dipakai keterangan: pita atas dan
 * pita bawah kartu. Dua jalur itu juga yang paling sering tertutup UI
 * Instagram, jadi aman dipakai untuk hal yang cuma manis dilihat.
 */
private fun drawConfetti(canvas: Canvas, random: Random, palette: List<Int>) {
    repeat(9) {
        val x = 110f + random.nextFloat() * 860f
        val y = if (random.nextBoolean()) {
            84f + random.nextFloat() * 160f
        } else {
            1652f + random.nextFloat() * 210f
        }
        val size = 22f + random.nextFloat() * 30f
        val color = palette[random.nextInt(palette.size)]
        when (random.nextInt(4)) {
            0 -> drawPaw(canvas, x, y, size, color)
            1 -> drawHeart(canvas, x, y, size, color)
            2 -> drawStar(canvas, x, y, size, color)
            else -> drawSquiggle(canvas, x, y, size, color)
        }
    }
}

private fun drawPaw(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
    val paint = fill(color)
    canvas.drawCircle(x, y + size * 0.3f, size * 0.55f, paint)
    listOf(
        -0.55f to -0.3f,
        -0.2f to -0.62f,
        0.2f to -0.62f,
        0.55f to -0.3f
    ).forEach { (dx, dy) ->
        canvas.drawCircle(x + size * dx, y + size * dy, size * 0.22f, paint)
    }
}

private fun drawHeart(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
    canvas.drawPath(
        Path().apply {
            moveTo(x, y + size * 0.6f)
            cubicTo(
                x - size * 1.15f, y - size * 0.1f,
                x - size * 0.5f, y - size * 0.85f,
                x, y - size * 0.25f
            )
            cubicTo(
                x + size * 0.5f, y - size * 0.85f,
                x + size * 1.15f, y - size * 0.1f,
                x, y + size * 0.6f
            )
            close()
        },
        fill(color)
    )
}

private fun drawStar(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
    val path = Path()
    repeat(10) { index ->
        val radius = if (index % 2 == 0) size else size * 0.45f
        val angle = (-PI / 2 + index * PI / 5).toFloat()
        val px = x + cos(angle) * radius
        val py = y + sin(angle) * radius
        if (index == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    canvas.drawPath(path, fill(color))
}

private fun drawSquiggle(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
    canvas.drawPath(
        Path().apply {
            moveTo(x - size, y)
            quadTo(x - size * 0.5f, y - size, x, y)
            quadTo(x + size * 0.5f, y + size, x + size, y)
        },
        stroke(color, size * 0.32f)
    )
}

/**
 * Bagian foto yang dipakai untuk mengisi bingkai tanpa merenggangkan gambarnya.
 *
 * Foto potret dan lanskap sama-sama harus penuh: yang dipotong kelebihan sisi
 * panjangnya, bukan dikecilkan sampai ada bidang kosong di kiri-kanan.
 */
private fun centerCropSource(photo: Bitmap, targetRatio: Float): Rect {
    val photoRatio = photo.width.toFloat() / photo.height
    return if (photoRatio > targetRatio) {
        val width = photo.height * targetRatio
        val left = (photo.width - width) / 2f
        Rect(left.toInt(), 0, (left + width).toInt(), photo.height)
    } else {
        val height = photo.width / targetRatio
        val top = (photo.height - height) / 2f
        Rect(0, top.toInt(), photo.width, (top + height).toInt())
    }
}

private fun fill(color: Int, alpha: Int = 255): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.alpha = alpha
    }

private fun stroke(color: Int, width: Float): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.STROKE
        strokeWidth = width
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

/** Inter Black untuk nama dan cap; jatuh ke bold bawaan kalau fontnya gagal dimuat. */
private fun displayTypeface(context: Context): Typeface {
    val base = runCatching { ResourcesCompat.getFont(context, R.font.inter) }.getOrNull()
    return base?.let { Typeface.create(it, 900, false) } ?: Typeface.DEFAULT_BOLD
}

/** Manrope tebal untuk label kecil. */
private fun bodyTypeface(context: Context): Typeface {
    val base = runCatching { ResourcesCompat.getFont(context, R.font.manrope) }.getOrNull()
    return base?.let { Typeface.create(it, 700, false) } ?: Typeface.DEFAULT_BOLD
}

/** Manrope sedang untuk isi keterangan. */
private fun valueTypeface(context: Context): Typeface {
    val base = runCatching { ResourcesCompat.getFont(context, R.font.manrope) }.getOrNull()
    return base?.let { Typeface.create(it, 500, false) } ?: Typeface.DEFAULT
}

/**
 * Menulis kartu ke cache, lalu mengembalikan filenya.
 *
 * Folder cache dipakai karena kartu ini bisa dibuat ulang kapan saja dari foto
 * aslinya; tidak ada gunanya ikut menempati penyimpanan catatan. File lama
 * dibuang lebih dulu supaya folder itu tidak menumpuk kartu dari setiap bagikan.
 */
private fun writeStoryCard(
    context: Context,
    sightingId: Long,
    design: StoryDesign,
    card: Bitmap
): File? {
    val directory = File(context.cacheDir, "story").apply { mkdirs() }
    directory.listFiles()?.forEach { it.delete() }

    val file = File(directory, "story_${sightingId}_${design.name.lowercase()}.png")
    val written = runCatching {
        FileOutputStream(file).use { output ->
            card.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
    }.getOrDefault(false)

    return if (written && file.isFile) file else null
}

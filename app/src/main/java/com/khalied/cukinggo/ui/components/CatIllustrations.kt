package com.khalied.cukinggo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.khalied.cukinggo.R
import com.khalied.cukinggo.ui.theme.BlushPink
import com.khalied.cukinggo.ui.theme.InkSoft
import com.khalied.cukinggo.ui.theme.MintPop
import com.khalied.cukinggo.ui.theme.PeachAccent
import com.khalied.cukinggo.ui.theme.appCardOutline
import kotlin.math.PI
import kotlin.math.sin

/**
 * Kucing tidur untuk empty state: ekornya goyang pelan dan badannya
 * "bernapas" halus.
 */
@Composable
fun SleepingCatIllustration(
    modifier: Modifier = Modifier,
    size: Dp = 150.dp
) {
    val transition = rememberInfiniteTransition(label = "sleeping-cat")
    val tailAngle by transition.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tail"
    )
    val breath by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val bodyColor = PeachAccent
        val detailColor = InkSoft

        // ekor
        rotate(degrees = tailAngle, pivot = Offset(w * 0.74f, h * 0.72f)) {
            drawArc(
                color = bodyColor,
                startAngle = 300f,
                sweepAngle = 150f,
                useCenter = false,
                topLeft = Offset(w * 0.6f, h * 0.44f),
                size = Size(w * 0.34f, h * 0.34f * breath),
                style = Stroke(width = w * 0.075f, cap = StrokeCap.Round)
            )
        }

        // badan
        drawOval(
            color = bodyColor,
            topLeft = Offset(w * 0.14f, h * 0.5f),
            size = Size(w * 0.66f, h * 0.34f * breath)
        )

        // kepala
        drawCircle(color = bodyColor, radius = w * 0.17f, center = Offset(w * 0.28f, h * 0.56f))

        // telinga
        drawPath(
            path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * 0.16f, h * 0.46f)
                lineTo(w * 0.15f, h * 0.36f)
                lineTo(w * 0.26f, h * 0.44f)
                close()
                moveTo(w * 0.40f, h * 0.46f)
                lineTo(w * 0.43f, h * 0.35f)
                lineTo(w * 0.30f, h * 0.44f)
                close()
            },
            color = bodyColor
        )

        // mata terpejam
        drawArc(
            color = detailColor,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(w * 0.18f, h * 0.52f),
            size = Size(w * 0.07f, h * 0.05f),
            style = Stroke(width = w * 0.014f, cap = StrokeCap.Round)
        )
        drawArc(
            color = detailColor,
            startAngle = 20f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(w * 0.30f, h * 0.52f),
            size = Size(w * 0.07f, h * 0.05f),
            style = Stroke(width = w * 0.014f, cap = StrokeCap.Round)
        )

        // hidung mungil
        drawCircle(color = BlushPink, radius = w * 0.018f, center = Offset(w * 0.275f, h * 0.575f))

        // belang badan
        drawArc(
            color = MintPop,
            startAngle = 250f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = Offset(w * 0.42f, h * 0.53f),
            size = Size(w * 0.16f, h * 0.2f),
            style = Stroke(width = w * 0.03f, cap = StrokeCap.Round)
        )
    }
}

/**
 * Loader "kucing mondar-mandir" sambil app nyari lokasi GPS.
 */
@Composable
fun WalkingCatLoader(
    text: String,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "walking-cat")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "walk"
    )
    val wobble by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wobble"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(34.dp)) {
            Text(
                text = "🐈",
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = (progress * 8f - 4f).dp)
                    .rotate(wobble)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

/** Satu lompatan penuh, dari menjejak sampai menyentuh tanah lagi. */
private const val HOP_DURATION_MILLIS = 620

/** Satu putaran naik-turun untuk semua hati. */
private const val HEART_CYCLE_MILLIS = 1500

private const val HEART_COUNT = 5

/** Setengah putaran lambaian kaki depan saat cukingnya berpamitan. */
private const val WAVE_HALF_CYCLE_MILLIS = 520

/** Setengah putaran goyangan ekor saat cukingnya berpamitan. */
private const val TAIL_SWAY_HALF_CYCLE_MILLIS = 900

/**
 * Kerangka satu kartu perayaan: layar di belakangnya diredupkan, dan satu
 * ketukan di mana saja melewatinya.
 *
 * Yang berbeda antara "catatan baru tersimpan" dan "catatan berpamitan" cuma isi
 * kartunya, jadi kerangkanya dipakai bersama supaya cara menutupnya tidak pernah
 * beda di antara dua momen itu. Batas waktunya sendiri dipasang oleh pemanggil,
 * supaya tidak ada yang dipaksa menonton sampai habis.
 */
@Composable
private fun CelebrationOverlay(
    hint: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = appCardOutline()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                content()
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Perayaan setelah satu cuking baru tersimpan: cukingnya melompat di tempat dan
 * hati kecil berhamburan di sekitarnya.
 *
 * Alasan (R-19): menyimpan cuking tidak punya umpan balik lain selain layar
 * yang tiba-tiba tertutup, dan ini satu-satunya saat di app yang memang layak
 * dirayakan. Gerakannya dikurung pada satu objek dan satu lingkaran hati, bukan
 * seluruh layar, jadi dial MOTION 2 tetap terjaga (lihat DESIGN.md bagian
 * "Animasi setelah cuking tersimpan").
 *
 * [streakDays] diisi kalau catatan barusan memanjangkan rentetan harian. Saat
 * itu lencana api dengan angkanya yang jadi berita utamanya, sedangkan gerakannya
 * tetap sama supaya tidak ada kosakata gerak baru yang perlu dipelajari.
 */
@Composable
fun CatSaveCelebration(
    message: String,
    hint: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    streakDays: Int? = null
) {
    // Lencananya muncul sekali dengan lompatan kecil, bukan berdenyut terus:
    // angkanya memang berita di versi ini, tapi lencana yang berdenyut terbaca
    // seperti tombol yang minta ditekan.
    var showStreakBadge by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { showStreakBadge = true }

    CelebrationOverlay(hint = hint, onDismiss = onDismiss, modifier = modifier) {
        if (streakDays != null) {
            AnimatedVisibility(
                visible = showStreakBadge,
                enter = fadeIn() + scaleIn(
                    initialScale = 0.6f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            ) {
                InfoChip(
                    text = stringResource(R.string.streak_chip, streakDays),
                    iconRes = R.drawable.ic_flame,
                    containerColor = PeachAccent,
                    contentColor = InkSoft
                )
            }
        }

        JumpingCatScene(Modifier.size(132.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Pamitan setelah satu cuking dihapus: cukingnya melambai dengan kaki depannya,
 * ekornya bergoyang pelan, lalu layarnya menutup sendiri.
 *
 * Alasan (R-19): penghapusan juga butuh satu tanda kalau sudah benar-benar
 * terjadi, dan dasarnya sama dengan perayaan simpan, cuma nadanya diturunkan:
 * tidak ada lompatan dan tidak ada hati, karena yang baru terjadi bukan sesuatu
 * yang dirayakan.
 */
@Composable
fun CatGoodbyeCelebration(
    message: String,
    hint: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    CelebrationOverlay(hint = hint, onDismiss = onDismiss, modifier = modifier) {
        WavingCatScene(Modifier.size(132.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun JumpingCatScene(modifier: Modifier = Modifier) {
    // Lompatannya dijalankan sebagai animasi yang mengulang sendiri, bukan
    // infinite transition: nilainya harus benar-benar mendarat di 0 tiap
    // putaran supaya badannya sempat memendek sesaat sebelum melompat lagi.
    val hop = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            hop.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = HOP_DURATION_MILLIS, easing = LinearEasing)
            )
            hop.snapTo(0f)
        }
    }

    val transition = rememberInfiniteTransition(label = "hearts")
    val heartProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = HEART_CYCLE_MILLIS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "heartProgress"
    )

    Canvas(modifier = modifier) {
        // Hati digambar lebih dulu supaya cukingnya selalu di depan dan tidak
        // pernah tertutup hatinya sendiri.
        drawHeartBurst(progress = heartProgress)
        drawJumpingCat(hop = hop.value)
    }
}

@Composable
private fun WavingCatScene(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "waving-cat")

    // Lambaiannya diputar dari bahunya, bukan kakinya digeser ke samping: tetap
    // satu kaki yang sama, jadi tidak ada anggota badan yang tiba-tiba muncul.
    val waveAngle by transition.animateFloat(
        initialValue = -20f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = WAVE_HALF_CYCLE_MILLIS,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave"
    )
    val tailAngle by transition.animateFloat(
        initialValue = -6f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = TAIL_SWAY_HALF_CYCLE_MILLIS,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tail"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Bayangan yang sama seperti saat melompat: kakinya menjejak di titik yang
        // sama, jadi kedua perayaan ini terbaca terjadi di tempat yang sama.
        drawOval(
            color = InkSoft.copy(alpha = 0.14f),
            topLeft = Offset(w * 0.32f, h * 0.88f),
            size = Size(w * 0.42f, h * 0.045f)
        )

        drawCat(w = w, h = h, tailAngle = tailAngle, wavingPawAngle = waveAngle)
    }
}

/** Lima hati kecil yang keluar bergiliran dari sekitar badan cuking. */
private fun DrawScope.drawHeartBurst(progress: Float, color: Color = BlushPink) {
    val w = size.width
    val h = size.height

    repeat(HEART_COUNT) { index ->
        // Tiap hati digeser seperlima putaran, jadi hatinya keluar satu per satu
        // dan bukan meledak bersamaan.
        val p = (progress + index / HEART_COUNT.toFloat()) % 1f
        val side = if (index % 2 == 0) -1f else 1f
        val spread = 0.14f + 0.05f * (index / 2).toFloat()

        // Muncul sambil naik, lalu memudar sebelum menyentuh tepi atas kotaknya.
        val alpha = when {
            p < 0.18f -> p / 0.18f
            p > 0.62f -> (1f - p) / 0.38f
            else -> 1f
        }

        drawHeart(
            center = Offset(
                x = w * (0.5f + side * spread * p),
                y = h * 0.78f - h * 0.52f * p
            ),
            radius = h * (0.045f + 0.02f * p),
            color = color,
            alpha = alpha.coerceIn(0f, 1f)
        )
    }
}

private fun DrawScope.drawHeart(center: Offset, radius: Float, color: Color, alpha: Float) {
    val path = Path().apply {
        moveTo(center.x, center.y + radius)
        cubicTo(
            center.x - radius * 2.2f, center.y - radius * 0.4f,
            center.x - radius * 0.9f, center.y - radius * 2.1f,
            center.x, center.y - radius * 0.7f
        )
        cubicTo(
            center.x + radius * 0.9f, center.y - radius * 2.1f,
            center.x + radius * 2.2f, center.y - radius * 0.4f,
            center.x, center.y + radius
        )
        close()
    }
    drawPath(path = path, color = color, alpha = alpha)
}

/**
 * Cuking yang melompat di tempat.
 *
 * [hop] berjalan 0 ke 1 untuk satu lompatan: 0 di tanah, 1 tepat sebelum
 * menyentuh tanah lagi. Tinggi badannya mengikuti busur sinus, dan badannya
 * memanjang saat naik lalu memendek saat menjejak. Gepeng-memanjang itu yang
 * membuat lompatannya terbaca punya berat badan, bukan sekadar gambar yang
 * digeser naik-turun.
 */
private fun DrawScope.drawJumpingCat(hop: Float) {
    val w = size.width
    val h = size.height
    val groundY = h * 0.88f

    val lift = sin(hop * PI).toFloat()
    val stretch = 1f + 0.12f * (lift - 0.5f) * 2f

    // Bayangan di tanah ikut menyusut saat cukingnya naik; tanpa itu,
    // lompatannya cuma terbaca sebagai gambar yang bergeser naik-turun.
    drawOval(
        color = InkSoft.copy(alpha = 0.14f),
        topLeft = Offset(w * (0.32f + 0.06f * lift), groundY),
        size = Size(w * (0.42f - 0.18f * lift), h * 0.045f)
    )

    translate(top = -h * 0.26f * lift) {
        rotate(degrees = 7f * (lift - 0.5f) * 2f, pivot = Offset(w * 0.62f, groundY)) {
            scale(
                scaleX = 0.4f + 0.6f / stretch,
                scaleY = stretch,
                pivot = Offset(w * 0.62f, groundY)
            ) {
                drawCat(
                    w = w,
                    h = h,
                    tailAngle = 18f * (lift - 0.5f) * 2f,
                    wavingPawAngle = null
                )
            }
        }
    }
}

/**
 * Satu cuking utuh, digambar dari kosakata bentuk yang sama dengan
 * [SleepingCatIllustration] supaya semuanya terbaca sebagai cuking yang sama:
 * badan oval, kepada bulat, telinga segitiga, dan belang mint di badannya.
 *
 * [wavingPawAngle] null berarti kedua kaki depannya menjejak tanah. Diisi angka
 * berarti kaki depan kanannya diangkat dan diputar dari bahunya sebanyak itu
 * derajat.
 */
private fun DrawScope.drawCat(
    w: Float,
    h: Float,
    tailAngle: Float,
    wavingPawAngle: Float?
) {
    val bodyColor = PeachAccent
    val detailColor = InkSoft

    // ekor
    rotate(degrees = tailAngle, pivot = Offset(w * 0.80f, h * 0.74f)) {
        drawArc(
            color = bodyColor,
            startAngle = 300f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(w * 0.72f, h * 0.50f),
            size = Size(w * 0.26f, h * 0.26f),
            style = Stroke(width = w * 0.07f, cap = StrokeCap.Round)
        )
    }

    // badan
    drawOval(
        color = bodyColor,
        topLeft = Offset(w * 0.38f, h * 0.62f),
        size = Size(w * 0.44f, h * 0.24f)
    )

    // kaki belakang, warna yang sama jadi menyatu dengan badannya
    drawOval(
        color = bodyColor,
        topLeft = Offset(w * 0.46f, h * 0.80f),
        size = Size(w * 0.10f, h * 0.10f)
    )

    if (wavingPawAngle == null) {
        drawOval(
            color = bodyColor,
            topLeft = Offset(w * 0.62f, h * 0.80f),
            size = Size(w * 0.10f, h * 0.10f)
        )
    } else {
        // Kakinya diangkat lurus ke atas dari bahunya, jadi yang bergerak cuma
        // sudutnya; bentuk kakinya sendiri tetap sama seperti saat menjejak.
        rotate(degrees = wavingPawAngle, pivot = Offset(w * 0.67f, h * 0.85f)) {
            drawOval(
                color = bodyColor,
                topLeft = Offset(w * 0.62f, h * 0.64f),
                size = Size(w * 0.10f, h * 0.21f)
            )
        }
    }

    // kepala
    drawCircle(color = bodyColor, radius = w * 0.15f, center = Offset(w * 0.30f, h * 0.58f))

    // telinga
    drawPath(
        path = Path().apply {
            moveTo(w * 0.185f, h * 0.465f)
            lineTo(w * 0.16f, h * 0.34f)
            lineTo(w * 0.275f, h * 0.43f)
            close()
            moveTo(w * 0.355f, h * 0.43f)
            lineTo(w * 0.42f, h * 0.32f)
            lineTo(w * 0.30f, h * 0.44f)
            close()
        },
        color = bodyColor
    )

    // mata melek: bedanya dengan cuking yang tidur di empty state
    drawCircle(color = detailColor, radius = w * 0.02f, center = Offset(w * 0.24f, h * 0.565f))
    drawCircle(color = detailColor, radius = w * 0.02f, center = Offset(w * 0.36f, h * 0.565f))

    // hidung mungil
    drawCircle(color = BlushPink, radius = w * 0.015f, center = Offset(w * 0.30f, h * 0.625f))

    // belang badan
    drawArc(
        color = MintPop,
        startAngle = 250f,
        sweepAngle = 60f,
        useCenter = false,
        topLeft = Offset(w * 0.50f, h * 0.65f),
        size = Size(w * 0.16f, h * 0.18f),
        style = Stroke(width = w * 0.03f, cap = StrokeCap.Round)
    )
}

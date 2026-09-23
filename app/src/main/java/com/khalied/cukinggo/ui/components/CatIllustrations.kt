package com.khalied.cukinggo.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.khalied.cukinggo.ui.theme.BlushPink
import com.khalied.cukinggo.ui.theme.InkSoft
import com.khalied.cukinggo.ui.theme.MintPop
import com.khalied.cukinggo.ui.theme.PeachAccent

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

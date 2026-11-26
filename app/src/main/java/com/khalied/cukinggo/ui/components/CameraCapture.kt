package com.khalied.cukinggo.ui.components

import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.TapToFocusInfo
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Observer
import com.khalied.cukinggo.R
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.delay

/**
 * Panjang fokus setara 35mm yang dianggap \"kamera biasa\".
 *
 * Sekitar 26 mm: itu jarak pandang lensa utama di hampir semua HP, sedangkan
 * lensa ultra-wide biasanya 13 sampai 18 mm dan telefoto 50 mm ke atas. Jadi
 * pemilih di bawah ini mencari yang paling dekat dengan angka ini.
 */
private const val STANDARD_FOCAL_LENGTH_MM = 26.0

/**
 * Kamera belakang yang paling mendekati lensa utama, bukan sekadar kamera
 * belakang pertama yang ditemukan sistem.
 *
 * `DEFAULT_BACK_CAMERA` memilih kamera belakang pertama menurut urutan yang
 * diberikan perangkat, dan di sebagian HP urutan itu justru mengarah ke lensa
 * ultra-wide, sehingga hasil fotonya terlihat lebih lebar dari yang dilihat mata.
 * Lensa utamanya dikenali dari panjang fokus setaranya, jadi pilihannya tidak
 * bergantung pada urutan itu.
 */
fun standardBackCameraSelector(): CameraSelector = CameraSelector.Builder()
    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
    .addCameraFilter { cameraInfos ->
        val chosen = cameraInfos.minByOrNull { info -> focalDistanceFromStandard(info) }
        if (chosen != null) listOf(chosen) else cameraInfos
    }
    .build()

/**
 * Jarak panjang fokus setara kamera ini dari [STANDARD_FOCAL_LENGTH_MM], kecil
 * berarti lebih dekat ke lensa utama.
 *
 * Yang tidak bisa dibaca sengaja dikasih nilai paling besar supaya tidak menang
 * melawan kamera yang datanya jelas; kalau semua tidak terbaca, `minByOrNull`
 * tetap mengembalikan salah satu, jadi kameranya tidak pernah kosong.
 */
@androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
private fun focalDistanceFromStandard(cameraInfo: CameraInfo): Double = runCatching {
    val camera2Info = Camera2CameraInfo.from(cameraInfo)
    val focalLength = camera2Info
        .getCameraCharacteristic(android.hardware.camera2.CameraCharacteristics
            .LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
        ?.firstOrNull()
    val sensorWidth = camera2Info
        .getCameraCharacteristic(android.hardware.camera2.CameraCharacteristics
            .SENSOR_INFO_PHYSICAL_SIZE)
        ?.width
    if (focalLength == null || sensorWidth == null || sensorWidth <= 0f) {
        Double.MAX_VALUE
    } else {
        val equivalent = focalLength / sensorWidth * 36.0
        abs(equivalent - STANDARD_FOCAL_LENGTH_MM)
    }
}.getOrDefault(Double.MAX_VALUE)

/**
 * Area kamera: pratinjau, tombol zoom, tombol lampu, dan tombol jepret.
 *
 * Dipakai dua layar yang sama-sama memotret: menandai cuking baru, dan mengganti
 * foto catatan yang sudah ada. Karena itu bentuknya satu komponen, bukan disalin
 * dua kali.
 *
 * Semua tombolnya menempel di dalam frame, bukan berbaris di bawahnya. Layar
 * pemanggil bisa ikut menggulir, dan tombol jepret yang harus digulir dulu baru
 * kelihatan bukan kamera: jepret selalu ada tepat di atas pratinjau, di tempat
 * yang sama setiap kali layarnya dibuka.
 *
 * Zoom-nya bisa dicubit langsung di pratinjau, dan tombol angkanya ada supaya
 * kemampuannya kelihatan tanpa harus menebak. Angka yang ditampilkan cuma yang
 * memang didukung kamera, jadi tidak ada tombol zoom yang diam saja.
 *
 * Lampu kilat punya tiga keadaan seperti kamera bawaan: mati, otomatis, dan
 * nyala. Satu tombol yang berganti-ganti, karena tiga tombol terpisah di atas
 * pratinjau menutupi bagian frame yang justru dipakai membidik. Tombolnya hanya
 * muncul kalau perangkatnya memang punya lampu, supaya tidak ada tombol yang
 * diam saja di HP tanpa flash.
 */
@Composable
fun CameraCaptureArea(
    controller: LifecycleCameraController,
    cameraFailed: Boolean,
    onCapture: () -> Unit,
    /** Isyarat dari detektor: cuking sedang terlihat di frame. Jepret tetap manual. */
    catDetected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // Satu getar saat isyaratnya menyala, bukan tiap frame: yang dikabarkan adalah
    // perubahannya, dan getar berulang justru bikin kamera terasa berisik.
    LaunchedEffect(catDetected) {
        if (catDetected) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
    // Perubahan zoom dibaca dari controller, bukan disimpan sendiri-sendiri:
    // controller bisa mereset zoomnya saat kameranya rebind, dan angka di layar
    // harus ikut.
    var zoomState by remember(controller) { mutableStateOf(controller.zoomState.value) }
    DisposableEffect(controller) {
        val observer = Observer<androidx.camera.core.ZoomState> { state -> zoomState = state }
        controller.zoomState.observeForever(observer)
        onDispose { controller.zoomState.removeObserver(observer) }
    }

    // Keadaan lampu disimpan sendiri, bukan dibaca dari controller: pada
    // komposisi pertama kameranya belum ter-bind, jadi belum ada nilai untuk
    // dibaca, dan pilihan pengguna tidak boleh hilang saat pratinjau diulang.
    var flashMode by remember(controller) { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    var flashAvailable by remember(controller) { mutableStateOf(false) }

    LaunchedEffect(controller) {
        // CameraInfo baru ada setelah kamera ter-bind, jadi ditanya berulang
        // sebentar sejak awal layar dibuka.
        repeat(20) {
            flashAvailable = runCatching { controller.cameraInfo?.hasFlashUnit() == true }
                .getOrDefault(false)
            if (flashAvailable) return@LaunchedEffect
            delay(100)
        }
    }

    LaunchedEffect(controller, flashMode) {
        runCatching { controller.imageCaptureFlashMode = flashMode }
    }

    // Fokus ketuk diurus `PreviewView` di dalam controller: satu sentuhan di
    // pratinjau memfokuskan sekaligus mengukur cahaya di titik itu. Cubitan zoom
    // tetap milik kita, jadi pinch-to-zoom bawaan controller tidak dinyalakan.
    LaunchedEffect(controller) {
        controller.isTapToFocusEnabled = true
    }

    // Titik ketuk terakhir dan hasil fokusnya. Ring kecil ini satu-satunya kabar
    // bahwa ketukan tadi benar-benar memfokuskan kamera; tanpa itu pengguna
    // hanya bisa menebak apakah ketukannya masuk.
    var focusPoint by remember(controller) { mutableStateOf<Offset?>(null) }
    var focusLocked by remember(controller) { mutableStateOf(false) }
    DisposableEffect(controller) {
        val observer = Observer<TapToFocusInfo> { info ->
            focusLocked = info.focusState == CameraController.TAP_TO_FOCUS_FOCUSED
            val tapPoint = info.tapPoint
            focusPoint = if (
                tapPoint == null || info.focusState == CameraController.TAP_TO_FOCUS_NOT_STARTED
            ) {
                null
            } else {
                Offset(tapPoint.x, tapPoint.y)
            }
        }
        controller.tapToFocusInfoState.observeForever(observer)
        onDispose { controller.tapToFocusInfoState.removeObserver(observer) }
    }
    // Ringnya hilang sendiri: kamera sudah mengunci fokus di titik itu, dan
    // lingkaran yang menetap di atas pratinjau terbaca seperti noda di lensa.
    LaunchedEffect(focusPoint) {
        if (focusPoint != null) {
            delay(1_200L)
            focusPoint = null
            focusLocked = false
        }
    }

    val minZoom = zoomState?.minZoomRatio ?: 1f
    val maxZoom = zoomState?.maxZoomRatio ?: 1f
    val currentZoom = zoomState?.zoomRatio ?: minZoom

    val zoomSteps = remember(minZoom, maxZoom) {
        if (maxZoom - minZoom < 0.1f) {
            emptyList()
        } else {
            buildList {
                add(1f)
                listOf(2f, 3f).forEach { step -> if (step <= maxZoom) add(step) }
                add(maxZoom)
            }.filter { step -> step >= minZoom }
                .distinct()
                .sorted()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                // Cubit untuk zoom. Dipasang di wadahnya, bukan di PreviewView,
                // supaya tidak bergantung pada cara view kamera menelan sentuhan.
                .pointerInput(controller) {
                    detectTransformGestures { _, _, zoomChange, _ ->
                        val state = controller.zoomState.value ?: return@detectTransformGestures
                        val next = (state.zoomRatio * zoomChange)
                            .coerceIn(state.minZoomRatio, state.maxZoomRatio)
                        controller.setZoomRatio(next)
                    }
                }
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    PreviewView(context).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        this.controller = controller
                    }
                }
            )
            CatTargetOverlay(
                detected = catDetected,
                modifier = Modifier.matchParentSize()
            )
            val ringPoint = focusPoint
            if (ringPoint != null) {
                // Warna ring diambil di luar Canvas supaya ikut tema; saat fokus
                // terkunci ia memakai warna utama, selagi mencari tetap putih
                // karena pratinjau bisa terang atau gelap.
                val ringColor = if (focusLocked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.White
                }
                Canvas(modifier = Modifier.matchParentSize()) {
                    val radius = 30.dp.toPx()
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.4f),
                        radius = radius,
                        center = ringPoint,
                        style = Stroke(width = 5.dp.toPx())
                    )
                    drawCircle(
                        color = ringColor,
                        radius = radius,
                        center = ringPoint,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
            if (cameraFailed) {
                Text(
                    text = stringResource(R.string.add_error_camera),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }

            // Kontrol ditaruh di dalam frame, menumpang di atas pratinjau. Urutannya
            // dari atas ke bawah: pilihan zoom, lalu lampu dan jepret sebaris.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (zoomSteps.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        zoomSteps.forEach { step ->
                            OptionChip(
                                label = formatZoom(step),
                                selected = abs(currentZoom - step) < 0.05f,
                                onClick = { controller.setZoomRatio(step) }
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    if (flashAvailable) {
                        FlashModeButton(
                            mode = flashMode,
                            onCycle = { flashMode = nextFlashMode(flashMode) },
                            modifier = Modifier.align(Alignment.CenterStart)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            // Garis tepi warna permukaan memisahkan tombol dari
                            // pratinjau yang bisa terang atau gelap di belakangnya.
                            .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .clickable(onClick = onCapture),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_paw),
                            contentDescription = stringResource(R.string.add_capture),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        Text(
            text = stringResource(R.string.add_camera_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.add_camera_zoom_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.add_camera_focus_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Tombol lampu di dalam frame: satu ketukan memutar mati, auto, nyala, kembali
 * ke mati. Labelnya menyebut keadaan yang sedang berlaku, bukan keadaan
 * berikutnya, karena yang perlu dibaca sekilas adalah lampunya sekarang menyala
 * atau tidak.
 */
@Composable
private fun FlashModeButton(
    mode: Int,
    onCycle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = when (mode) {
        ImageCapture.FLASH_MODE_AUTO -> R.string.add_flash_auto
        ImageCapture.FLASH_MODE_ON -> R.string.add_flash_on
        else -> R.string.add_flash_off
    }
    val active = mode != ImageCapture.FLASH_MODE_OFF
    Surface(
        modifier = modifier.clickable(
            onClickLabel = stringResource(R.string.add_flash_change),
            role = Role.Button,
            onClick = onCycle
        ),
        shape = CircleShape,
        color = if (active) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = if (active) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    ) {
        // 12dp vertikal, bukan 10 seperti OptionChip: tombol ini berdiri sendiri
        // di atas pratinjau, dan tingginya perlu tetap 44dp ke atas.
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = stringResource(label), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Urutan putaran tombol lampu: mati ke auto ke nyala, lalu kembali ke mati. */
private fun nextFlashMode(mode: Int): Int = when (mode) {
    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
    ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
    else -> ImageCapture.FLASH_MODE_OFF
}

/**
 * Tombol pilihan satu nilai, dipakai untuk tingkat zoom.
 * Minimal 44dp tingginya supaya lolos tap target, sama seperti chip pilihan lain
 * di app ini.
 */
@Composable
internal fun OptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.selectable(selected = selected, onClick = onClick),
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** \"1x\", \"2.5x\": angka bulat tidak perlu koma, sisanya satu angka di belakang. */
private fun formatZoom(ratio: Float): String {
    val text = if (abs(ratio - ratio.toInt()) < 0.05f) {
        ratio.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", ratio)
    }
    return "${text}x"
}

/**
 * Frame bidik di atas pratinjau kamera.
 *
 * Empat sudutnya selalu ada supaya pengguna tahu area yang dipantau, dan satu
 * baris di atasnya menyebut keadaannya: belum ada cuking, atau cuking terlihat.
 * Barisnya tidak pernah kosong, karena indikator yang cuma muncul sesekali
 * terbaca seperti indikator yang hilang, bukan seperti cuking yang belum masuk
 * frame. Yang dikabarkan cuma kehadiran, bukan kotak posisinya: jepret tetap
 * manual, jadi kotak yang bergerak mengikuti cuking akan terbaca seperti kamera
 * yang memilih sendiri.
 *
 * Sudutnya digambar dua kali, garis gelap lebih tebal di bawah garis berwarna.
 * Pratinjau kamera bisa terang atau gelap, dan satu warna saja tidak bisa
 * dijamin terlihat di keduanya.
 */
@Composable
internal fun CatTargetOverlay(
    detected: Boolean,
    modifier: Modifier = Modifier
) {
    val lineColor = if (detected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val lineAlpha by animateFloatAsState(
        targetValue = if (detected) 1f else 0.55f,
        label = "catTargetAlpha"
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val inset = 18.dp.toPx()
            val length = 26.dp.toPx()
            val stroke = 3.dp.toPx()
            val underlay = 7.dp.toPx()
            val right = size.width - inset
            val bottom = size.height - inset

            val corners = listOf(
                listOf(
                    Offset(inset, inset + length),
                    Offset(inset, inset),
                    Offset(inset + length, inset)
                ),
                listOf(
                    Offset(right - length, inset),
                    Offset(right, inset),
                    Offset(right, inset + length)
                ),
                listOf(
                    Offset(inset, bottom - length),
                    Offset(inset, bottom),
                    Offset(inset + length, bottom)
                ),
                listOf(
                    Offset(right - length, bottom),
                    Offset(right, bottom),
                    Offset(right, bottom - length)
                )
            )

            corners.forEach { corner ->
                for (index in 0 until corner.lastIndex) {
                    drawLine(
                        color = Color.Black.copy(alpha = 0.45f),
                        start = corner[index],
                        end = corner[index + 1],
                        strokeWidth = underlay,
                        cap = StrokeCap.Round
                    )
                }
                for (index in 0 until corner.lastIndex) {
                    drawLine(
                        color = lineColor.copy(alpha = lineAlpha),
                        start = corner[index],
                        end = corner[index + 1],
                        strokeWidth = stroke,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Barisnya ada dari awal, bukan baru muncul saat isyaratnya menyala:
        // layar yang diam-diam belum menampilkan apa-apa tidak bisa dibedakan
        // dari layar yang indikatornya rusak, dan itu yang bikin terasa hilang.
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 14.dp),
            shape = CircleShape,
            color = if (detected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (detected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        ) {
            Text(
                text = stringResource(
                    if (detected) {
                        R.string.add_camera_cat_seen
                    } else {
                        R.string.add_camera_cat_searching
                    }
                ),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

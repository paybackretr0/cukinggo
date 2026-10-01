package com.khalied.cukinggo.ui.components

import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Observer
import com.khalied.cukinggo.R
import java.util.Locale
import kotlin.math.abs

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
@OptIn(ExperimentalCamera2Interop::class)
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
 * Area kamera: pratinjau, tombol zoom, petunjuk, dan tombol jepret.
 *
 * Dipakai dua layar yang sama-sama memotret: menandai cuking baru, dan mengganti
 * foto catatan yang sudah ada. Karena itu bentuknya satu komponen, bukan disalin
 * dua kali.
 *
 * Zoom-nya bisa dicubit langsung di pratinjau, dan tombol angkanya ada supaya
 * kemampuannya kelihatan tanpa harus menebak. Angka yang ditampilkan cuma yang
 * memang didukung kamera, jadi tidak ada tombol zoom yang diam saja.
 */
@Composable
fun CameraCaptureArea(
    controller: LifecycleCameraController,
    cameraFailed: Boolean,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Perubahan zoom dibaca dari controller, bukan disimpan sendiri-sendiri:
    // controller bisa mereset zoomnya saat kameranya rebind, dan angka di layar
    // harus ikut.
    var zoomState by remember(controller) { mutableStateOf(controller.zoomState.value) }
    DisposableEffect(controller) {
        val observer = Observer<androidx.camera.core.ZoomState> { state -> zoomState = state }
        controller.zoomState.observeForever(observer)
        onDispose { controller.zoomState.removeObserver(observer) }
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
        }

        if (zoomSteps.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                zoomSteps.forEach { step ->
                    ZoomChip(
                        label = formatZoom(step),
                        selected = abs(currentZoom - step) < 0.05f,
                        onClick = { controller.setZoomRatio(step) }
                    )
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

        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
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

/**
 * Tombol satu tingkat zoom. Minimal 44dp tingginya supaya lolos tap target, sama
 * seperti chip pilihan lain di app ini.
 */
@Composable
private fun ZoomChip(
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

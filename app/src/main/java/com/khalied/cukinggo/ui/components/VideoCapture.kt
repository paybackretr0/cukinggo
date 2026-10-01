package com.khalied.cukinggo.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.video.AudioConfig
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.khalied.cukinggo.R
import com.khalied.cukinggo.appContainer
import com.khalied.cukinggo.util.openAppSettings
import java.io.File
import java.util.Locale
import kotlinx.coroutines.delay

/**
 * Perekam video pendamping: pratinjau, tombol rekam/stop, dan hitungan waktunya.
 *
 * Bentuknya satu komponen utuh, bukan potongan yang disusun lagi di tiap layar,
 * karena dua layar memakainya (menandai cuking baru dan mengedit catatan) dan
 * urutan pengelolaannya (izin mikrofon, mulai, stop, serahkan filenya) sama
 * persis di keduanya.
 *
 * Mikrofon diminta di sini, bukan di gerbang izin layar: izin itu baru berguna
 * saat pengguna memang memilih merekam, dan memintanya lebih awal cuma menambah
 * satu dialog di alur satu jepret.
 */
@Composable
fun VideoRecorder(
    onRecorded: (File) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val container = remember { context.appContainer }

    var micGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraFailed by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    // Rekaman yang sedang berjalan. CameraController tidak menyediakan stop,
    // jadi yang dipegang adalah objek Recording-nya dan stop dipanggil dari situ.
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var elapsedMillis by remember { mutableLongStateOf(0L) }
    // File yang sedang/baru direkam. Dibuang saat keluar kalau rekamannya tidak
    // selesai, supaya cache tidak menumpuk sisa rekaman setengah jalan.
    var activeFile by remember { mutableStateOf<File?>(null) }
    // Ditandai saat rekaman selesai dan filenya diserahkan ke layar pemanggil:
    // mulai saat itu filenya bukan milik komponen ini lagi.
    var handedOff by remember { mutableStateOf(false) }

    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> micGranted = granted }

    LaunchedEffect(Unit) {
        if (!micGranted) micLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.VIDEO_CAPTURE)
            // Lensa utama, sama seperti kamera foto: video yang lebarnya beda dari
            // yang dilihat mata bikin hasilnya terasa dari kamera lain.
            cameraSelector = standardBackCameraSelector()
        }
    }

    DisposableEffect(lifecycleOwner) {
        runCatching { controller.bindToLifecycle(lifecycleOwner) }
            .onFailure { cameraFailed = true }
        onDispose {
            runCatching { activeRecording?.stop() }
            runCatching { activeRecording?.close() }
            if (!handedOff) container.imageStorageHelper.discardCapture(activeFile)
            controller.unbind()
        }
    }

    LaunchedEffect(recording) {
        if (recording) {
            elapsedMillis = 0L
            while (true) {
                delay(1_000L)
                elapsedMillis += 1_000L
            }
        }
    }

    val startRecording = {
        val file = container.imageStorageHelper.createVideoCaptureFile()
        activeFile = file
        cameraFailed = false
        recording = true
        val options = FileOutputOptions.Builder(file).build()
        activeRecording = controller.startRecording(
            options,
            AudioConfig.create(true),
            ContextCompat.getMainExecutor(context)
        ) { event ->
            when (event) {
                is VideoRecordEvent.Finalize -> {
                    recording = false
                    activeRecording = null
                    if (event.hasError()) {
                        file.delete()
                        activeFile = null
                        cameraFailed = true
                    } else {
                        handedOff = true
                        onRecorded(file)
                    }
                }

                else -> Unit
            }
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
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    PreviewView(viewContext).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        this.controller = controller
                    }
                }
            )
            if (recording) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.video_recording),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = formatElapsed(elapsedMillis),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
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
        }

        if (!micGranted) {
            Text(
                text = stringResource(R.string.video_error_mic),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            TextButton(onClick = { context.openAppSettings() }) {
                Text(
                    text = stringResource(R.string.permission_open_settings),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Text(
            text = stringResource(R.string.video_camera_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text(
                    text = stringResource(R.string.action_cancel),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Rekam cuma jalan kalau suaranya boleh ikut: tanpa izin itu CameraX
            // menggagalkan rekamannya, jadi tombolnya jujur dimatikan dulu dan
            // alasannya sudah tertulis di atas.
            val canRecord = micGranted && !cameraFailed
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        if (recording) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primary
                    )
                    .then(
                        if (canRecord || recording) {
                            Modifier.clickable {
                                if (recording) {
                                    activeRecording?.stop()
                                } else {
                                    startRecording()
                                }
                            }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (recording) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.error)
                            .border(2.dp, MaterialTheme.colorScheme.error, MaterialTheme.shapes.small)
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_paw),
                        contentDescription = stringResource(R.string.video_record_start),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

/** "0:07", "1:12": menit dulu, detiknya selalu dua angka. */
private fun formatElapsed(millis: Long): String {
    val totalSeconds = millis / 1_000L
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

/**
 * Bagian video di layar review/edit: judul, kalimat penjelas, dan isinya.
 *
 * Bentuknya satu komponen supaya layar "tandai cuking" dan layar edit tidak
 * punya dua versi bagian yang sama. Yang berbeda cuma file mana yang ditunjuk,
 * dan itu diterima lewat [videoPath].
 *
 * [newlyRecorded] membedakan barusan direkam (belum ikut tersimpan) dari video
 * yang sudah ada di catatan, karena keduanya perlu kalimat yang berbeda: yang
 * pertama menunggu tombol Simpan, yang kedua sudah aman sampai diubah.
 */
@Composable
fun VideoSection(
    videoPath: String?,
    onAddVideo: () -> Unit,
    onRemoveVideo: () -> Unit,
    modifier: Modifier = Modifier,
    newlyRecorded: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.video_section_label),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stringResource(R.string.video_section_support),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (videoPath == null) {
            TextButton(onClick = onAddVideo) {
                Text(
                    text = stringResource(R.string.video_add),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            VideoPlayer(
                videoPath = videoPath,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(MaterialTheme.shapes.large)
            )
            if (newlyRecorded) {
                Text(
                    text = stringResource(R.string.video_ready_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onAddVideo) {
                    Text(
                        text = stringResource(R.string.video_change),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                TextButton(onClick = onRemoveVideo) {
                    Text(
                        text = stringResource(R.string.video_remove),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Pemutar video pendamping.
 *
 * Memakai `VideoView` bawaan platform, bukan pemutar pihak ketiga: yang diputar
 * selalu file mp4 lokal buatan kamera sendiri, dan kontrolnya (putar, jeda,
 * geser) sudah disediakan `MediaController`. Menambah satu pustaka pemutar
 * penuh untuk satu file lokal tidak sebanding dengan bobotnya.
 */
@Composable
fun VideoPlayer(
    videoPath: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            VideoView(viewContext).apply {
                setVideoURI(Uri.fromFile(File(videoPath)))
                // Kontrolnya dipasang, tapi tidak dipaksa muncul sendiri: pemutar
                // yang membuka kontrolnya tanpa diminta menutupi videonya sendiri.
                // Ketukan di video yang memunculkannya, seperti pemutar biasa.
                val media = MediaController(viewContext)
                media.setAnchorView(this)
                setMediaController(media)
            }
        }
    )
}

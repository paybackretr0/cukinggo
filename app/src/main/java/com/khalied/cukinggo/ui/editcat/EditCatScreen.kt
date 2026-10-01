package com.khalied.cukinggo.ui.editcat

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.khalied.cukinggo.R
import com.khalied.cukinggo.appContainer
import com.khalied.cukinggo.ui.components.CameraCaptureArea
import com.khalied.cukinggo.ui.components.PlayfulTopBar
import com.khalied.cukinggo.ui.components.VideoRecorder
import com.khalied.cukinggo.ui.components.VideoSection
import com.khalied.cukinggo.ui.components.WalkingCatLoader
import com.khalied.cukinggo.ui.components.standardBackCameraSelector
import com.khalied.cukinggo.ui.theme.InkSoft
import com.khalied.cukinggo.ui.theme.PeachAccent
import com.khalied.cukinggo.ui.theme.appCardOutline
import com.khalied.cukinggo.util.hasCameraPermission
import com.khalied.cukinggo.util.openAppSettings
import java.io.File

/**
 * Layar edit satu penemuan: nama panggilan cukingnya, catatan kegiatannya, dan
 * fotonya.
 *
 * Ketiganya dijadikan satu layar, bukan tiga aksi terpisah, karena yang diubah
 * pengguna biasanya memang satu catatan yang sama: dia ingat namanya, ingat
 * kegiatannya, dan mungkin mau mengganti foto yang kurang jelas. Foto baru baru
 * dipindahkan ke penyimpanan tetap saat Simpan ditekan, jadi keluar dari layar
 * ini tanpa menyimpan tidak meninggalkan file sisa.
 */
@Composable
fun EditCatScreen(
    viewModel: EditCatViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val container = remember(context) { context.appContainer }
    val lifecycleOwner = LocalLifecycleOwner.current

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var newPhotoCapture by remember { mutableStateOf<File?>(null) }
    var newVideoCapture by remember { mutableStateOf<File?>(null) }
    var showVideoRecorder by remember { mutableStateOf(false) }
    // Video lama yang diminta dibuang pengguna. Baru diterapkan ke database saat
    // Simpan ditekan, karena itulah satu-satunya saat perubahan di layar ini
    // benar-benar disimpan.
    var existingVideoRemoved by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }
    var cameraFailed by remember { mutableStateOf(false) }
    var cameraGranted by remember { mutableStateOf(context.hasCameraPermission()) }
    var cameraAsked by remember { mutableStateOf(false) }
    // Isian form cuma diambil sekali dari catatannya. Kalau diambil tiap kali
    // uiState berganti, ketikan pengguna akan tertimpa nilainya sendiri setiap
    // kali simpan gagal.
    var seeded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is EditCatUiState.Ready -> if (!seeded) {
                name = state.form.name
                description = state.form.description
                seeded = true
            }

            EditCatUiState.Saved -> onSaved()
            else -> Unit
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraGranted = granted
        cameraAsked = true
        if (granted) capturing = true
    }

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            cameraSelector = standardBackCameraSelector()
        }
    }

    DisposableEffect(lifecycleOwner, capturing, cameraGranted) {
        if (capturing && cameraGranted) {
            runCatching { controller.bindToLifecycle(lifecycleOwner) }
                .onFailure { cameraFailed = true }
        }
        onDispose { controller.unbind() }
    }

    val takePhoto = {
        val file = container.imageStorageHelper.createCaptureFile()
        cameraFailed = false
        controller.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    newPhotoCapture = file
                    capturing = false
                }

                override fun onError(exception: ImageCaptureException) {
                    cameraFailed = true
                    file.delete()
                }
            }
        )
    }

    // Kembali dari Pengaturan izin: status kamera dicek ulang, karena pengguna
    // bisa saja baru mengizinkannya di sana.
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                cameraGranted = context.hasCameraPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlayfulTopBar(title = stringResource(R.string.edit_title), onBack = onBack)

            when (val state = uiState) {
                EditCatUiState.Loading -> EditPlaceholder(
                    loaderText = stringResource(R.string.edit_loading)
                )

                EditCatUiState.Missing -> EditPlaceholder(
                    message = stringResource(R.string.edit_missing)
                )

                EditCatUiState.Failed -> EditPlaceholder(
                    message = stringResource(R.string.error_load_title),
                    actionLabel = stringResource(R.string.add_retry),
                    onAction = viewModel::retry
                )

                is EditCatUiState.Ready -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = appCardOutline()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (capturing && cameraGranted) {
                                CameraCaptureArea(
                                    controller = controller,
                                    cameraFailed = cameraFailed,
                                    onCapture = takePhoto
                                )
                            } else {
                                AsyncImage(
                                    model = newPhotoCapture ?: File(state.form.photoPath),
                                    contentDescription = stringResource(R.string.cd_cat_photo),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clip(MaterialTheme.shapes.large)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = {
                                        if (cameraGranted) {
                                            capturing = true
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    }) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_paw),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.size(6.dp))
                                        Text(
                                            text = stringResource(R.string.edit_change_photo),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                if (cameraAsked && !cameraGranted) {
                                    Text(
                                        text = stringResource(R.string.permission_denied),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(onClick = { context.openAppSettings() }) {
                                        Text(
                                            text = stringResource(R.string.permission_open_settings),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else if (cameraGranted) {
                                    Text(
                                        text = stringResource(R.string.add_camera_zoom_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (newPhotoCapture != null) {
                                Text(
                                    text = stringResource(R.string.edit_new_photo_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    VideoSection(
                        videoPath = newVideoCapture?.absolutePath
                            ?: state.form.videoPath.takeUnless { existingVideoRemoved },
                        newlyRecorded = newVideoCapture != null,
                        onAddVideo = { showVideoRecorder = true },
                        onRemoveVideo = {
                            if (newVideoCapture != null) {
                                container.imageStorageHelper.discardCapture(newVideoCapture)
                                newVideoCapture = null
                            } else {
                                existingVideoRemoved = true
                            }
                        }
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = {
                            Text(
                                text = stringResource(R.string.edit_name_label),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.edit_name_hint),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        shape = MaterialTheme.shapes.large
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text(
                                text = stringResource(R.string.edit_activity_label),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.edit_activity_hint),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        shape = MaterialTheme.shapes.large,
                        maxLines = 3
                    )

                    if (state.error) {
                        Text(
                            text = stringResource(R.string.edit_error_save),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.save(
                                name = name,
                                description = description,
                                newPhotoCapture = newPhotoCapture,
                                newVideoCapture = newVideoCapture,
                                removeVideo = existingVideoRemoved && newVideoCapture == null
                            )
                        },
                        enabled = !state.saving && !capturing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PeachAccent,
                            contentColor = InkSoft
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.edit_save),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                EditCatUiState.Saved -> EditPlaceholder(
                    loaderText = stringResource(R.string.edit_saving)
                )
            }
        }

        if (showVideoRecorder) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                PlayfulTopBar(
                    title = stringResource(R.string.video_add),
                    onBack = { showVideoRecorder = false }
                )
                VideoRecorder(
                    onRecorded = { file ->
                        newVideoCapture = file
                        showVideoRecorder = false
                    },
                    onCancel = { showVideoRecorder = false },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .navigationBarsPadding()
                )
            }
        }
    }
}

/** Tampilan untuk keadaan selain \"form siap\": memuat, sudah hilang, atau gagal. */
@Composable
private fun EditPlaceholder(
    modifier: Modifier = Modifier,
    loaderText: String? = null,
    message: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (loaderText != null) {
                WalkingCatLoader(text = loaderText)
            }
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            if (actionLabel != null && onAction != null) {
                Button(
                    onClick = onAction,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(text = actionLabel)
                }
            }
        }
    }
}

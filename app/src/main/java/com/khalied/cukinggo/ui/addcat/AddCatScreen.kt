package com.khalied.cukinggo.ui.addcat

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.khalied.cukinggo.R
import com.khalied.cukinggo.appContainer
import com.khalied.cukinggo.ui.components.CatSaveCelebration
import com.khalied.cukinggo.ui.components.PermissionCard
import com.khalied.cukinggo.ui.components.PlayfulTopBar
import com.khalied.cukinggo.ui.components.WalkingCatLoader
import com.khalied.cukinggo.ui.theme.InkSoft
import com.khalied.cukinggo.ui.theme.appCardOutline
import com.khalied.cukinggo.ui.theme.PeachAccent
import com.khalied.cukinggo.util.hasCameraPermission
import com.khalied.cukinggo.util.openAppSettings
import java.io.File
import kotlinx.coroutines.delay

/**
 * Jeda sebelum kamera menjepret sendiri saat dibuka dari tombol jepret di widget.
 *
 * Angkanya pilihan kasar: kamera perlu waktu menyala setelah di-bind, dan
 * memotret terlalu cepat bisa menghasilkan bingkai gelap. Belum diukur di
 * perangkat, jadi kalau ternyata fotonya masih gelap, angka inilah yang perlu
 * dinaikkan.
 */
private const val AUTO_CAPTURE_DELAY_MILLIS = 900L

/**
 * Lama animasi perayaan disimpan sebelum layar ini menutup sendiri.
 *
 * Sekitar satu setengah detik: cukup untuk melihat cukingnya melompat dua kali,
 * tapi belum terasa seperti menunggu. Ketukan di mana saja melompatinya, jadi
 * angka ini cuma batas paling lama, bukan waktu tontonan yang wajib.
 */
private const val CELEBRATION_DURATION_MILLIS = 1500L

/**
 * [autoCapture] dipakai tombol jepret di widget: kameranya langsung menjepret
 * sendiri begitu siap, jadi dari layar utama cukup satu ketukan. Tombol "Tandai
 * cuking" di Home membiarkannya false dan tetap menunggu jepretan dari pengguna.
 */
@Composable
fun AddCatScreen(
    viewModel: AddCatViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    autoCapture: Boolean = false
) {
    val context = LocalContext.current
    val container = remember { context.appContainer }
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraGranted by remember { mutableStateOf(context.hasCameraPermission()) }
    var locationGranted by remember {
        mutableStateOf(container.locationHelper.hasLocationPermission())
    }
    var permissionsAsked by remember { mutableStateOf(false) }
    var captureFile by remember { mutableStateOf<File?>(null) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var cameraFailed by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionsAsked = true
        cameraGranted = context.hasCameraPermission()
        locationGranted = container.locationHelper.hasLocationPermission()
    }

    // Setelah user balik dari Pengaturan, status izin dicek ulang.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                cameraGranted = context.hasCameraPermission()
                locationGranted = container.locationHelper.hasLocationPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Setelah tersimpan, layarnya ditahan sebentar untuk merayakannya. Satu
    // penanda dipakai bersama supaya ketukan dan batas waktu tidak sama-sama
    // memanggil onSaved, yang akan memundurkan dua layar sekaligus.
    var handedOff by remember { mutableStateOf(false) }
    val finishSaving = {
        if (!handedOff) {
            handedOff = true
            onSaved()
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is AddCatUiState.Saved) {
            delay(CELEBRATION_DURATION_MILLIS)
            finishSaving()
        }
    }

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    // Kamera dilepas saat user sedang mereview foto supaya tidak jalan terus.
    DisposableEffect(lifecycleOwner, cameraGranted, captureFile == null) {
        if (cameraGranted && captureFile == null) {
            runCatching { controller.bindToLifecycle(lifecycleOwner) }
                .onFailure { cameraFailed = true }
        }
        onDispose { controller.unbind() }
    }

    val takePhoto = {
        val file = container.imageStorageHelper.createCaptureFile()
        cameraFailed = false
        val options = ImageCapture.OutputFileOptions.Builder(file).build()
        controller.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    captureFile = file
                }

                override fun onError(exception: ImageCaptureException) {
                    cameraFailed = true
                    file.delete()
                }
            }
        )
    }

    // Jepretan otomatis cuma sekali per kunjungan: kalau hasilnya jelek, "Ambil
    // ulang" tetap ada, dan pengguna yang menekannya memang sedang sengaja
    // mengulang, jadi jangan dijepretkan lagi diam-diam.
    var autoCaptureDone by remember { mutableStateOf(false) }
    LaunchedEffect(autoCapture, cameraGranted, captureFile, cameraFailed) {
        if (!autoCapture || autoCaptureDone) return@LaunchedEffect
        if (!cameraGranted || captureFile != null || cameraFailed) return@LaunchedEffect
        // Ditandai sebelum menunggu, supaya kamera yang belum siap tidak memicu
        // penjadwalan jepretan kedua lewat perubahan status di tengah penantian.
        autoCaptureDone = true
        delay(AUTO_CAPTURE_DELAY_MILLIS)
        takePhoto()
    }

    val busy = uiState is AddCatUiState.Locating || uiState is AddCatUiState.Saving

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlayfulTopBar(title = stringResource(R.string.add_title), onBack = onBack)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!cameraGranted || !locationGranted) {
                    PermissionCard(
                        cameraGranted = cameraGranted,
                        locationGranted = locationGranted,
                        showSettingsHint = permissionsAsked,
                        onRequest = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        onOpenSettings = { context.openAppSettings() }
                    )
                } else if (captureFile == null) {
                    CameraCaptureArea(
                        controller = controller,
                        cameraFailed = cameraFailed,
                        onCapture = takePhoto
                    )
                } else {
                    PhotoReview(
                        photoFile = captureFile!!,
                        name = name,
                        onNameChange = { name = it },
                        description = description,
                        onDescriptionChange = { description = it },
                        onRetake = {
                            container.imageStorageHelper.discardCapture(captureFile)
                            captureFile = null
                            viewModel.clearError()
                        }
                    )

                    when (val state = uiState) {
                        is AddCatUiState.Error -> ErrorCard(
                            message = stringResource(state.messageRes),
                            onRetry = { viewModel.saveCat(captureFile, name, description) },
                            onDismiss = viewModel::clearError
                        )

                        else -> Button(
                            onClick = { viewModel.saveCat(captureFile, name, description) },
                            enabled = !busy,
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
                                text = stringResource(R.string.add_save),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }

                when (uiState) {
                    AddCatUiState.Locating -> WalkingCatLoader(
                        text = stringResource(R.string.add_locating),
                        modifier = Modifier.fillMaxWidth()
                    )

                    AddCatUiState.Saving -> WalkingCatLoader(
                        text = stringResource(R.string.add_saving),
                        modifier = Modifier.fillMaxWidth()
                    )

                    else -> Unit
                }
            }
        }

        // Digambar paling akhir supaya menutupi form yang baru diisi, bukan
        // menggeser isinya. Kalau catatan barusan memanjangkan rentetan harian,
        // pesannya berganti; gambar dan gerakannya tetap sama.
        val saved = uiState as? AddCatUiState.Saved
        if (saved != null) {
            CatSaveCelebration(
                message = stringResource(
                    if (saved.streakDays != null) {
                        R.string.add_saved_streak
                    } else {
                        R.string.add_saved_celebration
                    }
                ),
                hint = stringResource(R.string.celebration_tap_hint),
                streakDays = saved.streakDays,
                onDismiss = finishSaving
            )
        }
    }
}

@Composable
private fun CameraCaptureArea(
    controller: LifecycleCameraController,
    cameraFailed: Boolean,
    onCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
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

        Text(
            text = stringResource(R.string.add_camera_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(PeachAccent)
                .clickable(onClick = onCapture),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_paw),
                contentDescription = stringResource(R.string.add_capture),
                tint = InkSoft,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun PhotoReview(
    photoFile: File,
    name: String,
    onNameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    onRetake: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = appCardOutline()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            AsyncImage(
                model = photoFile,
                contentDescription = stringResource(R.string.cd_cat_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.large)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = {
                    Text(
                        text = stringResource(R.string.add_name_label),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.add_name_hint),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                supportingText = {
                    Text(
                        text = stringResource(R.string.add_name_support),
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                shape = MaterialTheme.shapes.large
            )
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.add_description_hint),
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                shape = MaterialTheme.shapes.large,
                maxLines = 3
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onRetake) {
                    Text(
                        text = stringResource(R.string.add_retake),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRetry,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = stringResource(R.string.add_retry))
                }
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        color = MaterialTheme.colorScheme.onTertiary
                    )
                }
            }
        }
    }
}

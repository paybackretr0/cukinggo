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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.khalied.cukinggo.ui.components.CameraCaptureArea
import com.khalied.cukinggo.ui.components.CatSaveCelebration
import com.khalied.cukinggo.ui.components.InfoChip
import com.khalied.cukinggo.ui.components.VideoSection
import com.khalied.cukinggo.ui.components.standardBackCameraSelector
import com.khalied.cukinggo.ui.components.PermissionCard
import com.khalied.cukinggo.ui.components.PlayfulTopBar
import com.khalied.cukinggo.ui.components.VideoRecorder
import com.khalied.cukinggo.ui.components.WalkingCatLoader
import com.khalied.cukinggo.ui.components.topBarContentPadding
import com.khalied.cukinggo.ui.components.rememberCatDetection
import com.khalied.cukinggo.ui.theme.appCardOutline
import com.khalied.cukinggo.ui.theme.brutalShadow
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
 *
 * Layar yang sama melayani dua hal, dan yang membedakannya cuma [catId]: nol
 * berarti cuking baru, selain itu berarti menambah penemuan ke cuking yang sudah
 * ada. Saat menambah penemuan, nama tidak ditanyakan lagi karena satu cuking
 * cuma punya satu nama.
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
    val targetCat by viewModel.targetCat.collectAsStateWithLifecycle()

    var cameraGranted by remember { mutableStateOf(context.hasCameraPermission()) }
    var locationGranted by remember {
        mutableStateOf(container.locationHelper.hasLocationPermission())
    }
    var permissionsAsked by remember { mutableStateOf(false) }
    var captureFile by remember { mutableStateOf<File?>(null) }
    var videoCapture by remember { mutableStateOf<File?>(null) }
    var showVideoRecorder by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var cameraFailed by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stickerState by viewModel.stickerState.collectAsStateWithLifecycle()

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
            // IMAGE_ANALYSIS dipakai deteksi cuking, tapi jepretnya tetap dari
            // IMAGE_CAPTURE. Keduanya bisa hidup bersamaan di controller yang sama.
            setEnabledUseCases(CameraController.IMAGE_CAPTURE or CameraController.IMAGE_ANALYSIS)
            // Lensa utama, bukan kamera belakang pertama yang ditemukan sistem:
            // urutan itu bisa menunjuk ke lensa ultra-wide di sebagian HP.
            cameraSelector = standardBackCameraSelector()
        }
    }

    // Deteksi cuma isyarat: layarnya memberi tahu saat cuking terlihat, tapi
    // jepret tetap menunggu sentuhan pengguna.
    val catDetected by rememberCatDetection(
        controller = controller,
        enabled = cameraGranted && captureFile == null
    )

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Inset keyboard membungkus area gulir, bukan jadi padding di
                // dalamnya: dengan begitu viewport-nya benar-benar menyusut di
                // atas keyboard, dan field yang sedang diketik ikut terdorong
                // naik oleh gulir otomatis Compose.
                // Keyboard dan bar navigasi diambil dengan `union`, bukan
                // ditumpuk: keduanya menempati sisi bawah, jadi menumpuk paddingnya
                // membuat tinggi bar navigasi terhitung dua kali saat keyboard
                // terbuka.
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Ruang di bawah bar atas. Sengaja jadi isi yang ikut menggulir, bukan
            // padding wadah: dengan begitu kontennya benar-benar lewat di belakang
            // bar, bukan terpotong di bawahnya.
            Spacer(Modifier.height(topBarContentPadding()))
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
                        onCapture = takePhoto,
                        catDetected = catDetected
                    )
                } else {
                    PhotoReview(
                        photoFile = captureFile!!,
                        stickerState = stickerState,
                        onMakeSticker = { viewModel.makeSticker(captureFile) },
                        onRemoveSticker = viewModel::removeSticker,
                        name = name,
                        onNameChange = { name = it },
                        showNameField = viewModel.isNewCat,
                        contextLabel = if (viewModel.isNewCat) {
                            null
                        } else {
                            targetCat?.name?.let { catName ->
                                stringResource(R.string.add_again_whose, catName)
                            } ?: stringResource(R.string.add_again_whose_unnamed)
                        },
                        description = description,
                        onDescriptionChange = { description = it },
                        videoCapture = videoCapture,
                        onAddVideo = { showVideoRecorder = true },
                        onRemoveVideo = {
                            container.imageStorageHelper.discardCapture(videoCapture)
                            videoCapture = null
                        },
                        onRetake = {
                            container.imageStorageHelper.discardCapture(captureFile)
                            container.imageStorageHelper.discardCapture(videoCapture)
                            captureFile = null
                            videoCapture = null
                            // Stiker dibuat dari foto yang barusan, jadi ia ikut
                            // dilepas bersama fotonya.
                            viewModel.removeSticker()
                            viewModel.clearError()
                        }
                    )

                    when (val state = uiState) {
                        is AddCatUiState.Error -> ErrorCard(
                            message = stringResource(state.messageRes),
                            onRetry = {
                                viewModel.saveCat(
                                    captureFile = captureFile,
                                    name = name,
                                    description = description,
                                    videoCapture = videoCapture,
                                    stickerFile = stickerFile(stickerState)
                                )
                            },
                            onDismiss = viewModel::clearError
                        )

                        else -> Button(
                            onClick = {
                                viewModel.saveCat(
                                    captureFile = captureFile,
                                    name = name,
                                    description = description,
                                    videoCapture = videoCapture,
                                    stickerFile = stickerFile(stickerState)
                                )
                            },
                            // Stiker yang sedang dipotong ikut ditunggu: kalau
                            // Simpan ditekan sekarang, hasil potongannya belum
                            // ikut tersimpan.
                            enabled = !busy && stickerState !is StickerUiState.Working,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = stringResource(
                                    if (viewModel.isNewCat) {
                                        R.string.add_save
                                    } else {
                                        R.string.add_again_save
                                    }
                                ),
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

        PlayfulTopBar(
            title = stringResource(
                if (viewModel.isNewCat) R.string.add_title else R.string.add_again_title
            ),
            onBack = onBack,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        )

        // Perekam video menutupi seluruh layar, sama seperti perayaan di bawah:
        // merekam butuh fokus pada satu pratinjau, bukan form yang menggoda untuk
        // diisi sambil memegang HP.
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
                        videoCapture = file
                        showVideoRecorder = false
                    },
                    onCancel = { showVideoRecorder = false },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .navigationBarsPadding()
                )
            }
        }

        // Digambar paling akhir supaya menutupi form yang baru diisi, bukan
        // menggeser isinya. Kalau catatan barusan memanjangkan rentetan harian,
        // pesannya berganti; gambar dan gerakannya tetap sama.
        val saved = uiState as? AddCatUiState.Saved
        if (saved != null) {
            CatSaveCelebration(
                message = stringResource(
                    when {
                        saved.streakDays != null -> R.string.add_saved_streak
                        viewModel.isNewCat -> R.string.add_saved_celebration
                        else -> R.string.add_saved_sighting
                    }
                ),
                hint = stringResource(R.string.celebration_tap_hint),
                streakDays = saved.streakDays,
                onDismiss = finishSaving
            )
        }
    }
}

/** File stiker hanya ikut disimpan kalau pembuatannya memang berhasil. */
private fun stickerFile(state: StickerUiState): File? =
    (state as? StickerUiState.Ready)?.file

@Composable
private fun PhotoReview(
    photoFile: File,
    stickerState: StickerUiState,
    onMakeSticker: () -> Unit,
    onRemoveSticker: () -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    /** Cuma ditanyakan untuk cuking baru: nama milik cukingnya, bukan penemuannya. */
    showNameField: Boolean,
    /** Sebutan cuking yang sedang ditambahi penemuan, null untuk cuking baru. */
    contextLabel: String?,
    description: String,
    onDescriptionChange: (String) -> Unit,
    /** Video pendamping yang baru direkam, null kalau belum ada. */
    videoCapture: File?,
    onAddVideo: () -> Unit,
    onRemoveVideo: () -> Unit,
    onRetake: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Stiker yang sudah jadi langsung ditampilkan: itu hasil yang ditunggu
    // pengguna, bukan fotonya lagi. Tombol "Lihat foto" mengembalikannya.
    val sticker = stickerState as? StickerUiState.Ready
    var showSticker by remember(sticker?.file) { mutableStateOf(sticker != null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = appCardOutline()
    ) {
        // Jarak diatur sekali di sini, bukan pakai Spacer satu-satu: dulu cuma ada
        // satu Spacer setelah foto, jadi chip nama cuking nempel ke field kegiatan.
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StickerPreview(
                photoFile = photoFile,
                sticker = sticker,
                showSticker = showSticker
            )
            StickerSection(
                state = stickerState,
                showingSticker = showSticker,
                onMakeSticker = onMakeSticker,
                onRemoveSticker = onRemoveSticker,
                onTogglePreview = { showSticker = !showSticker }
            )
            if (showNameField) {
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
            } else if (contextLabel != null) {
                // Nama tidak bisa diisi di sini, jadi yang ditampilkan justru nama
                // cukingnya: pengguna harus tahu penemuan ini masuk ke yang mana.
                InfoChip(
                    text = contextLabel,
                    iconRes = R.drawable.ic_paw,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
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
            VideoSection(
                videoPath = videoCapture?.absolutePath,
                onAddVideo = onAddVideo,
                onRemoveVideo = onRemoveVideo
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

/**
 * Foto atau stikernya, di tempat yang sama.
 *
 * Latar kotaknya warna permukaan, bukan kotak-kotak papan catur: yang perlu
 * terlihat cuma bahwa latarnya hilang, dan pola papan catur akan bersaing dengan
 * cukingnya sendiri.
 */
@Composable
private fun StickerPreview(
    photoFile: File,
    sticker: StickerUiState.Ready?,
    showSticker: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (showSticker && sticker != null) {
            AsyncImage(
                model = sticker.file,
                contentDescription = stringResource(R.string.cd_sticker_preview),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            )
        } else {
            AsyncImage(
                model = photoFile,
                contentDescription = stringResource(R.string.cd_cat_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Tombol dan kabar seputar stiker.
 *
 * Stiker sifatnya tambahan: fotonya tetap wajib, stikernya boleh dilewati. Karena
 * itu yang salah tidak pernah memblokir tombol Simpan, dan kegagalannya cuma
 * dijelaskan di sini.
 */
@Composable
private fun StickerSection(
    state: StickerUiState,
    showingSticker: Boolean,
    onMakeSticker: () -> Unit,
    onRemoveSticker: () -> Unit,
    onTogglePreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        when (state) {
            StickerUiState.None -> {
                OutlinedButton(
                    onClick = onMakeSticker,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        text = stringResource(R.string.add_sticker_make),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Text(
                    text = stringResource(R.string.add_sticker_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            StickerUiState.Working -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.add_sticker_working),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            is StickerUiState.Ready -> {
                InfoChip(
                    text = stringResource(R.string.add_sticker_ready),
                    iconRes = R.drawable.ic_paw,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
                // Dua tombolnya ditaruh di baris sendiri, bukan di samping chip:
                // di layar sempit atau saat huruf diperbesar, satu baris bertiga
                // akan saling mendesak.
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onTogglePreview) {
                        Text(
                            text = stringResource(
                                if (showingSticker) {
                                    R.string.add_sticker_see_photo
                                } else {
                                    R.string.add_sticker_see_sticker
                                }
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(onClick = onRemoveSticker) {
                        Text(
                            text = stringResource(R.string.add_sticker_remove),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            is StickerUiState.Error -> {
                Text(
                    text = stringResource(state.messageRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                TextButton(onClick = onMakeSticker) {
                    Text(
                        text = stringResource(R.string.add_retry),
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

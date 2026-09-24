package com.khalied.cukinggo.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.khalied.cukinggo.R
import com.khalied.cukinggo.appContainer
import com.khalied.cukinggo.data.local.NearbyAlertPreferences
import com.khalied.cukinggo.domain.model.Cat
import com.khalied.cukinggo.location.MAX_WATCH_AREAS
import com.khalied.cukinggo.location.NearbyAlertState
import com.khalied.cukinggo.location.NearbyAlerts
import com.khalied.cukinggo.location.NearbyRadius
import com.khalied.cukinggo.location.NearestCatResult
import com.khalied.cukinggo.location.findNearestCat
import com.khalied.cukinggo.location.nearbyAlertState
import com.khalied.cukinggo.ui.components.CatListCard
import com.khalied.cukinggo.ui.components.CatMapView
import com.khalied.cukinggo.ui.components.InfoChip
import com.khalied.cukinggo.ui.components.NearbyAlertDialog
import com.khalied.cukinggo.ui.components.SleepingCatIllustration
import com.khalied.cukinggo.ui.components.AppearancePickerDialog
import com.khalied.cukinggo.ui.components.WalkingCatLoader
import com.khalied.cukinggo.ui.theme.InkSoft
import com.khalied.cukinggo.ui.theme.MintPop
import com.khalied.cukinggo.ui.theme.PeachAccent
import com.khalied.cukinggo.ui.theme.ThemeMode
import com.khalied.cukinggo.ui.theme.appCardOutline
import com.khalied.cukinggo.widget.CatWidgets
import com.khalied.cukinggo.widget.WidgetSkinMode
import com.khalied.cukinggo.util.catStreak
import com.khalied.cukinggo.util.formatDistance
import com.khalied.cukinggo.util.hasBackgroundLocationPermission
import com.khalied.cukinggo.util.hasNotificationPermission
import com.khalied.cukinggo.util.openAppSettings
import java.io.File
import java.time.LocalDate

/**
 * Jumlah cuking di daftar Home.
 *
 * Lima: cukup untuk melihat jejak terakhir tanpa mendesak peta di atasnya keluar
 * dari layar, sedangkan sisanya ada di halaman "Semua cuking" lewat baris Lihat
 * semua. Peta, chip rentetan, dan kartu cuking terdekat tidak ikut dibatasi.
 */
private const val RECENT_CAT_LIMIT = 5

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddCat: () -> Unit,
    onCatClick: (Long) -> Unit,
    onSeeAllCats: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Peta tetap digambar walau data belum siap, marker-nya menyusul sendiri.
    val cats = (uiState as? HomeUiState.Content)?.cats.orEmpty()
    val context = LocalContext.current
    val container = remember(context) { context.appContainer }

    // Rentetan harian dihitung dari catatan yang sudah ada di layar ini, jadi
    // tidak perlu query tambahan. Kuncinya daftar kucing, artinya angkanya ikut
    // segar setiap kali ada kucing baru atau ada yang dihapus.
    // Tanggalnya disimpan sebagai state, bukan dipanggil langsung sebagai
    // LocalDate.now() di dalam remember, supaya angkanya ikut segar kalau app
    // dibuka lagi setelah melewati tengah malam (lihat ON_RESUME di bawah).
    var today by remember { mutableStateOf(LocalDate.now()) }
    val streakDays = remember(cats, today) {
        catStreak(timestamps = cats.map { cat -> cat.timestamp }, today = today)
    }
    val displayPreferences = container.displayPreferences
    val themeModeKey by displayPreferences.themeModeKey.collectAsStateWithLifecycle()
    val widgetSkinKey by displayPreferences.widgetSkinKey.collectAsStateWithLifecycle()
    var showAppearanceDialog by remember { mutableStateOf(false) }

    val nearbyPreferences = remember(context) { NearbyAlertPreferences(context) }
    var showNearbyDialog by remember { mutableStateOf(false) }
    var nearbyEnabled by remember { mutableStateOf(nearbyPreferences.isEnabled()) }
    var nearbyRadius by remember {
        mutableStateOf(NearbyRadius.fromMeters(nearbyPreferences.radiusMeters()))
    }
    var canNotify by remember { mutableStateOf(context.hasNotificationPermission()) }
    var hasForegroundLocation by remember {
        mutableStateOf(container.locationHelper.hasLocationPermission())
    }
    var hasBackgroundLocation by remember {
        mutableStateOf(context.hasBackgroundLocationPermission())
    }

    // Cuking terdekat yang disorot di atas peta saat kamu memang sedang dekat
    // dengannya. Dihitung ulang kalau daftar cukingnya berubah, kalau izin
    // lokasinya baru diberikan, dan setiap kali layar ini dibuka lagi.
    var locationRefreshKey by remember { mutableStateOf(0) }
    var nearestCat by remember { mutableStateOf<NearestCatResult.Found?>(null) }
    LaunchedEffect(cats, hasForegroundLocation, locationRefreshKey) {
        nearestCat = if (cats.isEmpty() || !hasForegroundLocation) {
            null
        } else {
            // Layar Home boleh menunggu sebentar kalau perangkat belum pernah
            // mencatat posisi sama sekali; widget tidak boleh, karena ia digambar
            // di latar belakang.
            findNearestCat(
                locationHelper = container.locationHelper,
                cats = cats,
                allowFreshFix = true
            ) as? NearestCatResult.Found
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> canNotify = granted }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasForegroundLocation = granted }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasBackgroundLocation = granted }

    fun refreshPermissions() {
        canNotify = context.hasNotificationPermission()
        hasForegroundLocation = container.locationHelper.hasLocationPermission()
        hasBackgroundLocation = context.hasBackgroundLocationPermission()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Izin bisa berubah / dicabut saat app di background, dan pengguna
                // sering kembali dari pengaturan izin atau pengaturan lokasi.
                refreshPermissions()
                // Rentetan dihitung per hari, jadi tanggalnya harus ikut diganti
                // kalau halaman ini dibuka lagi besoknya.
                today = LocalDate.now()
                // Posisinya bisa berubah selagi app di background, dan widget
                // "Cuking terdekat" membaca posisi saat digambar. Tanpa baris
                // ini, kartunya baru ikut menyesuaikan kalau ada cuking baru
                // yang ditandai.
                locationRefreshKey++
                CatWidgets.refreshAll(context)
                NearbyAlerts.syncAsync(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val nearbyState = nearbyAlertState(
        enabled = nearbyEnabled,
        canPostNotifications = canNotify,
        hasForegroundLocation = hasForegroundLocation,
        hasBackgroundLocation = hasBackgroundLocation
    )

    fun turnOffNearby() {
        nearbyEnabled = false
        NearbyAlerts.setEnabled(context, false)
    }

    fun fixPermission() {
        when (nearbyState) {
            // Pemeriksaan versi ini tidak akan pernah gagal saat dijalankan:
            // state ini hanya muncul kalau izin notifikasi memang belum ada, dan
            // di bawah Android 13 notifikasi tidak butuh izin sama sekali.
            NearbyAlertState.NEED_NOTIFICATION_PERMISSION ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }

            NearbyAlertState.NEED_FOREGROUND_LOCATION ->
                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

            NearbyAlertState.NEED_BACKGROUND_LOCATION ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // Sejak Android 11 dialog sistem tidak lagi punya opsi
                    // "sepanjang waktu", jadi pengguna harus diantar ke pengaturan
                    // app untuk memilihnya sendiri.
                    context.openAppSettings()
                } else {
                    backgroundPermissionLauncher.launch(
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    )
                }

            else -> Unit
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = { PawFab(onClick = onAddCat) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_title),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.home_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                NearbyPill(
                    active = nearbyState == NearbyAlertState.ACTIVE,
                    onClick = { showNearbyDialog = true }
                )
                AppearancePill(onClick = { showAppearanceDialog = true })
            }

            // Rentetan harian hanya muncul kalau sudah jalan. Widget pun begitu:
            // tidak ada yang menagih sebelum pengguna mulai sendiri.
            if (streakDays > 0) {
                Spacer(Modifier.height(10.dp))
                InfoChip(
                    text = stringResource(R.string.streak_chip, streakDays),
                    iconRes = R.drawable.ic_flame,
                    containerColor = PeachAccent,
                    contentColor = InkSoft
                )
            }

            // Cuking terdekat disorot di atas peta, bukan diselipkan ke dalam
            // daftar: begitu kamu masuk radiusnya, dia yang jadi hal pertama yang
            // terlihat di layar ini. Kalau tidak ada yang dekat, kartunya hilang
            // sama sekali dan peta tetap jadi isi utama seperti sebelumnya.
            val nearest = nearestCat
            if (nearest != null) {
                Spacer(Modifier.height(10.dp))
                NearestCatCard(
                    cat = nearest.cat,
                    distanceMeters = nearest.distanceMeters,
                    onClick = { onCatClick(nearest.cat.id) }
                )
            }

            Spacer(Modifier.height(14.dp))

            // Satu-satunya shadow di layar ini, dan memang alasannya: peta adalah
            // permukaan utama yang "duduk di atas" halaman. Kartu lain di bawahnya
            // cukup pakai garis tipis (lihat appCardOutline).
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.05f)
                    .clip(MaterialTheme.shapes.extraLarge),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.extraLarge,
                shadowElevation = 6.dp
            ) {
                CatMapView(
                    cats = cats,
                    onCatClick = onCatClick,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(Modifier.height(14.dp))

            when (val state = uiState) {
                HomeUiState.Loading -> CatsPlaceholderCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    WalkingCatLoader(text = stringResource(R.string.home_loading))
                }

                HomeUiState.Failed -> CatsErrorCard(
                    onRetry = viewModel::retry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                is HomeUiState.Content -> if (state.cats.isEmpty()) {
                    EmptyCatsCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        // Baris ini cuma muncul kalau memang masih ada sisa: kalau
                        // koleksinya belum lebih dari lima, halaman daftarnya isinya
                        // sama persis dengan yang sudah ada di layar ini.
                        if (state.cats.size > RECENT_CAT_LIMIT) {
                            SeeAllCatsRow(onClick = onSeeAllCats)
                            Spacer(Modifier.height(10.dp))
                        }

                        RecentCatsSection(
                            cats = state.cats.take(RECENT_CAT_LIMIT),
                            totalCount = state.cats.size,
                            onCatClick = onCatClick,
                            onDelete = viewModel::deleteCat,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }

    if (showAppearanceDialog) {
        AppearancePickerDialog(
            currentThemeMode = ThemeMode.fromKey(themeModeKey),
            currentWidgetSkin = WidgetSkinMode.fromKey(widgetSkinKey),
            onThemeModeSelected = { mode ->
                displayPreferences.setThemeModeKey(mode.storageKey)
                // Widget membaca pilihan tema saat digambar, jadi ia digambar
                // ulang sekarang juga; tanpa ini warnanya baru ikut setelah ada
                // sesuatu yang memicu pembaruan lain.
                CatWidgets.refreshAll(context)
                showAppearanceDialog = false
            },
            onWidgetSkinSelected = { skin ->
                displayPreferences.setWidgetSkinKey(skin.storageKey)
                CatWidgets.refreshAll(context)
                showAppearanceDialog = false
            },
            onDismiss = { showAppearanceDialog = false }
        )
    }

    if (showNearbyDialog) {
        NearbyAlertDialog(
            state = nearbyState,
            radius = nearbyRadius,
            watchedCount = cats.size.coerceAtMost(MAX_WATCH_AREAS),
            onRadiusChange = { radius ->
                nearbyRadius = radius
                NearbyAlerts.setRadius(context, radius)
            },
            onTurnOn = {
                nearbyEnabled = true
                NearbyAlerts.setEnabled(context, true)
            },
            onTurnOff = {
                turnOffNearby()
                showNearbyDialog = false
            },
            onFixPermission = ::fixPermission,
            onDismiss = { showNearbyDialog = false }
        )
    }
}

/** Wadah daftar kucing saat datanya belum siap, memakai bahasa loading app. */
@Composable
private fun CatsPlaceholderCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = appCardOutline()
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
    }
}

/** Error state Home: baca catatan lokal gagal, jadi dikasih jalan keluar. */
@Composable
private fun CatsErrorCard(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.error_load_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.home_error_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiary,
                    textAlign = TextAlign.Center
                )
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
            }
        }
    }
}

/**
 * Cuking terdekat, ditampilkan begitu kamu masuk radiusnya.
 *
 * Latarnya `secondaryContainer` (MintPop di tema terang) dan bukan warna kartu
 * biasa, karena makna kartu ini memang "dekat": warna yang sama sudah dipakai
 * pil lonceng saat fitur kabarnya aktif dan chip jumlah di bagian daftar. Semua
 * teks memakai `onSecondaryContainer` di atas latar itu, dan itu lolos WCAG AA di
 * kedua tema (7,7:1 di terang, 4,9:1 di gelap), jadi judulnya tidak boleh memakai
 * `primary` seperti kartu daftar: di atas MintPop warnanya cuma 3,2:1.
 */
@Composable
private fun NearestCatCard(
    cat: Cat,
    distanceMeters: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            // Satu kartu dibaca TalkBack sebagai satu simpul, sama seperti kartu di
            // daftar: label, nama, dan jaraknya cukup sekali.
            .semantics(mergeDescendants = true) {},
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Foto sengaja dekoratif, sama seperti di kartu daftar: teks di
            // sebelahnya sudah menjelaskan kartunya.
            AsyncImage(
                model = File(cat.photoPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.large)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_nearest_label),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = cat.name
                        ?: cat.description
                        ?: stringResource(R.string.detail_no_description),
                    style = MaterialTheme.typography.titleMedium,
                    fontStyle = if (cat.name == null && cat.description == null) {
                        FontStyle.Italic
                    } else {
                        FontStyle.Normal
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        R.string.detail_distance,
                        formatDistance(distanceMeters)
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Kontrol kabar "dekat kucing": bulat dan ikon saja supaya tidak menyempitkan
 * judul di sebelahnya. Warnanya berubah jadi MintPop waktu fiturnya aktif, jadi
 * statusnya kelihatan tanpa harus membuka dialog.
 *
 * Tinggi 44dp supaya tetap lolos tap target (R-03).
 */
@Composable
private fun NearbyPill(active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = CircleShape,
        color = if (active) MintPop else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (active) InkSoft else MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_bell),
                contentDescription = stringResource(R.string.nearby_pill_description),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Kontrol tampilan di header: satu-satunya setelan app, jadi cukup pill kecil.
 * Isinya tema dan bentuk widget (lihat AppearancePickerDialog).
 */
@Composable
private fun AppearancePill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        // Tinggi minimal 44dp supaya lolos tap target (R-03).
        Box(
            modifier = Modifier
                .heightIn(min = 44.dp)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.appearance_button),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

/**
 * Baris menuju halaman daftar lengkap, duduk di antara peta dan jejak terbaru.
 *
 * Warnanya `surfaceVariant`, bukan PeachAccent seperti FAB: yang jadi aksi utama di
 * layar ini tetap "Tandai cuking", sedangkan baris ini cuma jalan ke daftar.
 * Tingginya 48dp supaya lolos tap target (R-03), dan angkanya sengaja tidak
 * diulang di sini karena chip jumlah di bawahnya sudah menyebutkannya.
 */
@Composable
private fun SeeAllCatsRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.home_see_all),
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun RecentCatsSection(
    cats: List<Cat>,
    totalCount: Int,
    onCatClick: (Long) -> Unit,
    onDelete: (Cat) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.home_recent_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            // Angkanya jumlah seluruh koleksi, bukan jumlah kartu di bawahnya:
            // menulis "5 cuking ditemukan" saat catatannya ada 23 justru angka yang
            // salah, dan sisanya memang ada di halaman "Semua cuking".
            InfoChip(
                text = stringResource(R.string.home_counter, totalCount),
                containerColor = MintPop,
                contentColor = InkSoft
            )
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items(items = cats, key = { it.id }) { cat ->
                CatListCard(
                    cat = cat,
                    onClick = { onCatClick(cat.id) },
                    onDelete = { onDelete(cat) }
                )
            }
        }
    }
}

@Composable
private fun EmptyCatsCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = appCardOutline()
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SleepingCatIllustration(size = 140.dp)
                Text(
                    text = stringResource(R.string.home_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.home_empty_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PawFab(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fabSquish"
    )

    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = Modifier.scale(scale),
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = PeachAccent,
        contentColor = InkSoft,
        interactionSource = interactionSource,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_paw),
                contentDescription = null,
                modifier = Modifier.size(22.dp)
            )
        },
        text = {
            Text(
                text = stringResource(R.string.fab_add_cat),
                style = MaterialTheme.typography.labelLarge
            )
        }
    )
}

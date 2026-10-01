package com.khalied.cukinggo.ui.detail

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.khalied.cukinggo.R
import com.khalied.cukinggo.appContainer
import com.khalied.cukinggo.domain.model.Cat
import com.khalied.cukinggo.domain.model.CatSighting
import com.khalied.cukinggo.ui.components.CatGoodbyeCelebration
import com.khalied.cukinggo.ui.components.CatMapView
import com.khalied.cukinggo.ui.components.InfoChip
import com.khalied.cukinggo.ui.components.PlayfulTopBar
import com.khalied.cukinggo.ui.components.VideoPlayer
import com.khalied.cukinggo.ui.components.WalkingCatLoader
import com.khalied.cukinggo.ui.components.buildTrails
import com.khalied.cukinggo.ui.components.sharedCatPhoto
import com.khalied.cukinggo.ui.theme.BlushPink
import com.khalied.cukinggo.ui.theme.InkSoft
import com.khalied.cukinggo.ui.theme.MintPop
import com.khalied.cukinggo.ui.theme.PeachAccent
import com.khalied.cukinggo.ui.theme.appCardOutline
import com.khalied.cukinggo.util.catShareIntent
import com.khalied.cukinggo.util.catTextShareIntent
import com.khalied.cukinggo.util.catVideoShareIntent
import com.khalied.cukinggo.util.blankToNull
import com.khalied.cukinggo.util.distanceMeters
import com.khalied.cukinggo.util.formatCoordinates
import com.khalied.cukinggo.util.formatDayLabel
import com.khalied.cukinggo.util.formatDistance
import com.khalied.cukinggo.util.formatFullDateTime
import com.khalied.cukinggo.util.formatTime
import com.khalied.cukinggo.util.mapsLinkFor
import java.io.File
import kotlinx.coroutines.delay

/** Batas tunggu singkat: cukup untuk satu fix GPS kalau belum ada posisi tersimpan. */
private const val DETAIL_LOCATION_TIMEOUT_MILLIS = 5_000L

/**
 * Lama pamitan ditahan sebelum layar ini menutup sendiri.
 *
 * Sedikit lebih pendek dari perayaan setelah menyimpan: yang ini menutup sebuah
 * catatan, jadi cukup untuk satu-dua lambaian. Ketukan di mana saja melewatinya.
 */
private const val FAREWELL_DURATION_MILLIS = 1_400L

/** Tinggi baris riwayat: 56dp foto plus jarak, jadi lebih dari tap target minimum. */
private const val HISTORY_THUMB_SIZE_DP = 56

/**
 * Tinggi kartu peta lokasi cukingnya.
 *
 * Dua ratus dua puluh dp: cukup untuk membaca arah garis jejaknya atau mengenali
 * areanya, tanpa membuat layar ini jadi halaman peta, karena yang jadi isi utama
 * di sini tetap fotonya.
 */
private const val LOCATION_MAP_HEIGHT_DP = 220

@Composable
fun CatDetailScreen(
    viewModel: CatDetailViewModel,
    onAddSighting: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val container = remember(context) { context.appContainer }
    var distanceFromUser by remember { mutableStateOf<Double?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Catatannya sudah terhapus, tapi layarnya belum menutup: cukingnya melambai
    // dulu. Penanda yang sama dipakai oleh ketukan dan batas waktu, supaya onBack
    // tidak pernah terpanggil dua kali.
    var sayingGoodbye by remember { mutableStateOf(false) }
    var closedScreen by remember { mutableStateOf(false) }
    val closeScreen = {
        if (!closedScreen) {
            closedScreen = true
            onBack()
        }
    }

    LaunchedEffect(sayingGoodbye) {
        if (sayingGoodbye) {
            delay(FAREWELL_DURATION_MILLIS)
            closeScreen()
        }
    }

    val currentCat = (uiState as? CatDetailUiState.Content)?.cat

    // Penemuan yang sedang dipamerkan di atas: yang dibuka dari daftar, atau yang
    // dipilih pengguna dari riwayatnya. Kalau pilihannya sudah tidak ada (misalnya
    // barusan dihapus dari layar lain), yang terbaru yang dipakai.
    var selectedSightingId by remember { mutableStateOf(viewModel.sightingId) }
    val selectedSighting = currentCat?.let { cat ->
        cat.sightings.firstOrNull { it.id == selectedSightingId } ?: cat.latest
    }

    // Jarak dihitung untuk penemuan yang sedang ditampilkan. Kalau lokasi user
    // tidak tersedia, chip jaraknya cukup tidak muncul (bukan error).
    LaunchedEffect(selectedSighting?.id) {
        distanceFromUser = null
        val sighting = selectedSighting ?: return@LaunchedEffect
        if (!container.locationHelper.hasLocationPermission()) return@LaunchedEffect

        val location = container.locationHelper.lastKnownLocation()
            ?: container.locationHelper.getCurrentLocation(DETAIL_LOCATION_TIMEOUT_MILLIS)
            ?: return@LaunchedEffect

        distanceFromUser = distanceMeters(
            location.latitude,
            location.longitude,
            sighting.latitude,
            sighting.longitude
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Saat berpamitan, isi layarnya dilepas sama sekali: catatannya sudah tidak
        // ada, jadi tanpa ini yang ada di belakang peredup justru keterangan
        // "catatan sudah tidak ada" tepat saat cukingnya sedang melambai.
        if (!sayingGoodbye) {
            Column(modifier = Modifier.fillMaxSize()) {
                PlayfulTopBar(title = stringResource(R.string.detail_title), onBack = onBack)

                when (val state = uiState) {
                    CatDetailUiState.Loading -> DetailPlaceholder(
                        modifier = Modifier.fillMaxSize(),
                        loaderText = stringResource(R.string.detail_loading)
                    )

                    CatDetailUiState.Missing -> DetailPlaceholder(
                        modifier = Modifier.fillMaxSize(),
                        message = stringResource(R.string.detail_missing)
                    )

                    CatDetailUiState.Failed -> DetailPlaceholder(
                        modifier = Modifier.fillMaxSize(),
                        message = stringResource(R.string.error_load_title),
                        actionLabel = stringResource(R.string.add_retry),
                        onAction = viewModel::retry
                    )

                    is CatDetailUiState.Content -> if (selectedSighting != null) {
                        DetailContent(
                            cat = state.cat,
                            selectedSighting = selectedSighting,
                            distanceFromUser = distanceFromUser,
                            onSelectSighting = { sighting -> selectedSightingId = sighting.id },
                            onAddSighting = { onAddSighting(state.cat.id) },
                            onEdit = { onEdit(selectedSighting.id) },
                            onDeleteSighting = viewModel::deleteSighting,
                            onDeleteRequest = { showDeleteDialog = true },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        if (sayingGoodbye) {
            CatGoodbyeCelebration(
                message = stringResource(R.string.detail_goodbye_message),
                hint = stringResource(R.string.celebration_tap_hint),
                onDismiss = closeScreen
            )
        }
    }

    if (showDeleteDialog) {
        val sightingCount = currentCat?.sightings?.size ?: 1
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = stringResource(R.string.detail_delete_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                // Cuking yang punya beberapa penemuan: yang dihapus di sini semua
                // penemuannya, jadi jumlahnya disebut supaya tidak ada yang
                // terhapus tanpa pengguna tahu.
                Text(
                    text = if (sightingCount > 1) {
                        stringResource(R.string.detail_delete_message_history, sightingCount)
                    } else {
                        stringResource(R.string.detail_delete_message)
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        // Penghapusannya jalan sekarang, tapi kembalinya ke Home
                        // menunggu pamitannya selesai.
                        viewModel.deleteCat(onDeleted = { sayingGoodbye = true })
                    }
                ) {
                    Text(
                        text = stringResource(R.string.detail_delete_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
private fun DetailContent(
    cat: Cat,
    selectedSighting: CatSighting,
    distanceFromUser: Double?,
    onSelectSighting: (CatSighting) -> Unit,
    onAddSighting: () -> Unit,
    onEdit: () -> Unit,
    onDeleteSighting: (CatSighting) -> Unit,
    onDeleteRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showShareOptions by remember { mutableStateOf(false) }
    // Penemuan yang sedang ditanyakan mau dihapus, null berarti tidak ada
    // pertanyaan yang terbuka.
    var sightingToDelete by remember { mutableStateOf<CatSighting?>(null) }

    val shareMessage = shareMessageFor(selectedSighting)

    LazyColumn(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Foto penemuan yang sedang ditampilkan. Kartu di daftar memakai kunci
            // yang sama, jadi begitu kartunya disentuh, yang bergerak ke sini cuma
            // fotonya (lihat sharedCatPhoto).
            AsyncImage(
                model = File(selectedSighting.photoPath),
                contentDescription = stringResource(R.string.cd_cat_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .sharedCatPhoto(
                        sightingId = selectedSighting.id,
                        clip = MaterialTheme.shapes.extraLarge
                    )
            )
        }

        // Video pendamping ditaruh tepat di bawah fotonya, bukan di bagian lain:
        // ia pasangan dari foto itu, dan yang mencari gerak-gerik cukingnya akan
        // mencarinya di tempat yang sama dengan fotonya.
        selectedSighting.videoPath?.let { videoPath ->
            item {
                VideoPlayer(
                    videoPath = videoPath,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                        .clip(MaterialTheme.shapes.extraLarge)
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(
                    text = formatFullDateTime(selectedSighting.timestamp),
                    iconRes = R.drawable.ic_calendar,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
                InfoChip(
                    text = formatCoordinates(
                        selectedSighting.latitude,
                        selectedSighting.longitude
                    ),
                    iconRes = R.drawable.ic_location,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
                distanceFromUser?.let { distance ->
                    InfoChip(
                        text = stringResource(
                            R.string.detail_distance,
                            formatDistance(distance)
                        ),
                        iconRes = R.drawable.ic_my_location,
                        containerColor = MintPop,
                        contentColor = InkSoft
                    )
                }
            }
        }

        item {
            CatLocationMap(cat = cat, onSightingClick = onSelectSighting)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = appCardOutline()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (cat.name != null) {
                        Text(
                            text = cat.name,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "🐾 Catatan",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = selectedSighting.description
                            ?: stringResource(R.string.detail_no_description),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontStyle = if (selectedSighting.description == null) {
                            FontStyle.Italic
                        } else {
                            FontStyle.Normal
                        }
                    )
                    Spacer(Modifier.height(2.dp))
                    // Angka ini yang menjelaskan kenapa ada riwayat di bawah, jadi
                    // ia muncul bahkan saat cukingnya baru ketemu sekali.
                    InfoChip(
                        text = stringResource(R.string.detail_seen_count, cat.sightings.size)
                    )
                }
            }
        }

        item {
            Button(
                onClick = onAddSighting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PeachAccent,
                    contentColor = InkSoft
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_paw),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.detail_again),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        item { ShareCatButton(onClick = { showShareOptions = true }) }

        // Edit duduk di bawah bagikan dan di atas riwayat: ia mengubah catatan yang
        // sedang dilihat di atas (nama cukingnya dan kegiatan di penemuan ini),
        // bukan salah satu baris riwayat.
        item {
            Button(
                onClick = onEdit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_paw),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.edit_open),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        // Riwayat cuma ditampilkan kalau memang ada lebih dari satu penemuan:
        // dengan satu penemuan, isinya sama persis dengan foto di atas, dan
        // mengulangnya cuma menambah panjang layar.
        if (cat.sightings.size > 1) {
            item {
                Text(
                    text = stringResource(R.string.detail_history_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            itemsIndexed(
                items = cat.sightings,
                key = { _, sighting -> sighting.id }
            ) { _, sighting ->
                SightingHistoryRow(
                    sighting = sighting,
                    selected = sighting.id == selectedSighting.id,
                    onClick = { onSelectSighting(sighting) },
                    onDelete = { sightingToDelete = sighting }
                )
            }
        }

        item {
            Button(
                onClick = onDeleteRequest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlushPink,
                    contentColor = InkSoft
                )
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.detail_delete),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showShareOptions) {
        ShareOptionsDialog(
            sighting = selectedSighting,
            message = shareMessage,
            onDismiss = { showShareOptions = false }
        )
    }

    // Dialognya dibuka dari baris riwayat, dan yang dihapus adalah penemuan yang
    // ditunjuk baris itu. Kalau memang cuma tersisa satu penemuan, baris riwayatnya
    // tidak ada, dan menghapusnya tetap lewat tombol "Hapus cuking ini" di bawah.
    val pendingDelete = sightingToDelete
    if (pendingDelete != null) {
        DeleteSightingDialog(
            onConfirm = {
                sightingToDelete = null
                onDeleteSighting(pendingDelete)
            },
            onDismiss = { sightingToDelete = null }
        )
    }
}

/**
 * Peta cukingnya: tempat dia ketemu, dan garis perjalanannya kalau memang ada.
 *
 * Kartunya selalu ada, bukan cuma saat cukingnya punya jejak: dengan satu tempat
 * pun, koordinat di chip atas tidak menjelaskan dia di mana, sedangkan peta
 * menjelaskannya sekilas, termasuk saat dibandingkan dengan posisi pengguna.
 *
 * Judul dan keterangannya ikut menyesuaikan. Satu tempat tidak punya garis apa pun
 * untuk dijelaskan, jadi menyebutnya "jejak" justru menjanjikan sesuatu yang tidak
 * ada; dan keterangan tentang garis yang makin terang cuma masuk akal kalau
 * garisnya memang ada. Karena itu kartunya tanpa keterangan saat tempatnya cuma
 * satu, bukan dengan keterangan yang menjelaskan ketiadaan.
 *
 * Kartunya sengaja sama seperti kartu peta di Home, termasuk shadow-nya, karena
 * keduanya permukaan yang sama-sama "duduk di atas" halaman. Yang berbeda cuma
 * tombol lokasi yang disembunyikan: di sini yang dilihat tempat cukingnya, bukan
 * posisi pengguna.
 */
@Composable
private fun CatLocationMap(
    cat: Cat,
    onSightingClick: (CatSighting) -> Unit,
    modifier: Modifier = Modifier
) {
    // Satu tempat bukan perjalanan, jadi tidak ada garis yang perlu dijelaskan.
    val hasTrail = remember(cat.sightings) { buildTrails(cat.sightings).isNotEmpty() }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(
                if (hasTrail) R.string.detail_trail_title else R.string.detail_location_title
            ),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(LOCATION_MAP_HEIGHT_DP.dp)
                .clip(MaterialTheme.shapes.extraLarge),
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.extraLarge,
            shadowElevation = 6.dp
        ) {
            CatMapView(
                sightings = cat.sightings,
                // Menyentuh satu titik di sini memilih catatan itu di riwayat bawah,
                // bukan membuka layar baru: layar ini memang sudah layar cukingnya.
                onSightingClick = { sightingId ->
                    cat.sightings.firstOrNull { it.id == sightingId }?.let(onSightingClick)
                },
                modifier = Modifier.fillMaxSize(),
                showLocateButton = false,
                // Seluruh perjalanannya yang mau dilihat di kartu ini, jadi petanya
                // dipaskan ke bentangnya, bukan ke tempat terakhir saja.
                fitToContent = true
            )
        }
        if (hasTrail) {
            Text(
                text = stringResource(R.string.detail_trail_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Satu baris riwayat: satu penemuan yang pernah tercatat.
 *
 * Warnanya yang menandai pilihan, bukan hiasan tambahan: baris yang sedang
 * dipamerkan di atas memakai `secondaryContainer` (MintPop di tema terang),
 * warna yang sama dengan chip "dekat" di kartu Home. Semua teks memakai
 * `onSecondaryContainer` di atas latar itu supaya tetap lolos WCAG AA.
 *
 * `selectable` dipakai, bukan `clickable`, karena baris ini memang memilih:
 * TalkBack membacakannya sebagai baris yang terpilih atau tidak.
 */
@Composable
private fun SightingHistoryRow(
    sighting: CatSighting,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        border = appCardOutline()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Foto di sini dekoratif: tanggal dan catatan di sebelahnya sudah
            // menjelaskan barisnya, jadi label foto berulang tidak menambah makna.
            AsyncImage(
                model = File(sighting.photoPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(HISTORY_THUMB_SIZE_DP.dp)
                    .clip(MaterialTheme.shapes.large)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${
                        formatDayLabel(
                            sighting.timestamp,
                            stringResource(R.string.day_today),
                            stringResource(R.string.day_yesterday)
                        )
                    } · ${formatTime(sighting.timestamp)}",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = sighting.description
                        ?: stringResource(R.string.detail_no_description),
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = if (sighting.description == null) {
                        FontStyle.Italic
                    } else {
                        FontStyle.Normal
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            // Hapus satu kegiatan: yang dibuang cuma penemuan ini, bukan cukingnya,
            // jadi tombolnya menempel di baris yang mewakilinya.
            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = stringResource(R.string.detail_sighting_delete_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Isi pesan bagikan: template chat berisi nama cuking, catatan (kalau ada),
 * koordinat, dan link Google Maps.
 *
 * Template-nya dari string resource, bukan ditempel di kode, supaya kalimatnya
 * gampang diganti tanpa menyentuh logika intent-nya. Dipilih dari dua hal yang
 * boleh kosong, yaitu nama dan catatan.
 */
@Composable
private fun shareMessageFor(sighting: CatSighting): String {
    val name = blankToNull(sighting.catName)
    val note = blankToNull(sighting.description)
    val coordinates = formatCoordinates(sighting.latitude, sighting.longitude)
    val mapsLink = mapsLinkFor(sighting.latitude, sighting.longitude)

    return when {
        name != null && note != null -> stringResource(
            R.string.share_message_named_with_note, name, note, coordinates, mapsLink
        )

        name != null -> stringResource(
            R.string.share_message_named, name, coordinates, mapsLink
        )

        note != null -> stringResource(
            R.string.share_message_with_note, note, coordinates, mapsLink
        )

        else -> stringResource(R.string.share_message, coordinates, mapsLink)
    }
}

/** Tombol bagikan; pilihan bentuk bagikannya ada di [ShareOptionsDialog]. */
@Composable
private fun ShareCatButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = ButtonDefaults.buttonColors(
            containerColor = PeachAccent,
            contentColor = InkSoft
        )
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_share),
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.share_button),
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/**
 * Pilihan bentuk bagikan.
 *
 * Ada teks-saja dan salin-teks, bukan cuma foto+teks, karena sebagian app besar
 * (Instagram di antaranya) mengabaikan teks yang menempel pada gambar dan cuma
 * mengambil fotonya. Dengan pilihan ini pengguna tetap bisa membawa teksnya
 * (kirim sebagai teks, atau salin lalu tempel sendiri di caption), jadi hasilnya
 * tidak lagi bergantung pada selera app penerimanya.
 */
@Composable
private fun ShareOptionsDialog(
    sighting: CatSighting,
    message: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val chooserTitle = stringResource(R.string.share_chooser_title)
    val textChooserTitle = stringResource(R.string.share_chooser_text_title)
    val copiedMessage = stringResource(R.string.share_copy_done)

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(R.string.share_options_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                sighting.videoPath?.let { videoPath ->
                    ShareOptionRow(
                        title = stringResource(R.string.share_option_video_and_text),
                        description = stringResource(R.string.share_option_video_and_text_desc),
                        onClick = {
                            onDismiss()
                            context.startActivity(
                                Intent.createChooser(
                                    catVideoShareIntent(context, videoPath, message),
                                    chooserTitle
                                )
                            )
                        }
                    )
                }
                ShareOptionRow(
                    title = stringResource(R.string.share_option_photo_and_text),
                    description = stringResource(R.string.share_option_photo_and_text_desc),
                    onClick = {
                        onDismiss()
                        context.startActivity(
                            Intent.createChooser(
                                catShareIntent(context, sighting.photoPath, message),
                                chooserTitle
                            )
                        )
                    }
                )
                ShareOptionRow(
                    title = stringResource(R.string.share_option_text_only),
                    description = stringResource(R.string.share_option_text_only_desc),
                    onClick = {
                        onDismiss()
                        context.startActivity(
                            Intent.createChooser(catTextShareIntent(message), textChooserTitle)
                        )
                    }
                )
                ShareOptionRow(
                    title = stringResource(R.string.share_option_copy_text),
                    description = stringResource(R.string.share_option_copy_text_desc),
                    onClick = {
                        onDismiss()
                        clipboard.setText(AnnotatedString(message))
                        Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_close))
            }
        }
    )
}

@Composable
private fun ShareOptionRow(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Pertanyaan sebelum satu kegiatan dihapus dari riwayat. */
@Composable
private fun DeleteSightingDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(R.string.detail_delete_sighting),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Text(
                text = stringResource(R.string.detail_delete_sighting_message),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.detail_delete_sighting_confirm),
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        }
    )
}

/** Tampilan untuk keadaan selain "catatan siap": memuat, sudah dihapus, atau gagal baca. */
@Composable
private fun DetailPlaceholder(
    modifier: Modifier = Modifier,
    loaderText: String? = null,
    message: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
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

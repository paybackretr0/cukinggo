package com.khalied.cukinggo.ui.detail

import android.content.Intent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.khalied.cukinggo.R
import com.khalied.cukinggo.appContainer
import com.khalied.cukinggo.domain.model.Cat
import com.khalied.cukinggo.ui.components.InfoChip
import com.khalied.cukinggo.ui.components.PlayfulTopBar
import com.khalied.cukinggo.ui.components.WalkingCatLoader
import com.khalied.cukinggo.ui.theme.BlushPink
import com.khalied.cukinggo.ui.theme.InkSoft
import com.khalied.cukinggo.ui.theme.MintPop
import com.khalied.cukinggo.ui.theme.PeachAccent
import com.khalied.cukinggo.ui.theme.appCardOutline
import com.khalied.cukinggo.util.catShareIntent
import com.khalied.cukinggo.util.distanceMeters
import com.khalied.cukinggo.util.formatCoordinates
import com.khalied.cukinggo.util.formatDistance
import com.khalied.cukinggo.util.formatFullDateTime
import com.khalied.cukinggo.util.mapsLinkFor
import java.io.File

/** Batas tunggu singkat: cukup untuk satu fix GPS kalau belum ada posisi tersimpan. */
private const val DETAIL_LOCATION_TIMEOUT_MILLIS = 5_000L

@Composable
fun CatDetailScreen(
    viewModel: CatDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val container = remember(context) { context.appContainer }
    var distanceFromUser by remember { mutableStateOf<Double?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val currentCat = (uiState as? CatDetailUiState.Content)?.cat

    // Jarak dihitung sekali saat catatannya siap. Kalau lokasi user tidak tersedia,
    // chip jaraknya cukup tidak muncul (bukan error).
    LaunchedEffect(currentCat?.id) {
        distanceFromUser = null
        val cat = currentCat ?: return@LaunchedEffect
        if (!container.locationHelper.hasLocationPermission()) return@LaunchedEffect

        val location = container.locationHelper.lastKnownLocation()
            ?: container.locationHelper.getCurrentLocation(DETAIL_LOCATION_TIMEOUT_MILLIS)
            ?: return@LaunchedEffect

        distanceFromUser = distanceMeters(
            location.latitude,
            location.longitude,
            cat.latitude,
            cat.longitude
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
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

            is CatDetailUiState.Content -> DetailContent(
                cat = state.cat,
                distanceFromUser = distanceFromUser,
                onDeleteRequest = { showDeleteDialog = true },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    if (showDeleteDialog) {
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
                Text(
                    text = stringResource(R.string.detail_delete_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteCat(onDeleted = onBack)
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
    distanceFromUser: Double?,
    onDeleteRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            AsyncImage(
                model = File(cat.photoPath),
                contentDescription = stringResource(R.string.cd_cat_photo),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.extraLarge)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(
                    text = formatFullDateTime(cat.timestamp),
                    iconRes = R.drawable.ic_calendar,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
                InfoChip(
                    text = formatCoordinates(cat.latitude, cat.longitude),
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
                    Text(
                        text = "🐾 Catatan",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = cat.description
                            ?: stringResource(R.string.detail_no_description),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontStyle = if (cat.description == null) {
                            FontStyle.Italic
                        } else {
                            FontStyle.Normal
                        }
                    )
                }
            }
        }

        item { ShareCatButton(cat = cat) }

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
}

/**
 * Tombol bagikan: foto kucing dikirim bersama template chat berisi catatan
 * (kalau ada), koordinat, dan link Google Maps.
 *
 * Isi pesannya sengaja disusun dari string resource, bukan ditempel di kode,
 * supaya kalimatnya gampang diganti tanpa menyentuh logika intent-nya.
 */
@Composable
private fun ShareCatButton(cat: Cat, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val note = cat.description?.trim()?.takeIf { it.isNotEmpty() }
    val coordinates = formatCoordinates(cat.latitude, cat.longitude)
    val mapsLink = mapsLinkFor(cat.latitude, cat.longitude)

    val message = if (note != null) {
        stringResource(R.string.share_message_with_note, note, coordinates, mapsLink)
    } else {
        stringResource(R.string.share_message, coordinates, mapsLink)
    }
    val chooserTitle = stringResource(R.string.share_chooser_title)

    Button(
        onClick = {
            context.startActivity(
                Intent.createChooser(catShareIntent(context, cat.photoPath, message), chooserTitle)
            )
        },
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

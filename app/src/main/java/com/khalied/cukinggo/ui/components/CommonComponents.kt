package com.khalied.cukinggo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.khalied.cukinggo.R
import com.khalied.cukinggo.ui.theme.appCardOutline
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials

/** Chip kecil membulat untuk info lokasi/tanggal. */
@Composable
fun InfoChip(
    text: String,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(text = text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/**
 * Petunjuk satu baris bahwa kartu di daftar bisa dihapus dengan disapu ke kiri.
 *
 * Ditaruh di atas daftar, bukan di latar swipe di belakang kartu: apa pun yang
 * ditulis di sana cuma muncul sepotong-sepotong selagi kartunya disapu, jadi
 * kelihatan justru setelah orang menebak harus menyapu. Satu kalimat di tempat
 * yang tenang lebih terbaca daripada isyarat yang harus ditemukan dulu.
 *
 * Warnanya `error`, bukan `onSurfaceVariant` seperti teks bantu biasa: yang
 * ditunjuknya adalah tindakan menghapus, jadi ia harus terbaca sebagai peringatan
 * sejak sebelum jarinya menyentuh kartunya. `error` di tema terang #C0392B, di atas
 * latar FrostBg ~4,8:1, jadi tetap lolos ambang AA teks (R-25).
 */
@Composable
fun SwipeDeleteHint(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.list_delete_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = modifier
    )
}

/** Tinggi bar atas kecil Material3; angka ini yang disisakan konten di bawahnya. */
private val TOP_BAR_HEIGHT_DP = 64.dp

/**
 * Tinggi yang perlu disisakan konten supaya bar kaca bisa melayang di atasnya.
 *
 * App ini edge-to-edge, jadi status bar ikut dihitung. Bar-nya sendiri
 * (TopAppBar) sudah menangani inset itu; angka ini dipakai konten yang akan lewat
 * di belakang bar.
 */
@Composable
fun glassTopBarPadding(): Dp =
    WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TOP_BAR_HEIGHT_DP

/**
 * Bar judul dengan tombol kembali.
 *
 * [hazeState] diisi kalau konten di layar ini memang lewat di belakang bar
 * (misalnya daftar yang digulir), supaya bar jadi permukaan kaca yang memblur
 * isi di belakangnya. Null berarti tidak ada yang perlu diblur, dan bar memakai
 * latar tema seperti biasa.
 */
@OptIn(ExperimentalMaterial3Api::class, dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi::class)
@Composable
fun PlayfulTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val barModifier = if (hazeState != null) {
        modifier.hazeEffect(state = hazeState, style = HazeMaterials.ultraThin())
    } else {
        modifier
    }
    TopAppBar(
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
        modifier = barModifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_back),
                    contentDescription = stringResource(R.string.action_back)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = if (hazeState != null) {
                Color.Transparent
            } else {
                MaterialTheme.colorScheme.background
            },
            titleContentColor = MaterialTheme.colorScheme.primary,
            navigationIconContentColor = MaterialTheme.colorScheme.primary
        )
    )
}

/**
 * Penjelasan singkat kenapa izin dibutuhkan, ditampilkan sebelum dialog sistem muncul.
 */
@Composable
fun PermissionCard(
    cameraGranted: Boolean,
    locationGranted: Boolean,
    showSettingsHint: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = appCardOutline()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.permission_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            PermissionLine(
                granted = cameraGranted,
                text = stringResource(R.string.permission_camera)
            )
            PermissionLine(
                granted = locationGranted,
                text = stringResource(R.string.permission_location)
            )
            if (showSettingsHint && (!cameraGranted || !locationGranted)) {
                Text(
                    text = stringResource(R.string.permission_denied),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onRequest,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(text = stringResource(R.string.permission_grant))
                }
                if (showSettingsHint) {
                    TextButton(onClick = onOpenSettings) {
                        Text(
                            text = stringResource(R.string.permission_open_settings),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionLine(granted: Boolean, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = if (granted) "✅" else "🐾")
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

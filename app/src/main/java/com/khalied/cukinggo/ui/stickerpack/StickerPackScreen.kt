package com.khalied.cukinggo.ui.stickerpack

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.khalied.cukinggo.R
import com.khalied.cukinggo.ui.components.PlayfulTopBar
import com.khalied.cukinggo.ui.components.WalkingCatLoader
import com.khalied.cukinggo.ui.components.topBarContentPadding
import com.khalied.cukinggo.ui.theme.appCardOutline
import com.khalied.cukinggo.ui.theme.brutalShadow
import com.khalied.cukinggo.whatsapp.StickerPackRejection
import com.khalied.cukinggo.whatsapp.StickerPackRules
import com.khalied.cukinggo.whatsapp.StickerSource
import com.khalied.cukinggo.whatsapp.WhatsAppStickers
import java.io.File

/** Sisi satu kotak stiker: cukup untuk mengenali cukingnya, empat muat berjajar. */
private val STICKER_CELL_SIZE = 76.dp

/** Alpha stiker yang tidak dipilih, supaya yang masuk paket tetap menonjol. */
private const val UNSELECTED_ALPHA = 0.42f

/**
 * Paket stiker WhatsApp: memilih stiker yang ikut, lalu menambahkannya.
 *
 * Halamannya sengaja menjelaskan tiga hal yang tidak bisa dilihat dari tombolnya:
 * berapa stiker yang dipilih, apa yang terjadi pada filenya, dan kenapa tombolnya
 * kadang belum bisa dipakai. Semua itu supaya pengguna tidak menebak-nebak saat
 * paketnya tidak muncul di WhatsApp.
 */
@Composable
fun StickerPackScreen(
    viewModel: StickerPackViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var launchFailed by remember { mutableStateOf(false) }
    // Dibaca di sini, bukan di dalam lambda tombol: `context.getString` saat
    // diklik tidak ikut berganti kalau bahasa atau konfigurasi berubah.
    val chooserTitle = stringResource(R.string.sticker_pack_chooser)

    // Memakai hasil activity, bukan startActivity biasa: WhatsApp mengirim
    // alasan penolakan lewat result, dan tanpa itu dialog yang menutup sendiri
    // hanya terlihat seperti tombol yang diam.
    val addPackLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onAddPackResult(result.resultCode, result.data)
    }

    // Kembali dari WhatsApp: status "sudah ditambahkan" bisa berubah, dan itu
    // satu-satunya cara app ini tahu pengguna menyetujui dialognya.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onReturnedFromWhatsApp()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(Modifier.height(topBarContentPadding())) }

            item {
                Text(
                    text = stringResource(R.string.sticker_pack_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (uiState.isLoading) {
                item {
                    WalkingCatLoader(
                        text = stringResource(R.string.sticker_pack_loading),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    )
                }
            }

            if (uiState.stickers.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.sticker_pack_count, uiState.chosen.size, uiState.stickers.size),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                item {
                    StickerPicker(
                        stickers = uiState.stickers,
                        selected = uiState.selected,
                        selectionFull = uiState.selectionFull,
                        onToggle = viewModel::toggleSticker
                    )
                }

                item {
                    Text(
                        text = stringResource(R.string.sticker_pack_pick_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (uiState.selectionFull) {
                    item {
                        Text(
                            text = stringResource(R.string.sticker_pack_selection_full),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!uiState.isLoading && uiState.stickers.isEmpty()) {
                item {
                    NoticeCard(title = null, body = stringResource(R.string.sticker_pack_empty))
                }
            }

            if (!uiState.isLoading && uiState.stickers.isNotEmpty() &&
                uiState.chosen.size < StickerPackRules.MIN_STICKERS
            ) {
                item {
                    NoticeCard(
                        title = stringResource(
                            R.string.sticker_pack_too_few_title,
                            StickerPackRules.MIN_STICKERS
                        ),
                        body = stringResource(
                            R.string.sticker_pack_too_few_body,
                            uiState.chosen.size
                        )
                    )
                }
            }

            if (!uiState.whatsAppInstalled) {
                item {
                    NoticeCard(title = null, body = stringResource(R.string.sticker_pack_no_whatsapp))
                }
            }

            if (uiState.addedToWhatsApp) {
                item {
                    NoticeCard(title = null, body = stringResource(R.string.sticker_pack_added))
                }
            }

            if (uiState.exportFailed || launchFailed) {
                item {
                    NoticeCard(title = null, body = stringResource(R.string.sticker_pack_failed))
                }
            }

            uiState.rejection?.let { rejection ->
                item {
                    when (rejection) {
                        StickerPackRejection.ProviderNotVisible -> NoticeCard(
                            title = stringResource(R.string.sticker_pack_duplicate_title),
                            body = stringResource(R.string.sticker_pack_duplicate_body)
                        )

                        is StickerPackRejection.Other -> NoticeCard(
                            title = stringResource(R.string.sticker_pack_rejected_title),
                            body = stringResource(
                                R.string.sticker_pack_rejected_body,
                                rejection.reason
                            )
                        )
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.sticker_pack_size_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Text(
                    text = stringResource(R.string.sticker_pack_max_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Button(
                    onClick = {
                        launchFailed = false
                        viewModel.export {
                            val intent = WhatsAppStickers.addPackIntent(
                                context = context,
                                chooserTitle = chooserTitle
                            )
                            if (intent == null) {
                                launchFailed = true
                            } else {
                                runCatching { addPackLauncher.launch(intent) }
                                    .onFailure { launchFailed = true }
                            }
                        }
                    },
                    enabled = uiState.canExport && uiState.whatsAppInstalled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (uiState.isExporting) {
                            stringResource(R.string.sticker_pack_exporting)
                        } else if (uiState.addedToWhatsApp) {
                            stringResource(R.string.sticker_pack_update)
                        } else {
                            stringResource(R.string.sticker_pack_add)
                        }
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }

        PlayfulTopBar(
            title = stringResource(R.string.sticker_pack_title),
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

/**
 * Kisi stiker yang bisa diketuk.
 *
 * Yang dipakai `toggleable` + `Role.Checkbox`, bukan `clickable` biasa, supaya
 * TalkBack membacakan setiap stiker sebagai pilihan yang bisa dicentang, bukan
 * sebagai gambar tanpa arti.
 *
 * Saat 30 stiker sudah dipilih, yang belum dipilih tidak bisa ditekan: batasnya
 * milik WhatsApp, dan membiarkan ketukan yang tidak mengubah apa pun akan
 * terbaca seperti layar yang macet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StickerPicker(
    stickers: List<StickerSource>,
    selected: Set<String>,
    selectionFull: Boolean,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        stickers.forEach { sticker ->
            val isSelected = sticker.stickerPath in selected
            StickerCell(
                sticker = sticker,
                selected = isSelected,
                enabled = isSelected || !selectionFull,
                onToggle = { onToggle(sticker.stickerPath) }
            )
        }
    }
}

@Composable
private fun StickerCell(
    sticker: StickerSource,
    selected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .size(STICKER_CELL_SIZE)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (selected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier
                }
            )
            .toggleable(
                value = selected,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = { onToggle() }
            )
    ) {
        AsyncImage(
            model = File(sticker.stickerPath),
            contentDescription = stringResource(
                if (selected) R.string.cd_sticker_selected else R.string.cd_sticker_not_selected
            ),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .alpha(if (selected) 1f else UNSELECTED_ALPHA)
        )

        if (selected) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(22.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}

/**
 * Satu penjelasan pendek di kartu.
 *
 * Dipakai untuk semua pesan keadaan di layar ini supaya bentuknya sama, dan
 * supaya tidak ada pesan yang muncul sebagai teks telanjang di antara tombol.
 */
@Composable
private fun NoticeCard(title: String?, body: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = appCardOutline()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (title != null) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

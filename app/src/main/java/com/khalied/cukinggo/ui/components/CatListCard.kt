package com.khalied.cukinggo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.khalied.cukinggo.R
import com.khalied.cukinggo.domain.model.CatSighting
import com.khalied.cukinggo.ui.theme.appCardOutline
import com.khalied.cukinggo.util.formatDayLabel
import com.khalied.cukinggo.util.formatTime
import java.io.File

/**
 * Satu kartu mewakili satu penemuan, bukan satu cuking: daftar di Home tetap
 * berjalan menurut waktu, jadi ketemu tiga kali berarti tiga baris.
 */
@Composable
fun CatListCard(
    sighting: CatSighting,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Swipe sendiri tidak langsung menghapus: kartunya kembali ke tempatnya,
    // lalu muncul pertanyaan. Satu catatan berisi foto dan cerita yang tidak bisa
    // dikembalikan, jadi tidak boleh hilang cuma karena jari tergelincir.
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                showDeleteDialog = true
            }
            // Selalu false: yang memindahkan kartunya adalah jawaban pengguna di
            // dialog, bukan sapuannya sendiri.
            false
        }
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = MaterialTheme.shapes.extraLarge,
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = stringResource(R.string.home_delete_confirm_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.home_delete_confirm_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text(
                        text = stringResource(R.string.home_delete_confirm),
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

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            // Latarnya cuma digambar saat kartunya memang sedang disapu, jadi
            // kartu yang diam tidak pernah punya lapisan merah di belakangnya.
            //
            // Isinya cuma ikon, tanpa tulisan: ikon sudah cukup jadi isyarat
            // arah, sedangkan kalimat di sela-sela sapuan lebih sering terpotong
            // daripada terbaca. Penjelasan caranya ada di atas daftar (lihat
            // `SwipeDeleteHint`).
            if (dismissState.progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    // Tempat sampah, bukan jejak kaki: jejak kaki itu bahasa
                    // "cuking" di seluruh app, dan memakainya untuk menghapus
                    // justru salah baca. Hiasan, jadi tidak perlu dibacakan.
                    Icon(
                        painter = painterResource(R.drawable.ic_delete),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                // Satu kartu dibaca TalkBack sebagai satu simpul: judul dan tanggal
                // cukup sekali, tidak diulang per elemen di dalamnya.
                .semantics(mergeDescendants = true) {},
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                // Bidang pekat, bukan translusen seperti kartu kaca yang lain:
                // kartu ini punya latar swipe di belakangnya, dan kartu bening
                // membuat isi latar itu tembus, sehingga kartu yang diam terlihat
                // seperti membawa ikon hapus. Kepekatan kartu di daftar ini yang
                // memisahkan isi kartu dari isyarat sapuannya.
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = appCardOutline()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Foto sengaja dekoratif di daftar: teks di sebelahnya sudah
                // menjelaskan catatan ini, jadi label "Foto kucing" berulang di
                // setiap kartu hanya menambah bacaan tanpa menambah makna.
                // Di layar detail dan review foto, labelnya tetap dipakai karena
                // di sana fotonya memang isi utama.
                // Foto ini yang menyambung ke layar detail waktu kartunya disentuh
                // (lihat sharedCatPhoto), jadi potongan membulatnya dipasang lewat
                // helper itu, bukan lewat clip sendiri.
                AsyncImage(
                    model = File(sighting.photoPath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .sharedCatPhoto(sightingId = sighting.id, clip = RoundedCornerShape(18.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = sighting.catName
                            ?: sighting.description
                            ?: stringResource(R.string.detail_no_description),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontStyle = if (sighting.catName == null && sighting.description == null) {
                            FontStyle.Italic
                        } else {
                            FontStyle.Normal
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Catatan tetap ikut ditampilkan saat namanya ada, tapi dipotong
                    // satu baris supaya tingginya tidak mendorong kartu jadi gemuk.
                    if (sighting.catName != null && sighting.description != null) {
                        Text(
                            text = sighting.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "${
                            formatDayLabel(
                                sighting.timestamp,
                                stringResource(R.string.day_today),
                                stringResource(R.string.day_yesterday)
                            )
                        } · ${formatTime(sighting.timestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Paw ini hiasan, jadi tidak perlu ikut dibacakan TalkBack.
                Text(
                    text = "🐾",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clearAndSetSemantics {}
                )
            }
        }
    }
}

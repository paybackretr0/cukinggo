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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
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
import com.khalied.cukinggo.ui.theme.BlushPink
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
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.large)
                    .background(BlushPink)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_delete_hint),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onTertiary
                    )
                    Icon(
                        painter = painterResource(R.drawable.ic_paw),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiary,
                        modifier = Modifier.size(20.dp)
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
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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

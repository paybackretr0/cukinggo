package com.khalied.cukinggo.ui.catlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.khalied.cukinggo.R
import com.khalied.cukinggo.domain.model.Cat
import com.khalied.cukinggo.ui.components.CatListCard
import com.khalied.cukinggo.ui.components.InfoChip
import com.khalied.cukinggo.ui.components.PlayfulTopBar
import com.khalied.cukinggo.ui.components.SleepingCatIllustration
import com.khalied.cukinggo.ui.components.WalkingCatLoader
import com.khalied.cukinggo.ui.theme.InkSoft
import com.khalied.cukinggo.ui.theme.MintPop
import com.khalied.cukinggo.ui.theme.appCardOutline

/**
 * Daftar seluruh cuking yang pernah ditandai, dimuat sehalaman demi sehalaman.
 *
 * Yang berbeda dari daftar di Home cuma jumlah dan sumbernya: di sini seluruh
 * koleksi, dan ia dibaca dari Room per halaman supaya catatan yang sudah ribuan
 * tidak ikut dimuat semua saat halaman ini dibuka.
 */
@Composable
fun CatListScreen(
    viewModel: CatListViewModel,
    onCatClick: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cats = viewModel.cats.collectAsLazyPagingItems()
    val catCount by viewModel.catCount.collectAsStateWithLifecycle()

    val refreshState = cats.loadState.refresh
    val appendState = cats.loadState.append

    Column(modifier = modifier.fillMaxSize()) {
        PlayfulTopBar(title = stringResource(R.string.cat_list_title), onBack = onBack)

        // Chip jumlahnya disembunyikan selama angkanya belum kebaca, bukan
        // ditampilkan sebagai 0: angka 0 di layar terbaca seperti "koleksimu
        // kosong", padahal catatannya cuma belum selesai dihitung.
        val count = catCount
        if (count != null && count > 0) {
            InfoChip(
                text = stringResource(R.string.home_counter, count),
                containerColor = MintPop,
                contentColor = InkSoft,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
            )
        }

        when {
            // Halaman pertama gagal dibaca, dan belum ada satu pun kartu di layar:
            // jalan keluarnya cuma satu tombol, jadi layarnya tidak buntu.
            refreshState is LoadState.Error && cats.itemCount == 0 ->
                ListMessageCard { ListErrorBody(onRetry = cats::retry) }

            cats.itemCount > 0 -> CatList(
                cats = cats,
                appendState = appendState,
                onCatClick = onCatClick,
                onDelete = viewModel::deleteCat
            )

            // Paging bilang tidak ada apa-apa lagi: memang koleksinya kosong.
            refreshState.endOfPaginationReached -> ListMessageCard { ListEmptyBody() }

            // Sisanya berarti halaman pertamanya masih dalam perjalanan.
            else -> ListMessageCard {
                WalkingCatLoader(text = stringResource(R.string.home_loading))
            }
        }
    }
}

@Composable
private fun CatList(
    cats: LazyPagingItems<Cat>,
    appendState: LoadState,
    onCatClick: (Long) -> Unit,
    onDelete: (Cat) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp)
    ) {
        items(
            count = cats.itemCount,
            key = cats.itemKey { cat -> cat.id },
            contentType = cats.itemContentType { "cuking" }
        ) { index ->
            // Placeholder dimatikan di PagingConfig, jadi null cuma mungkin muncul
            // di antara dua pembaruan halaman; barisnya cukup dilewati.
            val cat = cats[index]
            if (cat != null) {
                CatListCard(
                    cat = cat,
                    onClick = { onCatClick(cat.id) },
                    onDelete = { onDelete(cat) }
                )
            }
        }

        // Halaman berikutnya sedang dimuat: kucing mondar-mandir, sama seperti
        // waktu app nyari lokasi, karena yang sedang terjadi memang "menyusul".
        if (appendState is LoadState.Loading) {
            item(key = "memuat-lagi") {
                WalkingCatLoader(
                    text = stringResource(R.string.cat_list_loading_more),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp)
                )
            }
        }

        // Halaman berikutnya gagal: yang sudah termuat tetap di layar, dan
        // menyambungnya cukup satu tombol.
        if (appendState is LoadState.Error) {
            item(key = "gagal-menyusul") {
                ListRetryRow(onRetry = cats::retry)
            }
        }

        // Penanda ujung daftar: tanpa ini, daftar yang berhenti karena sudah habis
        // terlihat sama seperti daftar yang berhenti karena macet.
        if (appendState.endOfPaginationReached && cats.itemCount > 0) {
            item(key = "ujung-daftar") {
                Text(
                    text = stringResource(R.string.cat_list_end),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp)
                )
            }
        }
    }
}

/** Wadah untuk keadaan selain "ada isinya", memakai bentuk yang sama dengan Home. */
@Composable
private fun ListMessageCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = appCardOutline()
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
    }
}

/**
 * Keadaan kosong di halaman ini memakai kalimat yang sama dengan keadaan kosong
 * Home, karena yang terjadi juga sama: belum ada satu catatan pun. Satu keadaan
 * tidak perlu punya dua versi kalimat.
 */
@Composable
private fun ListEmptyBody() {
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

@Composable
private fun ListErrorBody(onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary)
    ) {
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

/** Baris "coba lagi" untuk halaman yang gagal menyusul, bukan untuk layar yang buntu. */
@Composable
private fun ListRetryRow(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.error_load_title),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onRetry,
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Text(text = stringResource(R.string.add_retry))
        }
    }
}

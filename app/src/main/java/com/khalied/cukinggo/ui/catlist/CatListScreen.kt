package com.khalied.cukinggo.ui.catlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.khalied.cukinggo.R
import com.khalied.cukinggo.domain.model.CatSighting
import com.khalied.cukinggo.ui.components.CatListCard
import com.khalied.cukinggo.ui.components.InfoChip
import com.khalied.cukinggo.ui.components.OptionChip
import com.khalied.cukinggo.ui.components.PlayfulTopBar
import com.khalied.cukinggo.ui.components.SleepingCatIllustration
import com.khalied.cukinggo.ui.components.SwipeDeleteHint
import com.khalied.cukinggo.ui.components.topBarContentPadding
import com.khalied.cukinggo.ui.components.WalkingCatLoader
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
    onSightingClick: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sightings = viewModel.sightings.collectAsLazyPagingItems()
    val sightingCount by viewModel.sightingCount.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val isFiltering by viewModel.isFiltering.collectAsStateWithLifecycle()

    val refreshState = sightings.loadState.refresh
    val appendState = sightings.loadState.append

    val topBarPadding = topBarContentPadding()
    // Chip jumlahnya disembunyikan selama angkanya belum kebaca, bukan
    // ditampilkan sebagai 0: angka 0 di layar terbaca seperti "koleksimu
    // kosong", padahal catatannya cuma belum selesai dihitung.
    val count = sightingCount

    Box(modifier = modifier.fillMaxSize()) {
        when {
            // Halaman pertama gagal dibaca, dan belum ada satu pun kartu di layar:
            // jalan keluarnya cuma satu tombol, jadi layarnya tidak buntu.
            refreshState is LoadState.Error && sightings.itemCount == 0 ->
                ListMessageCard { ListErrorBody(onRetry = sightings::retry) }

            sightings.itemCount > 0 -> CatList(
                sightings = sightings,
                appendState = appendState,
                onSightingClick = onSightingClick,
                onDelete = viewModel::deleteSighting,
                count = count,
                query = query,
                filter = filter,
                onQueryChange = viewModel::onSearchChange,
                onFilterChange = viewModel::onFilterChange,
                topBarPadding = topBarPadding,
                modifier = Modifier
            )

            // Paging bilang tidak ada apa-apa lagi tapi pencariannya menyala:
            // koleksinya ada, yang tidak ada adalah yang dicari.
            refreshState.endOfPaginationReached && isFiltering -> ListMessageCard {
                NoMatchBody(onReset = viewModel::clearFilters)
            }

            // Paging bilang tidak ada apa-apa lagi: memang koleksinya kosong.
            refreshState.endOfPaginationReached -> ListMessageCard { ListEmptyBody() }

            // Sisanya berarti halaman pertamanya masih dalam perjalanan.
            else -> ListMessageCard {
                WalkingCatLoader(text = stringResource(R.string.home_loading))
            }
        }

        PlayfulTopBar(
            title = stringResource(R.string.cat_list_title),
            onBack = onBack,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
        )
    }
}

@Composable
private fun CatList(
    sightings: LazyPagingItems<CatSighting>,
    appendState: LoadState,
    onSightingClick: (Long) -> Unit,
    onDelete: (CatSighting) -> Unit,
    count: Int?,
    query: String,
    filter: CatListFilter,
    onQueryChange: (String) -> Unit,
    onFilterChange: (CatListFilter) -> Unit,
    topBarPadding: Dp,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = topBarPadding + 4.dp,
            bottom = 24.dp
        )
    ) {
        // Chip jumlah dibawa ke dalam daftar, bukan duduk tetap di bawah bar:
        // supaya isi halamannya benar-benar lewat di belakang bar kaca.
        if (count != null && count > 0) {
            item(key = "jumlah") {
                InfoChip(
                    text = stringResource(R.string.home_counter, count),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        // Kolom cari dan saringan ikut menggulir bersama daftarnya, bukan menetap
        // di bawah bar: yang dicari di sini adalah isi daftar, dan menetapkannya
        // berarti mengambil satu baris penuh dari layar yang isinya foto.
        if (count != null && count > 0) {
            item(key = "cari") {
                CatSearchField(query = query, onQueryChange = onQueryChange)
            }
            item(key = "saringan") {
                CatFilterRow(filter = filter, onFilterChange = onFilterChange)
            }
        }

        // Petunjuk hapus ditaruh di atas kartu-kartunya, bukan di latar swipe:
        // di latar swipe ia cuma terlihat sepotong selagi kartunya disapu, jadi
        // tidak terbaca sebagai petunjuk.
        item(key = "petunjuk-hapus") {
            SwipeDeleteHint()
        }

        items(
            count = sightings.itemCount,
            key = sightings.itemKey { sighting -> sighting.id },
            contentType = sightings.itemContentType { "cuking" }
        ) { index ->
            // Placeholder dimatikan di PagingConfig, jadi null cuma mungkin muncul
            // di antara dua pembaruan halaman; barisnya cukup dilewati.
            val sighting = sightings[index]
            if (sighting != null) {
                CatListCard(
                    sighting = sighting,
                    onClick = { onSightingClick(sighting.id) },
                    onDelete = { onDelete(sighting) }
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
                ListRetryRow(onRetry = sightings::retry)
            }
        }

        // Penanda ujung daftar: tanpa ini, daftar yang berhenti karena sudah habis
        // terlihat sama seperti daftar yang berhenti karena macet.
        if (appendState.endOfPaginationReached && sightings.itemCount > 0) {
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

/**
 * Kolom cari satu baris, dengan tombol hapus yang hanya muncul saat ada isinya.
 *
 * Tombol hapus ditaruh di dalam kolom, bukan sebagai tombol terpisah di sebelahnya:
 * satu kontrol yang muncul dan hilang mengikuti isi lebih ringkas daripada kolom
 * plus tombol yang harus selalu punya tempat.
 */
@Composable
private fun CatSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        placeholder = {
            Text(
                text = stringResource(R.string.cat_list_search_hint),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        trailingIcon = if (query.isEmpty()) {
            null
        } else {
            {
                Text(
                    text = stringResource(R.string.cat_list_search_clear),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { onQueryChange("") }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    )
}

/**
 * Saringan daftar, satu baris yang bisa digulir mendatar.
 *
 * Digulir, bukan dipaksa muat: di layar sempit atau saat huruf diperbesar, tiga
 * chip berteks panjang tidak akan muat dan yang terpotong justru pilihan terakhir.
 */
@Composable
private fun CatFilterRow(
    filter: CatListFilter,
    onFilterChange: (CatListFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            count = CatListFilter.entries.size,
            key = { index -> CatListFilter.entries[index].name }
        ) { index ->
            val entry = CatListFilter.entries[index]
            OptionChip(
                label = stringResource(entry.labelRes()),
                selected = filter == entry,
                onClick = { onFilterChange(entry) }
            )
        }
    }
}

private fun CatListFilter.labelRes(): Int = when (this) {
    CatListFilter.ALL -> R.string.cat_list_filter_all
    CatListFilter.UNNAMED -> R.string.cat_list_filter_unnamed
    CatListFilter.MOST_SEEN -> R.string.cat_list_filter_most_seen
}

/**
 * Keadaan "tidak ada yang cocok": beda dari koleksi kosong, karena yang kosong
 * cuma hasil saringannya. Jalan keluarnya satu tombol yang melepas saringan.
 */
@Composable
private fun NoMatchBody(onReset: () -> Unit) {
    Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SleepingCatIllustration(size = 120.dp)
        Text(
            text = stringResource(R.string.cat_list_no_match_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.cat_list_no_match_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onReset,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text(text = stringResource(R.string.cat_list_no_match_reset))
            }
        }
    }
}

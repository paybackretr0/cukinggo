package com.khalied.cukinggo.ui.weekly

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.khalied.cukinggo.R
import com.khalied.cukinggo.domain.model.CatSighting
import com.khalied.cukinggo.ui.components.glassTopBarPadding
import com.khalied.cukinggo.ui.components.InfoChip
import com.khalied.cukinggo.ui.components.PlayfulTopBar
import com.khalied.cukinggo.ui.components.SleepingCatIllustration
import com.khalied.cukinggo.ui.components.WalkingCatLoader
import com.khalied.cukinggo.ui.theme.IceAccent
import com.khalied.cukinggo.ui.theme.IceSoft
import com.khalied.cukinggo.ui.theme.appCardOutline
import com.khalied.cukinggo.ui.theme.frostBackdrop
import com.khalied.cukinggo.util.WeeklyRecap
import com.khalied.cukinggo.util.formatShortDate
import com.khalied.cukinggo.util.formatShortDay
import dev.chrisbanes.haze.HazeState
import java.io.File
import java.time.LocalDate

/** Tinggi area batang grafik, di luar label harinya. */
private val CHART_BAR_HEIGHT = 120.dp

/**
 * Tinggi minimum batang saat hari itu kosong: kalau nol betul-betul tidak
 * digambar, hari yang bolong justru terlihat seperti kolom yang hilang, bukan
 * seperti hari yang tidak ada catatannya.
 */
private const val CHART_EMPTY_BAR_FRACTION = 0.04f

/**
 * Rekap tujuh hari terakhir: berapa kali ketemu, berapa cuking baru, hari mana
 * yang aktif, dan cuking siapa saja yang lewat minggu ini.
 *
 * Isinya dihitung ulang dari koleksi yang sudah ada (lihat
 * [com.khalied.cukinggo.util.weeklyRecap]), jadi halaman ini tidak menyimpan
 * ringkasannya sendiri dan tidak bisa basi.
 */
@Composable
fun WeeklyRecapScreen(
    viewModel: WeeklyRecapViewModel,
    onSightingClick: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Sumber blur untuk bar kaca di atas, plus tinggi yang perlu disisakan daftar
    // di bawahnya supaya isinya benar-benar lewat di belakang bar itu.
    val hazeState = remember { HazeState() }
    val topBarPadding = glassTopBarPadding()

    Box(modifier = modifier.fillMaxSize()) {
        when (val state = uiState) {
            WeeklyRecapUiState.Loading -> RecapPlaceholder(
                loaderText = stringResource(R.string.home_loading)
            )

            WeeklyRecapUiState.Failed -> RecapPlaceholder(
                message = stringResource(R.string.error_load_title)
            )

            is WeeklyRecapUiState.Content -> RecapContent(
                recap = state.recap,
                onSightingClick = onSightingClick,
                topBarPadding = topBarPadding,
                modifier = Modifier.frostBackdrop(hazeState)
            )
        }

        PlayfulTopBar(
            title = stringResource(R.string.weekly_title),
            onBack = onBack,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            hazeState = hazeState
        )
    }
}

@Composable
private fun RecapContent(
    recap: WeeklyRecap,
    onSightingClick: (Long) -> Unit,
    topBarPadding: Dp,
    modifier: Modifier = Modifier
) {
    val firstDay = recap.days.first().date
    val lastDay = recap.days.last().date

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = topBarPadding + 4.dp,
            bottom = 24.dp
        )
    ) {
        item {
            Text(
                text = stringResource(
                    R.string.weekly_range,
                    formatShortDate(firstDay),
                    formatShortDate(lastDay)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (!recap.hasAnything) {
            item { WeeklyEmptyCard() }
        } else {
            item { WeeklyStats(recap = recap) }
            item { WeeklyChart(recap = recap, today = lastDay) }

            if (recap.catsSeen.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.weekly_cats_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(items = recap.catsSeen, key = { sighting -> sighting.id }) { sighting ->
                    WeeklyCatRow(
                        sighting = sighting,
                        onClick = { onSightingClick(sighting.id) }
                    )
                }
            }
        }
    }
}

/**
 * Statistik minggu ini. Barisnya bisa digulir mendatar supaya tiga chip tidak
 * pernah terpotong di layar sempit, dan angkanya boleh memanjang walau jumlah
 * catatannya besar.
 */
@Composable
private fun WeeklyStats(recap: WeeklyRecap, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InfoChip(
            text = stringResource(R.string.weekly_stat_sightings, recap.sightingCount),
            iconRes = R.drawable.ic_paw,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
        InfoChip(
            text = stringResource(R.string.weekly_stat_new_cats, recap.newCatCount),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        InfoChip(
            text = stringResource(R.string.weekly_stat_active_days, recap.activeDayCount),
            iconRes = R.drawable.ic_calendar
        )
    }
}

/**
 * Grafik batang tujuh hari. Tinggi batangnya proporsional terhadap hari paling
 * ramai minggu itu, karena angka mutlaknya sudah ada di chip statistik dan di
 * atas tiap batang, sedangkan yang dicari di grafik ini bentuk minggunya.
 */
@Composable
private fun WeeklyChart(
    recap: WeeklyRecap,
    today: LocalDate,
    modifier: Modifier = Modifier
) {
    val maxCount = recap.days.maxOfOrNull { day -> day.sightingCount }?.coerceAtLeast(1) ?: 1

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = appCardOutline()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.weekly_chart_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                recap.days.forEach { day ->
                    val isToday = day.date == today
                    val dayLabel = stringResource(
                        R.string.weekly_chart_day,
                        formatShortDay(day.date),
                        day.sightingCount
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = dayLabel },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (day.sightingCount > 0) day.sightingCount.toString() else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(CHART_BAR_HEIGHT),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            val fraction = (day.sightingCount.toFloat() / maxCount)
                                .coerceAtLeast(CHART_EMPTY_BAR_FRACTION)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.55f)
                                    .fillMaxHeight(fraction)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 6.dp,
                                            topEnd = 6.dp,
                                            bottomStart = 2.dp,
                                            bottomEnd = 2.dp
                                        )
                                    )
                                    // Hari ini dibedakan warnanya, bukan cuma
                                    // di-bold: aksen es penuh untuk hari ini,
                                    // versi pucatnya untuk enam hari lain.
                                    .background(
                                        if (isToday) IceAccent else IceSoft
                                    )
                            )
                        }
                        Text(
                            text = formatShortDay(day.date),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

/** Satu cuking yang ketemu minggu ini, tanpa aksi hapus: ini halaman ringkasan. */
@Composable
private fun WeeklyCatRow(
    sighting: CatSighting,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = appCardOutline()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AsyncImage(
                model = File(sighting.photoPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(MaterialTheme.shapes.large)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
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
                if (sighting.catName != null && sighting.description != null) {
                    Text(
                        text = sighting.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyEmptyCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = appCardOutline()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SleepingCatIllustration(size = 140.dp)
            Text(
                text = stringResource(R.string.weekly_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.weekly_empty_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RecapPlaceholder(
    modifier: Modifier = Modifier,
    loaderText: String? = null,
    message: String? = null
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
        }
    }
}

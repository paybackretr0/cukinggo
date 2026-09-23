package com.khalied.cukinggo.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.khalied.cukinggo.R
import com.khalied.cukinggo.appContainer
import com.khalied.cukinggo.domain.model.Cat
import com.khalied.cukinggo.ui.components.CatListCard
import com.khalied.cukinggo.ui.components.CatMapView
import com.khalied.cukinggo.ui.components.InfoChip
import com.khalied.cukinggo.ui.components.SleepingCatIllustration
import com.khalied.cukinggo.ui.components.ThemePickerDialog
import com.khalied.cukinggo.ui.components.WalkingCatLoader
import com.khalied.cukinggo.ui.theme.InkSoft
import com.khalied.cukinggo.ui.theme.MintPop
import com.khalied.cukinggo.ui.theme.PeachAccent
import com.khalied.cukinggo.ui.theme.ThemeMode
import com.khalied.cukinggo.ui.theme.appCardOutline

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddCat: () -> Unit,
    onCatClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Peta tetap digambar walau data belum siap, marker-nya menyusul sendiri.
    val cats = (uiState as? HomeUiState.Content)?.cats.orEmpty()
    val context = LocalContext.current
    val themePreferences = remember(context) { context.appContainer.themePreferences }
    val themeModeKey by themePreferences.themeModeKey.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }

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
                ThemePill(onClick = { showThemeDialog = true })
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
                    RecentCatsSection(
                        cats = state.cats,
                        onCatClick = onCatClick,
                        onDelete = viewModel::deleteCat,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    }

    if (showThemeDialog) {
        ThemePickerDialog(
            currentMode = ThemeMode.fromKey(themeModeKey),
            onModeSelected = { mode ->
                themePreferences.setThemeModeKey(mode.storageKey)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
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

/** Kontrol tema di header: satu-satunya setelan app, jadi cukup pill kecil. */
@Composable
private fun ThemePill(onClick: () -> Unit, modifier: Modifier = Modifier) {
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
                text = stringResource(R.string.theme_button),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun RecentCatsSection(
    cats: List<Cat>,
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
            InfoChip(
                text = stringResource(R.string.home_counter, cats.size),
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

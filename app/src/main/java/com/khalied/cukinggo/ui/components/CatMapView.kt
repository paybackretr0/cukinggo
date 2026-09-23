package com.khalied.cukinggo.ui.components

import android.Manifest
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.animation.OvershootInterpolator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.khalied.cukinggo.R
import com.khalied.cukinggo.appContainer
import com.khalied.cukinggo.domain.model.Cat
import com.khalied.cukinggo.location.LocationHelper
import com.khalied.cukinggo.ui.theme.BlushPink
import com.khalied.cukinggo.ui.theme.InkSoft
import com.khalied.cukinggo.util.isOnline
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.MapTileProviderBase
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker

private const val DEFAULT_LAT = -6.2
private const val DEFAULT_LNG = 106.816666
private const val MARKER_SIZE_DP = 52f

/** Di bawah zoom ini marker foto diganti gelembung angka supaya tidak saling nabrak. */
private const val PHOTO_ZOOM_THRESHOLD = 14.0

/** Zoom minimal saat peta "diantar" ke lokasi user. */
private const val MY_LOCATION_ZOOM = 16.0

private const val POP_IN_START_SCALE = 0.05f

/** Tombol target: boleh menunggu agak lama karena user memang minta dicarikan. */
private const val BUTTON_LOCATION_TIMEOUT_MILLIS = 10_000L

/** Penanda otomatis: singkat saja, supaya tidak menyalakan GPS kelamaan diam-diam. */
private const val AUTO_LOCATION_TIMEOUT_MILLIS = 5_000L

private const val MESSAGE_VISIBLE_MILLIS = 3_500L

/**
 * Satu dua tile gagal itu wajar (misal tile di tepi area), jadi kabar peta baru
 * muncul kalau kegagalannya menumpuk tanpa satu pun tile berhasil dimuat.
 */
private const val TILE_FAILURE_THRESHOLD = 3

/** Satu marker di peta: foto satu kucing, atau gelembung angka berisi beberapa kucing. */
private data class MarkerSpec(
    val key: String,
    val point: GeoPoint,
    val cat: Cat?,
    val count: Int
)

/**
 * Peta OpenStreetMap (osmdroid).
 *
 * Zoom dekat  -> marker berupa foto kucing.
 * Zoom jauh   -> kucing dikelompokkan per grid dan ditampilkan sebagai angka jumlahnya.
 * Titik mint  -> lokasi user, muncul otomatis begitu izin lokasi aktif.
 * Tombol target di kanan bawah -> peta loncat ke lokasi user saat ini.
 */
@Composable
fun CatMapView(
    cats: List<Cat>,
    onCatClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val container = remember(context) { context.appContainer }
    val currentOnCatClick by rememberUpdatedState(onCatClick)
    val density = context.resources.displayMetrics.density
    val markerSizePx = remember(density) { (MARKER_SIZE_DP * density).toInt().coerceAtLeast(32) }

    // Jumlah kegagalan tile berturut-turut, dipakai untuk kabar "peta belum bisa dimuat".
    var consecutiveTileFailures by remember { mutableStateOf(0) }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            setTilesScaledToDpi(true)
            minZoomLevel = 4.0
            maxZoomLevel = 20.0
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(14.0)
            controller.setCenter(GeoPoint(DEFAULT_LAT, DEFAULT_LNG))
            overlays.add(CopyrightOverlay(context))
        }
    }

    // Latar peta saat tile belum termuat: osmdroid menggambar pola kotak abu-abu
    // bawaan library yang tidak nyambung dengan palet app, padahal itu yang tampil
    // pertama kali peta dibuka (dan terus tampil kalau offline). Diganti isian datar
    // dari color role, jadi area peta tetap terbaca sebagai bagian dari app ini.
    val mapBaseColor = MaterialTheme.colorScheme.surfaceVariant.toArgb()
    LaunchedEffect(mapView, mapBaseColor) {
        mapView.setBackgroundColor(mapBaseColor)
        mapView.overlayManager.tilesOverlay?.apply {
            setLoadingBackgroundColor(mapBaseColor)
            // Warna garis disamakan supaya sisa pola kotak library hilang total.
            setLoadingLineColor(mapBaseColor)
        }
        mapView.invalidate()
    }

    // --- status lokasi user ---
    val scope = rememberCoroutineScope()
    var hasLocationPermission by remember {
        mutableStateOf(container.locationHelper.hasLocationPermission())
    }
    var locationRefreshKey by remember { mutableStateOf(0) }
    var myLocationMarker by remember { mutableStateOf<Marker?>(null) }
    var isLocating by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    // Diambil di sini supaya tetap configuration-aware (bukan context.getString di dalam callback).
    val locationUnknownMessage = stringResource(R.string.home_location_unknown)
    val locationPermissionMessage = stringResource(R.string.home_location_permission)

    // osmdroid melaporkan hasil tiap permintaan tile lewat Handler: MAPTILE_SUCCESS_ID
    // atau MAPTILE_FAIL_ID. Itu sinyal asli "tile gagal dimuat", jadi tidak perlu menebak.
    DisposableEffect(mapView) {
        val tileRequestHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(message: Message) {
                when (message.what) {
                    MapTileProviderBase.MAPTILE_SUCCESS_ID -> consecutiveTileFailures = 0
                    MapTileProviderBase.MAPTILE_FAIL_ID -> consecutiveTileFailures += 1
                }
                mapView.invalidate()
            }
        }
        mapView.tileProvider.setTileRequestCompleteHandler(tileRequestHandler)
        onDispose { /* handler ikut selesai bersama mapView-nya */ }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
                    // Izin bisa berubah / dicabut saat app di background.
                    hasLocationPermission = container.locationHelper.hasLocationPermission()
                    locationRefreshKey++
                }

                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    val showMyLocation: (Location, Boolean) -> Unit = { location, moveCamera ->
        val point = GeoPoint(location.latitude, location.longitude)
        val marker = myLocationMarker ?: Marker(mapView).apply {
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = BitmapDrawable(mapView.resources, CatMarkerRenderer.userDot(markerSizePx))
            mapView.overlays.add(this)
        }
        marker.position = point
        myLocationMarker = marker
        if (moveCamera) {
            if (mapView.zoomLevelDouble < MY_LOCATION_ZOOM) {
                mapView.controller.setZoom(MY_LOCATION_ZOOM)
            }
            mapView.controller.animateTo(point)
        }
        mapView.invalidate()
    }

    val hideMyLocation: () -> Unit = {
        myLocationMarker?.let { marker ->
            mapView.overlays.remove(marker)
            myLocationMarker = null
            mapView.invalidate()
        }
    }

    // Titik lokasi tampil sendiri saat izin sudah diberikan, tanpa perlu tekan tombol.
    // Dipakai posisi terakhir yang diketahui dulu (instan), baru GPS singkat kalau kosong.
    LaunchedEffect(hasLocationPermission, locationRefreshKey) {
        if (!hasLocationPermission) {
            hideMyLocation()
            return@LaunchedEffect
        }
        val location = container.locationHelper.lastKnownLocation()
            ?: container.locationHelper.getCurrentLocation(AUTO_LOCATION_TIMEOUT_MILLIS)
        if (location != null) {
            showMyLocation(location, false)
        }
    }

    val performLocate: suspend () -> Unit = {
        isLocating = true
        val location = findUserLocation(container.locationHelper)
        isLocating = false
        if (location != null) {
            showMyLocation(location, true)
        } else {
            message = locationUnknownMessage
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        hasLocationPermission = container.locationHelper.hasLocationPermission()
        if (hasLocationPermission) {
            scope.launch { performLocate() }
        } else {
            message = locationPermissionMessage
        }
    }

    val locateMyPosition: () -> Unit = {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else if (!isLocating) {
            scope.launch { performLocate() }
        }
    }

    // Pesan singkat muncul lalu hilang sendiri.
    LaunchedEffect(message) {
        if (message != null) {
            delay(MESSAGE_VISIBLE_MILLIS)
            message = null
        }
    }

    // --- marker kucing: foto di zoom dekat, angka di zoom jauh ---
    var zoomLevel by remember { mutableStateOf(mapView.zoomLevelDouble) }
    DisposableEffect(mapView) {
        val listener = object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean = false

            override fun onZoom(event: ZoomEvent?): Boolean {
                zoomLevel = event?.zoomLevel ?: mapView.zoomLevelDouble
                return false
            }
        }
        mapView.addMapListener(listener)
        onDispose { mapView.removeMapListener(listener) }
    }

    val showPhotos = zoomLevel >= PHOTO_ZOOM_THRESHOLD
    val zoomBucket = zoomLevel.roundToInt()
    val specs = remember(cats, showPhotos, zoomBucket) {
        buildMarkerSpecs(cats = cats, showPhotos = showPhotos, zoomLevel = zoomBucket)
    }
    val thumbnails = remember(markerSizePx) { PhotoThumbnailCache(markerSizePx) }

    val markers = remember { mutableMapOf<String, Marker>() }
    val animatedKeys = remember { mutableSetOf<String>() }
    var hasCenteredOnce by remember { mutableStateOf(false) }

    LaunchedEffect(specs, mapView) {
        val currentKeys = specs.map { it.key }.toSet()
        markers.filterKeys { it !in currentKeys }.forEach { (key, marker) ->
            mapView.overlays.remove(marker)
            markers.remove(key)
        }

        val added = specs.filter { it.key !in markers }
        added.forEach { spec ->
            val marker = Marker(mapView).apply {
                position = spec.point
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                val cat = spec.cat
                if (cat != null) {
                    setOnMarkerClickListener { _, _ ->
                        currentOnCatClick(cat.id)
                        true
                    }
                } else {
                    setOnMarkerClickListener { _, _ ->
                        // Gelembung angka: zoom masuk ke area itu.
                        mapView.controller.animateTo(spec.point)
                        mapView.controller.setZoom(
                            (mapView.zoomLevelDouble + 2.0).coerceAtMost(mapView.maxZoomLevel)
                        )
                        true
                    }
                }
            }
            mapView.overlays.add(marker)
            markers[spec.key] = marker
        }

        added.forEachIndexed { index, spec ->
            launch {
                val marker = markers[spec.key] ?: return@launch
                val render = rendererFor(spec, context, thumbnails, markerSizePx) ?: return@launch
                // Marker foto kucing baru tetap jadi "momen bintang": pop-in bertahap.
                val shouldAnimate = spec.cat != null && animatedKeys.add(spec.key)
                if (shouldAnimate) {
                    marker.icon = BitmapDrawable(mapView.resources, render(POP_IN_START_SCALE))
                    delay(index * 70L)
                    animateMarkerPopIn(mapView, marker, render)
                } else {
                    marker.icon = BitmapDrawable(mapView.resources, render(1f))
                }
            }
        }

        if (!hasCenteredOnce && cats.isNotEmpty()) {
            hasCenteredOnce = true
            val newest = cats.first()
            mapView.controller.setZoom(16.0)
            mapView.controller.animateTo(GeoPoint(newest.latitude, newest.longitude))
        }
        mapView.invalidate()
    }

    Box(modifier = modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        // FAB ukuran standar (56dp): satu-satunya kontrol di peta, harus nyaman
        // dijangkau jempol satu tangan dan memenuhi tap target minimum (R-03).
        FloatingActionButton(
            onClick = locateMyPosition,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(14.dp),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            if (isLocating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_my_location),
                    contentDescription = stringResource(R.string.home_my_location),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Kabar peta: muncul kalau tile gagal menumpuk, hilang sendiri begitu ada
        // satu tile yang berhasil, jadi tidak perlu tombol tutup.
        if (consecutiveTileFailures >= TILE_FAILURE_THRESHOLD) {
            val isOffline = remember(consecutiveTileFailures) { !context.isOnline() }
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, end = 14.dp, top = 14.dp),
                shape = MaterialTheme.shapes.large,
                color = BlushPink,
                contentColor = InkSoft,
                shadowElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(
                            if (isOffline) {
                                R.string.map_offline_notice
                            } else {
                                R.string.map_tiles_failed_notice
                            }
                        ),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(
                        onClick = {
                            consecutiveTileFailures = 0
                            mapView.invalidate()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.add_retry),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        val currentMessage = message
        AnimatedVisibility(
            visible = currentMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, end = 76.dp, bottom = 14.dp)
        ) {
            // Pill ini memang melayang di atas peta, jadi shadow-nya beralasan
            // (satu-satunya tempat shadow dipakai selain kartu peta dan FAB).
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shadowElevation = 3.dp
            ) {
                Text(
                    text = currentMessage.orEmpty(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/** Posisi terakhir yang diketahui (cepat), fallback ke pencarian lokasi baru. */
private suspend fun findUserLocation(locationHelper: LocationHelper): Location? =
    locationHelper.lastKnownLocation()
        ?: locationHelper.getCurrentLocation(BUTTON_LOCATION_TIMEOUT_MILLIS)

/** Renderer marker sesuai isi spec: foto kucing, atau gelembung angka. */
private suspend fun rendererFor(
    spec: MarkerSpec,
    context: Context,
    thumbnails: PhotoThumbnailCache,
    sizePx: Int
): ((Float) -> Bitmap)? {
    val cat = spec.cat
    if (cat != null) {
        val photo = thumbnails.thumbnail(cat.photoPath)
        if (photo != null) {
            return { scale -> CatMarkerRenderer.photoMarker(photo, sizePx, scale) }
        }
        // Fotonya hilang: tetap tampilkan sesuatu yang masuk akal.
        return { scale -> CatMarkerRenderer.countMarker(context, 1, sizePx, scale) }
    }
    return { scale -> CatMarkerRenderer.countMarker(context, spec.count, sizePx, scale) }
}

private fun buildMarkerSpecs(
    cats: List<Cat>,
    showPhotos: Boolean,
    zoomLevel: Int
): List<MarkerSpec> {
    if (showPhotos || cats.isEmpty()) {
        return cats.map { cat ->
            MarkerSpec(
                key = "cat_${cat.id}",
                point = GeoPoint(cat.latitude, cat.longitude),
                cat = cat,
                count = 1
            )
        }
    }

    return clusterCats(cats, zoomLevel).map { cluster ->
        MarkerSpec(
            key = "cluster_${cluster.cellLatitude}_${cluster.cellLongitude}_$zoomLevel",
            point = GeoPoint(cluster.latitude, cluster.longitude),
            cat = null,
            count = cluster.count
        )
    }
}

private suspend fun animateMarkerPopIn(
    mapView: MapView,
    marker: Marker,
    render: (Float) -> Bitmap
) {
    val resources = mapView.resources
    val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 420L
        interpolator = OvershootInterpolator(1.4f)
        addUpdateListener { animation ->
            val scale = (animation.animatedValue as Float).coerceAtLeast(POP_IN_START_SCALE)
            marker.icon = BitmapDrawable(resources, render(scale))
            mapView.invalidate()
        }
    }
    animator.start()
    delay(440L)
}

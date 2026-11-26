package com.khalied.cukinggo.ui.components

import android.Manifest
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.location.Location
import android.view.MotionEvent
import android.view.View
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.GroundOverlay
import com.google.android.gms.maps.model.GroundOverlayOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.khalied.cukinggo.R
import com.khalied.cukinggo.appContainer
import com.khalied.cukinggo.domain.model.CatSighting
import com.khalied.cukinggo.location.LocationHelper
import com.khalied.cukinggo.ui.theme.Cyan
import com.khalied.cukinggo.ui.theme.Ink
import com.khalied.cukinggo.ui.theme.brutalShadow
import com.khalied.cukinggo.util.isOnline
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

private const val DEFAULT_LAT = -6.2
private const val DEFAULT_LNG = 106.816666
private const val MARKER_SIZE_DP = 52f

/** Di bawah zoom ini marker foto diganti gelembung angka supaya tidak saling nabrak. */
private const val PHOTO_ZOOM_THRESHOLD = 14f

/** Zoom minimal saat peta "diantar" ke lokasi user. */
private const val MY_LOCATION_ZOOM = 16f

/** Zoom saat peta diantar ke satu titik saja, misalnya satu catatan cuking. */
private const val POINT_ZOOM = 16f

/**
 * Batas zoom peta. Bawahnya Indonesia sudah tidak terbaca sebagai negara, atasnya
 * tile Google tidak punya gambar lagi.
 */
private const val MIN_ZOOM = 4f
private const val MAX_ZOOM = 20f

private const val POP_IN_START_SCALE = 0.05f

/** Tombol target: boleh menunggu agak lama karena user memang minta dicarikan. */
private const val BUTTON_LOCATION_TIMEOUT_MILLIS = 10_000L

/** Penanda otomatis: singkat saja, supaya tidak menyalakan GPS kelamaan diam-diam. */
private const val AUTO_LOCATION_TIMEOUT_MILLIS = 5_000L

private const val MESSAGE_VISIBLE_MILLIS = 3_500L

/**
 * Jarak antar pemeriksaan koneksi selama peta terbuka.
 *
 * Google Maps tidak memberi kabar saat tile-nya gagal, jadi satu-satunya sinyal
 * jujur yang tersisa adalah "HP-nya punya internet atau tidak". Dua detik cukup
 * cepat untuk terasa langsung, dan cukup jarang untuk tidak membebani apa pun.
 */
private const val CONNECTIVITY_CHECK_MILLIS = 2_000L

/** Lebar garis jejak dalam dp: cukup untuk terbaca di atas gambar peta. */
private const val TRAIL_WIDTH_DP = 5f

/**
 * Lebar jejak yang sedang disorot: sedikit lebih tebal dari yang lain.
 *
 * Bezanya harus cukup untuk terbaca tanpa membuat jejak lain terlihat rusak,
 * jadi 2dp saja, bukan dua kali lipat.
 */
private const val TRAIL_HIGHLIGHT_WIDTH_DP = 7f

/** Jejak digambar paling bawah di antara lapisan yang kita tambahkan sendiri. */
private const val TRAIL_Z_INDEX = 1f

/** Titik lokasi user di atas jejak, tapi tetap di bawah marker cuking. */
private const val USER_DOT_Z_INDEX = 2f

/**
 * Kekuatan warna jejak yang sedang tidak disorot.
 *
 * Jejak lain tetap digambar, bukan disembunyikan: yang dilihat pengguna jadi
 * "yang ini di antara yang lain", bukan jejak yang berdiri sendiri tanpa konteks.
 */
private const val TRAIL_ALPHA_DIMMED = 0x33

/**
 * Kekuatan warna tiap ruas jejak, 0 sampai 255.
 *
 * Ruas paling lama dipudarkan dan ruas paling baru dibuat penuh, jadi arah
 * jejaknya terbaca tanpa panah atau nomor urut di tiap titik, dan tanpa marker
 * tambahan yang menutupi foto cukingnya.
 */
private const val TRAIL_ALPHA_OLDEST = 0x66
private const val TRAIL_ALPHA_NEWEST = 0xFF

/** Lapisan panas duduk paling bawah: jejak, titik user, dan marker tetap di atasnya. */
private const val HEAT_Z_INDEX = 0.5f

/** Sedikit bening supaya nama jalan dan tempat di bawahnya tetap terbaca. */
private const val HEAT_TRANSPARENCY = 0.35f

/** Satu marker di peta: foto satu penemuan, atau gelembung angka berisi beberapa penemuan. */
private data class MarkerSpec(
    val key: String,
    val point: LatLng,
    val sighting: CatSighting?,
    val count: Int
)

/**
 * Peta Google (Maps SDK untuk Android), digambar di dalam `MapView` biasa.
 *
 * Zoom dekat  -> marker berupa foto penemuan, disambung garis jejak tiap cuking.
 * Zoom jauh   -> penemuan dikelompokkan per grid dan ditampilkan sebagai angka jumlahnya.
 * Titik mint  -> lokasi user, muncul otomatis begitu izin lokasi aktif.
 * Tombol target di kanan bawah -> peta loncat ke lokasi user saat ini.
 *
 * Yang digambar adalah penemuannya, satu titik per penemuan: cuking yang pernah
 * ketemu di dua tempat memang layak terlihat di dua tempat. [onSightingClick]
 * menerima id penemuannya, karena itu yang dibuka layar detail.
 *
 * [showLocateButton] dipakai layar yang menaruh peta ini di dalam kartu sempit:
 * tombol "ke lokasi kamu" di sana memakan ruang tanpa menambah makna, karena yang
 * dilihat memang jejak cukingnya, bukan posisi pengguna.
 *
 * [fitToContent] dipakai tempat yang isinya jejak satu cuking saja: peta dipaskan
 * supaya seluruh jejaknya masuk layar, bukan berpusat di tempat terakhir dia ketemu.
 *
 * [highlightCatId] menyorot satu cuking: jejaknya jadi pekat dan sedikit lebih
 * tebal, jejak cuking lain tetap ada tapi diredupkan, dan kamera diantar ke
 * jejaknya. Null berarti tidak ada yang disorot.
 */
@Composable
fun CatMapView(
    sightings: List<CatSighting>,
    onSightingClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    showLocateButton: Boolean = true,
    fitToContent: Boolean = false,
    highlightCatId: Long? = null,
    /** Lapisan sebaran: makin pekat artinya makin banyak penemuan di situ. */
    showHeatmap: Boolean = false,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val container = remember(context) { context.appContainer }
    val currentOnSightingClick by rememberUpdatedState(onSightingClick)
    val density = context.resources.displayMetrics.density
    val markerSizePx = remember(density) { (MARKER_SIZE_DP * density).toInt().coerceAtLeast(32) }
    val mapBaseColor = MaterialTheme.colorScheme.surfaceVariant.toArgb()

    // Peta baru ada setelah Google Play Services menyerahkan objeknya, dan
    // beberapa efek di bawah memang harus menunggu itu. Null berarti "belum siap".
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }

    val mapView = remember {
        HoldableMapView(context).apply {
            // Latar View ini yang terlihat sebelum tile pertama datang, jadi ia
            // diisi warna permukaan kartu, bukan kotak abu-abu bawaan library.
            setBackgroundColor(mapBaseColor)
        }
    }

    LaunchedEffect(mapView, mapBaseColor) {
        mapView.setBackgroundColor(mapBaseColor)
    }

    // Siklus hidup MapView diurus di sini, karena Compose tidak tahu apa-apa soal
    // Surface yang dipakai peta untuk menggambar.
    DisposableEffect(mapView) {
        mapView.onCreate(null)
        mapView.onStart()
        mapView.onResume()
        mapView.getMapAsync { map ->
            map.uiSettings.apply {
                isZoomControlsEnabled = false
                // Peta ini selalu 2D: memutar dan memiringkan bidangnya cuma
                // membuat jejak cuking terlihat melengkung tanpa menambah arti.
                isRotateGesturesEnabled = false
                isTiltGesturesEnabled = false
                isCompassEnabled = false
                isMapToolbarEnabled = false
                isMyLocationButtonEnabled = false
            }
            map.setMinZoomPreference(MIN_ZOOM)
            map.setMaxZoomPreference(MAX_ZOOM)
            map.moveCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(DEFAULT_LAT, DEFAULT_LNG), PHOTO_ZOOM_THRESHOLD)
            )
            map.setOnMarkerClickListener { marker ->
                val spec = marker.tag as? MarkerSpec ?: return@setOnMarkerClickListener false
                val sighting = spec.sighting
                if (sighting != null) {
                    currentOnSightingClick(sighting.id)
                } else {
                    // Gelembung angka: zoom masuk ke area itu.
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            spec.point,
                            (map.cameraPosition.zoom + 2f).coerceAtMost(MAX_ZOOM)
                        )
                    )
                }
                true
            }
            googleMap = map
        }
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
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

    DisposableEffect(lifecycleOwner, mapView) {
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
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val showMyLocation: (Location, Boolean) -> Unit = { location, moveCamera ->
        val map = googleMap
        if (map != null) {
            val position = LatLng(location.latitude, location.longitude)
            val existing = myLocationMarker
            if (existing != null) {
                existing.position = position
            } else {
                myLocationMarker = map.addMarker(
                    MarkerOptions()
                        .position(position)
                        .anchor(0.5f, 0.5f)
                        .icon(
                            BitmapDescriptorFactory.fromBitmap(
                                CatMarkerRenderer.userDot(markerSizePx)
                            )
                        )
                        .zIndex(USER_DOT_Z_INDEX)
                )
            }
            if (moveCamera) {
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        position,
                        map.cameraPosition.zoom.coerceAtLeast(MY_LOCATION_ZOOM)
                    )
                )
            }
        }
    }

    val hideMyLocation: () -> Unit = {
        myLocationMarker?.remove()
        myLocationMarker = null
    }

    // Titik lokasi tampil sendiri saat izin sudah diberikan, tanpa perlu tekan tombol.
    // Dipakai posisi terakhir yang diketahui dulu (instan), baru GPS singkat kalau kosong.
    LaunchedEffect(hasLocationPermission, locationRefreshKey, googleMap) {
        if (!hasLocationPermission) {
            hideMyLocation()
            return@LaunchedEffect
        }
        if (googleMap == null) return@LaunchedEffect
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

    // --- kabar offline ---
    // Maps SDK tidak punya panggilan balik untuk "tile gagal", jadi yang dipakai
    // adalah kenyataan yang bisa diperiksa sendiri: ada internet atau tidak.
    var isOffline by remember { mutableStateOf(!context.isOnline()) }
    LaunchedEffect(mapView) {
        while (true) {
            isOffline = !context.isOnline()
            delay(CONNECTIVITY_CHECK_MILLIS)
        }
    }

    // --- marker kucing: foto di zoom dekat, angka di zoom jauh ---
    var zoomLevel by remember { mutableStateOf(PHOTO_ZOOM_THRESHOLD) }
    DisposableEffect(googleMap) {
        val map = googleMap
        if (map == null) {
            onDispose { }
        } else {
            map.setOnCameraMoveListener {
                zoomLevel = map.cameraPosition.zoom
                // Menyentuh peta juga membatalkan sorotan kamera? Tidak: yang
                // digeser pengguna tidak dikembalikan, dan memang tidak ada yang
                // mengembalikannya kecuali sorotannya berganti.
            }
            onDispose { map.setOnCameraMoveListener(null) }
        }
    }

    val showPhotos = zoomLevel >= PHOTO_ZOOM_THRESHOLD
    val zoomBucket = zoomLevel.roundToInt()
    val specs = remember(sightings, showPhotos, zoomBucket) {
        buildMarkerSpecs(
            sightings = sightings,
            showPhotos = showPhotos,
            zoomLevel = zoomBucket
        )
    }
    val thumbnails = remember(markerSizePx) { PhotoThumbnailCache(markerSizePx) }

    val markers = remember { mutableMapOf<String, Marker>() }
    val animatedKeys = remember { mutableSetOf<String>() }
    var hasFramedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(specs, googleMap, markerSizePx) {
        val map = googleMap ?: return@LaunchedEffect
        val currentKeys = specs.map { it.key }.toSet()
        markers.filterKeys { it !in currentKeys }.forEach { (key, marker) ->
            marker.remove()
            markers.remove(key)
        }

        val added = specs.filter { it.key !in markers }
        added.forEach { spec ->
            // Gelembung angka dipasang lebih dulu sebagai pengganti sesaat: marker
            // Google tidak bisa ditambahkan tanpa ikon, dan foto marker butuh
            // pembacaan file yang tidak boleh menahan frame.
            val placeholder = CatMarkerRenderer.countMarker(context, spec.count, markerSizePx)
            val marker = map.addMarker(
                MarkerOptions()
                    .position(spec.point)
                    .anchor(0.5f, 0.5f)
                    .icon(BitmapDescriptorFactory.fromBitmap(placeholder))
                    .zIndex(USER_DOT_Z_INDEX + 1f)
            ) ?: return@forEach
            marker.tag = spec
            markers[spec.key] = marker
        }

        added.forEachIndexed { index, spec ->
            launch {
                val marker = markers[spec.key] ?: return@launch
                val render = rendererFor(spec, context, thumbnails, markerSizePx) ?: return@launch
                // Marker foto penemuan baru tetap jadi "momen bintang": pop-in bertahap.
                val shouldAnimate = spec.sighting != null && animatedKeys.add(spec.key)
                if (shouldAnimate) {
                    marker.setIcon(
                        BitmapDescriptorFactory.fromBitmap(render(POP_IN_START_SCALE))
                    )
                    delay(index * 70L)
                    animateMarkerPopIn(marker, render)
                } else {
                    marker.setIcon(BitmapDescriptorFactory.fromBitmap(render(1f)))
                }
            }
        }
    }

    // --- kamera: diantar sekali saat peta dibuka, lalu tiap kali sorotannya berganti ---
    // Daftar catatannya juga jadi kunci, bukan cuma sorotannya, karena frame
    // pertama datang saat datanya masih kosong. Efek sampingnya: kalau ada catatan
    // baru sewaktu sorotannya menyala, petanya dipaskan ulang ke jejak itu, dan itu
    // memang yang diharapkan karena jejaknya barusan bertambah.
    LaunchedEffect(highlightCatId, sightings, googleMap) {
        val map = googleMap ?: return@LaunchedEffect
        if (sightings.isEmpty()) return@LaunchedEffect

        // Melepas sorotan sengaja tidak menggeser peta: yang dilihat pengguna setelah
        // itu adalah semua jejak di area yang sedang dia lihat, sedangkan
        // mengembalikan kamera ke tempat terakhir cuma membuat peta melompat tanpa
        // dia minta.
        if (highlightCatId == null && hasFramedOnce) return@LaunchedEffect

        val targets = if (highlightCatId == null) {
            sightings
        } else {
            sightings.filter { sighting -> sighting.catId == highlightCatId }
        }
        if (targets.isEmpty()) return@LaunchedEffect

        val isFirstFraming = !hasFramedOnce
        hasFramedOnce = true

        // Intinya ada dua: saat sorotan atau pemaskaan aktif, yang dibingkai adalah
        // bentang seluruh catatannya; kalau tidak, tempat yang paling akhir dicatat.
        val focus = if (highlightCatId != null || fitToContent) trailFocus(targets) else null
        val point = if (focus != null) {
            LatLng(focus.latitude, focus.longitude)
        } else {
            val newest = sightings.first()
            LatLng(newest.latitude, newest.longitude)
        }
        val zoom = (focus?.zoom ?: POINT_ZOOM.toDouble()).toFloat()

        if (isFirstFraming) {
            // Pembingkaian pertama dipasang langsung, bukan dianimasikan: peta baru
            // dibuat di titik bawaannya (Jakarta), jadi menganimasikannya berarti
            // petanya terbang dulu dari Jakarta ke lokasi cukingnya tiap kali layar
            // ini dibuka. Gerakan itu tidak menjelaskan apa pun, sedangkan yang
            // dibutuhkan justru petanya sudah berada di tempat yang benar.
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(point, zoom))
        } else {
            // Perubahan berikutnya memang perlu dianimasikan: sorotannya berganti
            // saat petanya sudah terlihat, dan lompatan mendadak di situ terbaca
            // seperti peta yang digambar ulang.
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(point, zoom))
        }
    }

    // --- garis jejak: perjalanan satu cuking dari tempat ke tempat ---
    // Hanya muncul di zoom dekat, sama seperti marker foto. Di zoom jauh penemuan
    // diganti gelembung angka, dan garis yang menghubungkan titik-titik yang tidak
    // lagi terlihat justru menyesatkan.
    val trails = remember(sightings, showPhotos) {
        if (showPhotos) buildTrails(sightings) else emptyList()
    }
    val trailOverlays = remember { mutableListOf<Polyline>() }

    LaunchedEffect(trails, googleMap, highlightCatId, density) {
        val map = googleMap ?: return@LaunchedEffect
        trailOverlays.forEach { overlay -> overlay.remove() }
        trailOverlays.clear()

        trails.forEach { trail ->
            val isEmphasised = highlightCatId == null || trail.catId == highlightCatId
            val color = trailColorFor(trail.catId)
            val segmentCount = trail.points.size - 1
            val widthDp = if (highlightCatId != null && isEmphasised) {
                TRAIL_HIGHLIGHT_WIDTH_DP
            } else {
                TRAIL_WIDTH_DP
            }
            trail.points.zipWithNext().forEachIndexed { index, (from, to) ->
                val strength = if (isEmphasised) {
                    TRAIL_ALPHA_OLDEST +
                        (TRAIL_ALPHA_NEWEST - TRAIL_ALPHA_OLDEST) * (index + 1) / segmentCount
                } else {
                    TRAIL_ALPHA_DIMMED
                }
                val polyline = map.addPolyline(
                    PolylineOptions()
                        // Dua titik, bukan geodesik: ruasnya garis lurus antara dua
                        // catatan, sama seperti sebelumnya, karena yang dicatat cuma
                        // titik ketemunya, bukan rute perjalanannya.
                        .add(
                            LatLng(from.latitude, from.longitude),
                            LatLng(to.latitude, to.longitude)
                        )
                        .color((strength shl 24) or (color and 0x00FFFFFF))
                        .width(widthDp * density)
                        .startCap(RoundCap())
                        .endCap(RoundCap())
                        .zIndex(TRAIL_Z_INDEX)
                )
                trailOverlays.add(polyline)
            }
        }
    }

    // --- peta panas: sebaran semua penemuan ---
    // Gambarnya dihitung di thread latar (512x512 piksel plus penghalusan), lalu
    // dipasang sebagai GroundOverlay supaya menempel di koordinat, bukan di layar.
    var heatOverlay by remember { mutableStateOf<GroundOverlay?>(null) }
    LaunchedEffect(showHeatmap, sightings, googleMap) {
        val map = googleMap ?: return@LaunchedEffect
        heatOverlay?.remove()
        heatOverlay = null
        if (!showHeatmap || sightings.isEmpty()) return@LaunchedEffect

        val layer = withContext(Dispatchers.Default) { CatHeatmap.render(sightings) }
            ?: return@LaunchedEffect
        heatOverlay = map.addGroundOverlay(
            GroundOverlayOptions()
                .image(BitmapDescriptorFactory.fromBitmap(layer.bitmap))
                .positionFromBounds(
                    LatLngBounds(
                        LatLng(layer.south, layer.west),
                        LatLng(layer.north, layer.east)
                    )
                )
                .transparency(HEAT_TRANSPARENCY)
                .zIndex(HEAT_Z_INDEX)
        )
    }

    Box(modifier = modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        // FAB ukuran standar (56dp): satu-satunya kontrol di peta, harus nyaman
        // dijangkau jempol satu tangan dan memenuhi tap target minimum (R-03).
        if (showLocateButton) {
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
        }

        // Kabar peta: muncul selama HP tidak punya internet, dan hilang sendiri
        // begitu koneksinya kembali. Tombolnya memeriksa ulang saat itu juga,
        // karena menunggu dua detik terasa seperti tombol yang tidak bekerja.
        if (isOffline) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, end = 14.dp, top = 14.dp)
                    .brutalShadow(offset = 3.dp),
                shape = MaterialTheme.shapes.large,
                // Sian, bukan permukaan biasa: ini kabar, bukan isi peta, dan
                // warnanya harus langsung terbaca sebagai pesan sekilas.
                color = Cyan,
                contentColor = Ink,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.map_offline_notice),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    TextButton(onClick = { isOffline = !context.isOnline() }) {
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
                // 48dp, bukan 14dp: logo "Google" yang wajib terlihat di kiri
                // bawah peta tidak boleh tertutup pill ini.
                .padding(start = 14.dp, end = 76.dp, bottom = 48.dp)
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

/** Renderer marker sesuai isi spec: foto penemuan, atau gelembung angka. */
private suspend fun rendererFor(
    spec: MarkerSpec,
    context: Context,
    thumbnails: PhotoThumbnailCache,
    sizePx: Int
): ((Float) -> Bitmap)? {
    val sighting = spec.sighting
    if (sighting != null) {
        val photo = thumbnails.thumbnail(sighting.photoPath)
        if (photo != null) {
            return { scale -> CatMarkerRenderer.photoMarker(photo, sizePx, scale) }
        }
        // Fotonya hilang: tetap tampilkan sesuatu yang masuk akal.
        return { scale -> CatMarkerRenderer.countMarker(context, 1, sizePx, scale) }
    }
    return { scale -> CatMarkerRenderer.countMarker(context, spec.count, sizePx, scale) }
}

private fun buildMarkerSpecs(
    sightings: List<CatSighting>,
    showPhotos: Boolean,
    zoomLevel: Int
): List<MarkerSpec> {
    if (showPhotos || sightings.isEmpty()) {
        return sightings.map { sighting ->
            MarkerSpec(
                key = "sighting_${sighting.id}",
                point = LatLng(sighting.latitude, sighting.longitude),
                sighting = sighting,
                count = 1
            )
        }
    }

    return clusterSightings(sightings, zoomLevel).map { cluster ->
        MarkerSpec(
            key = "cluster_${cluster.cellLatitude}_${cluster.cellLongitude}_$zoomLevel",
            point = LatLng(cluster.latitude, cluster.longitude),
            sighting = null,
            count = cluster.count
        )
    }
}

private suspend fun animateMarkerPopIn(marker: Marker, render: (Float) -> Bitmap) {
    val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 420L
        interpolator = OvershootInterpolator(1.4f)
        addUpdateListener { animation ->
            val scale = (animation.animatedValue as Float).coerceAtLeast(POP_IN_START_SCALE)
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(render(scale)))
        }
    }
    animator.start()
    delay(440L)
}

/**
 * `MapView` yang menahan gesturnya sendiri supaya tidak direbut daftar di luarnya.
 *
 * `setOnTouchListener` tidak bisa dipakai untuk ini: sentuhan peta ditangani View di
 * dalam `MapView`, jadi jalur listener milik `MapView` sendiri tidak pernah dilewati
 * dan listener-nya tidak pernah dipanggil. Semua sentuhan tetap lewat
 * `dispatchTouchEvent`, jadi permintaan tahannya dipasang di situ.
 */
private class HoldableMapView(context: Context) : MapView(context) {
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> keepGestureFromParent(keep = true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                keepGestureFromParent(keep = false)
        }
        return super.dispatchTouchEvent(event)
    }
}

/**
 * Meminta induk di luar peta tidak menyela gestur yang mulai di peta.
 *
 * Peta ini View Android biasa di dalam interop Compose, sedangkan daftar di luarnya
 * digulir oleh Compose. Tanpa permintaan ini, daftar memangsa geseran beberapa frame
 * setelah jari turun, jadi peta terasa perlu digeser dua kali: geseran pertama
 * memindah halaman, baru yang kedua memindah peta. Interop Compose memahami
 * `requestDisallowInterceptTouchEvent` dan menandai geseran sesudahnya sebagai
 * terpakai, sehingga daftar tidak mulai menggulir sama sekali.
 *
 * Ditelusuri sepanjang rantai induk, bukan cuma induk langsung, karena yang
 * memahami permintaan ini adalah wadah interop Compose, bukan tetangga terdekatnya.
 */
private fun View.keepGestureFromParent(keep: Boolean) {
    var parent = parent
    while (parent != null) {
        parent.requestDisallowInterceptTouchEvent(keep)
        parent = parent.parent
    }
}

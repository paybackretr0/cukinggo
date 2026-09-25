package com.khalied.cukinggo.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.khalied.cukinggo.appContainer
import com.khalied.cukinggo.ui.addcat.AddCatScreen
import com.khalied.cukinggo.ui.addcat.AddCatViewModel
import com.khalied.cukinggo.ui.addcat.NEW_CAT_ID
import com.khalied.cukinggo.ui.catlist.CatListScreen
import com.khalied.cukinggo.ui.catlist.CatListViewModel
import com.khalied.cukinggo.ui.components.LocalAnimatedVisibilityScope
import com.khalied.cukinggo.ui.components.LocalSharedTransitionScope
import com.khalied.cukinggo.ui.detail.CatDetailScreen
import com.khalied.cukinggo.ui.detail.CatDetailViewModel
import com.khalied.cukinggo.ui.home.HomeScreen
import com.khalied.cukinggo.ui.home.HomeViewModel

object Routes {
    const val HOME = "home"

    /** Daftar seluruh cuking, dibuka dari baris "Lihat semua" di Home. */
    const val CAT_LIST = "cat_list"

    /** Nama argumennya dipakai juga saat membaca nilainya dari back stack entry. */
    const val ARG_CAPTURE = "capture"

    /**
     * Cuking yang ditambahi penemuan. Nol (lihat `NEW_CAT_ID`) berarti bikin
     * cuking baru, dan itu yang dipakai tombol "Tandai cuking" di Home.
     */
    const val ARG_CAT = "cat"
    const val ADD_CAT = "add_cat?$ARG_CAPTURE={$ARG_CAPTURE}&$ARG_CAT={$ARG_CAT}"

    /**
     * Layar detail dibuka dengan id penemuan, bukan id cuking: semua yang bisa
     * disentuh pengguna (kartu di daftar, penanda di peta, widget, kabar dekat)
     * mewakili satu momen ketemu, dan dari situ cukingnya dicari.
     */
    const val ARG_SIGHTING_ID = "sightingId"
    const val CAT_DETAIL = "cat_detail/{$ARG_SIGHTING_ID}"

    /** [capture] true berarti kamera langsung menjepret sendiri begitu siap. */
    fun addCat(capture: Boolean = false, catId: Long = NEW_CAT_ID) =
        "add_cat?$ARG_CAPTURE=$capture&$ARG_CAT=$catId"

    fun catDetail(sightingId: Long) = "cat_detail/$sightingId"
}

/**
 * Tempo transisi antar layar.
 *
 * Sekitar tiga ratus milidetik: cukup untuk membaca arah geraknya, belum cukup
 * untuk terasa menunggu. Semua transisi di bawah memakai angka-angka ini supaya
 * tempo antar layarnya konsisten, dan durasi fade-nya lebih pendek dari
 * geserannya supaya tidak ada saat layar terlihat kosong di tengah jalan.
 */
private const val NAV_SLIDE_MILLIS = 300
private const val NAV_FADE_MILLIS = 220

/**
 * Layar kamera bergerak lebih jauh (satu layar penuh ke atas) daripada geseran
 * mendatar antar halaman, jadi ia dikasih waktu sedikit lebih longgar.
 */
private const val NAV_CAMERA_SLIDE_MILLIS = 380

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CukingGoNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    openSightingId: Long? = null,
    onOpenSightingConsumed: () -> Unit = {},
    openCapture: Boolean = false,
    onOpenCaptureConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val container = remember(context) { context.appContainer }

    // Tap widget di layar utama membawa id penemuan, dan itu langsung dibuka.
    LaunchedEffect(openSightingId) {
        if (openSightingId != null) {
            navController.navigate(Routes.catDetail(openSightingId))
            onOpenSightingConsumed()
        }
    }

    // Tombol jepret di widget membuka layar kamera tanpa mampir ke Home dulu,
    // lalu kameranya menjepret sendiri (lihat AddCatScreen).
    LaunchedEffect(openCapture) {
        if (openCapture) {
            navController.navigate(Routes.addCat(capture = true))
            onOpenCaptureConsumed()
        }
    }

    // Ruang gambar bersamanya ada di luar NavHost, karena foto yang menyambung
    // antar layar itu digambar di lapisan atas keduanya.
    SharedTransitionLayout(modifier = modifier) {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            // Ukuran layarnya sudah dipasang di SharedTransitionLayout, jadi di
            // sini tidak dipakai dua kali.
            modifier = Modifier,
            // Bawaan untuk alur telusur (Home ke Detail, Home ke daftar lengkap,
            // dan daftar ke Detail): layar barunya masuk dari kanan sambil layar
            // lama ikut mundur ke kiri, cara Android menunjukkan "masuk lebih dalam".
            enterTransition = {
                slideIntoContainer(
                    towards = SlideDirection.Left,
                    animationSpec = tween(NAV_SLIDE_MILLIS, easing = FastOutSlowInEasing)
                ) + fadeIn(tween(NAV_FADE_MILLIS))
            },
            exitTransition = {
                // Menuju layar kamera, layar lama tidak ikut bergeser: kameranya
                // masuk dari bawah, dan dua gerakan ke arah berbeda terbaca
                // seperti dua layar yang tidak berhubungan. Layar lama cukup
                // memudar sambil menyusut sedikit.
                if (targetState.destination.isCameraScreen()) {
                    fadeOut(tween(NAV_FADE_MILLIS)) +
                        scaleOut(
                            targetScale = 0.97f,
                            animationSpec = tween(NAV_CAMERA_SLIDE_MILLIS)
                        )
                } else {
                    slideOutOfContainer(
                        towards = SlideDirection.Left,
                        animationSpec = tween(NAV_SLIDE_MILLIS, easing = FastOutSlowInEasing)
                    ) + fadeOut(tween(NAV_FADE_MILLIS))
                }
            },
            popEnterTransition = {
                // Kembali dari kamera: layar tujuan muncul dengan memudar juga,
                // karena tadi pun ia tidak ikut bergeser.
                if (initialState.destination.isCameraScreen()) {
                    fadeIn(tween(NAV_FADE_MILLIS)) +
                        scaleIn(
                            initialScale = 0.97f,
                            animationSpec = tween(NAV_CAMERA_SLIDE_MILLIS)
                        )
                } else {
                    slideIntoContainer(
                        towards = SlideDirection.Right,
                        animationSpec = tween(NAV_SLIDE_MILLIS, easing = FastOutSlowInEasing)
                    ) + fadeIn(tween(NAV_FADE_MILLIS))
                }
            },
            popExitTransition = {
                slideOutOfContainer(
                    towards = SlideDirection.Right,
                    animationSpec = tween(NAV_SLIDE_MILLIS, easing = FastOutSlowInEasing)
                ) + fadeOut(tween(NAV_FADE_MILLIS))
            }
        ) {
            composable(Routes.HOME) {
                SharedTransitionScopes(
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                ) {
                    val viewModel: HomeViewModel = viewModel(
                        factory = HomeViewModel.factory(container)
                    )
                    HomeScreen(
                        viewModel = viewModel,
                        onAddCat = { navController.navigate(Routes.addCat()) },
                        onSightingClick = { sightingId ->
                            navController.navigate(Routes.catDetail(sightingId))
                        },
                        onSeeAllCats = { navController.navigate(Routes.CAT_LIST) }
                    )
                }
            }

            composable(Routes.CAT_LIST) {
                SharedTransitionScopes(
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                ) {
                    val viewModel: CatListViewModel = viewModel(
                        factory = CatListViewModel.factory(container)
                    )
                    CatListScreen(
                        viewModel = viewModel,
                        onSightingClick = { sightingId ->
                            navController.navigate(Routes.catDetail(sightingId))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            composable(
                route = Routes.ADD_CAT,
                arguments = listOf(
                    navArgument(Routes.ARG_CAPTURE) {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                    navArgument(Routes.ARG_CAT) {
                        type = NavType.LongType
                        defaultValue = NEW_CAT_ID
                    }
                ),
                // Layar kamera bukan tempat yang ditelusuri, tapi satu tugas yang
                // dikerjakan lalu ditinggalkan, jadi ia naik dari bawah dan turun
                // lagi ke bawah saat selesai atau dibatalkan. Layar ini tidak
                // memakai cakupan animasi bersama: tidak ada foto di sini yang
                // menyambung ke layar lain.
                enterTransition = {
                    slideIntoContainer(
                        towards = SlideDirection.Up,
                        animationSpec = tween(
                            NAV_CAMERA_SLIDE_MILLIS,
                            easing = FastOutSlowInEasing
                        )
                    ) + fadeIn(tween(NAV_FADE_MILLIS))
                },
                popExitTransition = {
                    slideOutOfContainer(
                        towards = SlideDirection.Down,
                        animationSpec = tween(
                            NAV_CAMERA_SLIDE_MILLIS,
                            easing = FastOutSlowInEasing
                        )
                    ) + fadeOut(tween(NAV_FADE_MILLIS))
                }
            ) { backStackEntry ->
                val catId = backStackEntry.arguments?.getLong(Routes.ARG_CAT) ?: NEW_CAT_ID
                val viewModel: AddCatViewModel = viewModel(
                    factory = AddCatViewModel.factory(container, catId)
                )
                AddCatScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                    autoCapture = backStackEntry.arguments
                        ?.getBoolean(Routes.ARG_CAPTURE)
                        ?: false
                )
            }

            composable(
                route = Routes.CAT_DETAIL,
                arguments = listOf(
                    navArgument(Routes.ARG_SIGHTING_ID) { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val sightingId = backStackEntry.arguments
                    ?.getLong(Routes.ARG_SIGHTING_ID)
                    ?: return@composable
                SharedTransitionScopes(
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this
                ) {
                    val viewModel: CatDetailViewModel = viewModel(
                        factory = CatDetailViewModel.factory(sightingId, container)
                    )
                    CatDetailScreen(
                        viewModel = viewModel,
                        onAddSighting = { catId ->
                            navController.navigate(Routes.addCat(catId = catId))
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

/**
 * Menyediakan cakupan animasi bersama untuk satu layar tujuan.
 *
 * Cuma layar yang punya foto bersama yang dibungkus ini, jadi cakupannya tidak
 * pernah ikut terbawa ke layar yang tidak memakainya.
 */
@Composable
private fun SharedTransitionScopes(
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalSharedTransitionScope provides sharedTransitionScope,
        LocalAnimatedVisibilityScope provides animatedVisibilityScope,
        content = content
    )
}

/**
 * Layar kamera dibandingkan dari route-nya, dan itu memang yang tersimpan di
 * NavDestination: polanya ("add_cat?capture={capture}"), bukan alamat yang sedang
 * dibuka.
 */
private fun NavDestination?.isCameraScreen(): Boolean = this?.route == Routes.ADD_CAT

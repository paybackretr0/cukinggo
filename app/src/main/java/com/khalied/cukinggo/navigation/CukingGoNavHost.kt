package com.khalied.cukinggo.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.khalied.cukinggo.appContainer
import com.khalied.cukinggo.ui.addcat.AddCatScreen
import com.khalied.cukinggo.ui.addcat.AddCatViewModel
import com.khalied.cukinggo.ui.detail.CatDetailScreen
import com.khalied.cukinggo.ui.detail.CatDetailViewModel
import com.khalied.cukinggo.ui.home.HomeScreen
import com.khalied.cukinggo.ui.home.HomeViewModel

object Routes {
    const val HOME = "home"
    const val ADD_CAT = "add_cat"
    const val CAT_DETAIL = "cat_detail/{catId}"

    fun catDetail(catId: Long) = "cat_detail/$catId"
}

@Composable
fun CukingGoNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    openCatId: Long? = null,
    onOpenCatConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val container = remember(context) { context.appContainer }

    // Tap widget di layar utama membawa id kucing, dan itu langsung dibuka.
    LaunchedEffect(openCatId) {
        if (openCatId != null) {
            navController.navigate(Routes.catDetail(openCatId))
            onOpenCatConsumed()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
        composable(Routes.HOME) {
            val viewModel: HomeViewModel = viewModel(
                factory = HomeViewModel.factory(container)
            )
            HomeScreen(
                viewModel = viewModel,
                onAddCat = { navController.navigate(Routes.ADD_CAT) },
                onCatClick = { catId -> navController.navigate(Routes.catDetail(catId)) }
            )
        }

        composable(Routes.ADD_CAT) {
            val viewModel: AddCatViewModel = viewModel(
                factory = AddCatViewModel.factory(container)
            )
            AddCatScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.CAT_DETAIL,
            arguments = listOf(navArgument("catId") { type = NavType.LongType })
        ) { backStackEntry ->
            val catId = backStackEntry.arguments?.getLong("catId") ?: return@composable
            val viewModel: CatDetailViewModel = viewModel(
                factory = CatDetailViewModel.factory(catId, container)
            )
            CatDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

package ru.kbzhu.tracker.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ru.kbzhu.tracker.R
import ru.kbzhu.tracker.ui.diary.DiaryScreen
import ru.kbzhu.tracker.ui.foods.FoodDetailScreen
import ru.kbzhu.tracker.ui.foods.FoodEditScreen
import ru.kbzhu.tracker.ui.foods.FoodSearchScreen
import ru.kbzhu.tracker.ui.foods.RecipeEditScreen
import ru.kbzhu.tracker.ui.onboarding.OnboardingScreen
import ru.kbzhu.tracker.ui.profile.ProfileScreen
import ru.kbzhu.tracker.ui.scanner.ScannerScreen
import ru.kbzhu.tracker.ui.stats.StatsScreen
import kotlin.reflect.KClass

private data class TopLevelTab(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val label: Int,
    val selectedIcon: ImageVector,
    val icon: ImageVector
)

private val tabs = listOf(
    TopLevelTab(DiaryRoute, DiaryRoute::class, R.string.nav_diary, Icons.AutoMirrored.Filled.MenuBook, Icons.AutoMirrored.Outlined.MenuBook),
    TopLevelTab(FoodsRoute, FoodsRoute::class, R.string.nav_foods, Icons.Filled.Restaurant, Icons.Outlined.Restaurant),
    TopLevelTab(StatsRoute, StatsRoute::class, R.string.nav_stats, Icons.Filled.BarChart, Icons.Outlined.BarChart),
    TopLevelTab(ProfileRoute, ProfileRoute::class, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person)
)

@Composable
fun KbzhuNavApp(startWithOnboarding: Boolean) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val showBottomBar = tabs.any { tab -> destination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true }

    Column(Modifier.fillMaxSize()) {
        // Нижняя панель сама учитывает системную навигацию — экраны над ней не должны добавлять отступ ещё раз
        Box(
            Modifier
                .weight(1f)
                .then(if (showBottomBar) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier)
        ) {
            KbzhuNavHost(navController, startWithOnboarding)
        }
        AnimatedVisibility(
            visible = showBottomBar,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            BottomBar(navController, destination)
        }
    }
}

@Composable
private fun BottomBar(navController: NavHostController, destination: NavDestination?) {
    NavigationBar {
        tabs.forEach { tab ->
            val selected = destination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(tab.route) {
                        // Дневник всегда лежит в основании стека вкладок
                        popUpTo<DiaryRoute> { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(if (selected) tab.selectedIcon else tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.label)) }
            )
        }
    }
}

private const val ANIM_MS = 300

private fun AnimatedContentTransitionScope<NavBackStackEntry>.enter(): EnterTransition =
    fadeIn(tween(ANIM_MS)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(ANIM_MS)) { it / 6 }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exit(): ExitTransition =
    fadeOut(tween(ANIM_MS)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(ANIM_MS)) { it / 6 }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnter(): EnterTransition =
    fadeIn(tween(ANIM_MS)) + slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(ANIM_MS)) { it / 6 }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popExit(): ExitTransition =
    fadeOut(tween(ANIM_MS)) + slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(ANIM_MS)) { it / 6 }

@Composable
private fun KbzhuNavHost(navController: NavHostController, startWithOnboarding: Boolean) {
    NavHost(
        navController = navController,
        startDestination = if (startWithOnboarding) OnboardingRoute() else DiaryRoute,
        enterTransition = { enter() },
        exitTransition = { exit() },
        popEnterTransition = { popEnter() },
        popExitTransition = { popExit() }
    ) {
        composable<OnboardingRoute> { entry ->
            val route = entry.toRoute<OnboardingRoute>()
            OnboardingScreen(
                onFinished = {
                    if (route.editMode) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(DiaryRoute) {
                            popUpTo<OnboardingRoute> { inclusive = true }
                        }
                    }
                },
                onClose = if (route.editMode) ({ navController.popBackStack(); Unit }) else null
            )
        }

        // ---------- Вкладки (переключаются без анимации сдвига) ----------
        composable<DiaryRoute>(
            enterTransition = { fadeIn(tween(200)) },
            exitTransition = { fadeOut(tween(200)) }
        ) {
            DiaryScreen(
                onAddFood = { meal, date -> navController.navigate(AddFoodRoute(meal.name, date.toEpochDay())) },
                onScan = { meal, date -> navController.navigate(ScannerRoute(meal.name, date.toEpochDay())) },
                onEditEntry = { entryId -> navController.navigate(FoodDetailRoute(entryId = entryId)) }
            )
        }

        composable<FoodsRoute>(
            enterTransition = { fadeIn(tween(200)) },
            exitTransition = { fadeOut(tween(200)) }
        ) {
            FoodSearchScreen(
                onFoodSelected = { id -> navController.navigate(FoodDetailRoute(foodId = id)) },
                onCreateFood = { navController.navigate(FoodEditRoute()) },
                onCreateRecipe = { navController.navigate(RecipeEditRoute()) },
                onEditFood = { food ->
                    navController.navigate(if (food.isRecipe) RecipeEditRoute(food.id) else FoodEditRoute(food.id))
                },
                onScan = { navController.navigate(ScannerRoute()) },
                onBack = null
            )
        }

        composable<StatsRoute>(
            enterTransition = { fadeIn(tween(200)) },
            exitTransition = { fadeOut(tween(200)) }
        ) {
            StatsScreen()
        }

        composable<ProfileRoute>(
            enterTransition = { fadeIn(tween(200)) },
            exitTransition = { fadeOut(tween(200)) }
        ) {
            ProfileScreen(
                onEditProfile = { navController.navigate(OnboardingRoute(editMode = true)) }
            )
        }

        // ---------- Вложенные экраны ----------
        composable<AddFoodRoute> { entry ->
            val route = entry.toRoute<AddFoodRoute>()
            FoodSearchScreen(
                onFoodSelected = { id ->
                    navController.navigate(FoodDetailRoute(foodId = id, meal = route.meal, date = route.date))
                },
                onCreateFood = { navController.navigate(FoodEditRoute(meal = route.meal, date = route.date)) },
                onCreateRecipe = { navController.navigate(RecipeEditRoute()) },
                onEditFood = { food ->
                    navController.navigate(if (food.isRecipe) RecipeEditRoute(food.id) else FoodEditRoute(food.id))
                },
                onScan = { navController.navigate(ScannerRoute(route.meal, route.date)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable<FoodDetailRoute> { entry ->
            val route = entry.toRoute<FoodDetailRoute>()
            FoodDetailScreen(
                onBack = { navController.popBackStack() },
                onEditFood = { food ->
                    navController.navigate(if (food.isRecipe) RecipeEditRoute(food.id) else FoodEditRoute(food.id))
                },
                onDone = {
                    // Добавили из дневника или правили запись — возвращаемся в дневник
                    val fromDiary = route.date != null || route.entryId != null
                    if (!(fromDiary && navController.popBackStack<DiaryRoute>(inclusive = false))) {
                        navController.popBackStack()
                    }
                }
            )
        }

        composable<FoodEditRoute> { entry ->
            val route = entry.toRoute<FoodEditRoute>()
            FoodEditScreen(
                onBack = { navController.popBackStack() },
                onSaved = { id, isNew ->
                    if (isNew) {
                        // Новый продукт сразу открываем для выбора порции
                        navController.navigate(FoodDetailRoute(foodId = id, meal = route.meal, date = route.date)) {
                            popUpTo<FoodEditRoute> { inclusive = true }
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
                onDeleted = {
                    // Удалили продукт — закрываем и редактор, и карточку
                    navController.popBackStack()
                    if (navController.currentDestination?.hasRoute(FoodDetailRoute::class) == true) {
                        navController.popBackStack()
                    }
                }
            )
        }

        composable<RecipeEditRoute> {
            RecipeEditScreen(
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
                onDeleted = {
                    navController.popBackStack()
                    if (navController.currentDestination?.hasRoute(FoodDetailRoute::class) == true) {
                        navController.popBackStack()
                    }
                }
            )
        }

        composable<ScannerRoute>(
            enterTransition = { fadeIn(tween(ANIM_MS)) },
            popExitTransition = { fadeOut(tween(ANIM_MS)) }
        ) { entry ->
            val route = entry.toRoute<ScannerRoute>()
            ScannerScreen(
                onBack = { navController.popBackStack() },
                onFound = { foodId ->
                    navController.navigate(FoodDetailRoute(foodId = foodId, meal = route.meal, date = route.date)) {
                        popUpTo<ScannerRoute> { inclusive = true }
                    }
                },
                onAddManually = { barcode ->
                    navController.navigate(FoodEditRoute(barcode = barcode, meal = route.meal, date = route.date)) {
                        popUpTo<ScannerRoute> { inclusive = true }
                    }
                }
            )
        }
    }
}

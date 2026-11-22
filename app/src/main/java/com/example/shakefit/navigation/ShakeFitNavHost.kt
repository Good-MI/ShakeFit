package com.example.shakefit.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.shakefit.ShakeFitApplication
import com.example.shakefit.model.ExerciseType
import com.example.shakefit.ui.ViewModelFactory
import com.example.shakefit.ui.screens.activity.ActivityScreen
import com.example.shakefit.ui.screens.goals.GoalsRoute
import com.example.shakefit.ui.screens.goals.GoalsViewModel
import com.example.shakefit.ui.screens.history.HistoryViewModel
import com.example.shakefit.ui.screens.home.HomeRoute
import com.example.shakefit.ui.screens.home.HomeViewModel
import com.example.shakefit.ui.screens.repsession.RepSessionRoute
import com.example.shakefit.ui.screens.repsession.RepSessionViewModel
import com.example.shakefit.ui.screens.profile.ProfileRoute
import com.example.shakefit.ui.screens.profile.ProfileViewModel
import com.example.shakefit.ui.screens.runsession.RunSessionRoute
import com.example.shakefit.ui.screens.runsession.RunSessionViewModel
import com.example.shakefit.ui.screens.sensordebug.SensorDebugRoute
import com.example.shakefit.ui.screens.sensordebug.SensorDebugViewModel
import com.example.shakefit.ui.screens.settings.SettingsRoute
import com.example.shakefit.ui.screens.settings.SettingsViewModel
import com.example.shakefit.ui.screens.stats.StatsViewModel

/**
 * Item untuk Bottom Navigation Bar (4 tab).
 */
private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(NavRoutes.HOME, "Beranda", Icons.Filled.Home),
    BottomNavItem(NavRoutes.GOALS, "Target", Icons.Filled.Star),
    BottomNavItem(NavRoutes.ACTIVITY, "Aktivitas", Icons.Filled.DateRange),
    BottomNavItem(NavRoutes.PROFILE, "Profil", Icons.Filled.Person)
)

/** Rute-rute yang menampilkan Bottom Navigation Bar. */
private val tabRoutes = setOf(NavRoutes.HOME, NavRoutes.GOALS, NavRoutes.ACTIVITY, NavRoutes.PROFILE)

/**
 * Composable utama navigasi ShakeFit.
 * Meng-hoist ViewModel dengan DI manual via AppContainer.
 */
@Composable
fun ShakeFitNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val appContainer = (context.applicationContext as ShakeFitApplication).container
    val viewModelFactory = ViewModelFactory(appContainer)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in tabRoutes

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = navBackStackEntry?.destination?.hierarchy?.any {
                            it.route == item.route
                        } == true

                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.HOME,
            modifier = Modifier.padding(innerPadding)
        ) {
            // === Tab utama ===
            composable(NavRoutes.HOME) {
                val homeViewModel: HomeViewModel = viewModel(factory = viewModelFactory)
                HomeRoute(
                    viewModel = homeViewModel,
                    onNavigateToExercise = { type ->
                        if (type == ExerciseType.RUNNING) {
                            navController.navigate(NavRoutes.RUN_SESSION)
                        } else {
                            navController.navigate(NavRoutes.repSession(type.name))
                        }
                    }
                )
            }
            composable(NavRoutes.GOALS) {
                val goalsViewModel: GoalsViewModel = viewModel(factory = viewModelFactory)
                GoalsRoute(viewModel = goalsViewModel)
            }
            composable(NavRoutes.ACTIVITY) {
                val historyViewModel: HistoryViewModel = viewModel(factory = viewModelFactory)
                val statsViewModel: StatsViewModel = viewModel(factory = viewModelFactory)
                ActivityScreen(
                    historyViewModel = historyViewModel,
                    statsViewModel = statsViewModel
                )
            }
            composable(NavRoutes.PROFILE) {
                val profileViewModel: ProfileViewModel = viewModel(factory = viewModelFactory)
                ProfileRoute(
                    viewModel = profileViewModel,
                    onOpenSettings = { navController.navigate(NavRoutes.SETTINGS) }
                )
            }

            // === Full-screen ===
            composable(
                route = NavRoutes.REP_SESSION,
                arguments = listOf(navArgument("exerciseType") { type = NavType.StringType })
            ) { backStackEntry ->
                val typeStr = backStackEntry.arguments?.getString("exerciseType") ?: "SQUAT"
                val exerciseType = try { ExerciseType.valueOf(typeStr) } catch (_: Exception) { ExerciseType.SQUAT }
                val repFactory = ViewModelFactory(appContainer, exerciseType)
                val repSessionViewModel: RepSessionViewModel = viewModel(factory = repFactory)

                RepSessionRoute(
                    viewModel = repSessionViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.RUN_SESSION) {
                val runSessionViewModel: RunSessionViewModel = viewModel(factory = viewModelFactory)
                RunSessionRoute(
                    viewModel = runSessionViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(NavRoutes.SETTINGS) {
                val settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
                SettingsRoute(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenSensorDebug = { navController.navigate(NavRoutes.SENSOR_DEBUG) }
                )
            }
            composable(NavRoutes.SENSOR_DEBUG) {
                val sensorDebugViewModel: SensorDebugViewModel = viewModel(factory = viewModelFactory)
                SensorDebugRoute(
                    viewModel = sensorDebugViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

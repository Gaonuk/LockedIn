package com.lockedin.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lockedin.ui.accessibility.AccessibilityPermissionScreen
import com.lockedin.ui.appselection.AppSelectionScreen
import com.lockedin.ui.home.HomeScreen
import com.lockedin.ui.schedule.ScheduleConfigScreen
import com.lockedin.ui.setup.SetupWizardScreen
import com.lockedin.ui.statistics.StatisticsScreen

object Routes {
    const val SETUP_WIZARD = "setup_wizard"
    const val ACCESSIBILITY_PERMISSION = "accessibility_permission"
    const val APP_SELECTION = "app_selection"
    const val SCHEDULE_CONFIG = "schedule_config"
    const val HOME = "home"
    const val STATISTICS = "statistics"
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.SETUP_WIZARD
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Routes.SETUP_WIZARD) {
            SetupWizardScreen(
                onSetupComplete = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SETUP_WIZARD) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToStatistics = {
                    navController.navigate(Routes.STATISTICS)
                }
            )
        }

        composable(Routes.STATISTICS) {
            StatisticsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.ACCESSIBILITY_PERMISSION) {
            AccessibilityPermissionScreen(
                onPermissionGranted = {
                    navController.navigate(Routes.APP_SELECTION) {
                        popUpTo(Routes.ACCESSIBILITY_PERMISSION) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.APP_SELECTION) {
            AppSelectionScreen(
                onNavigateToSchedule = {
                    navController.navigate(Routes.SCHEDULE_CONFIG)
                }
            )
        }

        composable(Routes.SCHEDULE_CONFIG) {
            ScheduleConfigScreen()
        }
    }
}

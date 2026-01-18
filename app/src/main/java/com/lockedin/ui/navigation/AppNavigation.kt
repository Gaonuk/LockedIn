package com.lockedin.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lockedin.service.BlockingStateManager
import com.lockedin.ui.accessibility.AccessibilityPermissionScreen
import com.lockedin.ui.appselection.AppSelectionScreen
import com.lockedin.ui.home.HomeScreen
import com.lockedin.ui.schedule.ScheduleConfigScreen
import com.lockedin.ui.settings.SettingsScreen
import com.lockedin.ui.setup.SetupWizardScreen
import com.lockedin.ui.statistics.StatisticsScreen

object Routes {
    const val SETUP_WIZARD = "setup_wizard"
    const val ACCESSIBILITY_PERMISSION = "accessibility_permission"
    const val APP_SELECTION = "app_selection"
    const val SCHEDULE_CONFIG = "schedule_config"
    const val HOME = "home"
    const val STATISTICS = "statistics"
    const val SETTINGS = "settings"
    const val SETTINGS_APP_SELECTION = "settings_app_selection"
    const val SETTINGS_SCHEDULE = "settings_schedule"
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
                },
                onNavigateToSettings = {
                    navController.navigate(Routes.SETTINGS)
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

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToAppSelection = {
                    navController.navigate(Routes.SETTINGS_APP_SELECTION)
                },
                onNavigateToSchedule = {
                    navController.navigate(Routes.SETTINGS_SCHEDULE)
                }
            )
        }

        composable(Routes.SETTINGS_APP_SELECTION) {
            AppSelectionScreen(
                isFromSettings = true,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.SETTINGS_SCHEDULE) {
            val context = LocalContext.current
            val blockingStateManager = BlockingStateManager.getInstance(context)
            val isBlocking by blockingStateManager.isBlocking.collectAsState()

            ScheduleConfigScreen(
                isFromSettings = true,
                isBlockingActive = isBlocking,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

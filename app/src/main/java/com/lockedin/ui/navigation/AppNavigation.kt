package com.lockedin.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lockedin.ui.accessibility.AccessibilityPermissionScreen
import com.lockedin.ui.appselection.AppSelectionScreen
import com.lockedin.ui.schedule.ScheduleConfigScreen

object Routes {
    const val ACCESSIBILITY_PERMISSION = "accessibility_permission"
    const val APP_SELECTION = "app_selection"
    const val SCHEDULE_CONFIG = "schedule_config"
}

@Composable
fun AppNavigation(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.ACCESSIBILITY_PERMISSION
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
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

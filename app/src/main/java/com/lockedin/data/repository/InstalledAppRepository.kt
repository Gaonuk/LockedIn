package com.lockedin.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.lockedin.data.model.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InstalledAppRepository(private val context: Context) {

    suspend fun getInstalledApps(includeSystemApps: Boolean = false): List<InstalledApp> =
        withContext(Dispatchers.IO) {
            val packageManager = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val launchableApps = packageManager.queryIntentActivities(mainIntent, 0)
                .mapNotNull { resolveInfo ->
                    val appInfo = resolveInfo.activityInfo.applicationInfo
                    val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                    if (!includeSystemApps && isSystemApp) {
                        null
                    } else {
                        InstalledApp(
                            packageName = appInfo.packageName,
                            appName = appInfo.loadLabel(packageManager).toString(),
                            icon = appInfo.loadIcon(packageManager),
                            isSystemApp = isSystemApp
                        )
                    }
                }
                .filter { it.packageName != context.packageName }
                .distinctBy { it.packageName }
                .sortedBy { it.appName.lowercase() }

            launchableApps
        }
}

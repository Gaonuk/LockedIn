package com.lockedin.ui.appselection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lockedin.data.AppDatabase
import com.lockedin.data.entity.BlockedApp
import com.lockedin.data.model.InstalledApp
import com.lockedin.data.repository.InstalledAppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AppSelectionUiState(
    val apps: List<InstalledApp> = emptyList(),
    val showSystemApps: Boolean = false,
    val isLoading: Boolean = true
)

class AppSelectionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InstalledAppRepository(application)
    private val blockedAppDao = AppDatabase.getInstance(application).blockedAppDao()

    private val _uiState = MutableStateFlow(AppSelectionUiState())
    val uiState: StateFlow<AppSelectionUiState> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    fun loadApps() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val installedApps = repository.getInstalledApps(_uiState.value.showSystemApps)
            val blockedApps = blockedAppDao.getAllBlockedApps().first()
            val blockedPackages = blockedApps.map { it.packageName }.toSet()

            val appsWithSelection = installedApps.map { app ->
                app.copy(isSelected = blockedPackages.contains(app.packageName))
            }

            _uiState.value = _uiState.value.copy(
                apps = appsWithSelection,
                isLoading = false
            )
        }
    }

    fun toggleAppSelection(packageName: String) {
        viewModelScope.launch {
            val currentApps = _uiState.value.apps
            val app = currentApps.find { it.packageName == packageName } ?: return@launch

            if (app.isSelected) {
                blockedAppDao.deleteByPackageName(packageName)
            } else {
                blockedAppDao.insert(
                    BlockedApp(
                        packageName = packageName,
                        appName = app.appName
                    )
                )
            }

            val updatedApps = currentApps.map {
                if (it.packageName == packageName) {
                    it.copy(isSelected = !it.isSelected)
                } else {
                    it
                }
            }
            _uiState.value = _uiState.value.copy(apps = updatedApps)
        }
    }

    fun toggleShowSystemApps() {
        _uiState.value = _uiState.value.copy(showSystemApps = !_uiState.value.showSystemApps)
        loadApps()
    }
}

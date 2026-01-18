package com.lockedin.ui.setup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lockedin.data.AppDatabase
import com.lockedin.data.entity.SetupState
import com.lockedin.service.AppBlockerAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SetupStep {
    ACCESSIBILITY_PERMISSION,
    APP_SELECTION,
    SCHEDULE_CONFIG,
    COMPLETE
}

data class SetupWizardUiState(
    val currentStep: SetupStep = SetupStep.ACCESSIBILITY_PERMISSION,
    val isAccessibilityPermissionGranted: Boolean = false,
    val isLoading: Boolean = true,
    val isSetupComplete: Boolean = false
)

class SetupWizardViewModel(application: Application) : AndroidViewModel(application) {

    private val setupStateDao = AppDatabase.getInstance(application).setupStateDao()

    private val _uiState = MutableStateFlow(SetupWizardUiState())
    val uiState: StateFlow<SetupWizardUiState> = _uiState.asStateFlow()

    init {
        checkInitialState()
    }

    private fun checkInitialState() {
        viewModelScope.launch {
            val isPermissionGranted = AppBlockerAccessibilityService.isAccessibilityServiceEnabled(
                getApplication()
            )

            val startStep = if (isPermissionGranted) {
                SetupStep.APP_SELECTION
            } else {
                SetupStep.ACCESSIBILITY_PERMISSION
            }

            _uiState.value = _uiState.value.copy(
                isAccessibilityPermissionGranted = isPermissionGranted,
                currentStep = startStep,
                isLoading = false
            )
        }
    }

    fun checkAccessibilityPermission() {
        val isGranted = AppBlockerAccessibilityService.isAccessibilityServiceEnabled(
            getApplication()
        )
        _uiState.value = _uiState.value.copy(isAccessibilityPermissionGranted = isGranted)
    }

    fun onAccessibilityPermissionGranted() {
        _uiState.value = _uiState.value.copy(
            isAccessibilityPermissionGranted = true,
            currentStep = SetupStep.APP_SELECTION
        )
    }

    fun onAppSelectionComplete() {
        _uiState.value = _uiState.value.copy(currentStep = SetupStep.SCHEDULE_CONFIG)
    }

    fun onScheduleConfigComplete() {
        _uiState.value = _uiState.value.copy(currentStep = SetupStep.COMPLETE)
    }

    fun completeSetup() {
        viewModelScope.launch {
            setupStateDao.upsert(
                SetupState(
                    id = 1,
                    isSetupCompleted = true,
                    completedAt = System.currentTimeMillis()
                )
            )
            _uiState.value = _uiState.value.copy(isSetupComplete = true)
        }
    }

    fun canNavigateBack(): Boolean {
        return when (_uiState.value.currentStep) {
            SetupStep.ACCESSIBILITY_PERMISSION -> false
            SetupStep.APP_SELECTION -> false // Can't go back to permission if already granted
            SetupStep.SCHEDULE_CONFIG -> true
            SetupStep.COMPLETE -> true
        }
    }

    fun navigateBack() {
        val currentStep = _uiState.value.currentStep
        val previousStep = when (currentStep) {
            SetupStep.ACCESSIBILITY_PERMISSION -> SetupStep.ACCESSIBILITY_PERMISSION
            SetupStep.APP_SELECTION -> SetupStep.ACCESSIBILITY_PERMISSION
            SetupStep.SCHEDULE_CONFIG -> SetupStep.APP_SELECTION
            SetupStep.COMPLETE -> SetupStep.SCHEDULE_CONFIG
        }
        _uiState.value = _uiState.value.copy(currentStep = previousStep)
    }
}

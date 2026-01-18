package com.lockedin.ui.accessibility

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.lockedin.service.AppBlockerAccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AccessibilityPermissionUiState(
    val isPermissionGranted: Boolean = false,
    val isChecking: Boolean = true
)

class AccessibilityPermissionViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AccessibilityPermissionUiState())
    val uiState: StateFlow<AccessibilityPermissionUiState> = _uiState.asStateFlow()

    init {
        checkPermissionStatus()
    }

    fun checkPermissionStatus() {
        _uiState.value = _uiState.value.copy(isChecking = true)

        val isEnabled = AppBlockerAccessibilityService.isAccessibilityServiceEnabled(
            getApplication()
        )

        _uiState.value = _uiState.value.copy(
            isPermissionGranted = isEnabled,
            isChecking = false
        )
    }
}

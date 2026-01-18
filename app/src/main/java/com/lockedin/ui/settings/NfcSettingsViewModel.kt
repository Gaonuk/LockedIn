package com.lockedin.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lockedin.data.entity.RegisteredNfcTag
import com.lockedin.service.NfcHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NfcSettingsUiState(
    val isRegistrationMode: Boolean = false,
    val registeredTag: RegisteredNfcTag? = null,
    val isLoading: Boolean = true
)

class NfcSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val nfcHandler = NfcHandler.getInstance(application)

    val registeredTag: StateFlow<RegisteredNfcTag?> = nfcHandler.getRegisteredTag()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isInRegistrationMode: StateFlow<Boolean> = nfcHandler.isInRegistrationMode

    private val _showRegistrationDialog = MutableStateFlow(false)
    val showRegistrationDialog: StateFlow<Boolean> = _showRegistrationDialog.asStateFlow()

    fun startRegistration() {
        nfcHandler.enterRegistrationMode()
        _showRegistrationDialog.value = true
    }

    fun cancelRegistration() {
        nfcHandler.exitRegistrationMode()
        _showRegistrationDialog.value = false
    }

    fun onTagRegistered() {
        _showRegistrationDialog.value = false
    }

    fun removeTag() {
        viewModelScope.launch {
            nfcHandler.unregisterNfcTag()
        }
    }

    fun isNfcSupported(): Boolean = nfcHandler.isNfcSupported()

    fun isNfcEnabled(): Boolean = nfcHandler.isNfcEnabled()
}

package com.lockedin.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lockedin.data.entity.StreakData
import com.lockedin.data.repository.StreakRepository
import com.lockedin.service.BlockingStateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val isLoading: Boolean = true,
    val milestoneToShow: Int? = null,
    val isSessionActive: Boolean = false,
    val activeScheduleName: String? = null,
    val remainingTimeMillis: Long? = null,
    val elapsedTimeMillis: Long? = null,
    val blockedAppsCount: Int = 0
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val streakRepository = StreakRepository(application)
    private val blockingStateManager = BlockingStateManager.getInstance(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadStreakData()
        checkStreakReset()
        observeBlockingState()
    }

    private fun loadStreakData() {
        viewModelScope.launch {
            streakRepository.getStreakData().collect { streakData ->
                val data = streakData ?: StreakData()
                _uiState.value = _uiState.value.copy(
                    currentStreak = data.currentStreak,
                    longestStreak = data.longestStreak,
                    isLoading = false
                )
            }
        }
    }

    private fun observeBlockingState() {
        viewModelScope.launch {
            blockingStateManager.isBlocking.collect { isBlocking ->
                _uiState.value = _uiState.value.copy(isSessionActive = isBlocking)

                if (isBlocking) {
                    startTimerUpdates()
                }
            }
        }

        viewModelScope.launch {
            blockingStateManager.activeScheduleName.collect { name ->
                _uiState.value = _uiState.value.copy(activeScheduleName = name)
            }
        }

        viewModelScope.launch {
            blockingStateManager.blockedAppsCount.collect { count ->
                _uiState.value = _uiState.value.copy(blockedAppsCount = count)
            }
        }
    }

    private fun startTimerUpdates() {
        viewModelScope.launch {
            while (blockingStateManager.isBlocking.value) {
                val remaining = blockingStateManager.getRemainingTimeMillis()
                val startTime = blockingStateManager.sessionStartTimeMillis.value
                val elapsed = startTime?.let { System.currentTimeMillis() - it }

                _uiState.value = _uiState.value.copy(
                    remainingTimeMillis = remaining,
                    elapsedTimeMillis = elapsed
                )

                delay(1000) // Update every second
            }

            // Clear session data when session ends
            _uiState.value = _uiState.value.copy(
                remainingTimeMillis = null,
                elapsedTimeMillis = null
            )
        }
    }

    private fun checkStreakReset() {
        viewModelScope.launch {
            streakRepository.checkAndResetStreakIfNeeded()
        }
    }

    fun dismissMilestone() {
        _uiState.value = _uiState.value.copy(milestoneToShow = null)
    }
}

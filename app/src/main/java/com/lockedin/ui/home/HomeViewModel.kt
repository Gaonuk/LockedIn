package com.lockedin.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lockedin.data.entity.StreakData
import com.lockedin.data.repository.StreakRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val isLoading: Boolean = true,
    val milestoneToShow: Int? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val streakRepository = StreakRepository(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadStreakData()
        checkStreakReset()
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

    private fun checkStreakReset() {
        viewModelScope.launch {
            streakRepository.checkAndResetStreakIfNeeded()
        }
    }

    fun dismissMilestone() {
        _uiState.value = _uiState.value.copy(milestoneToShow = null)
    }
}

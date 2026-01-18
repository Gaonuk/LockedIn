package com.lockedin.ui.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lockedin.data.AppDatabase
import com.lockedin.data.entity.Schedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ScheduleConfigUiState(
    val startTimeMinutes: Int = 9 * 60,
    val endTimeMinutes: Int = 17 * 60,
    val isEnabled: Boolean = true,
    val scheduleName: String = "Default Schedule",
    val daysOfWeek: Int = 0b1111111,
    val isSaving: Boolean = false,
    val isLoading: Boolean = true,
    val validationError: String? = null,
    val existingScheduleId: Long? = null
)

class ScheduleConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val scheduleDao = AppDatabase.getInstance(application).scheduleDao()

    private val _uiState = MutableStateFlow(ScheduleConfigUiState())
    val uiState: StateFlow<ScheduleConfigUiState> = _uiState.asStateFlow()

    init {
        loadExistingSchedule()
    }

    private fun loadExistingSchedule() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val schedules = scheduleDao.getAllSchedules().first()
            val existingSchedule = schedules.firstOrNull()

            if (existingSchedule != null) {
                _uiState.value = _uiState.value.copy(
                    startTimeMinutes = existingSchedule.startTimeMinutes,
                    endTimeMinutes = existingSchedule.endTimeMinutes,
                    isEnabled = existingSchedule.isEnabled,
                    scheduleName = existingSchedule.name,
                    daysOfWeek = existingSchedule.daysOfWeek,
                    existingScheduleId = existingSchedule.id,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun updateStartTime(hour: Int, minute: Int) {
        val minutes = hour * 60 + minute
        _uiState.value = _uiState.value.copy(
            startTimeMinutes = minutes,
            validationError = null
        )
        validateSchedule()
    }

    fun updateEndTime(hour: Int, minute: Int) {
        val minutes = hour * 60 + minute
        _uiState.value = _uiState.value.copy(
            endTimeMinutes = minutes,
            validationError = null
        )
        validateSchedule()
    }

    fun updateScheduleName(name: String) {
        _uiState.value = _uiState.value.copy(scheduleName = name)
    }

    fun toggleEnabled() {
        _uiState.value = _uiState.value.copy(isEnabled = !_uiState.value.isEnabled)
    }

    fun toggleDayOfWeek(dayIndex: Int) {
        val currentDays = _uiState.value.daysOfWeek
        val newDays = currentDays xor (1 shl dayIndex)
        _uiState.value = _uiState.value.copy(daysOfWeek = newDays)
    }

    private fun validateSchedule(): Boolean {
        val state = _uiState.value

        if (state.endTimeMinutes <= state.startTimeMinutes) {
            _uiState.value = state.copy(
                validationError = "End time must be after start time"
            )
            return false
        }

        _uiState.value = state.copy(validationError = null)
        return true
    }

    fun saveSchedule(onComplete: (() -> Unit)? = null) {
        if (!validateSchedule()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)

            val state = _uiState.value
            val schedule = Schedule(
                id = state.existingScheduleId ?: 0,
                name = state.scheduleName,
                startTimeMinutes = state.startTimeMinutes,
                endTimeMinutes = state.endTimeMinutes,
                daysOfWeek = state.daysOfWeek,
                isEnabled = state.isEnabled
            )

            if (state.existingScheduleId != null) {
                scheduleDao.update(schedule)
            } else {
                val newId = scheduleDao.insert(schedule)
                _uiState.value = _uiState.value.copy(existingScheduleId = newId)
            }

            _uiState.value = _uiState.value.copy(isSaving = false)
            onComplete?.invoke()
        }
    }

    companion object {
        fun formatTime(totalMinutes: Int): String {
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            val period = if (hours < 12) "AM" else "PM"
            val displayHour = when {
                hours == 0 -> 12
                hours > 12 -> hours - 12
                else -> hours
            }
            return String.format("%d:%02d %s", displayHour, minutes, period)
        }
    }
}

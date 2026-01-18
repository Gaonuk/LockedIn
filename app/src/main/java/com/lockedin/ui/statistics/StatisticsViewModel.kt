package com.lockedin.ui.statistics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lockedin.data.entity.SessionStatistic
import com.lockedin.data.repository.SessionStatisticsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

data class StatisticsUiState(
    val isLoading: Boolean = true,
    val totalTimeSavedSeconds: Long = 0,
    val totalBlockedAttempts: Int = 0,
    val completedSessionsCount: Int = 0,
    val dailyStats: List<DayStats> = emptyList(),
    val weeklyStats: List<WeekStats> = emptyList()
)

data class DayStats(
    val dayLabel: String,
    val blockedAttempts: Int,
    val timeSavedSeconds: Long,
    val isToday: Boolean = false
)

data class WeekStats(
    val weekLabel: String,
    val blockedAttempts: Int,
    val timeSavedSeconds: Long,
    val isCurrentWeek: Boolean = false
)

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SessionStatisticsRepository(application)

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadStatistics()
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val totalTimeSaved = repository.getTotalTimeSavedSeconds()
            val totalBlocked = repository.getTotalBlockedAttempts()
            val completedSessions = repository.getCompletedSessionsCount()

            // Get all sessions for trend calculation
            repository.getAllSessions().collect { sessions ->
                val dailyStats = calculateDailyStats(sessions)
                val weeklyStats = calculateWeeklyStats(sessions)

                _uiState.value = StatisticsUiState(
                    isLoading = false,
                    totalTimeSavedSeconds = totalTimeSaved,
                    totalBlockedAttempts = totalBlocked,
                    completedSessionsCount = completedSessions,
                    dailyStats = dailyStats,
                    weeklyStats = weeklyStats
                )
            }
        }
    }

    private fun calculateDailyStats(sessions: List<SessionStatistic>): List<DayStats> {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        val currentYear = calendar.get(Calendar.YEAR)

        val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        // Get stats for the last 7 days
        return (6 downTo 0).map { daysAgo ->
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)

            val dayStart = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val dayEnd = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            val daySessions = sessions.filter { session ->
                session.startTime in dayStart..dayEnd
            }

            val blockedAttempts = daySessions.sumOf { it.blockedAttempts }
            val timeSaved = daySessions.sumOf { it.timeSavedSeconds }

            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
            val isToday = daysAgo == 0

            DayStats(
                dayLabel = dayLabels[dayOfWeek],
                blockedAttempts = blockedAttempts,
                timeSavedSeconds = timeSaved,
                isToday = isToday
            )
        }
    }

    private fun calculateWeeklyStats(sessions: List<SessionStatistic>): List<WeekStats> {
        val calendar = Calendar.getInstance()

        // Get stats for the last 4 weeks
        return (3 downTo 0).map { weeksAgo ->
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            calendar.add(Calendar.WEEK_OF_YEAR, -weeksAgo)

            val weekStart = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            calendar.add(Calendar.DAY_OF_YEAR, 6)
            val weekEnd = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            val weekSessions = sessions.filter { session ->
                session.startTime in weekStart..weekEnd
            }

            val blockedAttempts = weekSessions.sumOf { it.blockedAttempts }
            val timeSaved = weekSessions.sumOf { it.timeSavedSeconds }

            val weekLabel = if (weeksAgo == 0) "This Week" else "${weeksAgo}w ago"

            WeekStats(
                weekLabel = weekLabel,
                blockedAttempts = blockedAttempts,
                timeSavedSeconds = timeSaved,
                isCurrentWeek = weeksAgo == 0
            )
        }
    }

    companion object {
        fun formatTimeSaved(seconds: Long): String {
            return when {
                seconds < 60 -> "${seconds}s"
                seconds < 3600 -> "${seconds / 60}m"
                else -> {
                    val hours = seconds / 3600
                    val minutes = (seconds % 3600) / 60
                    if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
                }
            }
        }

        fun formatTimeSavedFull(seconds: Long): String {
            return when {
                seconds < 60 -> "$seconds seconds"
                seconds < 3600 -> {
                    val minutes = seconds / 60
                    "$minutes minute${if (minutes != 1L) "s" else ""}"
                }
                else -> {
                    val hours = seconds / 3600
                    val minutes = (seconds % 3600) / 60
                    val hourStr = "$hours hour${if (hours != 1L) "s" else ""}"
                    if (minutes > 0) {
                        "$hourStr $minutes minute${if (minutes != 1L) "s" else ""}"
                    } else {
                        hourStr
                    }
                }
            }
        }
    }
}

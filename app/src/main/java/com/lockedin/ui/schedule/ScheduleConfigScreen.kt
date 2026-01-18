package com.lockedin.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleConfigScreen(
    modifier: Modifier = Modifier,
    viewModel: ScheduleConfigViewModel = viewModel(),
    isFromSettings: Boolean = false,
    isBlockingActive: Boolean = false,
    onNavigateBack: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Schedule Configuration") },
                navigationIcon = {
                    if (isFromSettings) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        ScheduleConfigContent(
            viewModel = viewModel,
            isBlockingActive = isBlockingActive,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleConfigContent(
    modifier: Modifier = Modifier,
    viewModel: ScheduleConfigViewModel = viewModel(),
    isBlockingActive: Boolean = false,
    onSaveComplete: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isBlockingActive) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Schedule cannot be modified while a blocking session is active. End the current session to make changes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            EnabledToggleCard(
                isEnabled = uiState.isEnabled,
                onToggle = { viewModel.toggleEnabled() },
                enabled = !isBlockingActive
            )

            TimeSelectionCard(
                title = "Start Time",
                timeMinutes = uiState.startTimeMinutes,
                onTimeSelected = { hour, minute ->
                    viewModel.updateStartTime(hour, minute)
                },
                enabled = !isBlockingActive
            )

            TimeSelectionCard(
                title = "End Time",
                timeMinutes = uiState.endTimeMinutes,
                onTimeSelected = { hour, minute ->
                    viewModel.updateEndTime(hour, minute)
                },
                enabled = !isBlockingActive
            )

            if (uiState.validationError != null) {
                Text(
                    text = uiState.validationError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            DaysOfWeekCard(
                daysOfWeek = uiState.daysOfWeek,
                onDayToggle = { viewModel.toggleDayOfWeek(it) },
                enabled = !isBlockingActive
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    viewModel.saveSchedule(onComplete = onSaveComplete)
                },
                enabled = !uiState.isSaving && uiState.validationError == null && !isBlockingActive,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save Schedule")
                }
            }
        }
    }
}

@Composable
private fun EnabledToggleCard(
    isEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val contentAlpha = if (enabled) 1f else 0.38f

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onToggle)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Schedule Enabled",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                )
                Text(
                    text = if (isEnabled) "Schedule is active" else "Schedule is disabled",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                )
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                enabled = enabled
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeSelectionCard(
    title: String,
    timeMinutes: Int,
    onTimeSelected: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var showTimePicker by remember { mutableStateOf(false) }
    val initialHour = timeMinutes / 60
    val initialMinute = timeMinutes % 60
    val contentAlpha = if (enabled) 1f else 0.38f

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                )
                Text(
                    text = ScheduleConfigViewModel.formatTime(timeMinutes),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha),
                    modifier = Modifier.clickable(enabled = enabled) { showTimePicker = !showTimePicker }
                )
            }

            if (showTimePicker && enabled) {
                Spacer(modifier = Modifier.height(16.dp))

                val timePickerState = rememberTimePickerState(
                    initialHour = initialHour,
                    initialMinute = initialMinute,
                    is24Hour = false
                )

                TimePicker(
                    state = timePickerState,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        onTimeSelected(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Set Time")
                }
            }
        }
    }
}

@Composable
private fun DaysOfWeekCard(
    daysOfWeek: Int,
    onDayToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val contentAlpha = if (enabled) 1f else 0.38f

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Active Days",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                dayLabels.forEachIndexed { index, label ->
                    val isSelected = (daysOfWeek and (1 shl index)) != 0
                    DayChip(
                        label = label,
                        isSelected = isSelected,
                        onClick = { onDayToggle(index) },
                        enabled = enabled
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val selectedDays = dayNames.filterIndexed { index, _ ->
                (daysOfWeek and (1 shl index)) != 0
            }
            val summaryText = when {
                selectedDays.size == 7 -> "Every day"
                selectedDays.size == 5 && selectedDays.containsAll(listOf("Mon", "Tue", "Wed", "Thu", "Fri")) -> "Weekdays"
                selectedDays.size == 2 && selectedDays.containsAll(listOf("Sat", "Sun")) -> "Weekends"
                selectedDays.isEmpty() -> "No days selected"
                else -> selectedDays.joinToString(", ")
            }

            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun DayChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val contentAlpha = if (enabled) 1f else 0.38f

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = contentAlpha)
                else MaterialTheme.colorScheme.surface.copy(alpha = contentAlpha)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary.copy(alpha = contentAlpha)
            else MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
        )
    }
}

package com.lockedin.ui.setup

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lockedin.ui.appselection.AppSelectionContent
import com.lockedin.ui.schedule.ScheduleConfigContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupWizardScreen(
    modifier: Modifier = Modifier,
    viewModel: SetupWizardViewModel = viewModel(),
    onSetupComplete: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSetupComplete) {
        if (uiState.isSetupComplete) {
            onSetupComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LockedIn Setup") },
                navigationIcon = {
                    if (viewModel.canNavigateBack()) {
                        IconButton(onClick = { viewModel.navigateBack() }) {
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
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                StepIndicator(
                    currentStep = uiState.currentStep,
                    modifier = Modifier.padding(16.dp)
                )

                AnimatedContent(
                    targetState = uiState.currentStep,
                    transitionSpec = {
                        val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                        slideInHorizontally { width -> direction * width } togetherWith
                                slideOutHorizontally { width -> -direction * width }
                    },
                    label = "step_transition",
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) { step ->
                    when (step) {
                        SetupStep.ACCESSIBILITY_PERMISSION -> AccessibilityStepContent(
                            isPermissionGranted = uiState.isAccessibilityPermissionGranted,
                            onCheckPermission = { viewModel.checkAccessibilityPermission() },
                            onPermissionGranted = { viewModel.onAccessibilityPermissionGranted() }
                        )
                        SetupStep.APP_SELECTION -> AppSelectionStepContent(
                            onContinue = { viewModel.onAppSelectionComplete() }
                        )
                        SetupStep.SCHEDULE_CONFIG -> ScheduleStepContent(
                            onContinue = { viewModel.onScheduleConfigComplete() }
                        )
                        SetupStep.COMPLETE -> SetupCompleteContent(
                            onFinish = { viewModel.completeSetup() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(
    currentStep: SetupStep,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        "Permission" to SetupStep.ACCESSIBILITY_PERMISSION,
        "Apps" to SetupStep.APP_SELECTION,
        "Schedule" to SetupStep.SCHEDULE_CONFIG,
        "Done" to SetupStep.COMPLETE
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, (label, step) ->
            val isCompleted = step.ordinal < currentStep.ordinal
            val isCurrent = step == currentStep

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCompleted -> MaterialTheme.colorScheme.primary
                                isCurrent -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isCompleted || isCurrent -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .height(2.dp)
                        .background(
                            if (step.ordinal < currentStep.ordinal) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun AccessibilityStepContent(
    isPermissionGranted: Boolean,
    onCheckPermission: () -> Unit,
    onPermissionGranted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            onCheckPermission()
        }
    }

    LaunchedEffect(isPermissionGranted) {
        if (isPermissionGranted) {
            onPermissionGranted()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PermissionStatusCard(isGranted = isPermissionGranted)

        ExplanationCard()

        if (!isPermissionGranted) {
            InstructionsCard()

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { openAccessibilitySettings(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Accessibility Settings")
            }

            Text(
                text = "Setup cannot be completed without granting this permission",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PermissionStatusCard(
    isGranted: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (isGranted) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isGranted) "Permission Granted" else "Permission Required",
                style = MaterialTheme.typography.titleMedium,
                color = if (isGranted) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                }
            )

            Text(
                text = if (isGranted) {
                    "LockedIn can now detect blocked apps"
                } else {
                    "Accessibility service is not enabled"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (isGranted) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onErrorContainer
                },
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ExplanationCard(modifier: Modifier = Modifier) {
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
                text = "Why is this needed?",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "LockedIn needs accessibility service permission to detect when you open blocked apps during a focus session. This allows the app to redirect you away from distracting apps and help you stay focused.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Your privacy matters:",
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "• We only detect app launches, not screen content\n• No data is collected or sent to servers\n• The service only runs during active focus sessions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InstructionsCard(modifier: Modifier = Modifier) {
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
                text = "How to enable",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "1. Tap the button below to open settings\n2. Find \"LockedIn\" in the list\n3. Tap on it and toggle \"Use LockedIn\"\n4. Confirm by tapping \"Allow\" in the dialog",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
}

@Composable
private fun AppSelectionStepContent(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppSelectionContent(
        onContinue = onContinue,
        modifier = modifier
    )
}

@Composable
private fun ScheduleStepContent(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScheduleConfigContent(
        onSaveComplete = onContinue,
        modifier = modifier
    )
}

@Composable
private fun SetupCompleteContent(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Setup Complete",
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Setup Complete!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "LockedIn is now ready to help you stay focused. Use your NFC tag to start a focus session anytime.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Get Started")
        }
    }
}

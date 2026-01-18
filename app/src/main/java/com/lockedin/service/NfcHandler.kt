package com.lockedin.service

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.Toast
import com.lockedin.data.AppDatabase
import com.lockedin.data.entity.RegisteredNfcTag
import com.lockedin.ui.dialog.ActiveSessionDialogActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class NfcTagEvent {
    data class SessionStarted(val scheduleName: String) : NfcTagEvent()
    data class SessionEnded(val wasConfirmed: Boolean) : NfcTagEvent()
    data class TagRegistered(val tagId: String) : NfcTagEvent()
    data class WrongTag(val scannedTagId: String, val expectedTagId: String) : NfcTagEvent()
    object NoScheduleConfigured : NfcTagEvent()
    object RegistrationCancelled : NfcTagEvent()
}

class NfcHandler(private val context: Context) {

    private var nfcAdapter: NfcAdapter? = null
    private val database: AppDatabase by lazy { AppDatabase.getInstance(context) }
    private val blockingStateManager: BlockingStateManager by lazy { BlockingStateManager.getInstance(context) }

    private val _isSessionActive = MutableStateFlow(false)
    val isSessionActive: StateFlow<Boolean> = _isSessionActive.asStateFlow()

    private val _lastActivationTime = MutableStateFlow<Long?>(null)
    val lastActivationTime: StateFlow<Long?> = _lastActivationTime.asStateFlow()

    private val _isInRegistrationMode = MutableStateFlow(false)
    val isInRegistrationMode: StateFlow<Boolean> = _isInRegistrationMode.asStateFlow()

    private val _lastNfcEvent = MutableStateFlow<NfcTagEvent?>(null)
    val lastNfcEvent: StateFlow<NfcTagEvent?> = _lastNfcEvent.asStateFlow()

    init {
        nfcAdapter = NfcAdapter.getDefaultAdapter(context)
    }

    fun isNfcSupported(): Boolean = nfcAdapter != null

    fun isNfcEnabled(): Boolean = nfcAdapter?.isEnabled == true

    fun enableForegroundDispatch(activity: Activity) {
        val adapter = nfcAdapter ?: return

        val intent = Intent(activity, activity.javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            activity,
            0,
            intent,
            PendingIntent.FLAG_MUTABLE
        )

        val filters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        )

        try {
            adapter.enableForegroundDispatch(activity, pendingIntent, filters, null)
            Log.d(TAG, "NFC foreground dispatch enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling foreground dispatch", e)
        }
    }

    fun disableForegroundDispatch(activity: Activity) {
        try {
            nfcAdapter?.disableForegroundDispatch(activity)
            Log.d(TAG, "NFC foreground dispatch disabled")
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling foreground dispatch", e)
        }
    }

    fun handleIntent(intent: Intent, scope: CoroutineScope): Boolean {
        val action = intent.action ?: return false

        if (action == NfcAdapter.ACTION_NDEF_DISCOVERED ||
            action == NfcAdapter.ACTION_TECH_DISCOVERED ||
            action == NfcAdapter.ACTION_TAG_DISCOVERED
        ) {
            val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }

            if (tag != null) {
                val tagId = tag.id.toHexString()
                Log.d(TAG, "NFC tag detected: $tagId")
                scope.launch {
                    handleNfcTagDetected(tagId)
                }
                return true
            }
        }
        return false
    }

    /**
     * Handles NFC tag detection by checking:
     * 1. If in registration mode, register the tag
     * 2. If a registered tag exists, validate it matches
     * 3. If blocking is already active, handle confirmation/dialog
     * 4. Otherwise, activate a new schedule
     */
    private suspend fun handleNfcTagDetected(tagId: String) {
        // Handle registration mode
        if (_isInRegistrationMode.value) {
            Log.d(TAG, "In registration mode, registering tag: $tagId")
            registerNfcTag(tagId)
            return
        }

        // Check if a tag is registered and validate
        val registeredTagId = withContext(Dispatchers.IO) {
            database.registeredNfcTagDao().getRegisteredTagId()
        }

        if (registeredTagId != null && registeredTagId != tagId) {
            Log.d(TAG, "Wrong NFC tag! Expected: $registeredTagId, Got: $tagId")
            provideErrorHapticFeedback()
            _lastNfcEvent.value = NfcTagEvent.WrongTag(tagId, registeredTagId)
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Wrong NFC tag. Please use your registered tag.",
                    Toast.LENGTH_LONG
                ).show()
            }
            return
        }

        // Handle active session
        if (blockingStateManager.isBlocking.value) {
            if (blockingStateManager.awaitingEndConfirmation.value) {
                Log.d(TAG, "NFC confirmation received, ending blocking session")
                provideHapticFeedback()
                _lastNfcEvent.value = NfcTagEvent.SessionEnded(wasConfirmed = true)
                withContext(Dispatchers.Main) {
                    BlockingForegroundService.stop(context)
                }
            } else {
                Log.d(TAG, "Blocking is active, showing active session dialog")
                provideHapticFeedback()
                withContext(Dispatchers.Main) {
                    ActiveSessionDialogActivity.start(context)
                }
            }
        } else {
            activateSchedule()
        }
    }

    private suspend fun activateSchedule() {
        withContext(Dispatchers.IO) {
            val enabledSchedules = database.scheduleDao().getEnabledSchedules().first()

            withContext(Dispatchers.Main) {
                if (enabledSchedules.isNotEmpty()) {
                    val schedule = enabledSchedules.first()

                    _isSessionActive.value = true
                    _lastActivationTime.value = System.currentTimeMillis()

                    // Start the blocking foreground service
                    BlockingForegroundService.start(context, schedule.id)

                    provideHapticFeedback()
                    showActivationToast(schedule.name)

                    Log.d(TAG, "Focus session activated with schedule: ${schedule.name}")
                } else {
                    provideErrorHapticFeedback()
                    Toast.makeText(
                        context,
                        "No schedule configured. Please set up a schedule first.",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.w(TAG, "No enabled schedules found")
                }
            }
        }
    }

    private fun provideHapticFeedback() {
        val vibrator = getVibrator()
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(
                    longArrayOf(0, 100, 50, 100),
                    -1
                )
                it.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(longArrayOf(0, 100, 50, 100), -1)
            }
        }
    }

    private fun provideErrorHapticFeedback() {
        val vibrator = getVibrator()
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE)
                it.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(300)
            }
        }
    }

    private fun getVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun showActivationToast(scheduleName: String) {
        Toast.makeText(
            context,
            "Focus session activated: $scheduleName",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun deactivateSession() {
        _isSessionActive.value = false
        BlockingForegroundService.stop(context)
        Log.d(TAG, "Focus session deactivated")
    }

    /**
     * Enter registration mode - the next NFC tag tap will register that tag
     */
    fun enterRegistrationMode() {
        _isInRegistrationMode.value = true
        Log.d(TAG, "Entered NFC tag registration mode")
    }

    /**
     * Exit registration mode without registering a tag
     */
    fun exitRegistrationMode() {
        _isInRegistrationMode.value = false
        _lastNfcEvent.value = NfcTagEvent.RegistrationCancelled
        Log.d(TAG, "Exited NFC tag registration mode")
    }

    /**
     * Register the NFC tag with the given ID
     */
    private suspend fun registerNfcTag(tagId: String) {
        withContext(Dispatchers.IO) {
            val tag = RegisteredNfcTag(
                id = 1,
                tagId = tagId,
                registeredAt = System.currentTimeMillis()
            )
            database.registeredNfcTagDao().insert(tag)
        }

        _isInRegistrationMode.value = false
        _lastNfcEvent.value = NfcTagEvent.TagRegistered(tagId)

        provideHapticFeedback()
        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                "NFC tag registered successfully!",
                Toast.LENGTH_SHORT
            ).show()
        }

        Log.d(TAG, "NFC tag registered: $tagId")
    }

    /**
     * Remove the registered NFC tag
     */
    suspend fun unregisterNfcTag() {
        withContext(Dispatchers.IO) {
            database.registeredNfcTagDao().delete()
        }
        Log.d(TAG, "NFC tag unregistered")
    }

    /**
     * Get the registered NFC tag flow for UI observation
     */
    fun getRegisteredTag() = database.registeredNfcTagDao().getRegisteredTag()

    /**
     * Clear the last NFC event
     */
    fun clearLastEvent() {
        _lastNfcEvent.value = null
    }

    private fun ByteArray.toHexString(): String {
        return joinToString("") { "%02X".format(it) }
    }

    companion object {
        private const val TAG = "NfcHandler"

        @Volatile
        private var instance: NfcHandler? = null

        fun getInstance(context: Context): NfcHandler {
            return instance ?: synchronized(this) {
                instance ?: NfcHandler(context.applicationContext).also { instance = it }
            }
        }
    }
}

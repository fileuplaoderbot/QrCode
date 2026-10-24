package com.example.viewmodel

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.backup.BackupManager
import com.example.data.local.AppDatabase
import com.example.data.local.ScanEntity
import com.example.data.preferences.AppPreferences
import com.example.data.preferences.LockType
import com.example.data.repository.ScanRepository
import com.example.decoder.ImageDecoder
import com.example.model.ParsedPayload
import com.example.security.SecurityManager
import com.example.utils.VibrationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Screen {
    HOME,
    SCANNER,
    GENERATOR,
    HISTORY,
    FAVORITES,
    SETTINGS
}

data class LockState(
    val isLocked: Boolean = false,
    val lockType: LockType = LockType.NONE,
    val pinLength: Int = 4,
    val biometricsAllowed: Boolean = false,
    val errorMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ScanRepository
    val prefs: AppPreferences = AppPreferences(application)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ScanRepository(database.scanDao())
    }

    val allScans: StateFlow<List<ScanEntity>> = repository.allScans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteScans: StateFlow<List<ScanEntity>> = repository.favoriteScans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val themeMode: StateFlow<String> = prefs.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    val vibrateOnScan: StateFlow<Boolean> = prefs.vibrateOnScan
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val preventDuplicates: StateFlow<Boolean> = prefs.preventDuplicates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val saveSensitiveHistory: StateFlow<Boolean> = prefs.saveSensitiveHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val lockType: StateFlow<LockType> = prefs.lockType
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LockType.NONE)

    val pinLength: StateFlow<Int> = prefs.pinLength
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)

    val biometricEnabled: StateFlow<Boolean> = prefs.biometricEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isLockActive: StateFlow<Boolean> = prefs.isLockActive
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val pinLockEnabled: StateFlow<Boolean> = isLockActive

    // Activity result / picker tracking to prevent unexpected locking during external interactions
    private val _isAwaitingActivityResult = MutableStateFlow(false)
    val isAwaitingActivityResult: StateFlow<Boolean> = _isAwaitingActivityResult.asStateFlow()

    fun setAwaitingActivityResult(awaiting: Boolean) {
        _isAwaitingActivityResult.value = awaiting
    }

    private val _isScanningImage = MutableStateFlow(false)
    val isScanningImage: StateFlow<Boolean> = _isScanningImage.asStateFlow()

    // Navigation
    private val _currentScreen = MutableStateFlow(Screen.HOME)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Scanner state
    private val _torchEnabled = MutableStateFlow(false)
    val torchEnabled: StateFlow<Boolean> = _torchEnabled.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(false)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    // Active Result Preview
    private val _activePayload = MutableStateFlow<ParsedPayload?>(null)
    val activePayload: StateFlow<ParsedPayload?> = _activePayload.asStateFlow()

    private val _activeScanEntityId = MutableStateFlow<Long?>(null)
    val activeScanEntityId: StateFlow<Long?> = _activeScanEntityId.asStateFlow()

    // Lock Screen
    private val _lockState = MutableStateFlow(LockState())
    val lockState: StateFlow<LockState> = _lockState.asStateFlow()

    // UI Message/Snackbar
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Cached salt & hash for fast synchronous validation
    @Volatile
    private var currentSalt: String? = null
    @Volatile
    private var currentHash: String? = null

    init {
        // Cache credentials
        viewModelScope.launch {
            prefs.lockSalt.collect { currentSalt = it }
        }
        viewModelScope.launch {
            prefs.lockHash.collect { currentHash = it }
        }

        // Initialize lock state
        viewModelScope.launch {
            val type = prefs.lockType.first()
            val length = prefs.pinLength.first()
            val bio = prefs.biometricEnabled.first()
            if (type != LockType.NONE) {
                _lockState.value = LockState(
                    isLocked = true,
                    lockType = type,
                    pinLength = length,
                    biometricsAllowed = bio
                )
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun toggleTorch() {
        _torchEnabled.value = !_torchEnabled.value
    }

    fun toggleCameraLens() {
        _isFrontCamera.value = !_isFrontCamera.value
    }

    fun showPayloadPreview(payload: ParsedPayload, existingEntityId: Long? = null) {
        _activePayload.value = payload
        _activeScanEntityId.value = existingEntityId
    }

    fun dismissPayloadPreview() {
        _activePayload.value = null
        _activeScanEntityId.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun clearMessage() {
        _snackbarMessage.value = null
    }

    /**
     * Called when a QR or barcode is scanned via camera stream or gallery
     */
    fun onCodeScanned(text: String, format: String) {
        viewModelScope.launch {
            val payload = ParsedPayload.parse(text, format)

            // Vibrate if setting is on
            if (vibrateOnScan.value) {
                VibrationHelper.vibrateShort(getApplication())
            }

            // Check duplicate prevention
            val isDup = preventDuplicates.value
            val existing = if (isDup) repository.findByContent(text) else null

            var entityId: Long? = existing?.id

            // Check if sensitive and user disabled saving sensitive
            val shouldSave = !payload.isSensitive || saveSensitiveHistory.value

            if (existing == null && shouldSave) {
                val entity = ScanEntity(
                    content = text,
                    rawFormat = format,
                    contentType = payload.type.name,
                    title = payload.title,
                    timestamp = System.currentTimeMillis(),
                    isFavorite = false,
                    isSensitive = payload.isSensitive
                )
                entityId = repository.insertScan(entity)
            } else if (existing != null) {
                showMessage("Duplicate scan detected")
            }

            showPayloadPreview(payload, entityId)
        }
    }

    /**
     * Decode image picked from Gallery with comprehensive multi-pass decoding
     */
    fun scanImageBytes(bytes: ByteArray) {
        viewModelScope.launch {
            _isScanningImage.value = true
            val decoded = ImageDecoder.decodeFromByteArray(bytes)
            _isScanningImage.value = false
            if (decoded != null) {
                Toast.makeText(getApplication(), "QR Code Detected!", Toast.LENGTH_SHORT).show()
                onCodeScanned(decoded.first, decoded.second)
            } else {
                Toast.makeText(getApplication(), "No QR Code Detected", Toast.LENGTH_LONG).show()
                showMessage("No QR Code Detected")
            }
        }
    }

    fun scanImageUri(uri: Uri) {
        viewModelScope.launch {
            _isScanningImage.value = true
            val decoded = ImageDecoder.decodeFromUri(getApplication(), uri)
            _isScanningImage.value = false
            if (decoded != null) {
                Toast.makeText(getApplication(), "QR Code Detected!", Toast.LENGTH_SHORT).show()
                onCodeScanned(decoded.first, decoded.second)
            } else {
                Toast.makeText(getApplication(), "No QR Code Detected", Toast.LENGTH_LONG).show()
                showMessage("No QR Code Detected")
            }
        }
    }

    fun toggleFavorite(scan: ScanEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(scan)
        }
    }

    fun deleteScan(scan: ScanEntity) {
        viewModelScope.launch {
            repository.deleteScan(scan)
            if (_activeScanEntityId.value == scan.id) {
                _activeScanEntityId.value = null
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
            showMessage("History cleared")
        }
    }

    // Settings actions
    fun setThemeMode(mode: String) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }

    fun setVibrateOnScan(enabled: Boolean) {
        viewModelScope.launch { prefs.setVibrateOnScan(enabled) }
    }

    fun setPreventDuplicates(enabled: Boolean) {
        viewModelScope.launch { prefs.setPreventDuplicates(enabled) }
    }

    fun setSaveSensitiveHistory(enabled: Boolean) {
        viewModelScope.launch { prefs.setSaveSensitiveHistory(enabled) }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setBiometricEnabled(enabled)
            _lockState.value = _lockState.value.copy(biometricsAllowed = enabled)
        }
    }

    // Configures any of the 3 lock types (PIN, PASSWORD, PATTERN)
    fun setLock(type: LockType, secret: String, length: Int = secret.length, enableBiometrics: Boolean = false) {
        viewModelScope.launch {
            val salt = SecurityManager.generateSalt()
            val hash = SecurityManager.hashPin(secret, salt)
            currentSalt = salt
            currentHash = hash
            prefs.setSecurityLock(
                type = type,
                salt = salt,
                hash = hash,
                length = length,
                enableBiometrics = enableBiometrics
            )
            _lockState.value = _lockState.value.copy(
                isLocked = false,
                lockType = type,
                pinLength = length,
                biometricsAllowed = enableBiometrics,
                errorMessage = null
            )
            val typeTitle = when (type) {
                LockType.PIN -> "$length-Digit Number PIN"
                LockType.PASSWORD -> "Alphanumeric Password"
                LockType.PATTERN -> "Pattern Lock"
                LockType.NONE -> "Security"
            }
            showMessage("$typeTitle enabled successfully")
        }
    }

    fun setPin(pin: String, enableBiometrics: Boolean = false) {
        setLock(LockType.PIN, pin, pin.length, enableBiometrics)
    }

    fun verifySecretSync(enteredSecret: String): Boolean {
        val s = currentSalt ?: return false
        val h = currentHash ?: return false
        return SecurityManager.verifyPin(enteredSecret, s, h)
    }

    fun disablePin(enteredPin: String): Boolean {
        if (verifySecretSync(enteredPin)) {
            viewModelScope.launch {
                prefs.disableLock()
                currentSalt = null
                currentHash = null
                _lockState.value = LockState(isLocked = false, lockType = LockType.NONE)
                showMessage("Security lock removed")
            }
            return true
        }
        return false
    }

    fun disableSecurity(enteredSecret: String, onResult: (Boolean) -> Unit) {
        if (verifySecretSync(enteredSecret)) {
            viewModelScope.launch {
                prefs.disableLock()
                currentSalt = null
                currentHash = null
                _lockState.value = LockState(isLocked = false, lockType = LockType.NONE)
                showMessage("Security lock removed")
                onResult(true)
            }
        } else {
            // Also try fallback to DataStore read in case cache wasn't populated yet
            viewModelScope.launch {
                val salt = prefs.lockSalt.first()
                val hash = prefs.lockHash.first()
                if (salt != null && hash != null && SecurityManager.verifyPin(enteredSecret, salt, hash)) {
                    prefs.disableLock()
                    currentSalt = null
                    currentHash = null
                    _lockState.value = LockState(isLocked = false, lockType = LockType.NONE)
                    showMessage("Security lock removed")
                    onResult(true)
                } else {
                    onResult(false)
                }
            }
        }
    }

    fun unlockWithSecret(enteredSecret: String): Boolean {
        if (verifySecretSync(enteredSecret)) {
            _lockState.value = _lockState.value.copy(isLocked = false, errorMessage = null)
            return true
        } else {
            val err = when (_lockState.value.lockType) {
                LockType.PIN -> "Incorrect PIN. Try again."
                LockType.PASSWORD -> "Incorrect Password. Try again."
                LockType.PATTERN -> "Incorrect Pattern. Try again."
                else -> "Invalid credential."
            }
            _lockState.value = _lockState.value.copy(errorMessage = err)
            return false
        }
    }

    fun unlockWithBiometrics() {
        _lockState.value = _lockState.value.copy(isLocked = false, errorMessage = null)
    }

    fun lockApp() {
        if (!_isAwaitingActivityResult.value && lockType.value != LockType.NONE) {
            _lockState.value = _lockState.value.copy(isLocked = true, errorMessage = null)
        }
    }

    fun lockAppNow() {
        if (lockType.value != LockType.NONE) {
            _lockState.value = _lockState.value.copy(isLocked = true, errorMessage = null)
        }
    }

    // Backup actions
    fun exportBackup(uri: Uri, includeSensitive: Boolean) {
        viewModelScope.launch {
            val allList = repository.getAllList()
            val success = BackupManager.exportToJson(getApplication(), uri, allList, includeSensitive)
            if (success) {
                showMessage("Backup exported successfully (${allList.size} items)")
            } else {
                showMessage("Export failed")
            }
        }
    }

    fun importBackup(uri: Uri, replaceExisting: Boolean) {
        viewModelScope.launch {
            val result = BackupManager.importFromJson(getApplication(), uri)
            result.onSuccess { importedItems ->
                if (replaceExisting) {
                    repository.clearAll()
                }
                repository.insertAll(importedItems)
                showMessage("Imported ${importedItems.size} items successfully")
            }.onFailure { err ->
                showMessage("Import failed: ${err.message}")
            }
        }
    }
}

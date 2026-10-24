package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "offline_qr_prefs")

enum class LockType {
    NONE,
    PIN,
    PASSWORD,
    PATTERN
}

class AppPreferences(private val context: Context) {
    companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode") // "system", "light", "dark"
        val VIBRATE_ON_SCAN = booleanPreferencesKey("vibrate_on_scan")
        val PREVENT_DUPLICATES = booleanPreferencesKey("prevent_duplicates")
        val SAVE_SENSITIVE_HISTORY = booleanPreferencesKey("save_sensitive_history")

        val LOCK_TYPE = stringPreferencesKey("lock_type") // "NONE", "PIN", "PASSWORD", "PATTERN"
        val PIN_LENGTH = intPreferencesKey("pin_length") // 4..8
        val LOCK_SALT = stringPreferencesKey("lock_salt")
        val LOCK_HASH = stringPreferencesKey("lock_hash")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")

        // Legacy keys for backward-compatibility
        val PIN_LOCK_ENABLED = booleanPreferencesKey("pin_lock_enabled")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val PIN_HASH = stringPreferencesKey("pin_hash")
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[THEME_MODE] ?: "system"
    }

    val vibrateOnScan: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[VIBRATE_ON_SCAN] ?: true
    }

    val preventDuplicates: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PREVENT_DUPLICATES] ?: true
    }

    val saveSensitiveHistory: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SAVE_SENSITIVE_HISTORY] ?: true
    }

    val lockType: Flow<LockType> = context.dataStore.data.map { prefs ->
        val raw = prefs[LOCK_TYPE]
        if (raw != null) {
            try {
                LockType.valueOf(raw)
            } catch (e: Exception) {
                LockType.NONE
            }
        } else if (prefs[PIN_LOCK_ENABLED] == true) {
            LockType.PIN
        } else {
            LockType.NONE
        }
    }

    val pinLength: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PIN_LENGTH] ?: 4
    }

    val lockSalt: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[LOCK_SALT] ?: prefs[PIN_SALT]
    }

    val lockHash: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[LOCK_HASH] ?: prefs[PIN_HASH]
    }

    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[BIOMETRIC_ENABLED] ?: false
    }

    // Convenience property for whether any security lock is active
    val isLockActive: Flow<Boolean> = lockType.map { it != LockType.NONE }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[THEME_MODE] = mode }
    }

    suspend fun setVibrateOnScan(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[VIBRATE_ON_SCAN] = enabled }
    }

    suspend fun setPreventDuplicates(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[PREVENT_DUPLICATES] = enabled }
    }

    suspend fun setSaveSensitiveHistory(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[SAVE_SENSITIVE_HISTORY] = enabled }
    }

    suspend fun setSecurityLock(
        type: LockType,
        salt: String? = null,
        hash: String? = null,
        length: Int = 4,
        enableBiometrics: Boolean = false
    ) {
        context.dataStore.edit { prefs ->
            prefs[LOCK_TYPE] = type.name
            prefs[PIN_LENGTH] = length
            if (salt != null) prefs[LOCK_SALT] = salt else prefs.remove(LOCK_SALT)
            if (hash != null) prefs[LOCK_HASH] = hash else prefs.remove(LOCK_HASH)
            prefs[BIOMETRIC_ENABLED] = enableBiometrics

            // Sync legacy
            prefs[PIN_LOCK_ENABLED] = (type != LockType.NONE)
            if (salt != null) prefs[PIN_SALT] = salt else prefs.remove(PIN_SALT)
            if (hash != null) prefs[PIN_HASH] = hash else prefs.remove(PIN_HASH)
        }
    }

    suspend fun disableLock() {
        context.dataStore.edit { prefs ->
            prefs[LOCK_TYPE] = LockType.NONE.name
            prefs[PIN_LOCK_ENABLED] = false
            prefs.remove(LOCK_SALT)
            prefs.remove(LOCK_HASH)
            prefs.remove(PIN_SALT)
            prefs.remove(PIN_HASH)
            prefs[BIOMETRIC_ENABLED] = false
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[BIOMETRIC_ENABLED] = enabled }
    }
}

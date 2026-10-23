package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "offline_qr_prefs")

class AppPreferences(private val context: Context) {
    companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode") // "system", "light", "dark"
        val VIBRATE_ON_SCAN = booleanPreferencesKey("vibrate_on_scan")
        val PREVENT_DUPLICATES = booleanPreferencesKey("prevent_duplicates")
        val SAVE_SENSITIVE_HISTORY = booleanPreferencesKey("save_sensitive_history")
        val PIN_LOCK_ENABLED = booleanPreferencesKey("pin_lock_enabled")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
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

    val pinLockEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PIN_LOCK_ENABLED] ?: false
    }

    val pinSalt: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PIN_SALT]
    }

    val pinHash: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PIN_HASH]
    }

    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[BIOMETRIC_ENABLED] ?: false
    }

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

    suspend fun setPinSecurity(enabled: Boolean, salt: String? = null, hash: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[PIN_LOCK_ENABLED] = enabled
            if (salt != null) prefs[PIN_SALT] = salt else if (!enabled) prefs.remove(PIN_SALT)
            if (hash != null) prefs[PIN_HASH] = hash else if (!enabled) prefs.remove(PIN_HASH)
            if (!enabled) prefs[BIOMETRIC_ENABLED] = false
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[BIOMETRIC_ENABLED] = enabled }
    }
}

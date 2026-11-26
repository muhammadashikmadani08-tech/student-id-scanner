package com.institution.idscanner.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

/**
 * Institution name and the Admin PIN, stored only on the device.
 * Keys are suffixed with the account key ("default" when no Gmail is set),
 * so each Gmail account has its own institution name and its own Admin PIN
 * (default 1234 for every account).
 */
class SettingsStore(
    private val context: Context,
    accountKey: String = "default"
) {
    private val KEY_NAME = stringPreferencesKey("institution_name_$accountKey")
    private val KEY_PIN = stringPreferencesKey("admin_pin_$accountKey")

    // Legacy keys from before per-account settings, used only as a fallback
    // for the default account so existing users keep their saved values.
    private val LEGACY_NAME = stringPreferencesKey("institution_name")
    private val LEGACY_PIN = stringPreferencesKey("admin_pin")
    private val isDefaultAccount = accountKey == "default"

    val institutionName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_NAME]
            ?: (if (isDefaultAccount) prefs[LEGACY_NAME] else null)
            ?: "আপনার প্রতিষ্ঠানের নাম"
    }
    val adminPin: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_PIN]
            ?: (if (isDefaultAccount) prefs[LEGACY_PIN] else null)
            ?: "1234"
    }

    suspend fun setInstitutionName(name: String) {
        context.dataStore.edit { it[KEY_NAME] = name }
    }

    suspend fun setAdminPin(pin: String) {
        context.dataStore.edit { it[KEY_PIN] = pin }
    }
}

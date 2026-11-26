package com.institution.idscanner.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.MessageDigest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.accountsDataStore: DataStore<Preferences> by preferencesDataStore("accounts")

/**
 * Which Gmail account is active. There is NO Google sign-in and NO cloud
 * upload: the email is only a local key. Each Gmail gets its own separate
 * database file and its own settings on this phone, so switching Gmail
 * switches to a completely separate set of students.
 */
class AccountStore(private val context: Context) {

    val currentAccountEmail: Flow<String> = context.accountsDataStore.data.map {
        it[KEY_EMAIL].orEmpty()
    }

    suspend fun setCurrentAccountEmail(email: String) {
        val normalised = normaliseEmail(email)
        context.accountsDataStore.edit { it[KEY_EMAIL] = normalised }
    }

    companion object {
        private val KEY_EMAIL = stringPreferencesKey("current_account_email")

        /** Trim + lowercase. Returns "" for a blank input. */
        fun normaliseEmail(raw: String): String = raw.trim().lowercase()

        fun isValidEmail(email: String): Boolean = normaliseEmail(email).contains("@")

        /**
         * Stable per-account key used for database file names and settings
         * key suffixes: "default" for no Gmail, otherwise the first 16 hex
         * chars of the SHA-256 of the normalised email.
         */
        fun accountKey(email: String): String {
            val normalised = normaliseEmail(email)
            if (normalised.isEmpty()) return "default"
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(normalised.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }.take(16)
        }

        /** Synchronous read for startup, before the UI is built. */
        fun readCurrentEmail(context: Context): String = runBlocking {
            context.accountsDataStore.data.first()[KEY_EMAIL].orEmpty()
        }
    }
}

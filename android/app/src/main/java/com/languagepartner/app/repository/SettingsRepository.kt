package com.languagepartner.app.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private val SERVER_ADDRESS_KEY = stringPreferencesKey("server_address")

/**
 * Validation regex for IP:port format.
 * Matches: ddd.ddd.ddd.ddd:ppppp (1-3 digit octets, 2-5 digit port)
 */
val SERVER_ADDRESS_REGEX = Regex("""^\d{1,3}(\.\d{1,3}){3}:\d{2,5}$""")

class SettingsRepository(private val context: Context) {

    val serverAddress: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SERVER_ADDRESS_KEY] ?: ""
    }

    suspend fun saveServerAddress(address: String) {
        context.dataStore.edit { preferences ->
            preferences[SERVER_ADDRESS_KEY] = address
        }
    }
}

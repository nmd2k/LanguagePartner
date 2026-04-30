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
private val SOURCE_LANGUAGE_KEY = stringPreferencesKey("source_language")
private val TARGET_LANGUAGE_KEY = stringPreferencesKey("target_language")

val SERVER_ADDRESS_REGEX = Regex("""^\d{1,3}(\.\d{1,3}){3}:\d{2,5}$""")

class SettingsRepository(private val context: Context) {

    val serverAddress: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SERVER_ADDRESS_KEY] ?: ""
    }

    val sourceLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SOURCE_LANGUAGE_KEY] ?: "en"
    }

    val targetLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TARGET_LANGUAGE_KEY] ?: "zh"
    }

    suspend fun saveServerAddress(address: String) {
        context.dataStore.edit { preferences ->
            preferences[SERVER_ADDRESS_KEY] = address
        }
    }

    suspend fun saveSourceLanguage(code: String) {
        context.dataStore.edit { preferences ->
            preferences[SOURCE_LANGUAGE_KEY] = code
        }
    }

    suspend fun saveTargetLanguage(code: String) {
        context.dataStore.edit { preferences ->
            preferences[TARGET_LANGUAGE_KEY] = code
        }
    }

    suspend fun saveBothLanguages(sourceCode: String, targetCode: String) {
        context.dataStore.edit { preferences ->
            preferences[SOURCE_LANGUAGE_KEY] = sourceCode
            preferences[TARGET_LANGUAGE_KEY] = targetCode
        }
    }
}

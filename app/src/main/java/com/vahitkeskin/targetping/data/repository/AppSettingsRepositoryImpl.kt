package com.vahitkeskin.targetping.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.vahitkeskin.targetping.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AppSettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : AppSettingsRepository {

    private object Keys {
        val STEALTH_MODE = booleanPreferencesKey("stealth_mode")
    }

    // DataStore'dan veriyi oku, varsayılan değer false
    override val isStealthModeEnabled: Flow<Boolean> = dataStore.data
        .map { preferences ->
            preferences[Keys.STEALTH_MODE] ?: false
        }

    // DataStore'a veriyi yaz
    override suspend fun setStealthMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.STEALTH_MODE] = enabled
        }
    }
}
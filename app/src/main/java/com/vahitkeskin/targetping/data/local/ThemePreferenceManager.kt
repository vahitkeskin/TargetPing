package com.vahitkeskin.targetping.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vahitkeskin.targetping.domain.model.MapStyleConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Context extension ile DataStore oluşturma (Singleton olması önerilir)
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

class ThemePreferenceManager(private val context: Context) {

    private val MAP_STYLE_KEY = stringPreferencesKey("map_style_config")

    // Seçili stili okuma (Flow olarak)
    val mapStyleFlow: Flow<MapStyleConfig> = context.dataStore.data
        .map { preferences ->
            val key = preferences[MAP_STYLE_KEY] ?: MapStyleConfig.HYBRID.storageKey
            MapStyleConfig.fromKey(key)
        }

    // Seçili stili kaydetme
    suspend fun saveMapStyle(style: MapStyleConfig) {
        context.dataStore.edit { preferences ->
            preferences[MAP_STYLE_KEY] = style.storageKey
        }
    }
}
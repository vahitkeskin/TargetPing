package com.vahitkeskin.targetping.domain.repository

import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    // Gizli modun durumunu anlık takip etmek için Flow
    val isStealthModeEnabled: Flow<Boolean>

    // Gizli modu açıp kapatmak için fonksiyon
    suspend fun setStealthMode(enabled: Boolean)
}
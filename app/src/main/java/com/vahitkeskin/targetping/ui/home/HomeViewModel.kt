package com.vahitkeskin.targetping.ui.home

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vahitkeskin.targetping.data.local.ThemePreferenceManager
import com.vahitkeskin.targetping.data.local.entity.LogEventType
import com.vahitkeskin.targetping.data.service.LocationTrackingService
import com.vahitkeskin.targetping.domain.model.MapStyleConfig
import com.vahitkeskin.targetping.domain.model.TargetLocation
import com.vahitkeskin.targetping.domain.repository.AppSettingsRepository
import com.vahitkeskin.targetping.domain.repository.LogRepository
import com.vahitkeskin.targetping.domain.repository.TargetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TargetRepository,
    private val logRepository: LogRepository,
    private val settingsRepository: AppSettingsRepository,
    private val application: Application
) : ViewModel() {

    // ============================================================================================
    // STATE FLOWS (UI Durumları)
    // ============================================================================================

    // 1. Hedef Listesi
    private val _targets = MutableStateFlow<List<TargetLocation>>(emptyList())
    val targets: StateFlow<List<TargetLocation>> = _targets.asStateFlow()

    // 2. Takip Durumu (Servis Çalışıyor mu?)
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    // 3. Navigasyon (Pusula Kilitli Hedef)
    private val _navigationTarget = MutableStateFlow<TargetLocation?>(null)
    val navigationTarget: StateFlow<TargetLocation?> = _navigationTarget.asStateFlow()

    val logs = logRepository.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 4. Gizli Mod (Stealth Mode)
    // _isStealthMode silindi. Veri kaynağı doğrudan Repository (DataStore) oldu.
    val isStealthMode: StateFlow<Boolean> = settingsRepository.isStealthModeEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _activityLogs = MutableStateFlow<List<LogModel>>(
        listOf(
            LogModel("Target Alpha", "Bölgeye Giriş Yapıldı", "14:20", true),
            LogModel("Home Base", "Bölgeden Çıkış", "12:15", false),
            LogModel("Office", "Takip Başlatıldı", "09:00", true),
            LogModel("System", "Güvenlik Taraması Tamamlandı", "08:55", false),
        )
    )
    val activityLogs = _activityLogs.asStateFlow()

    // --- YENİ: AYARLAR STATE ---
    // Gerçekte DataStore'dan gelir.
    var isBiometricEnabled = MutableStateFlow(true)
    var isDarkThemeForced = MutableStateFlow(true)
    var notificationSound = MutableStateFlow(true)
    private val themeManager = ThemePreferenceManager(application)

    init {
        viewModelScope.launch {
            themeManager.mapStyleFlow.collect { savedStyle ->
                _currentMapStyle.value = savedStyle
            }
        }
        fetchTargets()
    }

    private val _currentMapStyle = MutableStateFlow(MapStyleConfig.STANDARD)
    val currentMapStyle: StateFlow<MapStyleConfig> = _currentMapStyle.asStateFlow()

    fun updateMapStyle(newStyle: MapStyleConfig) {
        viewModelScope.launch {
            _currentMapStyle.value = newStyle
            themeManager.saveMapStyle(newStyle) // DataStore'a yaz
        }
    }

    // Logları temizleme fonksiyonu
    fun clearLogs() {
        viewModelScope.launch {
            logRepository.clearAll()
        }
    }

    // (Opsiyonel) Test için manuel log ekleme fonksiyonu
    fun addTestLog() {
        viewModelScope.launch {
            logRepository.logEvent("Test Hedef", LogEventType.ENTRY, "Simülasyon Girişi")
        }
    }

    private fun fetchTargets() {
        viewModelScope.launch {
            repository.getTargets().collect { list ->
                _targets.value = list
            }
        }
    }

    fun toggleTargetActive(id: String, isActive: Boolean) {
        viewModelScope.launch {
            repository.updateTargetStatus(id, isActive)
        }
    }

    fun addTarget(name: String, lat: Double, lng: Double, radius: Int) {
        viewModelScope.launch {
            val newTarget = TargetLocation(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                latitude = lat,
                longitude = lng,
                radiusMeters = radius,
                isActive = true
            )
            repository.insertTarget(newTarget)
        }
    }

    fun deleteTarget(id: String) {
        viewModelScope.launch {
            repository.deleteTarget(id)
        }
    }

    // ============================================================================================
    // NAVİGASYON & TAKİP İŞLEMLERİ (Tracking & Navigation)
    // ============================================================================================

    fun startNavigation(target: TargetLocation) {
        _navigationTarget.value = target
    }

    fun stopNavigation() {
        _navigationTarget.value = null
    }

    fun toggleTracking(enable: Boolean) {
        _isTracking.value = enable
        val serviceIntent = Intent(application, LocationTrackingService::class.java).apply {
            action = if (enable) LocationTrackingService.ACTION_START else LocationTrackingService.ACTION_STOP
        }

        if (enable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                application.startForegroundService(serviceIntent)
            } else {
                application.startService(serviceIntent)
            }
        } else {
            application.startService(serviceIntent)
        }
    }

    // ============================================================================================
    // GİZLİ MOD İŞLEMLERİ (Stealth Mode Actions)
    // ============================================================================================

    fun setStealthMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setStealthMode(enabled)
        }
    }
}
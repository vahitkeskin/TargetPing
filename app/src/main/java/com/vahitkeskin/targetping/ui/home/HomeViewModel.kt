package com.vahitkeskin.targetping.ui.home

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vahitkeskin.targetping.data.service.LocationTrackingService
import com.vahitkeskin.targetping.domain.model.TargetLocation
import com.vahitkeskin.targetping.domain.repository.TargetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TargetRepository,
    private val application: Application // Servisi başlatmak için Context lazım
) : ViewModel() {

    // --- STATE: Hedef Listesi ---
    private val _targets = MutableStateFlow<List<TargetLocation>>(emptyList())
    val targets: StateFlow<List<TargetLocation>> = _targets.asStateFlow()

    // --- STATE: Takip Açık mı? ---
    // (Gerçek bir uygulamada bu durumu DataStore veya SharedPreferences'ta tutmak daha iyidir)
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    init {
        // ViewModel oluştuğunda verileri dinlemeye başla
        fetchTargets()
    }

    private fun fetchTargets() {
        viewModelScope.launch {
            // Flow kullandığımız için veritabanı değişince burası otomatik tetiklenir
            repository.getTargets().collect { list ->
                _targets.value = list
            }
        }
    }

    // --- FONKSİYON 1: Takibi Aç/Kapa (toggleTracking) ---
    fun toggleTracking(enable: Boolean) {
        _isTracking.value = enable

        val serviceIntent = Intent(application, LocationTrackingService::class.java).apply {
            action = if (enable) LocationTrackingService.ACTION_START else LocationTrackingService.ACTION_STOP
        }

        if (enable) {
            // Servisi Başlat (Android O ve üzeri için Foreground şart)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                application.startForegroundService(serviceIntent)
            } else {
                application.startService(serviceIntent)
            }
        } else {
            // Servisi Durdur (Intent göndererek servise dur emri veriyoruz)
            application.startService(serviceIntent)
        }
    }

    // --- FONKSİYON 2: Hedefi Aktif/Pasif Yap (toggleTargetActive) ---
    fun toggleTargetActive(id: String, isActive: Boolean) {
        viewModelScope.launch {
            repository.updateTargetStatus(id, isActive)
        }
    }

    // --- Diğer Fonksiyonlar (Ekleme / Silme) ---

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
}
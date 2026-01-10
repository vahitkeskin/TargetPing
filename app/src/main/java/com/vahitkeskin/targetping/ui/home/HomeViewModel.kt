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
    private val application: Application
) : ViewModel() {

    // --- LİSTE VERİSİ ---
    private val _targets = MutableStateFlow<List<TargetLocation>>(emptyList())
    val targets: StateFlow<List<TargetLocation>> = _targets.asStateFlow()

    // --- TAKİP DURUMU (Play/Stop) ---
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    // --- PUSULA NAVİGASYONU (Yeni Eklenen) ---
    // Hangi hedefe kilitlendiğimizi tutar. Null ise navigasyon kapalıdır.
    private val _navigationTarget = MutableStateFlow<TargetLocation?>(null)
    val navigationTarget: StateFlow<TargetLocation?> = _navigationTarget.asStateFlow()

    init {
        fetchTargets()
    }

    private fun fetchTargets() {
        viewModelScope.launch {
            repository.getTargets().collect { list ->
                _targets.value = list
            }
        }
    }

    // --- NAVİGASYON FONKSİYONLARI (Bunlar eksikti) ---
    fun startNavigation(target: TargetLocation) {
        _navigationTarget.value = target
    }

    fun stopNavigation() {
        _navigationTarget.value = null
    }

    // --- TAKİP SERVİSİ (Play/Stop) ---
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

    // --- HEDEF YÖNETİMİ ---
    fun toggleTargetActive(id: String, isActive: Boolean) {
        viewModelScope.launch {
            repository.updateTargetStatus(id, isActive)
        }
    }

    // Haritadan veya Listeden ekleme yapmak için
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
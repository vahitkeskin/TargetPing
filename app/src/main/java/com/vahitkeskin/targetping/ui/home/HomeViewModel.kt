package com.vahitkeskin.targetping.ui.home

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vahitkeskin.targetping.data.service.LocationTrackingService
import com.vahitkeskin.targetping.domain.model.TargetLocation
import com.vahitkeskin.targetping.domain.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: LocationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val targets = repository.getAllTargets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isTracking = MutableStateFlow(false)
    val isTracking = _isTracking.asStateFlow()

    fun toggleTracking(enable: Boolean) {
        _isTracking.value = enable
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            action = if (enable) LocationTrackingService.ACTION_START else LocationTrackingService.ACTION_STOP
        }
        if (enable) context.startForegroundService(intent) else context.stopService(intent)
    }

    fun deleteTarget(id: String) {
        viewModelScope.launch { repository.deleteTarget(id) }
    }

    fun toggleTargetActive(id: String, newActiveState: Boolean) {
        viewModelScope.launch {
            repository.toggleActiveState(id, newActiveState)
        }
    }

    fun addTarget(name: String, lat: Double, lng: Double, radius: Int) {
        viewModelScope.launch {
            repository.insertTarget(
                TargetLocation(name = name, latitude = lat, longitude = lng, radiusMeters = radius)
            )
        }
    }
}
package com.vahitkeskin.targetping.ui.features.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.res.Resources
import android.location.Location
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LocationDisabled
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SatelliteAlt
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.vahitkeskin.targetping.R
import com.vahitkeskin.targetping.ui.components.CompassOverlay
import com.vahitkeskin.targetping.ui.features.add_edit.GlassCard
import com.vahitkeskin.targetping.ui.home.HomeViewModel
import com.vahitkeskin.targetping.ui.home.components.RadarPulseAnimation
import com.vahitkeskin.targetping.utils.openAppSettings
import com.vahitkeskin.targetping.utils.uninstallSelf
import kotlinx.coroutines.launch

private val CyberTeal = Color(0xFF00E5FF)
private val AlertRed = Color(0xFFFF2A68)
private val DarkSurface = Color(0xFF121212)

// Bekleyen işlemi takip etmek için Enum
private enum class MapAction {
    NONE,
    TOGGLE_TRACKING, // Başlat/Durdur
    ADD_NEW,        // Yeni Ekle
    MY_LOCATION     // Konumum
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class, ExperimentalFoundationApi::class)
@Composable
fun MapScreen(
    viewModel: HomeViewModel,
    onNavigateToAdd: (LatLng) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val targets by viewModel.targets.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()
    val navigationTarget by viewModel.navigationTarget.collectAsState()

    val cameraPositionState = rememberCameraPositionState()
    val scope = rememberCoroutineScope()

    // --- İZİN YÖNETİCİLERİ ---
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberMultiplePermissionsState(permissions = listOf(Manifest.permission.POST_NOTIFICATIONS))
    } else null

    var userLocation by remember { mutableStateOf<Location?>(null) }

    // --- STATE YÖNETİMİ ---
    // Kullanıcının en son hangi butona bastığını hatırlar.
    // Bu değişken sayesinde "İzin Ver" denildiğinde işlem otomatik devam eder.
    var pendingAction by remember { mutableStateOf(MapAction.NONE) }

    var hasRequestedLocationPermission by rememberSaveable { mutableStateOf(false) }
    var hasRequestedNotificationPermission by rememberSaveable { mutableStateOf(false) }

    // --- DİYALOG GÖRÜNÜRLÜKLERİ ---
    var showLocationSettingsDialog by remember { mutableStateOf(false) }
    var showNotificationSettingsDialog by remember { mutableStateOf(false) }
    var showNoTargetDialog by remember { mutableStateOf(false) }

    // --- ORTAK İŞLEM FONKSİYONU ---
    // Tüm butonlar bu fonksiyonu çağırır.
    fun executeAction(action: MapAction) {
        // İşlemi hafızaya al (Otomatik devam için)
        pendingAction = action

        // 1. ADIM: Bildirim İzni Kontrolü
        if (notificationPermissionState != null && !notificationPermissionState.allPermissionsGranted) {
            if (notificationPermissionState.shouldShowRationale) {
                notificationPermissionState.launchMultiplePermissionRequest()
            } else {
                if (!hasRequestedNotificationPermission) {
                    notificationPermissionState.launchMultiplePermissionRequest()
                    hasRequestedNotificationPermission = true
                } else {
                    showNotificationSettingsDialog = true
                }
            }
            return // İzin yoksa dur, LaunchedEffect izin gelince tekrar çağıracak
        }

        // 2. ADIM: Konum İzni Kontrolü
        if (!locationPermissionsState.allPermissionsGranted) {
            if (locationPermissionsState.shouldShowRationale) {
                locationPermissionsState.launchMultiplePermissionRequest()
            } else {
                if (!hasRequestedLocationPermission) {
                    locationPermissionsState.launchMultiplePermissionRequest()
                    hasRequestedLocationPermission = true
                } else {
                    showLocationSettingsDialog = true
                }
            }
            return // İzin yoksa dur, LaunchedEffect izin gelince tekrar çağıracak
        }

        // 3. ADIM: İzinler Tamam -> İşlemi Gerçekleştir
        when (action) {
            MapAction.TOGGLE_TRACKING -> {
                if (targets.isEmpty()) {
                    showNoTargetDialog = true
                } else {
                    viewModel.toggleTracking(!isTracking)
                }
            }

            MapAction.ADD_NEW -> {
                onNavigateToAdd(cameraPositionState.position.target)
            }

            MapAction.MY_LOCATION -> {
                userLocation?.let {
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(it.latitude, it.longitude),
                                16f
                            )
                        )
                    }
                }
            }

            MapAction.NONE -> {}
        }

        // İşlem başarıyla yapıldıysa bekleyen işlemi temizle
        pendingAction = MapAction.NONE
    }

    // --- OTOMATİK TETİKLEME (LaunchedEffect) ---
    // İzin durumları değiştiğinde (Sistem popup'ı veya Ayarlar dönüşü) çalışır
    val isNotifGranted = notificationPermissionState?.allPermissionsGranted ?: true
    val isLocGranted = locationPermissionsState.allPermissionsGranted

    LaunchedEffect(isNotifGranted, isLocGranted) {
        // Eğer Konum İzni YENİ geldiyse ve konumu henüz almadıysak hemen alalım
        if (isLocGranted && userLocation == null) {
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.lastLocation.addOnSuccessListener { loc ->
                userLocation = loc
                if (loc != null && cameraPositionState.position.target.latitude == 0.0) {
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(
                                    loc.latitude,
                                    loc.longitude
                                ), 15f
                            )
                        )
                    }
                }

                // Konum alındıktan sonra eğer bekleyen işlem "Konumum" ise onu tekrar tetikle
                if (pendingAction == MapAction.MY_LOCATION) {
                    executeAction(MapAction.MY_LOCATION)
                }
            }
        }

        // Bekleyen bir işlem varsa ve gerekli izinler sağlanmışsa otomatik devam et
        if (pendingAction != MapAction.NONE) {

            // Eğer işlem "Başlat" ise ama hiç hedef yoksa sessizce iptal et (Hata popup'ı açma)
            if (pendingAction == MapAction.TOGGLE_TRACKING && targets.isEmpty()) {
                pendingAction = MapAction.NONE
            }
            // Konumum işlemi için yukarıdaki location success listener'ı bekliyoruz, burayı pas geç
            else if (pendingAction == MapAction.MY_LOCATION && userLocation == null) {
                // Bekle, konum gelince yukarıdaki blok çalıştıracak
            } else {
                // Diğer tüm durumlarda (Yeni Ekle, vb.) işlemi otomatik yap
                executeAction(pendingAction)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // 1. HARİTA
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = locationPermissionsState.allPermissionsGranted,
                mapStyleOptions = try {
                    MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
                } catch (e: Resources.NotFoundException) {
                    null
                }
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                compassEnabled = false,
                myLocationButtonEnabled = false
            ),
            onMapLongClick = { latLng -> onNavigateToAdd(latLng) }
        ) {
            targets.forEach { target ->
                val markerHue = if (target.isActive) 180f else 0f
                val circleColor = if (target.isActive) CyberTeal else Color.Gray
                val circleAlpha = if (target.isActive) 0.15f else 0.05f
                val strokeAlpha = if (target.isActive) 0.8f else 0.3f

                Marker(
                    state = MarkerState(position = LatLng(target.latitude, target.longitude)),
                    title = target.name,
                    icon = BitmapDescriptorFactory.defaultMarker(markerHue),
                    alpha = if (target.isActive) 1f else 0.6f,
                    onClick = {
                        viewModel.startNavigation(target)
                        false
                    }
                )

                Circle(
                    center = LatLng(target.latitude, target.longitude),
                    radius = target.radiusMeters.toDouble(),
                    strokeColor = circleColor.copy(alpha = strokeAlpha),
                    strokeWidth = 3f,
                    fillColor = circleColor.copy(alpha = circleAlpha)
                )
            }
        }

        // 2. RADAR
        if (isTracking) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                RadarPulseAnimation(
                    modifier = Modifier.size(300.dp),
                    isPlaying = true,
                    color = AlertRed
                )
            }
        }

        // 3. PUSULA
        if (navigationTarget != null) {
            CompassOverlay(
                target = navigationTarget!!,
                userLocation = userLocation,
                onStopNavigation = { viewModel.stopNavigation() }
            )
        }

        // 4. ÜST BİLGİ KARTI
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .padding(all = 16.dp)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .combinedClickable(
                    onClick = { },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        context.uninstallSelf()
                    }
                )
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "SİSTEM DURUMU", // TÜRKÇE
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Text(
                            "${targets.count { it.isActive }} / ${targets.size} AKTİF", // TÜRKÇE
                            style = MaterialTheme.typography.titleMedium,
                            color = CyberTeal,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.SatelliteAlt,
                        contentDescription = null,
                        tint = if (userLocation != null) CyberTeal else Color.Gray
                    )
                }
            }
        }

        // 5. SAĞ ALT BUTONLAR (Bottom Navigation Üstüne Konumlandırma)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 120.dp),
            horizontalAlignment = Alignment.End
        ) {
            // A) BAŞLAT / DURDUR BUTONU
            FloatingActionButton(
                onClick = { executeAction(MapAction.TOGGLE_TRACKING) },
                containerColor = if (isTracking) AlertRed else CyberTeal,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = if (isTracking) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // B) YENİ YER EKLE BUTONU
            SmallFloatingActionButton(
                onClick = { executeAction(MapAction.ADD_NEW) },
                containerColor = DarkSurface,
                contentColor = Color.White
            ) {
                Icon(Icons.Rounded.Add, null)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // C) KONUMUM BUTONU
            SmallFloatingActionButton(
                onClick = { executeAction(MapAction.MY_LOCATION) },
                containerColor = DarkSurface,
                contentColor = Color.White
            ) {
                Icon(
                    if (locationPermissionsState.allPermissionsGranted) Icons.Rounded.MyLocation else Icons.Rounded.LocationDisabled,
                    null
                )
            }
        }

        // --- DİYALOGLAR (TÜRKÇE) ---

        if (showLocationSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showLocationSettingsDialog = false },
                title = { Text("Konum İzni Gerekli") },
                text = { Text("İşlemlere devam edebilmek için ayarlardan Konum iznini açmanız gerekmektedir.") },
                confirmButton = {
                    TextButton(onClick = {
                        showLocationSettingsDialog = false
                        context.openAppSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    }) { Text("AYARLARA GİT", color = CyberTeal) }
                },
                dismissButton = {
                    TextButton(onClick = { showLocationSettingsDialog = false }) {
                        Text("İPTAL", color = Color.Gray)
                    }
                },
                containerColor = DarkSurface,
                titleContentColor = CyberTeal,
                textContentColor = Color.White
            )
        }

        if (showNotificationSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showNotificationSettingsDialog = false },
                title = { Text("Bildirim İzni Gerekli") },
                text = { Text("Sistemin kararlı çalışması için bildirim izni vermeniz şarttır.") },
                confirmButton = {
                    TextButton(onClick = {
                        showNotificationSettingsDialog = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.openAppSettings(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        } else {
                            context.openAppSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        }
                    }) { Text("BİLDİRİM AYARLARI", color = CyberTeal) }
                },
                dismissButton = {
                    TextButton(onClick = { showNotificationSettingsDialog = false }) {
                        Text("İPTAL", color = Color.Gray)
                    }
                },
                containerColor = DarkSurface,
                titleContentColor = CyberTeal,
                textContentColor = Color.White
            )
        }

        if (showNoTargetDialog) {
            AlertDialog(
                onDismissRequest = { showNoTargetDialog = false },
                title = { Text("Hedef Bulunamadı") },
                text = { Text("Sistemi başlatmak için en az bir hedef nokta eklemelisiniz.") },
                confirmButton = {
                    TextButton(onClick = {
                        showNoTargetDialog = false
                        // Direkt ekleme fonksiyonunu çağırıyoruz
                        executeAction(MapAction.ADD_NEW)
                    }) { Text("EKLE", color = CyberTeal) }
                },
                dismissButton = {
                    TextButton(onClick = { showNoTargetDialog = false }) {
                        Text("İPTAL", color = Color.Gray)
                    }
                },
                containerColor = DarkSurface,
                titleContentColor = CyberTeal,
                textContentColor = Color.White
            )
        }
    }
}
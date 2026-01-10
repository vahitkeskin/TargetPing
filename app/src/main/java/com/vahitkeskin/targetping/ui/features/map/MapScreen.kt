package com.vahitkeskin.targetping.ui.features.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.res.Resources
import android.location.Location
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import com.vahitkeskin.targetping.R
import com.vahitkeskin.targetping.ui.components.CompassOverlay
import com.vahitkeskin.targetping.ui.home.GlassCard
import com.vahitkeskin.targetping.ui.home.HomeViewModel
import com.vahitkeskin.targetping.ui.home.components.RadarPulseAnimation
import com.vahitkeskin.targetping.utils.openAppSettings
import kotlinx.coroutines.launch

private val CyberTeal = Color(0xFF00E5FF)
private val AlertRed = Color(0xFFFF2A68)
private val DarkSurface = Color(0xFF121212)

@SuppressLint("MissingPermission")
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    viewModel: HomeViewModel,
    onNavigateToAdd: (LatLng) -> Unit
) {
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

    // Android 13+ (API 33) için Bildirim İzni
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberMultiplePermissionsState(permissions = listOf(Manifest.permission.POST_NOTIFICATIONS))
    } else null

    var userLocation by remember { mutableStateOf<Location?>(null) }

    // --- KONTROL BAYRAKLARI ---
    // Kullanıcının "İzin İste" butonuna basıp basmadığını takip eder.
    var hasRequestedLocationPermission by rememberSaveable { mutableStateOf(false) }
    var hasRequestedNotificationPermission by rememberSaveable { mutableStateOf(false) }

    // --- DİYALOG GÖRÜNÜRLÜKLERİ ---
    var showLocationSettingsDialog by remember { mutableStateOf(false) }
    var showNotificationSettingsDialog by remember { mutableStateOf(false) }
    var showNoTargetDialog by remember { mutableStateOf(false) }

    // Harita yüklendiğinde Konum İzni varsa konumu al
    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.lastLocation.addOnSuccessListener { loc ->
                userLocation = loc
                if (loc != null) {
                    scope.launch {
                        if (cameraPositionState.position.target.latitude == 0.0) {
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 15f))
                        }
                    }
                }
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
                mapStyleOptions = try { MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark) } catch (e: Resources.NotFoundException) { null }
            ),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, compassEnabled = false, myLocationButtonEnabled = false),
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
                RadarPulseAnimation(modifier = Modifier.size(300.dp), isPlaying = true, color = AlertRed)
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
        GlassCard(modifier = Modifier.statusBarsPadding().padding(16.dp).fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("SYSTEM STATUS", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("${targets.count { it.isActive }} / ${targets.size} ONLINE", style = MaterialTheme.typography.titleMedium, color = CyberTeal, fontWeight = FontWeight.Bold)
                }
                Icon(imageVector = Icons.Rounded.SatelliteAlt, contentDescription = null, tint = if (userLocation != null) CyberTeal else Color.Gray)
            }
        }

        // 5. SAĞ ALT BUTONLAR
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp)
                .padding(bottom = 130.dp),
            horizontalAlignment = Alignment.End
        ) {
            // A) BAŞLAT / DURDUR BUTONU
            FloatingActionButton(
                onClick = {
                    // ADIM 0: Hedef Kontrolü
                    if (targets.isEmpty()) {
                        showNoTargetDialog = true
                        return@FloatingActionButton
                    }

                    // ADIM 1: BİLDİRİM İZNİ KONTROLÜ (Öncelikli)
                    if (notificationPermissionState != null && !notificationPermissionState.allPermissionsGranted) {
                        // Eğer Sistem İzin Penceresi gösterilmesi gerekiyorsa (Rationale)
                        if (notificationPermissionState.shouldShowRationale) {
                            notificationPermissionState.launchMultiplePermissionRequest()
                        } else {
                            // Rationale False ise iki durum vardır: Ya ilk kez, ya kalıcı red.
                            if (!hasRequestedNotificationPermission) {
                                // İlk kez basıyor -> Sistem Penceresini Aç
                                notificationPermissionState.launchMultiplePermissionRequest()
                                hasRequestedNotificationPermission = true
                            } else {
                                // Zaten basmışız ve izin yok -> KALICI RED -> Ayarlar Popup'ı
                                showNotificationSettingsDialog = true
                            }
                        }
                        // İzin yoksa işlemi burada kes. Konum kontrolüne geçme.
                        return@FloatingActionButton
                    }

                    // ADIM 2: KONUM İZNİ KONTROLÜ (Bildirim varsa buraya gelir)
                    if (!locationPermissionsState.allPermissionsGranted) {
                        if (locationPermissionsState.shouldShowRationale) {
                            locationPermissionsState.launchMultiplePermissionRequest()
                        } else {
                            if (!hasRequestedLocationPermission) {
                                // İlk kez -> Sistem Penceresini Aç
                                locationPermissionsState.launchMultiplePermissionRequest()
                                hasRequestedLocationPermission = true
                            } else {
                                // Zaten basmışız ve izin yok -> KALICI RED -> Ayarlar Popup'ı
                                showLocationSettingsDialog = true
                            }
                        }
                        // İzin yoksa işlemi kes.
                        return@FloatingActionButton
                    }

                    // ADIM 3: HER ŞEY TAMAM -> SERVİSİ BAŞLAT
                    viewModel.toggleTracking(!isTracking)
                },
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

            // B) YENİ YER EKLE BUTONU (Sadece Konum Kontrolü)
            SmallFloatingActionButton(
                onClick = {
                    if (locationPermissionsState.allPermissionsGranted) {
                        onNavigateToAdd(cameraPositionState.position.target)
                    } else {
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
                    }
                },
                containerColor = DarkSurface,
                contentColor = Color.White
            ) {
                Icon(Icons.Rounded.Add, null)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // C) KONUMUM BUTONU (Sadece Konum Kontrolü)
            SmallFloatingActionButton(
                onClick = {
                    if (locationPermissionsState.allPermissionsGranted) {
                        userLocation?.let {
                            scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16f)) }
                        }
                    } else {
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
                    }
                },
                containerColor = DarkSurface,
                contentColor = Color.White
            ) {
                Icon(if (locationPermissionsState.allPermissionsGranted) Icons.Rounded.MyLocation else Icons.Rounded.LocationDisabled, null)
            }
        }

        // --- DİYALOGLAR VE YÖNLENDİRMELER ---

        // 1. KONUM AYARLARI (Uygulama İzinlerine Gider)
        if (showLocationSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showLocationSettingsDialog = false },
                title = { Text("Konum İzni Gerekli") },
                text = { Text("Haritayı kullanabilmek için 'İzinler' menüsünden Konum iznini açmanız gerekmektedir.") },
                confirmButton = {
                    TextButton(onClick = {
                        showLocationSettingsDialog = false
                        // Konum için: Uygulama Detayları Ekranı
                        context.openAppSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    }) { Text("AYARLARA GİT", color = CyberTeal) }
                },
                dismissButton = {
                    TextButton(onClick = { showLocationSettingsDialog = false }) { Text("İPTAL", color = Color.Gray) }
                },
                containerColor = DarkSurface,
                titleContentColor = CyberTeal,
                textContentColor = Color.White
            )
        }

        // 2. BİLDİRİM AYARLARI (Direkt Bildirim Ayarlarına Gider)
        if (showNotificationSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showNotificationSettingsDialog = false },
                title = { Text("Bildirim İzni Gerekli") },
                text = { Text("Takip sistemini başlatmak için bildirim izni vermeniz şarttır. Lütfen açılan ekranda izni verin.") },
                confirmButton = {
                    TextButton(onClick = {
                        showNotificationSettingsDialog = false
                        // Bildirim için: Direkt Bildirim Ekranı
                        context.openAppSettings(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    }) { Text("BİLDİRİM AYARLARI", color = CyberTeal) }
                },
                dismissButton = {
                    TextButton(onClick = { showNotificationSettingsDialog = false }) { Text("İPTAL", color = Color.Gray) }
                },
                containerColor = DarkSurface,
                titleContentColor = CyberTeal,
                textContentColor = Color.White
            )
        }

        // 3. Hedef Yok Uyarısı
        if (showNoTargetDialog) {
            AlertDialog(
                onDismissRequest = { showNoTargetDialog = false },
                title = { Text("Hedef Bulunamadı") },
                text = { Text("Sistemi başlatmak için en az bir hedef nokta eklemelisiniz.") },
                confirmButton = {
                    TextButton(onClick = {
                        showNoTargetDialog = false
                        onNavigateToAdd(cameraPositionState.position.target)
                    }) { Text("EKLE", color = CyberTeal) }
                },
                dismissButton = {
                    TextButton(onClick = { showNoTargetDialog = false }) { Text("İPTAL", color = Color.Gray) }
                },
                containerColor = DarkSurface,
                titleContentColor = CyberTeal,
                textContentColor = Color.White
            )
        }
    }
}
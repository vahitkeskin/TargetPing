package com.vahitkeskin.targetping.ui.features.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import com.vahitkeskin.targetping.R
import com.vahitkeskin.targetping.ui.components.CompassOverlay
import com.vahitkeskin.targetping.ui.features.add_edit.GlassCard
import com.vahitkeskin.targetping.ui.home.HomeViewModel
import com.vahitkeskin.targetping.ui.home.components.RadarPulseAnimation
import com.vahitkeskin.targetping.utils.rememberPermissionAction // YENİ IMPORT
import com.vahitkeskin.targetping.utils.uninstallSelf
import kotlinx.coroutines.launch

private val CyberTeal = Color(0xFF00E5FF)
private val AlertRed = Color(0xFFFF2A68)
private val DarkSurface = Color(0xFF121212)

@SuppressLint("MissingPermission")
@OptIn(ExperimentalFoundationApi::class)
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

    var userLocation by remember { mutableStateOf<Location?>(null) }
    var showNoTargetDialog by remember { mutableStateOf(false) }

    // Harita Mavi Nokta Kontrolü
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    // --- AKSİYON TANIMLARI (BURASI YENİ) ---

    // 1. BAŞLAT/DURDUR İÇİN GÜVENLİ TIKLAMA
    val onToggleClick = rememberPermissionAction(
        isNotificationRequired = true,
        isLocationRequired = true
    ) {
        hasLocationPermission = true // İzin alındıysa mavi noktayı aç
        if (targets.isEmpty()) {
            showNoTargetDialog = true
        } else {
            viewModel.toggleTracking(!isTracking)
        }
    }

    // 2. EKLEME BUTONU İÇİN GÜVENLİ TIKLAMA (Sadece Konum)
    val onAddClick = rememberPermissionAction(
        isNotificationRequired = false,
        isLocationRequired = true
    ) {
        hasLocationPermission = true
        onNavigateToAdd(cameraPositionState.position.target)
    }

    // 3. KONUMUM BUTONU İÇİN GÜVENLİ TIKLAMA (Sadece Konum)
    val onMyLocationClick = rememberPermissionAction(
        isNotificationRequired = false,
        isLocationRequired = true
    ) {
        hasLocationPermission = true
        userLocation?.let {
            scope.launch {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16f)
                )
            }
        }
    }

    // --- İLK AÇILIŞ KONUM ALMA ---
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission && userLocation == null) {
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.lastLocation.addOnSuccessListener { loc ->
                userLocation = loc
                if (loc != null && cameraPositionState.position.target.latitude == 0.0) {
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 15f)
                        )
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
                isMyLocationEnabled = hasLocationPermission,
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
                            "SİSTEM DURUMU",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                        Text(
                            "${targets.count { it.isActive }} / ${targets.size} AKTİF",
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

        // 5. SAĞ ALT BUTONLAR
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp)
                .navigationBarsPadding()
                .padding(bottom = 120.dp),
            horizontalAlignment = Alignment.End
        ) {
            // A) BAŞLAT / DURDUR
            FloatingActionButton(
                onClick = onToggleClick, // DİREKT AKSİYON VERİYORUZ
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

            // B) EKLEME
            SmallFloatingActionButton(
                onClick = onAddClick, // DİREKT AKSİYON
                containerColor = DarkSurface,
                contentColor = Color.White
            ) {
                Icon(Icons.Rounded.Add, null)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // C) KONUMUM
            SmallFloatingActionButton(
                onClick = onMyLocationClick, // DİREKT AKSİYON
                containerColor = DarkSurface,
                contentColor = Color.White
            ) {
                Icon(
                    if (hasLocationPermission) Icons.Rounded.MyLocation else Icons.Rounded.LocationDisabled,
                    null
                )
            }
        }

        // --- HEDEF YOK DİYALOĞU ---
        if (showNoTargetDialog) {
            AlertDialog(
                onDismissRequest = { showNoTargetDialog = false },
                title = { Text("Hedef Bulunamadı") },
                text = { Text("Sistemi başlatmak için en az bir hedef nokta eklemelisiniz.") },
                confirmButton = {
                    TextButton(onClick = {
                        showNoTargetDialog = false
                        // Konum izni zaten alınmış olacağı için direkt fonksiyonu çağırıyoruz
                        onNavigateToAdd(cameraPositionState.position.target)
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
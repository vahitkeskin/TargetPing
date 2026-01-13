package com.vahitkeskin.targetping.ui.features.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Resources
import android.location.Location
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.vahitkeskin.targetping.ui.theme.AlertRed
import com.vahitkeskin.targetping.ui.theme.PrimaryColor
import com.vahitkeskin.targetping.ui.theme.SurfaceColor
import com.vahitkeskin.targetping.utils.rememberPermissionAction
import com.vahitkeskin.targetping.utils.uninstallSelf
import kotlinx.coroutines.launch

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
                val circleColor = if (target.isActive) PrimaryColor else Color.Gray
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
                            color = PrimaryColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.SatelliteAlt,
                        contentDescription = null,
                        tint = if (userLocation != null) PrimaryColor else Color.Gray
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
                containerColor = if (isTracking) AlertRed else PrimaryColor,
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
                containerColor = SurfaceColor,
                contentColor = Color.White
            ) {
                Icon(Icons.Rounded.Add, null)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // C) KONUMUM
            SmallFloatingActionButton(
                onClick = onMyLocationClick, // DİREKT AKSİYON
                containerColor = SurfaceColor,
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
                    }) { Text("EKLE", color = PrimaryColor) }
                },
                dismissButton = {
                    TextButton(onClick = { showNoTargetDialog = false }) {
                        Text("İPTAL", color = Color.Gray)
                    }
                },
                containerColor = SurfaceColor,
                titleContentColor = PrimaryColor,
                textContentColor = Color.White
            )
        }
    }
}
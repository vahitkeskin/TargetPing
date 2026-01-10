package com.vahitkeskin.targetping.ui.features.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.res.Resources
import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    val navigationTarget by viewModel.navigationTarget.collectAsState() // State'i burada aldık

    val cameraPositionState = rememberCameraPositionState()
    val scope = rememberCoroutineScope()

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )
    var userLocation by remember { mutableStateOf<Location?>(null) }

    // --- HATALI YER BURASIYDI ---
    // CompassOverlay burada DEĞİL, aşağıda Box'ın içinde olmalı.

    // Konum İzin Kontrolü
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.lastLocation.addOnSuccessListener { loc ->
                userLocation = loc
                if (loc != null) {
                    scope.launch {
                        // Sadece ilk açılışta (0.0 ise) konuma git
                        if (cameraPositionState.position.target.latitude == 0.0) {
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 15f))
                        }
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // --- 1. HARİTA (En altta) ---
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = permissionsState.allPermissionsGranted,
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
                        viewModel.startNavigation(target) // ViewModel'deki fonksiyonu çağır
                        false // Varsayılan davranışı koru (Kamerayı ortala)
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

        // --- 2. RADAR ANİMASYONU ---
        if (isTracking) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                RadarPulseAnimation(modifier = Modifier.size(300.dp), isPlaying = true, color = AlertRed)
            }
        }

        // --- 3. PUSULA (COMPASS) NAVİGASYON KATMANI ---
        // (DÜZELTME: Bunu Box'ın içine aldık ki haritanın üzerinde görünsün)
        if (navigationTarget != null) {
            CompassOverlay(
                target = navigationTarget!!,
                userLocation = userLocation,
                onStopNavigation = { viewModel.stopNavigation() }
            )
        }

        // --- 4. ÜST BİLGİ KARTI ---
        GlassCard(modifier = Modifier.statusBarsPadding().padding(16.dp).fillMaxWidth()) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("SYSTEM STATUS", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("${targets.count { it.isActive }} / ${targets.size} ONLINE", style = MaterialTheme.typography.titleMedium, color = CyberTeal, fontWeight = FontWeight.Bold)
                }
                Icon(imageVector = Icons.Rounded.SatelliteAlt, contentDescription = null, tint = if (userLocation != null) CyberTeal else Color.Gray)
            }
        }

        // --- 5. SAĞ ALT BUTONLAR (Bottom Nav Üstüne Hizalandı) ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp)
                // KRİTİK AYAR: 130.dp alttan boşluk bırakıyoruz.
                .padding(bottom = 130.dp),
            horizontalAlignment = Alignment.End
        ) {
            // A) BAŞLAT / DURDUR BUTONU
            FloatingActionButton(
                onClick = { viewModel.toggleTracking(!isTracking) },
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
                onClick = { onNavigateToAdd(cameraPositionState.position.target) },
                containerColor = DarkSurface,
                contentColor = Color.White
            ) {
                Icon(Icons.Rounded.Add, null)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // C) KONUMUM BUTONU
            SmallFloatingActionButton(
                onClick = {
                    if (permissionsState.allPermissionsGranted) {
                        userLocation?.let { scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16f)) } }
                    } else { permissionsState.launchMultiplePermissionRequest() }
                },
                containerColor = DarkSurface,
                contentColor = Color.White
            ) {
                Icon(if (permissionsState.allPermissionsGranted) Icons.Rounded.MyLocation else Icons.Rounded.LocationDisabled, null)
            }
        }
    }
}
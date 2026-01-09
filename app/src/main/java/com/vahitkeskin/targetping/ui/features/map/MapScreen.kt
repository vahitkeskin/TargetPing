package com.vahitkeskin.targetping.ui.features.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import com.vahitkeskin.targetping.R
import com.vahitkeskin.targetping.domain.model.TargetLocation
import com.vahitkeskin.targetping.ui.home.GlassCard
import com.vahitkeskin.targetping.ui.home.HomeViewModel
import com.vahitkeskin.targetping.ui.home.components.RadarPulseAnimation
import kotlinx.coroutines.launch

// Renkler
private val CyberTeal = Color(0xFF00E5FF)
private val AlertRed = Color(0xFFFF2A68)
private val DarkSurface = Color(0xFF121212)
private val CardBackground = Color(0xFF1E1E1E)

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    viewModel: HomeViewModel,
    onNavigateToList: () -> Unit,
    onNavigateToAdd: (LatLng) -> Unit
) {
    val context = LocalContext.current
    val targets by viewModel.targets.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()

    val cameraPositionState = rememberCameraPositionState()
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState()

    // --- İZİN YÖNETİMİ ---
    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // İzin var mı?
    val hasPermission = permissionsState.allPermissionsGranted

    // Ayarlar Dialogunu Göster/Gizle
    var showSettingsDialog by remember { mutableStateOf(false) }

    // DÜZELTME: Kullanıcının izin isteme butonuna ilk kez basıp basmadığını takip ediyoruz.
    // rememberSaveable kullanıyoruz ki ekran dönünce sıfırlanmasın.
    var isFirstPermissionClick by rememberSaveable { mutableStateOf(true) }

    // Kullanıcının anlık konumu
    var userLocation by remember { mutableStateOf<Location?>(null) }

    // Konum Alma Mantığı
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.lastLocation.addOnSuccessListener { loc ->
                userLocation = loc
                if (loc != null) {
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 15f)
                        )
                    }
                }
            }
        }
    }

    // --- AYARLARA YÖNLENDİRME DİYALOĞU ---
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Permission Required", color = Color.White) },
            text = {
                Text(
                    "Location permission is permanently denied. The app cannot find your location automatically. Please enable it in Settings.",
                    color = Color.Gray
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSettingsDialog = false
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                ) { Text("Go to Settings", color = CyberTeal) }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Cancel", color = AlertRed)
                }
            },
            containerColor = CardBackground,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContainerColor = DarkSurface,
        sheetContentColor = Color.White,
        sheetPeekHeight = 220.dp,
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetDragHandle = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 12.dp)) {
                Box(modifier = Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(10.dp)).background(Color.Gray.copy(0.4f)))
            }
        },
        sheetContent = {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("SURVEILLANCE LIST", style = MaterialTheme.typography.labelSmall, color = CyberTeal, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Text("${targets.count { it.isActive }} ACTIVE / ${targets.size} TOTAL", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }

                if (targets.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("No targets defined. Add one +", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(targets, key = { it.id }) { target ->
                            CompactTargetItem(
                                target = target,
                                userLocation = userLocation,
                                onToggle = { viewModel.toggleTargetActive(target.id, !target.isActive) },
                                onClick = {
                                    scope.launch {
                                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(target.latitude, target.longitude), 16f))
                                        scaffoldState.bottomSheetState.partialExpand()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = hasPermission,
                    mapStyleOptions = MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
                ),
                uiSettings = MapUiSettings(zoomControlsEnabled = false, compassEnabled = false, myLocationButtonEnabled = false),
                onMapLongClick = { latLng -> onNavigateToAdd(latLng) }
            ) {
                targets.forEach { target ->
                    val color = if (target.isActive) CyberTeal else Color.Gray
                    Marker(
                        state = MarkerState(position = LatLng(target.latitude, target.longitude)),
                        title = target.name,
                        icon = BitmapDescriptorFactory.defaultMarker(if (target.isActive) 180f else 0f)
                    )
                    Circle(
                        center = LatLng(target.latitude, target.longitude),
                        radius = target.radiusMeters.toDouble(),
                        strokeColor = color,
                        strokeWidth = 3f,
                        fillColor = color.copy(alpha = 0.15f)
                    )
                }
            }

            if (isTracking) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    RadarPulseAnimation(modifier = Modifier.size(300.dp), isPlaying = true, color = AlertRed)
                }
            }

            GlassCard(modifier = Modifier.statusBarsPadding().padding(16.dp).fillMaxWidth()) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val alpha = if (isTracking) 1f else 0.3f
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(AlertRed.copy(alpha = alpha)))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isTracking) "TRACKING ONLINE" else "SYSTEM STANDBY", color = if (isTracking) Color.White else Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                    }
                    Icon(imageVector = Icons.Rounded.SatelliteAlt, contentDescription = null, tint = if (userLocation != null) CyberTeal else Color.Gray, modifier = Modifier.size(16.dp))
                }
            }

            Column(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp, bottom = 240.dp)) {
                FloatingActionButton(
                    onClick = { viewModel.toggleTracking(!isTracking) },
                    containerColor = if (isTracking) AlertRed else CyberTeal,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(imageVector = if (isTracking) Icons.Rounded.Stop else Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.Black)
                }

                SmallFloatingActionButton(
                    onClick = { onNavigateToAdd(cameraPositionState.position.target) },
                    containerColor = DarkSurface,
                    contentColor = Color.White
                ) { Icon(Icons.Rounded.Add, null) }

                Spacer(modifier = Modifier.height(12.dp))

                // --- AKILLI "BENİ BUL" BUTONU ---
                SmallFloatingActionButton(
                    onClick = {
                        if (permissionsState.allPermissionsGranted) {
                            // DURUM 1: İzin zaten var, konuma git.
                            userLocation?.let {
                                scope.launch {
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16f))
                                }
                            }
                        } else {
                            // DURUM 2: İzin yok.
                            if (permissionsState.shouldShowRationale) {
                                // Kullanıcı "Reddet" dedi ama "Bir daha sorma" demedi.
                                // Sistem diyaloğunu tekrar aç.
                                permissionsState.launchMultiplePermissionRequest()
                            } else {
                                // Burası ya "İlk Kurulum" ya da "Kalıcı Reddedildi".
                                if (isFirstPermissionClick) {
                                    // İlk kez basıyor: Sistem diyaloğunu açmayı dene.
                                    permissionsState.launchMultiplePermissionRequest()
                                    isFirstPermissionClick = false
                                } else {
                                    // İkinci kez basıyor ve rationale hala false.
                                    // Demek ki sistem diyaloğu açılmıyor (Kalıcı Red).
                                    // Ayarlar diyaloğunu göster.
                                    showSettingsDialog = true
                                }
                            }
                        }
                    },
                    containerColor = if (hasPermission) DarkSurface else AlertRed,
                    contentColor = Color.White
                ) {
                    Icon(if (hasPermission) Icons.Rounded.MyLocation else Icons.Rounded.LocationDisabled, null)
                }
            }
        }
    }
}

// CompactTargetItem aynı şekilde kalacak
@Composable
fun CompactTargetItem(
    target: TargetLocation,
    userLocation: Location?,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    val distanceStr = remember(userLocation, target) {
        if (userLocation == null) "Wait GPS..."
        else {
            val results = FloatArray(1)
            Location.distanceBetween(userLocation.latitude, userLocation.longitude, target.latitude, target.longitude, results)
            val dist = results[0].toInt()
            if (dist > 1000) "${String.format("%.1f", dist / 1000f)} km" else "$dist m"
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(CardBackground).clickable { onClick() }.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(if (target.isActive) CyberTeal.copy(0.1f) else Color.Gray.copy(0.1f)), contentAlignment = Alignment.Center) {
            Icon(imageVector = Icons.Rounded.Radar, contentDescription = null, tint = if (target.isActive) CyberTeal else Color.Gray)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = target.name, style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Straighten, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = distanceStr, style = MaterialTheme.typography.bodySmall, color = CyberTeal)
                Text(text = " • Radius: ${target.radiusMeters}m", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
        Switch(
            checked = target.isActive,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = CyberTeal, uncheckedThumbColor = Color.Gray, uncheckedTrackColor = Color.Black)
        )
    }
}
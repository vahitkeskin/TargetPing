package com.vahitkeskin.targetping.ui.features.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.location.Location
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
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

// --- RENK PALETİ ---
private val CyberTeal = Color(0xFF00E5FF)
private val AlertRed = Color(0xFFFF2A68)
private val DarkSurface = Color(0xFF121212)
private val CardBackground = Color(0xFF1E1E1E)
private val NeonGreen = Color(0xFF00FF9D)

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
    val hasPermission = permissionsState.allPermissionsGranted
    var showSettingsDialog by remember { mutableStateOf(false) }
    var isFirstPermissionClick by rememberSaveable { mutableStateOf(true) }
    var userLocation by remember { mutableStateOf<Location?>(null) }

    // Konum Alma
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

    // --- ÖZEL CYBER DIALOG ---
    if (showSettingsDialog) {
        CyberAlertDialog(
            title = "PERMISSION DENIED",
            text = "Tactical access to GPS satellites is strictly required for operation. Please grant permissions manually from system settings.",
            onConfirm = {
                showSettingsDialog = false
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContainerColor = DarkSurface.copy(alpha = 0.95f),
        sheetContentColor = Color.White,
        sheetPeekHeight = 220.dp, // Alt panelin görünen kısmı
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        sheetDragHandle = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 12.dp)) {
                // Neon Tutamak
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberTeal.copy(0.5f))
                )
            }
        },
        sheetContent = {
            // --- LİSTE İÇERİĞİ ---
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ACTIVE TARGETS", style = MaterialTheme.typography.labelSmall, color = CyberTeal, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Text("${targets.count { it.isActive }} / ${targets.size} ONLINE", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                }

                if (targets.isEmpty()) {
                    // Boş Liste Durumu
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp))
                            .clickable {
                                // Boş alana tıklayınca kullanıcıyı bulunduğu yere hedef eklemeye yönlendir
                                userLocation?.let { onNavigateToAdd(LatLng(it.latitude, it.longitude)) }
                                    ?: onNavigateToAdd(cameraPositionState.position.target)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.AddLocationAlt, null, tint = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("NO TARGETS FOUND", color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("TAP TO ADD NEW SECTOR", color = CyberTeal, fontSize = 10.sp)
                        }
                    }
                } else {
                    // Dolu Liste
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 80.dp), // Listenin en altı rahat görünsün
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(targets, key = { it.id }) { target ->
                            CompactTargetItem(
                                target = target,
                                userLocation = userLocation,
                                onToggle = { viewModel.toggleTargetActive(target.id, !target.isActive) },
                                onClick = {
                                    scope.launch {
                                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(target.latitude, target.longitude), 17f))
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

            // --- GOOGLE MAP ---
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                // KRİTİK: Google Logosunu BottomSheet'in üstüne çıkarıyoruz.
                contentPadding = PaddingValues(bottom = 220.dp),
                properties = MapProperties(
                    isMyLocationEnabled = hasPermission,
                    // Eğer raw dosyası yoksa varsayılan stili kullan, çökmesin
                    mapStyleOptions = try {
                        MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
                    } catch (e: Resources.NotFoundException) { null }
                ),
                uiSettings = MapUiSettings(zoomControlsEnabled = false, compassEnabled = false, myLocationButtonEnabled = false),
                onMapLongClick = { latLng -> onNavigateToAdd(latLng) }
            ) {
                targets.forEach { target ->
                    val color = if (target.isActive) CyberTeal else Color.Gray

                    // İşaretçi (Marker)
                    Marker(
                        state = MarkerState(position = LatLng(target.latitude, target.longitude)),
                        title = target.name,
                        icon = BitmapDescriptorFactory.defaultMarker(if (target.isActive) 180f else 0f) // Aktifse Cyan, değilse Kırmızı
                    )

                    // Alan (Circle)
                    Circle(
                        center = LatLng(target.latitude, target.longitude),
                        radius = target.radiusMeters.toDouble(),
                        strokeColor = color.copy(alpha = 0.8f),
                        strokeWidth = 3f,
                        fillColor = color.copy(alpha = 0.15f)
                    )
                }
            }

            // --- RADAR ANİMASYONU ---
            if (isTracking) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    RadarPulseAnimation(
                        modifier = Modifier
                            .size(300.dp)
                            .padding(bottom = 220.dp), // Radarı da yukarı kaydırdık
                        isPlaying = true,
                        color = AlertRed
                    )
                }
            }

            // --- ÜST DURUM ÇUBUĞU ---
            GlassCard(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .fillMaxWidth(),
                cornerRadius = 16.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Yanıp Sönen Durum Işığı
                        val infiniteTransition = rememberInfiniteTransition(label = "status")
                        val alpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f, targetValue = 1f,
                            animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "alpha"
                        )

                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isTracking) AlertRed.copy(alpha = alpha) else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isTracking) "SYSTEM ARMED" else "STANDBY MODE",
                            color = if (isTracking) Color.White else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.SatelliteAlt,
                        contentDescription = null,
                        tint = if (userLocation != null) CyberTeal else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // --- SAĞ BUTON GRUBU ---
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
                    // BottomSheet'in hemen üzerinde dursun
                    .padding(bottom = 240.dp)
            ) {
                // 1. Play/Stop Butonu
                FloatingActionButton(
                    onClick = { viewModel.toggleTracking(!isTracking) },
                    containerColor = if (isTracking) AlertRed else CyberTeal,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = if (isTracking) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black
                    )
                }

                // 2. Hızlı Ekleme Butonu
                SmallFloatingActionButton(
                    onClick = { onNavigateToAdd(cameraPositionState.position.target) },
                    containerColor = DarkSurface.copy(0.9f),
                    contentColor = Color.White
                ) { Icon(Icons.Rounded.Add, null) }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Konum Butonu (Akıllı Mantık)
                SmallFloatingActionButton(
                    onClick = {
                        if (permissionsState.allPermissionsGranted) {
                            userLocation?.let {
                                scope.launch {
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 16f))
                                }
                            }
                        } else {
                            if (permissionsState.shouldShowRationale) {
                                permissionsState.launchMultiplePermissionRequest()
                            } else {
                                if (isFirstPermissionClick) {
                                    permissionsState.launchMultiplePermissionRequest()
                                    isFirstPermissionClick = false
                                } else {
                                    showSettingsDialog = true
                                }
                            }
                        }
                    },
                    containerColor = if (hasPermission) DarkSurface.copy(0.9f) else AlertRed,
                    contentColor = Color.White
                ) {
                    Icon(if (hasPermission) Icons.Rounded.MyLocation else Icons.Rounded.LocationDisabled, null)
                }
            }
        }
    }
}

// --- KOMPONENTLER ---

@Composable
fun CompactTargetItem(
    target: TargetLocation,
    userLocation: Location?,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    val distanceStr = remember(userLocation, target) {
        if (userLocation == null) "CALC..."
        else {
            val results = FloatArray(1)
            Location.distanceBetween(userLocation.latitude, userLocation.longitude, target.latitude, target.longitude, results)
            val dist = results[0].toInt()
            if (dist > 1000) "${String.format("%.1f", dist / 1000f)} KM" else "$dist M"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // İkon Kutusu
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (target.isActive) CyberTeal.copy(0.1f) else Color.Gray.copy(0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Radar,
                contentDescription = null,
                tint = if (target.isActive) CyberTeal else Color.Gray
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Metinler
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = target.name,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Straighten, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = distanceStr, style = MaterialTheme.typography.bodySmall, color = CyberTeal, fontWeight = FontWeight.Bold)
                Text(text = " • RAD: ${target.radiusMeters}M", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }

        // Switch
        Switch(
            checked = target.isActive,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = CyberTeal,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.Black,
                uncheckedBorderColor = Color.Gray
            ),
            modifier = Modifier.scale(0.8f) // Biraz küçülttük
        )
    }
}

@Composable
fun CyberAlertDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = CyberTeal, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
        text = { Text(text, color = Color.Gray) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = CyberTeal, contentColor = Color.Black)
            ) {
                Text("SETTINGS", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.White)
            }
        },
        containerColor = Color(0xFF1E1E1E), // Koyu Gri Arkaplan
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp))
    )
}
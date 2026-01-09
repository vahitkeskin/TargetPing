package com.vahitkeskin.targetping.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.vahitkeskin.targetping.ui.home.components.AlertRed
import com.vahitkeskin.targetping.ui.home.components.CyberTeal
import com.vahitkeskin.targetping.ui.home.components.DarkSurface
import com.vahitkeskin.targetping.ui.home.components.RadarPulseAnimation
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToAdd: () -> Unit // Bu parametre opsiyonel kalabilir, sheet kullanıyoruz
) {
    val targets by viewModel.targets.collectAsState()
    val isTracking by viewModel.isTracking.collectAsState()

    // Kamera ve UI State'leri
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(41.0082, 28.9784), 14f)
    }
    var showBottomSheet by remember { mutableStateOf(false) }
    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    val scope = rememberCoroutineScope()

    // Snack bar host
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // Ana Aksiyon Butonu (Play/Stop)
            FloatingActionButton(
                onClick = { viewModel.toggleTracking(!isTracking) },
                containerColor = if (isTracking) AlertRed else CyberTeal,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(72.dp).border(2.dp, Color.White.copy(0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isTracking) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    contentDescription = "Toggle Tracking",
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {

            // --- KATMAN 1: HARİTA ---
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = true,
                    mapType = mapType
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false,
                    compassEnabled = false
                )
            ) {
                // Hedefleri Çiz
                targets.forEach { target ->
                    Marker(
                        state = MarkerState(position = LatLng(target.latitude, target.longitude)),
                        title = target.name,
                        snippet = "Radius: ${target.radiusMeters}m",
                        icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                            if (target.isActive) 180f else 0f // Cyan or Red hue
                        )
                    )
                    Circle(
                        center = LatLng(target.latitude, target.longitude),
                        radius = target.radiusMeters.toDouble(),
                        strokeColor = if (target.isActive) CyberTeal else Color.Gray,
                        strokeWidth = 3f,
                        fillColor = if (target.isActive) CyberTeal.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.1f)
                    )
                }
            }

            // --- KATMAN 2: RADAR EFEKTİ (Tracking Aktifse) ---
            if (isTracking) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    RadarPulseAnimation(
                        modifier = Modifier.size(300.dp),
                        isPlaying = true,
                        color = if (targets.isNotEmpty()) CyberTeal else AlertRed
                    )
                }
            }

            // --- KATMAN 3: HUD (HEADS-UP DISPLAY) - ÜST BİLGİ KARTI ---
            GlassCard(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 50.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isTracking) "SYSTEM ARMED" else "SYSTEM IDLE",
                            color = if (isTracking) CyberTeal else Color.Gray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${targets.size} Active Targets",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp
                        )
                    }
                    // Canlı yanıp sönen durum ışığı
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isTracking) AlertRed else Color.Gray)
                    )
                }
            }

            // --- KATMAN 4: SAĞ KONTROL PANELİ ---
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Harita Tipi Değiştirme
                ControlButton(icon = Icons.Rounded.Layers) {
                    mapType = if (mapType == MapType.NORMAL) MapType.HYBRID else MapType.NORMAL
                }

                // Konuma Git
                ControlButton(icon = Icons.Rounded.GpsFixed) {
                    // Kullanıcı konumuna zoom yap (gerçek uygulamada location servisten alınmalı)
                    // Şimdilik demo lokasyonuna animasyonla gidiyor
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(LatLng(41.0082, 28.9784), 16f)
                        )
                    }
                }

                // Yeni Ekle
                ControlButton(
                    icon = Icons.Rounded.Add,
                    tint = Color.Black,
                    bgColor = CyberTeal
                ) {
                    showBottomSheet = true
                }
            }
        }

        // --- BOTTOM SHEET (EKLEME PANELİ) ---
        if (showBottomSheet) {
            AddTargetBottomSheet(
                cameraPosition = cameraPositionState.position.target,
                onDismiss = { showBottomSheet = false },
                onAdd = { name, radius ->
                    viewModel.addTarget(
                        name,
                        cameraPositionState.position.target.latitude,
                        cameraPositionState.position.target.longitude,
                        radius
                    )
                    showBottomSheet = false
                    scope.launch { snackbarHostState.showSnackbar("Target Locked: $name") }
                }
            )
        }
    }
}

// --- YARDIMCI COMPONENTLER ---

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = DarkSurface, // Arkaplan rengi
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Brush.linearGradient(
            colors = listOf(Color.White.copy(0.2f), Color.Transparent)
        )),
        shadowElevation = 8.dp
    ) {
        content()
    }
}

@Composable
fun ControlButton(
    icon: ImageVector,
    tint: Color = Color.White,
    bgColor: Color = DarkSurface,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bgColor,
        contentColor = tint,
        modifier = Modifier.size(50.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(0.1f)),
        shadowElevation = 4.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTargetBottomSheet(
    cameraPosition: LatLng,
    onDismiss: () -> Unit,
    onAdd: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var radius by remember { mutableStateOf("100") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E), // Koyu tema
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .padding(bottom = 32.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "NEW SURVEILLANCE TARGET",
                style = MaterialTheme.typography.labelLarge.copy(
                    letterSpacing = 2.sp,
                    color = CyberTeal,
                    fontWeight = FontWeight.Bold
                )
            )

            // Bilgi Kartı
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${cameraPosition.latitude.toString().take(7)}, ${cameraPosition.longitude.toString().take(7)}",
                    color = Color.White.copy(0.7f),
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Designation (Name)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberTeal,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = CyberTeal,
                    unfocusedLabelColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = radius,
                onValueChange = { if (it.all { char -> char.isDigit() }) radius = it },
                label = { Text("Radius (Meters)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberTeal,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = CyberTeal,
                    unfocusedLabelColor = Color.Gray
                ),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = { Text("m", color = Color.Gray, modifier = Modifier.padding(end = 16.dp)) }
            )

            Button(
                onClick = { onAdd(name.ifBlank { "Unknown Target" }, radius.toIntOrNull() ?: 100) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyberTeal),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("INITIALIZE TARGET", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
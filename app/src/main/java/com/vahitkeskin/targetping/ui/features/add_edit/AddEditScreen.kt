package com.vahitkeskin.targetping.ui.features.add_edit

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.vahitkeskin.targetping.ui.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private val CyberTeal = Color(0xFF00E5FF)
private val AlertRed = Color(0xFFFF2A68)
private val DarkSurface = Color(0xFF1E1E1E).copy(alpha = 0.95f)

@SuppressLint("MissingPermission")
@Composable
fun AddEditScreen(
    viewModel: HomeViewModel,
    targetId: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val targets by viewModel.targets.collectAsState()
    val scope = rememberCoroutineScope()

    // State'ler
    val existingTarget = remember(targetId, targets) { targets.find { it.id == targetId } }
    val isEditing = existingTarget != null
    var name by remember { mutableStateOf(existingTarget?.name ?: "") }
    var radiusStr by remember { mutableStateOf(existingTarget?.radiusMeters?.toString() ?: "100") }
    var addressText by remember { mutableStateOf("Sektör taranıyor...") }
    var isAddressLoading by remember { mutableStateOf(false) }

    // Bottom panel yüksekliği (padding için)
    var bottomPanelHeight by remember { mutableStateOf(0.dp) }

    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    val initialPos = existingTarget?.let { LatLng(it.latitude, it.longitude) } ?: LatLng(0.0, 0.0)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPos, 16f)
    }

    // --- AUTO FOCUS ---
    LaunchedEffect(Unit) {
        if (!isEditing && hasLocationPermission) {
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 16f),
                            1000
                        )
                    }
                } else {
                    client.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                        .addOnSuccessListener { currLoc ->
                            if (currLoc != null) {
                                scope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(LatLng(currLoc.latitude, currLoc.longitude), 16f),
                                        1000
                                    )
                                }
                            }
                        }
                }
            }
        }
    }

    // --- ADRES ÇÖZÜMLEME ---
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val target = cameraPositionState.position.target
            if (target.latitude != 0.0 && target.longitude != 0.0) {
                isAddressLoading = true
                scope.launch(Dispatchers.IO) {
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            geocoder.getFromLocation(target.latitude, target.longitude, 1) { addresses ->
                                val address = addresses.firstOrNull()
                                val addrString = if (address != null) {
                                    address.thoroughfare ?: address.subLocality ?: address.locality ?: "Bilinmeyen Konum"
                                } else "Konum Bulunamadı"
                                scope.launch {
                                    addressText = addrString
                                    isAddressLoading = false
                                }
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(target.latitude, target.longitude, 1)
                            val address = addresses?.firstOrNull()
                            val addrString = address?.thoroughfare ?: address?.subLocality ?: address?.locality ?: "Konum Bulunamadı"
                            withContext(Dispatchers.Main) {
                                addressText = addrString
                                isAddressLoading = false
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            addressText = "Bağlantı Hatası"
                            isAddressLoading = false
                        }
                    }
                }
            }
        } else {
            if (!isAddressLoading) {
                addressText = "Konumlanıyor..."
                isAddressLoading = true
            }
        }
    }

    val centerTarget = cameraPositionState.position.target
    val radiusInt = radiusStr.toIntOrNull() ?: 100

    // Scaffold insets'i sıfırlıyoruz ki tam ekran harita olsun
    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. HARİTA (Alt panel kadar padding bırakıyoruz ki Copyright yazısı kapanmasın)
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                contentPadding = PaddingValues(bottom = bottomPanelHeight),
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission, mapType = MapType.HYBRID),
                uiSettings = MapUiSettings(zoomControlsEnabled = false, compassEnabled = false, myLocationButtonEnabled = false)
            ) {
                Circle(
                    center = centerTarget,
                    radius = radiusInt.toDouble(),
                    strokeColor = CyberTeal.copy(alpha = 0.9f),
                    strokeWidth = 3f,
                    fillColor = CyberTeal.copy(alpha = 0.15f)
                )
            }

            // 2. ARAYÜZ KATMANLARI
            TacticalGridOverlay()

            Box(modifier = Modifier.align(Alignment.Center).padding(bottom = bottomPanelHeight)) {
                TacticalCrosshair(color = if (radiusInt > 0) CyberTeal else AlertRed)
            }

            // 3. ÜST BAR
            Column(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp).background(DarkSurface.copy(0.8f), CircleShape).border(1.dp, Color.White.copy(0.2f), CircleShape)
                    ) { Icon(Icons.Rounded.ArrowBack, null, tint = Color.White) }

                    IconButton(
                        onClick = {
                            if (hasLocationPermission) {
                                val client = LocationServices.getFusedLocationProviderClient(context)
                                client.lastLocation.addOnSuccessListener { loc ->
                                    if (loc != null) scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 16f)) }
                                }
                            }
                        },
                        modifier = Modifier.size(48.dp).background(CyberTeal, CircleShape).border(2.dp, Color.Black.copy(0.1f), CircleShape)
                    ) { Icon(Icons.Rounded.GpsFixed, null, tint = Color.Black) }
                }
                Spacer(modifier = Modifier.height(16.dp))
                AddressHudPill(addressText, isAddressLoading)
            }

            // 4. ALT PANEL (KLAVYE DÜZELTMESİ BURADA)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onGloballyPositioned { coordinates ->
                        bottomPanelHeight = with(density) { coordinates.size.height.toDp() }
                    }
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(0.8f), Color.Black)
                        )
                    )
                    // ÖNEMLİ: Navigation Bar ve Klavye (IME) paddinglerini buraya uyguluyoruz.
                    // Bu sayede klavye açıldığında bu kutu yukarı kalkıyor.
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Scroll State: Eğer klavye açılınca alan daralırsa kaydırılabilir olsun
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .verticalScroll(scrollState) // Klavye açılınca kaydırma özelliği
                ) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        cornerRadius = 24.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Radar, null, tint = CyberTeal, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("HEDEF PARAMETRELERİ", style = MaterialTheme.typography.labelSmall, color = CyberTeal, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                            }

                            CyberInput(
                                value = name,
                                onValueChange = { name = it },
                                label = "HEDEF TANIMI / ADI",
                                placeholder = "Örn: Ev, Ofis, Üs Bölgesi"
                            )

                            CyberInput(
                                value = radiusStr,
                                onValueChange = { if (it.all { c -> c.isDigit() }) radiusStr = it },
                                label = "OPERASYON YARIÇAPI (METRE)",
                                keyboardType = KeyboardType.Number,
                                suffix = "M"
                            )

                            ActionPrimaryButton(
                                text = if (isEditing) "HEDEFİ GÜNCELLE" else "KONUMU ONAYLA",
                                onClick = {
                                    if (name.isNotBlank()) {
                                        if (existingTarget != null) viewModel.deleteTarget(existingTarget.id)
                                        viewModel.addTarget(name, centerTarget.latitude, centerTarget.longitude, radiusInt)
                                        onBack()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddressHudPill(address: String, isLoading: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassCard(
            modifier = Modifier.wrapContentSize(),
            cornerRadius = 50.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "hud_pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                    label = "alpha"
                )

                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = if (isLoading) AlertRed.copy(alpha) else CyberTeal,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = address,
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    modifier = Modifier.basicMarquee() // Uzun adresler kaysın
                )
            }
        }
    }
}

@Composable
fun TacticalCrosshair(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "crosshair")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "rotation"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            tween(1000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ), label = "scale"
    )

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(80.dp)) {
            rotate(rotation) {
                drawCircle(
                    color = color.copy(alpha = 0.3f),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
                    )
                )
                val s = size.minDimension
                val c = s * 0.1f
                val st = 3.dp.toPx()
                // Köşe Çizgileri
                drawLine(color, Offset(s / 2, 0f), Offset(s / 2, c), st)
                drawLine(color, Offset(s / 2, s), Offset(s / 2, s - c), st)
                drawLine(color, Offset(0f, s / 2), Offset(c, s / 2), st)
                drawLine(color, Offset(s, s / 2), Offset(s - c, s / 2), st)
            }
        }
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = AlertRed, radius = size.minDimension / 2 * scale)
            drawCircle(color = Color.White, radius = size.minDimension / 4, alpha = 0.8f)
        }
    }
}

@Composable
fun TacticalGridOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val step = 100.dp.toPx()
        val color = Color.White.copy(alpha = 0.05f)
        for (x in 0..size.width.toInt() step step.toInt()) drawLine(
            color,
            Offset(x.toFloat(), 0f),
            Offset(x.toFloat(), size.height),
            1f
        )
        for (y in 0..size.height.toInt() step step.toInt()) drawLine(
            color,
            Offset(0f, y.toFloat()),
            Offset(size.width, y.toFloat()),
            1f
        )
    }
}

@Composable
fun CyberInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    suffix: String? = null
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text(placeholder, color = Color.Gray.copy(0.5f)) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            trailingIcon = suffix?.let {
                {
                    Text(
                        it,
                        color = CyberTeal,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Black.copy(0.4f),
                unfocusedContainerColor = Color.Black.copy(0.2f),
                focusedBorderColor = CyberTeal,
                unfocusedBorderColor = Color.White.copy(0.1f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = CyberTeal
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
fun ActionPrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = CyberTeal,
            contentColor = Color.Black
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Save, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            )
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = Color(0xFF1E1E1E).copy(alpha = 0.85f),
        shape = RoundedCornerShape(cornerRadius),
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(listOf(Color.White.copy(0.15f), Color.White.copy(0.02f)))
        ),
        shadowElevation = 8.dp
    ) {
        content()
    }
}
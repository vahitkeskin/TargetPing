package com.vahitkeskin.targetping.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.vahitkeskin.targetping.ui.home.GlassCard
import com.vahitkeskin.targetping.ui.home.HomeViewModel
import com.vahitkeskin.targetping.utils.getNavigationBarHeightDp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// Renk Paleti
private val CyberTeal = Color(0xFF00E5FF)
private val AlertRed = Color(0xFFFF2A68)
private val DarkSurface = Color(0xFF1E1E1E).copy(alpha = 0.95f)

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("MissingPermission")
@Composable
fun AddEditScreen(
    viewModel: HomeViewModel,
    targetId: String?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val targets by viewModel.targets.collectAsState()
    val scope = rememberCoroutineScope()

    // Düzenleme modu mu?
    val existingTarget = remember(targetId, targets) { targets.find { it.id == targetId } }
    val isEditing = existingTarget != null

    // Form Değişkenleri
    var name by remember { mutableStateOf(existingTarget?.name ?: "") }
    var radiusStr by remember { mutableStateOf(existingTarget?.radiusMeters?.toString() ?: "100") }

    // Adres State'i
    var addressText by remember { mutableStateOf("Locating sector...") }
    var isAddressLoading by remember { mutableStateOf(false) }

    // İzin Kontrolü
    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    val defaultPos = LatLng(41.0082, 28.9784) // İstanbul
    val initialPos = existingTarget?.let { LatLng(it.latitude, it.longitude) } ?: defaultPos

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPos, 16f)
    }

    // --- ADRES ÇÖZÜMLEME (REVERSE GEOCODING) ---
    // Harita hareket etmeyi bıraktığında (isMoving = false), 1 saniye bekle ve adresi bul.
    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving) {
            addressText = "Acquiring coordinates..."
            isAddressLoading = true
        } else {
            // Harita durdu, 1 saniye bekle (Debounce)
            delay(1000)

            val target = cameraPositionState.position.target
            try {
                // Arkaplan thread'inde çalıştır
                val addresses = withContext(Dispatchers.IO) {
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        // API 33 öncesi ve sonrası uyumluluğu için basit yaklaşım (Blocking call in IO dispatcher)
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(target.latitude, target.longitude, 1)
                    } catch (e: Exception) {
                        null
                    }
                }

                if (!addresses.isNullOrEmpty()) {
                    addressText = addresses[0].getAddressLine(0) ?: "Unknown Sector"
                } else {
                    // Adres bulunamazsa koordinatları göster
                    addressText = "Lat: ${String.format("%.4f", target.latitude)} Lng: ${String.format("%.4f", target.longitude)}"
                }
            } catch (e: Exception) {
                addressText = "Signal Lost"
            } finally {
                isAddressLoading = false
            }
        }
    }

    // --- AUTO-FOCUS ---
    LaunchedEffect(Unit) {
        if (!isEditing && hasLocationPermission) {
            val client = LocationServices.getFusedLocationProviderClient(context)
            client.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    scope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(location.latitude, location.longitude),
                                16f
                            )
                        )
                    }
                }
            }
        }
    }

    val centerTarget = cameraPositionState.position.target
    val radiusInt = radiusStr.toIntOrNull() ?: 100

    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // 1. HARİTA
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = hasLocationPermission,
                    mapType = MapType.HYBRID,
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    compassEnabled = false,
                    myLocationButtonEnabled = false
                )
            ) {
                Circle(
                    center = centerTarget,
                    radius = radiusInt.toDouble(),
                    strokeColor = CyberTeal.copy(alpha = 0.9f),
                    strokeWidth = 3f,
                    fillColor = CyberTeal.copy(alpha = 0.15f)
                )
            }

            // 2. GRID OVERLAY
            TacticalGridOverlay()

            // 3. CROSSHAIR
            Box(modifier = Modifier.align(Alignment.Center)) {
                TacticalCrosshair(color = if (radiusInt > 0) CyberTeal else AlertRed)
            }

            // 4. ÜST BUTONLAR
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .background(DarkSurface.copy(0.8f), CircleShape)
                        .border(1.dp, Color.White.copy(0.2f), CircleShape)
                ) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                }

                IconButton(
                    onClick = {
                        if (hasLocationPermission) {
                            val client = LocationServices.getFusedLocationProviderClient(context)
                            client.lastLocation.addOnSuccessListener { loc ->
                                if (loc != null) {
                                    scope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 16f)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(CyberTeal, CircleShape)
                        .border(2.dp, Color.Black.copy(0.1f), CircleShape)
                ) {
                    Icon(Icons.Rounded.GpsFixed, null, tint = Color.Black)
                }
            }

            // 5. ALT PANEL (INFO CARD)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .fillMaxWidth()
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = context.getNavigationBarHeightDp()),
                    cornerRadius = 24.dp
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // --- ADRES GÖSTERİM ALANI ---
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(0.3f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Yanıp sönen ikon
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 0.5f, targetValue = 1f,
                                animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
                                label = "alpha"
                            )

                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = if (isAddressLoading) AlertRed.copy(alpha) else CyberTeal,
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // Kayan Yazı (Marquee)
                            Text(
                                text = addressText,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                ),
                                maxLines = 1,
                                modifier = Modifier
                                    .weight(1f)
                                    .basicMarquee( // Kayan yazı efekti
                                        iterations = Int.MAX_VALUE,
                                        velocity = 40.dp
                                    )
                            )
                        }

                        // --- HEADER ---
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Rounded.Radar, null, tint = CyberTeal, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TARGET ACQUISITION",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberTeal,
                                letterSpacing = 2.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // --- INPUTS ---
                        CyberInput(
                            value = name,
                            onValueChange = { name = it },
                            label = "DESIGNATION NAME",
                            placeholder = "e.g. Base Alpha"
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f)) {
                                CyberInput(
                                    value = radiusStr,
                                    onValueChange = { if (it.all { c -> c.isDigit() }) radiusStr = it },
                                    label = "PERIMETER RADIUS",
                                    keyboardType = KeyboardType.Number,
                                    suffix = "M"
                                )
                            }
                        }

                        // --- AKSİYON BUTONU ---
                        ActionPrimaryButton(
                            text = if (isEditing) "UPDATE COORDINATES" else "ESTABLISH LINK",
                            onClick = {
                                if (name.isNotBlank()) {
                                    if (existingTarget != null) viewModel.deleteTarget(existingTarget.id)
                                    viewModel.addTarget(
                                        name,
                                        centerTarget.latitude,
                                        centerTarget.longitude,
                                        radiusInt
                                    )
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

// --- CANVAS NİŞANGAH ---
@Composable
fun TacticalCrosshair(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "crosshair")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "rotation"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "scale"
    )

    Box(contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(80.dp)) {
            rotate(rotation) {
                drawCircle(
                    color = color.copy(alpha = 0.3f),
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f))
                )
                val s = size.minDimension; val c = s * 0.1f; val st = 3.dp.toPx()
                drawLine(color, Offset(s/2, 0f), Offset(s/2, c), st)
                drawLine(color, Offset(s/2, s), Offset(s/2, s-c), st)
                drawLine(color, Offset(0f, s/2), Offset(c, s/2), st)
                drawLine(color, Offset(s, s/2), Offset(s-c, s/2), st)
            }
        }
        Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color = AlertRed, radius = size.minDimension/2 * scale)
            drawCircle(color = Color.White, radius = size.minDimension/4, alpha = 0.8f)
        }
    }
}

// --- GRID OVERLAY ---
@Composable
fun TacticalGridOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val step = 100.dp.toPx()
        val color = Color.White.copy(alpha = 0.05f)
        for (x in 0..size.width.toInt() step step.toInt()) drawLine(color, Offset(x.toFloat(), 0f), Offset(x.toFloat(), size.height), 1f)
        for (y in 0..size.height.toInt() step step.toInt()) drawLine(color, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), 1f)
    }
}

// --- CYBER INPUT ---
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
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        OutlinedTextField(
            value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text(placeholder, color = Color.Gray.copy(0.5f)) },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            trailingIcon = suffix?.let { { Text(it, color = CyberTeal, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 16.dp)) } },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Black.copy(0.4f), unfocusedContainerColor = Color.Black.copy(0.2f),
                focusedBorderColor = CyberTeal, unfocusedBorderColor = Color.White.copy(0.1f),
                focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = CyberTeal
            ),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

// --- ACTION BUTTON ---
@Composable
fun ActionPrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick, modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = CyberTeal, contentColor = Color.Black),
        shape = RoundedCornerShape(12.dp), elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Save, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp))
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp, // Dinamik köşe yuvarlaklığı eklendi
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        // Arkaplan rengini %85 opaklıkta tutuyoruz ki alttaki harita/zemin hafifçe hissedilsin (Gerçek cam hissi)
        color = Color(0xFF1E1E1E).copy(alpha = 0.85f),
        shape = RoundedCornerShape(cornerRadius),
        // Kenarlara "Dikey Gradyan" (Vertical Gradient) ekliyoruz.
        // Bu, ışığın yukarıdan vurduğu hissini verir ve kartı 3 boyutlu gösterir.
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.15f), // Üst kenar hafif parlak
                    Color.White.copy(alpha = 0.02f)  // Alt kenar sönük
                )
            )
        ),
        shadowElevation = 8.dp // Hafif derinlik
    ) {
        content()
    }
}
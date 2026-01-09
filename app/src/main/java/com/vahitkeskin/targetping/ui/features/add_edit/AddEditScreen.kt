package com.vahitkeskin.targetping.ui.features.add_edit

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.vahitkeskin.targetping.ui.home.GlassCard
import com.vahitkeskin.targetping.ui.home.HomeViewModel
import kotlinx.coroutines.launch

// Önceki dosyalarda tanımladığımız renkleri burada tekrar kullanıyoruz
// (Eğer Theme.kt içine taşıdıysan oradan import et)
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
    // State'ler
    val targets by viewModel.targets.collectAsState()
    val scope = rememberCoroutineScope()

    // Düzenleme modu mu? Eğer ID varsa ilgili veriyi bul.
    val existingTarget = remember(targetId, targets) { targets.find { it.id == targetId } }

    // Form Değişkenleri
    var name by remember { mutableStateOf(existingTarget?.name ?: "") }
    var radiusStr by remember { mutableStateOf(existingTarget?.radiusMeters?.toString() ?: "100") }

    // Harita Kamera Pozisyonu (Varsayılan İstanbul veya Mevcut Hedef)
    val initialPos = existingTarget?.let { LatLng(it.latitude, it.longitude) }
        ?: LatLng(41.0082, 28.9784)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPos, 16f)
    }

    // Haritanın tam ortası (Hedeflenen Koordinat)
    val centerTarget = cameraPositionState.position.target
    val radiusInt = radiusStr.toIntOrNull() ?: 100

    // AddEditScreen.kt içine ekle:

    val context = LocalContext.current
    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    Scaffold(
        containerColor = Color.Black,
        floatingActionButton = {
            // Kaydet Butonu (Floating)
            ExtendedFloatingActionButton(
                onClick = {
                    if (name.isNotBlank()) {
                        // Eğer ID varsa güncelleme (delete+insert veya update), yoksa ekleme
                        // Basitlik için burada addTarget çağırıyoruz,
                        // gerçek senaryoda updateTarget fonksiyonu VM'e eklenmeli.
                        if (existingTarget != null) viewModel.deleteTarget(existingTarget.id)

                        viewModel.addTarget(
                            name,
                            centerTarget.latitude,
                            centerTarget.longitude,
                            radiusInt
                        )
                        onBack()
                    }
                },
                containerColor = CyberTeal,
                contentColor = Color.Black,
                icon = { Icon(Icons.Rounded.Save, null) },
                text = { Text("CONFIRM TARGET", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {

            // --- 1. HARİTA KATMANI ---
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = hasLocationPermission, // Burayı güncelle
                    mapType = MapType.HYBRID,
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    compassEnabled = false,
                    myLocationButtonEnabled = false // Kendimiz yapacağız
                )
            ) {
                // Seçim Alanını Gösteren Çember (Kamera hareket ettikçe güncellenir)
                Circle(
                    center = centerTarget,
                    radius = radiusInt.toDouble(),
                    strokeColor = CyberTeal.copy(alpha = 0.8f),
                    strokeWidth = 4f,
                    fillColor = CyberTeal.copy(alpha = 0.2f)
                )
            }

            // --- 2. MERKEZ NİŞANGAH (CROSSHAIR) ---
            // Haritanın tam ortasında sabit durur
            Box(modifier = Modifier.align(Alignment.Center)) {
                // Dış Halka
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(2.dp, Color.White, CircleShape)
                )
                // İç Nokta
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(AlertRed, CircleShape)
                        .align(Alignment.Center)
                )
                // Çizgiler
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(1.dp)
                        .background(Color.White.copy(0.5f))
                        .align(Alignment.Center)
                )
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .width(1.dp)
                        .background(Color.White.copy(0.5f))
                        .align(Alignment.Center)
                )
            }

            // --- 3. ÜST BAR (GERİ TUŞU) ---
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Geri Butonu (Glass Effect)
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .background(DarkSurface.copy(0.6f), CircleShape)
                        .border(1.dp, Color.White.copy(0.1f), CircleShape)
                ) {
                    Icon(Icons.Rounded.ArrowBack, null, tint = Color.White)
                }

                // Konumumu Bul Butonu
                IconButton(
                    onClick = {
                        // 1. İzin kontrolü yap
                        if (hasLocationPermission) {
                            val client = LocationServices.getFusedLocationProviderClient(context)
                            // 2. Son konumu al
                            client.lastLocation.addOnSuccessListener { location ->
                                // 3. Konum null değilse oraya animasyonla git
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
                    },
                    modifier = Modifier
                        .background(CyberTeal, CircleShape)
                ) {
                    Icon(Icons.Rounded.GpsFixed, null, tint = Color.Black)
                }
            }

            // --- 4. ALT KONTROL PANELİ (GLASSMORPHISM) ---
            // Klavye açılınca yukarı kayması için imePadding ekliyoruz
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .imePadding()
                    .padding(bottom = 100.dp) // FAB için yer bırak
                    .padding(horizontal = 16.dp)
            ) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "TARGET PARAMETERS",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyberTeal,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // İsim Alanı
                        CustomTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Target Designation"
                        )

                        // Yarıçap Alanı
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f)) {
                                CustomTextField(
                                    value = radiusStr,
                                    onValueChange = {
                                        if (it.all { c -> c.isDigit() }) radiusStr = it
                                    },
                                    label = "Radius",
                                    keyboardType = KeyboardType.Number
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "METERS",
                                color = Color.White.copy(0.5f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- YARDIMCI ÖZEL TEXT FIELD ---
@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.Black.copy(0.3f),
            unfocusedContainerColor = Color.Black.copy(0.3f),
            focusedBorderColor = CyberTeal,
            unfocusedBorderColor = Color.Gray.copy(0.5f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedLabelColor = CyberTeal,
            unfocusedLabelColor = Color.Gray
        ),
        shape = RoundedCornerShape(12.dp)
    )
}
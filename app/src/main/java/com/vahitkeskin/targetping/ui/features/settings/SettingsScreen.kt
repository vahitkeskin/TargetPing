package com.vahitkeskin.targetping.ui.features.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.vahitkeskin.targetping.ui.features.add_edit.GlassCard
import com.vahitkeskin.targetping.ui.features.settings.components.MapThemeSelector
import com.vahitkeskin.targetping.ui.home.HomeViewModel
import com.vahitkeskin.targetping.ui.theme.DarkBackground
import com.vahitkeskin.targetping.ui.theme.PrimaryColor

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val isSound by viewModel.notificationSound.collectAsState()
    val currentMapStyle by viewModel.currentMapStyle.collectAsState()

    // --- İZİN STATE'LERİ ---
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else null

    var showSettingsRedirectDialog by remember { mutableStateOf<String?>(null) }
    var hasRequestedLocationInThisSession by rememberSaveable { mutableStateOf(false) }
    var hasRequestedNotificationInThisSession by rememberSaveable { mutableStateOf(false) }

    fun openAppSettings(action: String = Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
        val intent = Intent(action).apply {
            if (action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
                data = Uri.fromParts("package", context.packageName, null)
            }
        }
        context.startActivity(intent)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

        // Başlık Alanı
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Settings, null, tint = PrimaryColor)
            Spacer(Modifier.width(8.dp))
            Text(
                "SİSTEM AYARLARI",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }

        Spacer(Modifier.height(24.dp))

        LazyColumn(
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // HARİTA TEMA SEÇİCİ ---
            item { SectionHeader("HARİTA GÖRÜNÜMÜ") }
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(vertical = 16.dp)) {
                        MapThemeSelector(
                            currentStyle = currentMapStyle,
                            onStyleSelected = { viewModel.updateMapStyle(it) }
                        )
                    }
                }
            }

            // MEVCUT BÖLÜMLER
            item { SectionHeader("SİSTEM İZİNLERİ") }

            // 1. KONUM İZNİ
            item {
                val isLocationGranted = locationPermissionsState.allPermissionsGranted
                SettingsItem(
                    icon = Icons.Rounded.MyLocation,
                    title = "Konum Servisi",
                    subtitle = if(isLocationGranted) "Erişim izni verildi" else "Takip için gerekli",
                    isChecked = isLocationGranted,
                    onCheckedChange = {
                        if (isLocationGranted) {
                            showSettingsRedirectDialog = "Konum iznini kapatmak uygulamanın çalışmasını durduracaktır. Ayarlardan kapatmak istiyor musunuz?"
                        } else {
                            val shouldShowRationale = locationPermissionsState.shouldShowRationale
                            if (!shouldShowRationale && hasRequestedLocationInThisSession) {
                                showSettingsRedirectDialog = "Konum izni kalıcı olarak reddedilmiş. Ayarlardan manuel izin vermelisiniz."
                            } else {
                                locationPermissionsState.launchMultiplePermissionRequest()
                                hasRequestedLocationInThisSession = true
                            }
                        }
                    }
                )
            }

            // 2. BİLDİRİM İZNİ
            item {
                val isNotificationGranted = notificationPermissionState?.status?.isGranted ?: true
                SettingsItem(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "Bildirim İzni",
                    subtitle = if(isNotificationGranted) "Bildirimler aktif" else "Sesli uyarılar için gerekli",
                    isChecked = isNotificationGranted,
                    onCheckedChange = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && notificationPermissionState != null) {
                            if (isNotificationGranted) {
                                showSettingsRedirectDialog = "Bildirimleri kapatmak için ayarlara gitmeniz gerekmektedir."
                            } else {
                                val shouldShowRationale = notificationPermissionState.status.shouldShowRationale
                                if (!shouldShowRationale && hasRequestedNotificationInThisSession) {
                                    showSettingsRedirectDialog = "Bildirim izni vermeniz gerekmektedir. Ayarlara giderek izni açabilirsiniz."
                                } else {
                                    notificationPermissionState.launchPermissionRequest()
                                    hasRequestedNotificationInThisSession = true
                                }
                            }
                        }
                    }
                )
            }

            item { SectionHeader("UYGULAMA TERCİHLERİ") }
            item {
                SettingsItem(
                    icon = Icons.Rounded.VolumeUp,
                    title = "Uyarı Sesleri",
                    subtitle = "Hedefe girince ses çal",
                    isChecked = isSound,
                    onCheckedChange = { viewModel.notificationSound.value = it }
                )
            }

            item { SectionHeader("UYGULAMA BİLGİSİ") }
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("TargetPing v1.0.2 (Beta)", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Build: 2024.10.25_RC", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // --- DIALOG ---
        if (showSettingsRedirectDialog != null) {
            AlertDialog(
                onDismissRequest = { showSettingsRedirectDialog = null },
                title = { Text("İzin Ayarları", fontWeight = FontWeight.Bold) },
                text = { Text(showSettingsRedirectDialog!!) },
                confirmButton = {
                    TextButton(onClick = {
                        val action = if (showSettingsRedirectDialog?.contains("Bildirim") == true) {
                            Settings.ACTION_APP_NOTIFICATION_SETTINGS
                        } else {
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        }
                        openAppSettings(action)
                        showSettingsRedirectDialog = null
                    }) {
                        Text("AYARLARA GİT", color = PrimaryColor, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSettingsRedirectDialog = null }) {
                        Text("İPTAL", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF1E1E1E),
                titleContentColor = PrimaryColor,
                textContentColor = Color.White
            )
        }
    }
}

// Yardımcı Composable'lar (Aynı dosyada veya ayrı dosyada tutabilirsiniz)
@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = PrimaryColor,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 8.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = if(isChecked) PrimaryColor else Color.Gray)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = PrimaryColor,
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color.Black.copy(alpha = 0.5f),
                    uncheckedBorderColor = Color.Gray
                )
            )
        }
    }
}
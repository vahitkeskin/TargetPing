package com.vahitkeskin.targetping.ui.features.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.*
import com.vahitkeskin.targetping.ui.features.add_edit.GlassCard
import com.vahitkeskin.targetping.ui.home.HomeViewModel
import com.vahitkeskin.targetping.utils.openAppSettings
import com.vahitkeskin.targetping.ui.theme.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(viewModel: HomeViewModel) {
    val context = LocalContext.current
    val isSound by viewModel.notificationSound.collectAsState()

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

    // Diyalog kontrolü
    var showSettingsRedirectDialog by remember { mutableStateOf<String?>(null) }

    // Kullanıcının bu ekranda butona basıp basmadığını takip etmek için.
    // Bu, "Kalıcı Red" durumunu anlamamıza yardımcı olur.
    var hasRequestedLocationInThisSession by rememberSaveable { mutableStateOf(false) }
    var hasRequestedNotificationInThisSession by rememberSaveable { mutableStateOf(false) }

    // --- YARDIMCI FONKSİYONLAR ---
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
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
                            // İzin zaten var, kullanıcı kapatmak istiyor -> Ayarlara yönlendir
                            showSettingsRedirectDialog = "Konum iznini kapatmak uygulamanın çalışmasını durduracaktır. Ayarlardan kapatmak istiyor musunuz?"
                        } else {
                            // İzin YOK. Talep edilecek.
                            val shouldShowRationale = locationPermissionsState.shouldShowRationale

                            // Mantık: Eğer Rationale göstermemiz gerekmiyorsa (ya ilk kez, ya da kalıcı red)
                            // VE daha önce bu oturumda istediysek -> Demek ki kalıcı red yemişiz.
                            if (!shouldShowRationale && hasRequestedLocationInThisSession) {
                                showSettingsRedirectDialog = "Konum izni kalıcı olarak reddedilmiş görünüyor. Kullanabilmek için ayarlardan manuel izin vermelisiniz."
                            } else {
                                // Diğer tüm durumlarda (İlk kez veya Rationale true ise) direkt sistem popup'ını aç.
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
                                // Kapatmak istiyor -> Ayarlara yönlendir
                                showSettingsRedirectDialog = "Bildirimleri kapatmak için ayarlara gitmeniz gerekmektedir."
                            } else {
                                // Açmak istiyor
                                val shouldShowRationale = notificationPermissionState.status.shouldShowRationale

                                if (!shouldShowRationale && hasRequestedNotificationInThisSession) {
                                    // Kalıcı red durumu tespiti
                                    showSettingsRedirectDialog = "Bildirim izni vermeniz gerekmektedir. Ayarlara giderek izni açabilirsiniz."
                                } else {
                                    // İlk kez veya tekrar sorulabilir durum -> Sistem Popup'ı
                                    notificationPermissionState.launchPermissionRequest()
                                    hasRequestedNotificationInThisSession = true
                                }
                            }
                        }
                    }
                )
            }

            // --- DİĞER AYARLAR ---
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
                        // Hangi ayar için diyalog açıldıysa ona göre yönlendirme yapıyoruz
                        val action = if (showSettingsRedirectDialog?.contains("Bildirim") == true) {
                            Settings.ACTION_APP_NOTIFICATION_SETTINGS // Direkt Bildirim Ekranı
                        } else {
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS // Uygulama Detayları (Konum için)
                        }

                        context.openAppSettings(action)
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

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = PrimaryColor,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
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
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = if(isChecked) PrimaryColor else Color.Gray)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
            }
            // Switch yerine tıklanabilir bir durum göstergesi kullanıyoruz
            // Ancak kullanıcı "switch" gibi hissetsin diye Switch componentini koruyoruz
            // Fakat Switch'in kendi toggle animasyonunu, permission state yönetiyor.
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
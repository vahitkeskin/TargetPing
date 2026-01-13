package com.vahitkeskin.targetping.utils

import android.Manifest
import android.app.Activity
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker

/**
 * İzin işlemlerini yöneten ve geriye "Güvenli Tıklama" fonksiyonu döndüren Composable.
 *
 * @param isNotificationRequired Bildirim izni kontrol edilsin mi?
 * @param isLocationRequired Konum izni kontrol edilsin mi?
 * @param onGranted İzinler tamsa çalışacak asıl kod bloğu.
 * @return Butonların onClick parametresine verilecek lambda fonksiyonu.
 */
@Composable
fun rememberPermissionAction(
    isNotificationRequired: Boolean = true,
    isLocationRequired: Boolean = true,
    onGranted: () -> Unit
): () -> Unit {
    val context = LocalContext.current
    val activity = context as? Activity

    // --- STATE ---
    var showSettingsDialog by remember { mutableStateOf<PermissionType?>(null) }

    // --- HELPER FUNCTIONS ---
    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PermissionChecker.PERMISSION_GRANTED
    }

    fun shouldShowRationale(permission: String): Boolean {
        return activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, permission)
        } ?: false
    }

    // --- LAUNCHERS ---

    // 2. Konum Launcher
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions.values.all { it }
        if (isGranted) {
            onGranted()
        } else {
            val isPermanentlyDenied = !shouldShowRationale(Manifest.permission.ACCESS_FINE_LOCATION)
            if (isPermanentlyDenied) {
                showSettingsDialog = PermissionType.LOCATION
            }
        }
    }

    // 1. Bildirim Launcher
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Bildirim alındı, Konum lazım mı?
            if (isLocationRequired) {
                if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    onGranted()
                } else {
                    locationLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            } else {
                onGranted()
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val isPermanentlyDenied = !shouldShowRationale(Manifest.permission.POST_NOTIFICATIONS)
                if (isPermanentlyDenied) {
                    showSettingsDialog = PermissionType.NOTIFICATION
                }
            }
        }
    }

    // --- DIALOG ---
    if (showSettingsDialog != null) {
        val type = showSettingsDialog!!
        AlertDialog(
            onDismissRequest = { showSettingsDialog = null },
            title = { Text(text = type.title) },
            text = { Text(text = type.message) },
            confirmButton = {
                TextButton(onClick = {
                    showSettingsDialog = null
                    context.openAppSettings(type.action)
                }) {
                    Text("AYARLARI AÇ", color = Color(0xFF00E5FF))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = null }) {
                    Text("İPTAL", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color(0xFF00E5FF),
            textContentColor = Color.White
        )
    }

    // --- TRIGGER FUNCTION ---
    // Bu fonksiyonu döndürüyoruz
    return {
        // 1. Adım: Bildirim
        if (isNotificationRequired && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasPermission(Manifest.permission.POST_NOTIFICATIONS)) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // Bildirim var, Konuma geç
                if (isLocationRequired) {
                    if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                        locationLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    } else {
                        onGranted()
                    }
                } else {
                    onGranted()
                }
            }
        }
        // Android 13 altı veya bildirim gerekmiyor
        else if (isLocationRequired) {
            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                locationLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            } else {
                onGranted()
            }
        }
        // Hiçbir izin gerekmiyor
        else {
            onGranted()
        }
    }
}

private enum class PermissionType(val title: String, val message: String, val action: String) {
    NOTIFICATION(
        title = "Bildirim İzni Gerekli",
        message = "Sistemin arka planda sizi uyarabilmesi için bildirim iznini ayarlardan açmanız gerekmektedir.",
        action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
    ),
    LOCATION(
        title = "Konum İzni Gerekli",
        message = "Hedef takibi yapabilmek için konum iznini 'Uygulamayı kullanırken' veya 'Her zaman' olarak ayarlamanız gerekmektedir.",
        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    )
}
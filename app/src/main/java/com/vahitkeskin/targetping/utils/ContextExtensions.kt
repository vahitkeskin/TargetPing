package com.vahitkeskin.targetping.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Kullanıcıyı belirtilen ayar ekranına yönlendirir.
 * @param action: Settings.ACTION_... sabitleri.
 * Varsayılan: Settings.ACTION_APPLICATION_DETAILS_SETTINGS (Uygulama Bilgileri)
 */
fun Context.openAppSettings(
    action: String = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
) {
    val intent = when (action) {
        // Bildirim Ayarları (Android 8+ için özel extra verisi gerekir)
        Settings.ACTION_APP_NOTIFICATION_SETTINGS -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
            } else {
                // Eski cihazlarda detay ekranına git
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
            }
        }
        // Diğer tüm ayarlar (Konum, Genel İzinler vb.) için Paket URI'si eklenir
        else -> {
            Intent(action).apply {
                if (action == Settings.ACTION_APPLICATION_DETAILS_SETTINGS) {
                    data = Uri.fromParts("package", packageName, null)
                }
            }
        }
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}
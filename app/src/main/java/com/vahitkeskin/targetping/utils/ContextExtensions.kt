package com.vahitkeskin.targetping.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Uygulama Ayarlarını Açar.
 *
 * NOT: Android, güvenlik nedeniyle direkt "Konum İzni" sayfasına gidilmesine izin vermez.
 * Bu yüzden "Uygulama Bilgileri" sayfasına gidilir.
 * Ancak Bildirimler için direkt ayar sayfasına gidilebilir.
 */
fun Context.openAppSettings(action: String) {
    try {
        val intent = Intent(action).apply {
            when (action) {
                // 1. Durum: Uygulama Detayları (Konum vb. izinler için burası açılır)
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS -> {
                    data = Uri.fromParts("package", packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                // 2. Durum: Bildirim Ayarları (Direkt bildirim ekranı açılabilir)
                Settings.ACTION_APP_NOTIFICATION_SETTINGS -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    } else {
                        data = Uri.fromParts("package", packageName, null)
                    }
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                // Diğer durumlar
                else -> {
                    data = Uri.fromParts("package", packageName, null)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }
        }
        startActivity(intent)
    } catch (e: Exception) {
        // Herhangi bir hata olursa (örn: Çin menşeili bazı telefonlarda)
        // en garanti yol olan genel Uygulama Detaylarını açarız.
        try {
            val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            fallbackIntent.data = Uri.fromParts("package", packageName, null)
            fallbackIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(fallbackIntent)
        } catch (e2: Exception) {
            e2.printStackTrace()
        }
    }
}

// Eğer üstteki kod hala popup açıp kapatıyorsa SADECE BUNU kullan:
fun Context.uninstallSelf() {
    // Direkt silme engellendiği için kullanıcıyı "Kaldır" butonunun olduğu ayarlar sayfasına atar.
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    intent.data = Uri.fromParts("package", packageName, null)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    startActivity(intent)
}
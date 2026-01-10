package com.vahitkeskin.targetping.data.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.vahitkeskin.targetping.MainActivity
import com.vahitkeskin.targetping.R
import com.vahitkeskin.targetping.domain.repository.TargetRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class LocationTrackingService : Service(), TextToSpeech.OnInitListener {

    @Inject
    lateinit var repository: TargetRepository

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var tts: TextToSpeech? = null

    // Spam engellemek için son uyarılan hedefleri tutar
    private val alertedTargets = mutableSetOf<String>()

    // --- BU KISIM EKSİKTİ, EKLENDİ ---
    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    checkProximity(location)
                }
            }
        }
    }

    // --- START COMMAND EKLENDİ ---
    // UI'dan gelen emri burada karşılıyoruz
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTrackingService()
            ACTION_STOP -> stopTrackingService()
        }
        return START_STICKY
    }

    private fun startTrackingService() {
        // Kalıcı Bildirimi Başlat
        startForeground(1, createNotification("Sistem Devrede", "Uydu takibi aktif. Hedefler taranıyor..."))
        startLocationUpdates()
    }

    private fun stopTrackingService() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateDistanceMeters(10f)
            .build()

        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    // TTS Hazır olduğunda dili Türkçe yap
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("tr", "TR"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Dil desteklenmiyorsa logla
            }
        }
    }

    private fun checkProximity(userLocation: Location) {
        serviceScope.launch {
            val targets = repository.getAllTargets()

            targets.filter { it.isActive }.forEach { target ->
                val results = FloatArray(1)
                Location.distanceBetween(
                    userLocation.latitude, userLocation.longitude,
                    target.latitude, target.longitude,
                    results
                )
                val distanceInMeters = results[0]

                // Son tetiklenme zamanını kontrol et (Örn: 5 dakika geçmiş mi?)
                val currentTime = System.currentTimeMillis()
                val cooldown = 5 * 60 * 1000 // 5 Dakika

                if (distanceInMeters <= target.radiusMeters) {
                    // Eğer listede yoksa veya süresi dolduysa öt
                    if (!alertedTargets.contains(target.id) && (currentTime - target.lastTriggered > cooldown)) {

                        sendAlertNotification(target.name)
                        speakOut("Dikkat! ${target.name} hedefine ulaştınız.")

                        // Veritabanını güncelle (Spam koruması için)
                        // Not: lastTriggered alanını entity'e eklemiştik
                        // Burası için Repository'de updateLastTriggered fonksiyonu gerekebilir
                        // Şimdilik bellek içi (RAM) koruma yapıyoruz:
                        alertedTargets.add(target.id)
                    }
                } else {
                    alertedTargets.remove(target.id)
                }
            }
        }
    }

    private fun speakOut(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun sendAlertNotification(targetName: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, "alert_channel")
            .setContentTitle("HEDEF TEMASI SAĞLANDI")
            .setContentText("$targetName konumuna giriş yapıldı.")
            .setSmallIcon(R.drawable.ic_target_ping_logo)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(Notification.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotification(title: String, content: String): Notification {
        val channelId = "tracking_channel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Takip Servisi", NotificationManager.IMPORTANCE_LOW)
            val alertChannel = NotificationChannel("alert_channel", "Hedef Alarmları", NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
            manager.createNotificationChannel(alertChannel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_target_ping_logo)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTrackingService()
        tts?.stop()
        tts?.shutdown()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
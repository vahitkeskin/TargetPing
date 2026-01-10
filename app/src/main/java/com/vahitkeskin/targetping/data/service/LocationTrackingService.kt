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
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.vahitkeskin.targetping.MainActivity
import com.vahitkeskin.targetping.R
import com.vahitkeskin.targetping.domain.model.TargetLocation
import com.vahitkeskin.targetping.domain.repository.TargetRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject
    lateinit var repository: TargetRepository

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    // Anlık olarak takip edilecek hedeflerin listesi (Veritabanından otomatik güncellenir)
    private var activeTargets: List<TargetLocation> = emptyList()

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "tracking_channel"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 1. ADIM: Veritabanındaki aktif hedefleri sürekli dinle ve listeyi güncel tut
        repository.getTargets()
            .onEach { targets ->
                activeTargets = targets.filter { it.isActive }
            }
            .launchIn(serviceScope)

        // Konum Geri Çağırımı (Her konum değiştiğinde burası çalışır)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    // 2. ADIM: Konum her değiştiğinde bildirimdeki metni güncelle
                    checkDistanceAndUpdateNotification(location)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun startTracking() {
        // İlk bildirimi oluştur ve servisi başlat
        startForeground(NOTIFICATION_ID, createNotification("Sistem Başlatılıyor...", "Uydu bağlantısı kuruluyor."))

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L) // 1 saniyede bir güncelle
            .setMinUpdateDistanceMeters(2f) // Veya 2 metre hareket edince
            .build()

        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        serviceScope.cancel()
    }

    // 1. BU FONKSİYON HAREKETİ ALGILAR VE İKONU SEÇER
    private fun checkDistanceAndUpdateNotification(currentLoc: Location) {
        if (activeTargets.isEmpty()) {
            updateNotification("😴 Tarama Modu", "Aktif hedef bulunamadı.")
            return
        }

        // --- HAREKET MANTIĞI ---
        // Hız 0.5 m/s'den (yaklaşık 1.8 km/s) büyükse YÜRÜYOR sayalım.
        // hasSpeed() kontrolü bazı eski cihazlar için güvenliktir.
        val isMoving = currentLoc.hasSpeed() && currentLoc.speed > 0.5f

        // Duruma göre Emoji İkonu Seçimi
        val statusIcon = if (isMoving) "🏃" else "🧍"

        // Başlığa ikonu ekle
        val dynamicTitle = "$statusIcon HEDEF TAKİBİ AKTİF"

        // --- MESAFE HESABI (Standart) ---
        var nearestDistance = Float.MAX_VALUE
        var nearestTargetName = ""

        activeTargets.forEach { target ->
            val results = FloatArray(1)
            Location.distanceBetween(
                currentLoc.latitude, currentLoc.longitude,
                target.latitude, target.longitude,
                results
            )

            if (results[0] < nearestDistance) {
                nearestDistance = results[0]
                nearestTargetName = target.name
            }
        }

        val distanceStr = if (nearestDistance > 1000) {
            String.format("%.1f KM", nearestDistance / 1000)
        } else {
            "${nearestDistance.toInt()} M"
        }

        // 2. GÜNCELLEMEYİ TETİKLE
        updateNotification(dynamicTitle, "$nearestTargetName: $distanceStr kaldı")
    }

    // --- SENİN MEVCUT KODUN (HİÇ BOZULMADI) ---
    // Sadece title parametresi artık emojili geliyor.
    private fun createNotification(title: String, content: String): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Konum Takibi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Arkaplan konum takibi bildirimleri"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title) // Buraya artık "🏃 HEDEF TAKİBİ" geliyor
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_target_ping_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    // Yardımcı fonksiyon (Aynı ID ile güncelleme yapar)
    private fun updateNotification(title: String, text: String) {
        val notification = createNotification(title, text)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
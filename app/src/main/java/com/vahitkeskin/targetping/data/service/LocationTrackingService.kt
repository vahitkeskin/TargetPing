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
import com.vahitkeskin.targetping.data.local.entity.LogEventType
import com.vahitkeskin.targetping.domain.model.TargetLocation
import com.vahitkeskin.targetping.domain.repository.LogRepository
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

    @Inject
    lateinit var logRepository: LogRepository

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    // Veritabanından gelen aktif hedefler
    private var activeTargets: List<TargetLocation> = emptyList()

    // RAM'de tutulan, şu an içinde bulunduğumuz hedeflerin ID listesi.
    // Bu sayede hedef içindeyken sürekli log atılmasını engelleriz.
    private val insideTargets = mutableSetOf<String>()

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "tracking_channel"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 1. Veritabanındaki aktif hedefleri dinle
        repository.getTargets()
            .onEach { targets ->
                activeTargets = targets.filter { it.isActive }
            }
            .launchIn(serviceScope)

        // 2. Konum Callback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    checkDistanceAndLog(location)
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
        startForeground(NOTIFICATION_ID, createNotification("Sistem Başlatılıyor...", "Uydu bağlantısı kuruluyor."))

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateDistanceMeters(2f)
            .build()

        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        serviceScope.cancel()
    }

    // --- LOGIC: Hem Bildirimi Güncelle Hem Giriş/Çıkış Logla ---
    private fun checkDistanceAndLog(currentLoc: Location) {
        if (activeTargets.isEmpty()) {
            updateNotification("😴 Tarama Modu", "Aktif hedef bulunamadı.")
            return
        }

        // Hareket durumu
        val isMoving = currentLoc.hasSpeed() && currentLoc.speed > 0.5f
        val statusIcon = if (isMoving) "🏃" else "🧍"
        val dynamicTitle = "$statusIcon HEDEF TAKİBİ AKTİF"

        var nearestDistance = Float.MAX_VALUE
        var nearestTargetName = ""

        activeTargets.forEach { target ->
            val results = FloatArray(1)
            Location.distanceBetween(
                currentLoc.latitude, currentLoc.longitude,
                target.latitude, target.longitude,
                results
            )
            val distanceInMeters = results[0]

            // 1. En yakını bul (Bildirim metni için)
            if (distanceInMeters < nearestDistance) {
                nearestDistance = distanceInMeters
                nearestTargetName = target.name
            }

            // 2. LOGLAMA MANTIĞI (Giriş / Çıkış)
            val isInsideNow = distanceInMeters <= target.radiusMeters

            if (isInsideNow) {
                // Eğer menzil içindeyiz AMA set içinde yoksa -> YENİ GİRİŞ
                if (!insideTargets.contains(target.id)) {
                    insideTargets.add(target.id)

                    // Veritabanına Log At (ENTRY)
                    serviceScope.launch {
                        logRepository.logEvent(
                            targetName = target.name,
                            type = LogEventType.ENTRY,
                            message = "Hedefe giriş yapıldı (${distanceInMeters.toInt()}m)"
                        )
                    }

                    // İstersen burada bildirim sesini/titreşimi tetikleyebilirsin
                }
            } else {
                // Eğer menzil dışındayız AMA set içinde varsa -> YENİ ÇIKIŞ
                if (insideTargets.contains(target.id)) {
                    insideTargets.remove(target.id)

                    // Veritabanına Log At (EXIT)
                    serviceScope.launch {
                        logRepository.logEvent(
                            targetName = target.name,
                            type = LogEventType.EXIT,
                            message = "Bölgeden çıkıldı"
                        )
                    }
                }
            }
        }

        // 3. Bildirimi Güncelle
        val distanceStr = if (nearestDistance > 1000) {
            String.format("%.1f KM", nearestDistance / 1000)
        } else {
            "${nearestDistance.toInt()} M"
        }

        updateNotification(dynamicTitle, "$nearestTargetName: $distanceStr kaldı")
    }

    private fun createNotification(title: String, content: String): Notification {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

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
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_target_ping_logo)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun updateNotification(title: String, text: String) {
        val notification = createNotification(title, text)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
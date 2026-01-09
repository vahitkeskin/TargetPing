package com.vahitkeskin.targetping.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.vahitkeskin.targetping.MainActivity
import com.vahitkeskin.targetping.R
import com.vahitkeskin.targetping.domain.repository.LocationRepository
import com.vahitkeskin.targetping.domain.usecase.CheckProximityUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject lateinit var repository: LocationRepository
    @Inject lateinit var checkProximityUseCase: CheckProximityUseCase

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeTargets = listOf<com.vahitkeskin.targetping.domain.model.TargetLocation>()

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val CHANNEL_ID = "location_tracking_channel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Listen to DB updates
        repository.getActiveTargets()
            .onEach { activeTargets = it }
            .launchIn(serviceScope)

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_STICKY
    }

    private fun startTracking() {
        val notification = createNotification("Tracking Active", "Monitoring ${activeTargets.size} locations...")
        startForeground(NOTIFICATION_ID, notification)

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
            .setMinUpdateDistanceMeters(10f) // Optimize: Only update if moved 10m
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    checkFences(location)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            stopTracking()
        }
    }

    private fun checkFences(location: android.location.Location) {
        val triggered = checkProximityUseCase(location, activeTargets)

        triggered.forEach { target ->
            sendAlertNotification(target.name)
            // Update lastTriggered timestamp in DB to prevent spam
            serviceScope.launch {
                repository.updateTarget(target.copy(lastTriggered = System.currentTimeMillis()))
            }
        }

        // Update persistent notification
        val updateNotif = createNotification("Tracking Active", "Lat: ${location.latitude}, Lng: ${location.longitude}")
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.notify(NOTIFICATION_ID, updateNotif)
    }

    private fun sendAlertNotification(targetName: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("📍 Entered Zone!")
            .setContentText("You have arrived at $targetName")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Assume resource exists
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        manager.notify(targetName.hashCode(), notification)
    }

    private fun stopTracking() {
        if(::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(title: String, content: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Tracking Service", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
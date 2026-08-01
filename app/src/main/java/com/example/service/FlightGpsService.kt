package com.example.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.GpxPoint
import kotlinx.coroutines.*

class FlightGpsService : Service() {

    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null

    companion object {
        const val CHANNEL_ID = "flight_gps_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "ACTION_START_GPS"
        const val ACTION_STOP = "ACTION_STOP_GPS"

        fun startService(context: Context) {
            val intent = Intent(context, FlightGpsService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        fun stopService(context: Context) {
            try {
                val intent = Intent(context, FlightGpsService::class.java).apply {
                    action = ACTION_STOP
                }
                context.stopService(intent)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.action == ACTION_STOP) {
            stopGpsAndSelf()
            return START_NOT_STICKY
        }

        try {
            val notification = buildNotification("Enregistrement du vol en cours...")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            // If startForeground fails on Android 14 (e.g. background restriction), stop service gracefully
            stopSelf()
            return START_NOT_STICKY
        }

        startGpsTracking()
        return START_NOT_STICKY
    }

    private fun startGpsTracking() {
        // Acquire WakeLock so CPU stays active during screen lock / standby
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        try {
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Paramoteur::FlightGpsWakeLock")?.apply {
                acquire(3 * 3600 * 1000L) // max 3 hours
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager

        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (location.hasAccuracy() && location.accuracy > 100f) return

                val speedKmh = if (location.hasSpeed()) location.speed * 3.6 else 0.0
                val timeMs = if (location.time > 0) location.time else System.currentTimeMillis()

                val gpxPt = GpxPoint(
                    lat = location.latitude,
                    lng = location.longitude,
                    ele = if (location.hasAltitude()) location.altitude else null,
                    time = timeMs
                )

                GpsTrackerManager.addPoint(gpxPt, speedKmh)

                val ptsCount = GpsTrackerManager.recordedPoints.size
                val durSec = GpsTrackerManager.durationSeconds.value
                val min = durSec / 60
                val sec = durSec % 60
                val infoText = String.format("%02d:%02d • %.0f km/h (%d pts)", min, sec, speedKmh, ptsCount)

                updateNotification(infoText)
            }
        }

        try {
            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                0.0f,
                locationListener!!
            )
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        try {
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                1000L,
                0.0f,
                locationListener!!
            )
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (GpsTrackerManager.isRecording.value) {
                GpsTrackerManager.updateDuration()
                delay(1000)
            }
        }
    }

    private fun updateNotification(content: String) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.notify(NOTIFICATION_ID, buildNotification(content))
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    private fun buildNotification(content: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚁 Vol Paramoteur en cours")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Enregistrement Vol GPS",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Notification permanente de suivi GPS du vol"
                }
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.createNotificationChannel(channel)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    private fun stopGpsAndSelf() {
        try {
            locationListener?.let { locationManager?.removeUpdates(it) }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        try {
            wakeLock?.let { if (it.isHeld) it.release() }
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        timerJob?.cancel()
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        stopSelf()
    }

    override fun onDestroy() {
        stopGpsAndSelf()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}


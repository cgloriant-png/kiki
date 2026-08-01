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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FlightGpsService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startGpsAndForeground()
            ACTION_STOP -> stopGpsAndSelf()
            else -> startGpsAndForeground()
        }
        return START_STICKY
    }

    private fun startGpsAndForeground() {
        GpsTrackerManager.startTracking()

        val notification = buildNotification("Enregistrement du vol en cours...")
        startForeground(NOTIFICATION_ID, notification)

        // Acquire WakeLock so CPU stays active during screen lock / standby
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Paramoteur::FlightGpsWakeLock")?.apply {
            acquire(3 * 3600 * 1000L) // max 3 hours
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
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
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
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, buildNotification(content))
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
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Enregistrement Vol GPS",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification permanente de suivi GPS du vol"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun stopGpsAndSelf() {
        locationListener?.let { locationManager?.removeUpdates(it) }
        wakeLock?.let { if (it.isHeld) it.release() }
        GpsTrackerManager.stopTracking()
        timerJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopGpsAndSelf()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

package com.example.service

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import com.example.data.model.GpxPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object GpsTrackerManager {
    val recordedPoints = mutableListOf<GpxPoint>()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _pointsFlow = MutableStateFlow<List<GpxPoint>>(emptyList())
    val pointsFlow: StateFlow<List<GpxPoint>> = _pointsFlow.asStateFlow()

    private val _currentSpeedKmh = MutableStateFlow(0.0)
    val currentSpeedKmh: StateFlow<Double> = _currentSpeedKmh.asStateFlow()

    private val _durationSeconds = MutableStateFlow(0L)
    val durationSeconds: StateFlow<Long> = _durationSeconds.asStateFlow()

    var startTimeMs: Long = 0L

    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null

    fun startTracking(context: Context) {
        synchronized(recordedPoints) {
            recordedPoints.clear()
        }
        _pointsFlow.value = emptyList()
        _currentSpeedKmh.value = 0.0
        _durationSeconds.value = 0L
        startTimeMs = System.currentTimeMillis()
        _isRecording.value = true

        // Try launching FlightGpsService for foreground notifications safely
        try {
            FlightGpsService.startService(context)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        // Direct LocationManager listener as a failsafe
        try {
            locationManager = context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
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
                    addPoint(gpxPt, speedKmh)
                }
            }

            locationManager?.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                0.0f,
                locationListener!!
            )
            locationManager?.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                1000L,
                0.0f,
                locationListener!!
            )
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    fun addPoint(point: GpxPoint, speedKmh: Double) {
        synchronized(recordedPoints) {
            // Deduplicate if identical timestamp
            if (recordedPoints.isNotEmpty() && recordedPoints.last().time == point.time) {
                return
            }
            recordedPoints.add(point)
            _pointsFlow.value = recordedPoints.toList()
        }
        _currentSpeedKmh.value = speedKmh
        if (startTimeMs > 0) {
            _durationSeconds.value = (System.currentTimeMillis() - startTimeMs) / 1000
        }
    }

    fun updateDuration() {
        if (_isRecording.value && startTimeMs > 0) {
            _durationSeconds.value = (System.currentTimeMillis() - startTimeMs) / 1000
        }
    }

    fun stopTracking(context: Context? = null) {
        _isRecording.value = false
        locationListener?.let { listener ->
            try {
                locationManager?.removeUpdates(listener)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        locationListener = null

        context?.let {
            try {
                FlightGpsService.stopService(it)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}


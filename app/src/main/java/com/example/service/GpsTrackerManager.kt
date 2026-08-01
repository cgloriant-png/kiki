package com.example.service

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import com.example.data.model.GpxPoint
import kotlinx.coroutines.*
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
    private var managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null

    fun startTracking(context: Context) {
        synchronized(recordedPoints) {
            recordedPoints.clear()
        }
        _pointsFlow.value = emptyList()
        _currentSpeedKmh.value = 0.0
        _durationSeconds.value = 0L
        startTimeMs = System.currentTimeMillis()
        _isRecording.value = true

        // Timer job to update duration every second
        timerJob?.cancel()
        timerJob = managerScope.launch {
            while (_isRecording.value) {
                if (startTimeMs > 0) {
                    _durationSeconds.value = (System.currentTimeMillis() - startTimeMs) / 1000
                }
                delay(1000)
            }
        }

        // Try launching FlightGpsService for foreground notifications safely
        try {
            FlightGpsService.startService(context)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        // Direct LocationManager listener as a failsafe
        try {
            locationManager = context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

            // Check for last known location to start immediately
            try {
                val lastGps = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    ?: locationManager?.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)

                lastGps?.let { loc ->
                    val speedKmh = if (loc.hasSpeed()) loc.speed * 3.6 else 0.0
                    val timeMs = if (loc.time > 0) loc.time else System.currentTimeMillis()
                    val gpxPt = GpxPoint(
                        lat = loc.latitude,
                        lng = loc.longitude,
                        ele = if (loc.hasAltitude()) loc.altitude else null,
                        time = timeMs
                    )
                    addPoint(gpxPt, speedKmh)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }

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
        timerJob?.cancel()
        timerJob = null

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


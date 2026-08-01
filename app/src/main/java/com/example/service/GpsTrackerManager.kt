package com.example.service

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

    fun startTracking() {
        synchronized(recordedPoints) {
            recordedPoints.clear()
        }
        _pointsFlow.value = emptyList()
        _currentSpeedKmh.value = 0.0
        _durationSeconds.value = 0L
        startTimeMs = System.currentTimeMillis()
        _isRecording.value = true
    }

    fun addPoint(point: GpxPoint, speedKmh: Double) {
        synchronized(recordedPoints) {
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

    fun stopTracking() {
        _isRecording.value = false
    }
}

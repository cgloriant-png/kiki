package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.ParamoteurRepository
import com.example.data.model.*
import com.example.ui.components.MapTileProvider
import com.example.ui.components.MapToolMode
import com.example.util.GeometryUtils
import com.example.util.GpxParser
import com.example.util.JsonExportUtils
import com.example.util.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ParamoteurViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ParamoteurRepository

    init {
        val dao = AppDatabase.getDatabase(application).paramoteurDao()
        repository = ParamoteurRepository(dao)
        observeSavedData()
    }

    // State
    private val _courseData = MutableStateFlow(CourseData(name = "Parcours Entraînement"))
    val courseData: StateFlow<CourseData> = _courseData.asStateFlow()

    private val _currentCourseSlug = MutableStateFlow<String?>(null)
    val currentCourseSlug: StateFlow<String?> = _currentCourseSlug.asStateFlow()

    private val _savedCourses = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val savedCourses: StateFlow<List<Pair<String, String>>> = _savedCourses.asStateFlow()

    private val _competitionData = MutableStateFlow(CompetitionData(name = "Compétition Paramoteur"))
    val competitionData: StateFlow<CompetitionData> = _competitionData.asStateFlow()

    private val _currentCompSlug = MutableStateFlow<String?>(null)
    val currentCompSlug: StateFlow<String?> = _currentCompSlug.asStateFlow()

    private val _savedCompetitions = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val savedCompetitions: StateFlow<List<Pair<String, String>>> = _savedCompetitions.asStateFlow()

    private val _traceRaw = MutableStateFlow<List<GpxPoint>?>(null)
    val traceRaw: StateFlow<List<GpxPoint>?> = _traceRaw.asStateFlow()

    private val _traceCorrected = MutableStateFlow<List<GpxPoint>?>(null)
    val traceCorrected: StateFlow<List<GpxPoint>?> = _traceCorrected.asStateFlow()

    private val _conformity = MutableStateFlow<ConformityStats?>(null)
    val conformity: StateFlow<ConformityStats?> = _conformity.asStateFlow()

    private val _flightResult = MutableStateFlow<FlightAnalysisResult?>(null)
    val flightResult: StateFlow<FlightAnalysisResult?> = _flightResult.asStateFlow()

    private val _flightHistory = MutableStateFlow<List<FlightHistoryEntity>>(emptyList())
    val flightHistory: StateFlow<List<FlightHistoryEntity>> = _flightHistory.asStateFlow()

    private val _toolMode = MutableStateFlow(MapToolMode.NAVIGATE)
    val toolMode: StateFlow<MapToolMode> = _toolMode.asStateFlow()

    private val _addPointType = MutableStateFlow("SP")
    val addPointType: StateFlow<String> = _addPointType.asStateFlow()

    private val _tileProvider = MutableStateFlow(MapTileProvider.IGN_PLAN)
    val tileProvider: StateFlow<MapTileProvider> = _tileProvider.asStateFlow()

    private val _declaredTimesMap = MutableStateFlow<Map<String, Double>>(emptyMap())
    val declaredTimesMap: StateFlow<Map<String, Double>> = _declaredTimesMap.asStateFlow()

    private val _isCleanMapMode = MutableStateFlow(false)
    val isCleanMapMode: StateFlow<Boolean> = _isCleanMapMode.asStateFlow()

    private val _mapFocusLocation = MutableStateFlow<LatLng?>(null)
    val mapFocusLocation: StateFlow<LatLng?> = _mapFocusLocation.asStateFlow()

    fun focusOnMapLocation(location: LatLng) {
        _mapFocusLocation.value = location
    }

    // GPS Live Recording State
    private val _isRecordingGps = MutableStateFlow(false)
    val isRecordingGps: StateFlow<Boolean> = _isRecordingGps.asStateFlow()

    private val _recordedGpsCount = MutableStateFlow(0)
    val recordedGpsCount: StateFlow<Int> = _recordedGpsCount.asStateFlow()

    private val _flightDurationSeconds = MutableStateFlow(0L)
    val flightDurationSeconds: StateFlow<Long> = _flightDurationSeconds.asStateFlow()

    private val _currentSpeedKmh = MutableStateFlow(0.0)
    val currentSpeedKmh: StateFlow<Double> = _currentSpeedKmh.asStateFlow()

    private val _lastGpsLocation = MutableStateFlow<GpxPoint?>(null)
    val lastGpsLocation: StateFlow<GpxPoint?> = _lastGpsLocation.asStateFlow()

    private var locationManager: android.location.LocationManager? = null
    private var locationListener: android.location.LocationListener? = null
    private var flightTimerJob: kotlinx.coroutines.Job? = null
    private var flightStartTimestampMs: Long = 0L
    private val recordedPointsList = mutableListOf<GpxPoint>()

    private var pointCounter = 0
    private var vertexCounter = 0
    private var competitorCounter = 0

    private var historyJob: kotlinx.coroutines.Job? = null

    private fun observeSavedData() {
        viewModelScope.launch {
            try {
                repository.allCourses.collect { list ->
                    _savedCourses.value = list.map { Pair(it.slug, it.name) }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        viewModelScope.launch {
            try {
                repository.allCompetitions.collect { list ->
                    _savedCompetitions.value = list.map { Pair(it.slug, it.name) }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        viewModelScope.launch {
            try {
                com.example.service.GpsTrackerManager.isRecording.collect { rec ->
                    _isRecordingGps.value = rec
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        viewModelScope.launch {
            try {
                com.example.service.GpsTrackerManager.pointsFlow.collect { pts ->
                    _recordedGpsCount.value = pts.size
                    if (pts.isNotEmpty()) {
                        _lastGpsLocation.value = pts.last()
                        _traceRaw.value = pts
                        _traceCorrected.value = pts
                        recalculateConformity()
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        viewModelScope.launch {
            try {
                com.example.service.GpsTrackerManager.currentSpeedKmh.collect { speed ->
                    _currentSpeedKmh.value = speed
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        viewModelScope.launch {
            try {
                com.example.service.GpsTrackerManager.durationSeconds.collect { dur ->
                    _flightDurationSeconds.value = dur
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    // --- Course Actions ---
    fun updateCourseName(name: String) {
        _courseData.value = _courseData.value.copy(name = name)
    }

    fun saveCourse() {
        viewModelScope.launch {
            val course = _courseData.value
            if (course.name.isBlank()) course.name = "Parcours Sans Nom"
            val slug = repository.saveCourse(course, _currentCourseSlug.value)
            _currentCourseSlug.value = slug
            loadHistoryForCurrentCourse(slug)
        }
    }

    fun loadCourse(slug: String) {
        viewModelScope.launch {
            val loaded = repository.loadCourse(slug)
            if (loaded != null) {
                _courseData.value = loaded
                _currentCourseSlug.value = slug
                pointCounter = loaded.points.size
                vertexCounter = loaded.routeVertices.size
                recalculateConformity()
                loadHistoryForCurrentCourse(slug)
            }
        }
    }

    fun deleteCourse(slug: String) {
        viewModelScope.launch {
            repository.deleteCourse(slug)
            if (_currentCourseSlug.value == slug) {
                _currentCourseSlug.value = null
                _courseData.value = CourseData(name = "Nouveau Parcours")
            }
        }
    }

    fun updateCorridorWidth(width: Double) {
        val updated = _courseData.value.copy(corridorWidth = width)
        _courseData.value = updated
        recalculateConformity()
    }

    fun updatePenalties(penalties: CoursePenalties) {
        _courseData.value = _courseData.value.copy(penalties = penalties)
    }

    // Point & Vertex actions
    fun addPoint(type: String, lat: Double, lng: Double) {
        val currentPts = _courseData.value.points.toMutableList()

        if (type == "SP" && currentPts.any { it.type == "SP" }) return
        if (type == "FP" && currentPts.any { it.type == "FP" }) return

        val newPoint = CoursePoint(
            id = "p${++pointCounter}",
            type = type,
            lat = lat,
            lng = lng,
            radius = if (type == "cachee") 250.0 else 100.0,
            width = if (type == "tg") 200.0 else 150.0
        )
        currentPts.add(newPoint)

        // Enforce SP at front, FP at end
        enforceGateOrder(currentPts)

        _courseData.value = _courseData.value.copy(points = currentPts)
        recalculateConformity()
    }

    private fun enforceGateOrder(pts: MutableList<CoursePoint>) {
        val spIdx = pts.indexOfFirst { it.type == "SP" }
        if (spIdx > 0) {
            val sp = pts.removeAt(spIdx)
            pts.add(0, sp)
        }
        val fpIdx = pts.indexOfFirst { it.type == "FP" }
        if (fpIdx in 0 until (pts.size - 1)) {
            val fp = pts.removeAt(fpIdx)
            pts.add(fp)
        }
    }

    fun updatePointType(id: String, newType: String) {
        val pts = _courseData.value.points.toMutableList()
        val idx = pts.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val pt = pts[idx].copy(
                type = newType,
                radius = if (newType == "cachee") 250.0 else 100.0,
                width = if (newType == "tg") 200.0 else 150.0
            )
            pts[idx] = pt
            enforceGateOrder(pts)
            _courseData.value = _courseData.value.copy(points = pts)
            recalculateConformity()
        }
    }

    fun updatePointDimension(id: String, newDim: Double) {
        val pts = _courseData.value.points.toMutableList()
        val idx = pts.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val pt = pts[idx]
            if (pt.type == "balise" || pt.type == "cachee") {
                pts[idx] = pt.copy(radius = newDim)
            } else {
                pts[idx] = pt.copy(width = newDim)
            }
            _courseData.value = _courseData.value.copy(points = pts)
            recalculateConformity()
        }
    }

    fun movePointOrder(id: String, direction: Int) {
        val pts = _courseData.value.points.toMutableList()
        val idx = pts.indexOfFirst { it.id == id }
        val targetIdx = idx + direction
        if (idx >= 0 && targetIdx in 0 until pts.size) {
            val temp = pts[idx]
            pts[idx] = pts[targetIdx]
            pts[targetIdx] = temp
            enforceGateOrder(pts)
            _courseData.value = _courseData.value.copy(points = pts)
            recalculateConformity()
        }
    }

    fun deletePoint(id: String) {
        val pts = _courseData.value.points.filter { it.id != id }.toMutableList()
        enforceGateOrder(pts)
        _courseData.value = _courseData.value.copy(points = pts)
        recalculateConformity()
    }

    fun dragPoint(id: String, newLat: Double, newLng: Double) {
        val pts = _courseData.value.points.toMutableList()
        val idx = pts.indexOfFirst { it.id == id }
        if (idx >= 0) {
            pts[idx] = pts[idx].copy(lat = newLat, lng = newLng)
            _courseData.value = _courseData.value.copy(points = pts)
            recalculateConformity()
        }
    }

    // Corridor Vertices
    fun addRouteVertex(lat: Double, lng: Double) {
        val verts = _courseData.value.routeVertices.toMutableList()
        verts.add(RouteVertex("v${++vertexCounter}", lat, lng, smooth = false))
        _courseData.value = _courseData.value.copy(routeVertices = verts)
        recalculateConformity()
    }

    fun addDrawnRouteVertices(stroke: List<LatLng>) {
        if (stroke.size < 2) return
        val gpxStroke = stroke.map { GpxPoint(it.lat, it.lng) }
        val simplified = GeometryUtils.simplifyDP(gpxStroke, 6.0)
        val newVerts = simplified.map { RouteVertex("v${++vertexCounter}", it.lat, it.lng, smooth = false) }
        val combined = _courseData.value.routeVertices + newVerts
        _courseData.value = _courseData.value.copy(routeVertices = combined.toMutableList())
        recalculateConformity()
    }

    fun insertVertexNear(lat: Double, lng: Double) {
        val verts = _courseData.value.routeVertices.toMutableList()
        if (verts.size < 2) {
            addRouteVertex(lat, lng)
            return
        }
        val origin = GeometryUtils.courseOrigin(_courseData.value)
        val local = verts.map { GeometryUtils.toXY(LatLng(it.lat, it.lng), origin) }
        val clickLocal = GeometryUtils.toXY(LatLng(lat, lng), origin)

        var bestSeg = 1
        var bestD = Double.POSITIVE_INFINITY
        for (i in 1 until local.size) {
            val d = GeometryUtils.distToSegment(clickLocal, local[i - 1], local[i])
            if (d < bestD) {
                bestD = d
                bestSeg = i
            }
        }
        verts.add(bestSeg, RouteVertex("v${++vertexCounter}", lat, lng, smooth = false))
        _courseData.value = _courseData.value.copy(routeVertices = verts)
        recalculateConformity()
    }

    fun deleteNearestItem(lat: Double, lng: Double) {
        val origin = GeometryUtils.courseOrigin(_courseData.value)
        val clickLocal = GeometryUtils.toXY(LatLng(lat, lng), origin)

        var closestPt: CoursePoint? = null
        var closestVert: RouteVertex? = null
        var minD = 250.0

        _courseData.value.points.forEach { p ->
            val pLocal = GeometryUtils.toXY(LatLng(p.lat, p.lng), origin)
            val d = Math.hypot(pLocal.x - clickLocal.x, pLocal.y - clickLocal.y)
            if (d < minD) {
                minD = d
                closestPt = p
            }
        }

        _courseData.value.routeVertices.forEach { v ->
            val vLocal = GeometryUtils.toXY(LatLng(v.lat, v.lng), origin)
            val d = Math.hypot(vLocal.x - clickLocal.x, vLocal.y - clickLocal.y)
            if (d < minD) {
                minD = d
                closestVert = v
                closestPt = null
            }
        }

        closestPt?.let { deletePoint(it.id) }
        closestVert?.let { deleteVertex(it.id) }
    }

    private fun deleteVertex(id: String) {
        val verts = _courseData.value.routeVertices.filter { it.id != id }.toMutableList()
        _courseData.value = _courseData.value.copy(routeVertices = verts)
        recalculateConformity()
    }

    fun toggleSmoothVertex(lat: Double, lng: Double) {
        val origin = GeometryUtils.courseOrigin(_courseData.value)
        val clickLocal = GeometryUtils.toXY(LatLng(lat, lng), origin)
        val verts = _courseData.value.routeVertices.toMutableList()

        var bestIdx = -1
        var minD = 250.0
        verts.forEachIndexed { i, v ->
            val vLocal = GeometryUtils.toXY(LatLng(v.lat, v.lng), origin)
            val d = Math.hypot(vLocal.x - clickLocal.x, vLocal.y - clickLocal.y)
            if (d < minD) {
                minD = d
                bestIdx = i
            }
        }

        if (bestIdx >= 0) {
            verts[bestIdx] = verts[bestIdx].copy(smooth = !verts[bestIdx].smooth)
            _courseData.value = _courseData.value.copy(routeVertices = verts)
            recalculateConformity()
        }
    }

    fun dragVertex(id: String, newLat: Double, newLng: Double) {
        val verts = _courseData.value.routeVertices.toMutableList()
        val idx = verts.indexOfFirst { it.id == id }
        if (idx >= 0) {
            verts[idx] = verts[idx].copy(lat = newLat, lng = newLng)
            _courseData.value = _courseData.value.copy(routeVertices = verts)
            recalculateConformity()
        }
    }

    fun undoLastVertex() {
        val verts = _courseData.value.routeVertices.toMutableList()
        if (verts.isNotEmpty()) {
            verts.removeAt(verts.size - 1)
            _courseData.value = _courseData.value.copy(routeVertices = verts)
            recalculateConformity()
        }
    }

    fun clearCorridor() {
        _courseData.value = _courseData.value.copy(routeVertices = mutableListOf())
        recalculateConformity()
    }

    fun clearAll() {
        _courseData.value = CourseData(name = _courseData.value.name)
        _traceRaw.value = null
        _traceCorrected.value = null
        _conformity.value = null
        _flightResult.value = null
    }

    // --- GPS Live Recording & Flight Workflow ---
    fun startGpsRecording(context: android.content.Context) {
        val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            return
        }

        _isRecordingGps.value = true
        _traceRaw.value = emptyList()
        _traceCorrected.value = emptyList()
        _flightResult.value = null

        com.example.service.GpsTrackerManager.startTracking(context)
    }

    fun stopGpsRecordingAndAnalyze(context: android.content.Context? = null) {
        com.example.service.GpsTrackerManager.stopTracking(context)

        val recordedPts = com.example.service.GpsTrackerManager.recordedPoints.toList()
        val pointsToAnalyze = if (recordedPts.isNotEmpty()) {
            recordedPts
        } else {
            _traceCorrected.value ?: _traceRaw.value ?: emptyList()
        }

        if (pointsToAnalyze.isNotEmpty()) {
            _traceRaw.value = pointsToAnalyze
            _traceCorrected.value = pointsToAnalyze

            if (pointsToAnalyze.size > 5) {
                cleanOutliers(250.0)
            }

            analyzeFlight(EpreuveType.PRECISION, ScoringRef(), _declaredTimesMap.value)
            saveFlightToHistory()
        } else {
            _flightResult.value = FlightAnalysisResult(
                score = 0,
                label = "Aucune trace",
                bannerTxt = "⚠ Aucun point GPS n'a été enregistré pendant le vol. Assurez-vous que la géolocalisation GPS est activée.",
                results = emptyList(),
                distMeters = 0.0,
                durationSeconds = null,
                error = "Aucun point GPS enregistré."
            )
        }
    }

    fun setDeclaredTime(pointId: String, seconds: Double) {
        val updated = _declaredTimesMap.value.toMutableMap()
        if (seconds <= 0) {
            updated.remove(pointId)
        } else {
            updated[pointId] = seconds
        }
        _declaredTimesMap.value = updated

        // Re-analyze if trace already exists
        if (_traceCorrected.value != null || _traceRaw.value != null) {
            analyzeFlight(EpreuveType.PRECISION, ScoringRef(), _declaredTimesMap.value)
        }
    }

    // --- Trace GPX Actions ---
    fun loadGpxFromStream(inputStream: InputStream) {
        val points = GpxParser.parse(inputStream)
        if (points.isNotEmpty()) {
            _traceRaw.value = points
            _traceCorrected.value = points
            recalculateConformity()
        }
    }

    fun setSimulatedFlightTrace(stroke: List<LatLng>, speedKmh: Double) {
        val simPoints = GeometryUtils.buildSimulatedTrace(stroke, speedKmh)
        if (simPoints.isNotEmpty()) {
            _traceRaw.value = simPoints
            _traceCorrected.value = simPoints
            recalculateConformity()
        }
    }

    fun cleanOutliers(maxSpeedKmh: Double) {
        _traceCorrected.value?.let { pts ->
            val cleaned = GeometryUtils.removeOutliers(pts, maxSpeedKmh)
            _traceCorrected.value = cleaned
            recalculateConformity()
        }
    }

    fun applySimplification(toleranceMeters: Double) {
        _traceCorrected.value?.let { pts ->
            val simplified = GeometryUtils.simplifyDP(pts, toleranceMeters)
            _traceCorrected.value = simplified
            recalculateConformity()
        }
    }

    fun resetTrace() {
        _traceCorrected.value = _traceRaw.value
        recalculateConformity()
    }

    fun clearTrace() {
        _traceRaw.value = null
        _traceCorrected.value = null
        _conformity.value = null
        _flightResult.value = null
    }

    private fun recalculateConformity() {
        try {
            val trace = _traceCorrected.value ?: _traceRaw.value
            if (trace != null && trace.isNotEmpty()) {
                val spResult = _flightResult.value?.results?.find { it.point.type.equals("SP", true) || it.point.id.equals("SP", true) }
                val fpResult = _flightResult.value?.results?.find { it.point.type.equals("FP", true) || it.point.id.equals("FP", true) }
                _conformity.value = GeometryUtils.conformity(_courseData.value, trace, spResult?.traceIndex, fpResult?.traceIndex)
            } else {
                _conformity.value = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _conformity.value = null
        }
    }

    // --- Flight Analysis ---
    fun analyzeFlight(epreuveType: EpreuveType, ref: ScoringRef, declMap: Map<String, Double>) {
        val trace = _traceCorrected.value ?: _traceRaw.value
        if (trace == null || trace.isEmpty()) {
            _flightResult.value = FlightAnalysisResult(
                score = 0, label = "", bannerTxt = "", results = emptyList(),
                distMeters = 0.0, durationSeconds = null, error = "Aucune trace GPS chargée."
            )
            return
        }
        val result = GeometryUtils.scoreFlight(_courseData.value, trace, epreuveType, ref, declMap)
        _flightResult.value = result
    }

    fun saveFlightToHistory() {
        val res = _flightResult.value ?: return
        val slug = _currentCourseSlug.value ?: GeometryUtils.slugify(_courseData.value.name)
        val trace = _traceCorrected.value ?: _traceRaw.value ?: emptyList()
        val isoDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        val resultJson = JsonExportUtils.serializeFlightResult(res)
        val traceJson = if (trace.isNotEmpty()) JsonExportUtils.serializeTrace(trace) else null

        viewModelScope.launch {
            // Ensure course is saved in DB if not already
            repository.saveCourse(_courseData.value, slug)
            _currentCourseSlug.value = slug

            repository.addHistoryItem(
                slug = slug,
                epreuveType = res.label.ifBlank { "Analyse Vol" },
                score = res.score,
                dateIso = isoDate,
                resultJson = resultJson,
                traceJson = traceJson
            )
            loadHistoryForCurrentCourse(slug)
        }
    }

    fun loadHistoryFlight(item: FlightHistoryEntity) {
        if (!item.resultJson.isNullOrBlank()) {
            try {
                val res = JsonExportUtils.deserializeFlightResult(item.resultJson)
                _flightResult.value = res
            } catch (e: Exception) { e.printStackTrace() }
        }
        if (!item.traceJson.isNullOrBlank()) {
            try {
                val trace = JsonExportUtils.deserializeTrace(item.traceJson)
                _traceRaw.value = trace
                _traceCorrected.value = trace
                recalculateConformity()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun deleteHistoryFlight(id: Long) {
        viewModelScope.launch {
            repository.deleteHistoryById(id)
            _currentCourseSlug.value?.let { loadHistoryForCurrentCourse(it) }
        }
    }

    private fun loadHistoryForCurrentCourse(slug: String) {
        historyJob?.cancel()
        historyJob = viewModelScope.launch {
            try {
                repository.getHistoryForCourse(slug).collect { list ->
                    _flightHistory.value = list
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    // --- Competition Actions ---
    fun updateCompetitionName(name: String) {
        _competitionData.value = _competitionData.value.copy(name = name)
    }

    fun saveCompetition() {
        viewModelScope.launch {
            val comp = _competitionData.value
            if (comp.name.isBlank()) comp.name = "Compétition Sans Nom"
            val slug = repository.saveCompetition(comp, _currentCompSlug.value)
            _currentCompSlug.value = slug
        }
    }

    fun loadCompetition(slug: String) {
        viewModelScope.launch {
            val loaded = repository.loadCompetition(slug)
            if (loaded != null) {
                _competitionData.value = loaded
                _currentCompSlug.value = slug
            }
        }
    }

    fun deleteCompetition(slug: String) {
        viewModelScope.launch {
            repository.deleteCompetition(slug)
            if (_currentCompSlug.value == slug) {
                _currentCompSlug.value = null
                _competitionData.value = CompetitionData(name = "Nouvelle Compétition")
            }
        }
    }

    fun addCompetitor(name: String) {
        val list = _competitionData.value.competitors.toMutableList()
        list.add(Competitor("c${++competitorCounter}", name))
        _competitionData.value = _competitionData.value.copy(competitors = list)
    }

    fun removeCompetitor(id: String) {
        val list = _competitionData.value.competitors.filter { it.id != id }.toMutableList()
        _competitionData.value = _competitionData.value.copy(competitors = list)
    }

    fun addManche(name: String, courseSlug: String, epreuveType: EpreuveType) {
        viewModelScope.launch {
            val course = repository.loadCourse(courseSlug)
            val courseLabel = _savedCourses.value.find { it.first == courseSlug }?.second ?: courseSlug
            val newManche = Manche(
                id = "m${System.currentTimeMillis()}",
                name = name,
                courseSlug = courseSlug,
                courseLabel = courseLabel,
                epreuveTypeCode = epreuveType.code,
                courseData = course
            )
            val list = _competitionData.value.manches.toMutableList()
            list.add(newManche)
            _competitionData.value = _competitionData.value.copy(manches = list)
        }
    }

    fun deleteManche(id: String) {
        val list = _competitionData.value.manches.filter { it.id != id }.toMutableList()
        _competitionData.value = _competitionData.value.copy(manches = list)
    }

    fun evaluateCompetitorTrace(mancheId: String, competitorId: String, trace: List<GpxPoint>, simulated: Boolean) {
        val manches = _competitionData.value.manches.toMutableList()
        val idx = manches.indexOfFirst { it.id == mancheId }
        if (idx < 0) return
        val manche = manches[idx]

        viewModelScope.launch {
            val course = manche.courseData ?: repository.loadCourse(manche.courseSlug) ?: _courseData.value
            val epreuve = EpreuveType.fromCode(manche.epreuveTypeCode)
            val ref = ScoringRef(
                maxTimeMin = manche.refMaxTimeMin,
                nbmax = manche.refNbmax,
                tmin = manche.refTmin,
                dmax = manche.refDmax,
                tmax = manche.refTmax,
                wGates = manche.refWGates,
                wTime = manche.refWTime,
                wSpeed = manche.refWSpeed,
                wCouloir = manche.refWCouloir
            )

            val res = GeometryUtils.scoreFlight(course, trace, epreuve, ref)
            val conf = GeometryUtils.conformity(course, trace)

            val mResult = MancheResult(
                score = res.score,
                distMeters = res.distMeters,
                durationSeconds = res.durationSeconds,
                pctDist = conf?.pctDist,
                dateIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
                simulated = simulated,
                breakdown = res.breakdown
            )

            val updatedResults = manche.results.toMutableMap()
            updatedResults[competitorId] = mResult
            manches[idx] = manche.copy(results = updatedResults)

            _competitionData.value = _competitionData.value.copy(manches = manches)
        }
    }

    // --- Mode & UI selections ---
    fun setToolMode(mode: MapToolMode) { _toolMode.value = mode }
    fun setAddPointType(type: String) { _addPointType.value = type }
    fun setTileProvider(provider: MapTileProvider) { _tileProvider.value = provider }
    fun setCleanMapMode(isClean: Boolean) { _isCleanMapMode.value = isClean }

    fun exportCourseJson(): String = JsonExportUtils.serializeCourse(_courseData.value)
    fun importCourseJson(jsonStr: String) {
        val imported = JsonExportUtils.deserializeCourse(jsonStr)
        if (imported.name.isBlank()) imported.name = "Parcours Importé"
        viewModelScope.launch {
            val slug = repository.saveCourse(imported, null)
            _courseData.value = imported
            _currentCourseSlug.value = slug
            pointCounter = imported.points.size
            vertexCounter = imported.routeVertices.size
            recalculateConformity()
            loadHistoryForCurrentCourse(slug)
        }
    }

    fun exportCompetitionJson(): String = JsonExportUtils.serializeCompetition(_competitionData.value)
    fun importCompetitionJson(jsonStr: String) {
        val imported = JsonExportUtils.deserializeCompetition(jsonStr)
        _competitionData.value = imported
        _currentCompSlug.value = null
    }

    fun exportRankingCsv(): String = JsonExportUtils.buildRankingCsv(_competitionData.value)
}

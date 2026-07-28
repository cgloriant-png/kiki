package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CourseData
import com.example.data.model.EpreuveType
import com.example.data.model.GpxPoint
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ParamoteurViewModel
import com.example.util.GeometryUtils
import com.example.util.GpxParser
import kotlinx.coroutines.launch

enum class MainTab(val title: String) {
    COURSE("PARCOURS"),
    PENALTIES("PÉNALITÉS"),
    TRACE("TRACE GPS"),
    ANALYSE("ANALYSE"),
    COMPETITION("COMPÉTITION"),
    PRINT("IMPRIMER")
}

class MainActivity : ComponentActivity() {

    private val viewModel: ParamoteurViewModel by viewModels()

    // Temporary storage for pending competitor file import
    private var pendingMancheId: String? = null
    private var pendingCompetitorId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            ParamoteurTheme {
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()

                // State collections
                val courseData by viewModel.courseData.collectAsStateWithLifecycle()
                val currentCourseSlug by viewModel.currentCourseSlug.collectAsStateWithLifecycle()
                val savedCourses by viewModel.savedCourses.collectAsStateWithLifecycle()

                val competitionData by viewModel.competitionData.collectAsStateWithLifecycle()
                val currentCompSlug by viewModel.currentCompSlug.collectAsStateWithLifecycle()
                val savedCompetitions by viewModel.savedCompetitions.collectAsStateWithLifecycle()

                val traceRaw by viewModel.traceRaw.collectAsStateWithLifecycle()
                val traceCorrected by viewModel.traceCorrected.collectAsStateWithLifecycle()
                val conformity by viewModel.conformity.collectAsStateWithLifecycle()

                val flightResult by viewModel.flightResult.collectAsStateWithLifecycle()
                val flightHistory by viewModel.flightHistory.collectAsStateWithLifecycle()

                val toolMode by viewModel.toolMode.collectAsStateWithLifecycle()
                val addPointType by viewModel.addPointType.collectAsStateWithLifecycle()
                val tileProvider by viewModel.tileProvider.collectAsStateWithLifecycle()
                val isCleanMapMode by viewModel.isCleanMapMode.collectAsStateWithLifecycle()

                // GPS Recording state
                val isRecordingGps by viewModel.isRecordingGps.collectAsStateWithLifecycle()
                val recordedGpsCount by viewModel.recordedGpsCount.collectAsStateWithLifecycle()
                val flightDurationSeconds by viewModel.flightDurationSeconds.collectAsStateWithLifecycle()
                val currentSpeedKmh by viewModel.currentSpeedKmh.collectAsStateWithLifecycle()
                val declaredTimesMap by viewModel.declaredTimesMap.collectAsStateWithLifecycle()

                // Mode Concurrent vs Mode Organisateur
                var isCompetitorMode by remember { mutableStateOf(true) }
                var selectedTab by remember { mutableStateOf(MainTab.COURSE) }

                // Automatically force navigate mode and IGN Classic map in competitor mode
                LaunchedEffect(isCompetitorMode) {
                    if (isCompetitorMode) {
                        viewModel.setToolMode(MapToolMode.NAVIGATE)
                        viewModel.setTileProvider(MapTileProvider.IGN_PLAN)
                    }
                }

                // Permission Launcher for GPS
                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                            permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    if (granted) {
                        viewModel.startGpsRecording(context)
                        Toast.makeText(context, "Enregistrement GPS du vol démarré !", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Permission GPS requise pour enregistrer le vol", Toast.LENGTH_SHORT).show()
                    }
                }

                fun startFlightGps() {
                    val fineGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    if (fineGranted) {
                        viewModel.startGpsRecording(context)
                        Toast.makeText(context, "Enregistrement GPS du vol démarré !", Toast.LENGTH_SHORT).show()
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }

                // Activity Launchers for File Import
                val importGpxLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    uri?.let {
                        try {
                            contentResolver.openInputStream(it)?.use { stream ->
                                viewModel.loadGpxFromStream(stream)
                                Toast.makeText(context, "Trace GPX importée !", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erreur lecture GPX", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                val importCompetitorGpxLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    uri?.let { u ->
                        val mId = pendingMancheId
                        val cId = pendingCompetitorId
                        if (mId != null && cId != null) {
                            try {
                                contentResolver.openInputStream(u)?.use { stream ->
                                    val points = GpxParser.parse(stream)
                                    if (points.isNotEmpty()) {
                                        viewModel.evaluateCompetitorTrace(mId, cId, points, false)
                                        Toast.makeText(context, "Trace concurrent évaluée !", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Erreur lecture GPX concurrent", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                val importCourseJsonLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    uri?.let {
                        try {
                            contentResolver.openInputStream(it)?.use { stream ->
                                val json = stream.bufferedReader().readText()
                                viewModel.importCourseJson(json)
                                Toast.makeText(context, "Parcours importé !", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erreur import parcours JSON", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                val importCompJsonLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    uri?.let {
                        try {
                            contentResolver.openInputStream(it)?.use { stream ->
                                val json = stream.bufferedReader().readText()
                                viewModel.importCompetitionJson(json)
                                Toast.makeText(context, "Compétition importée !", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erreur import compétition JSON", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                // Share / Export helpers
                fun shareTextFile(content: String, filename: String, mimeType: String) {
                    try {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, content)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Exporter $filename")
                        context.startActivity(shareIntent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Échec du partage", Toast.LENGTH_SHORT).show()
                    }
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    topBar = {
                        GaugeHeader(
                            courseName = courseData.name,
                            pointsCount = courseData.points.size,
                            traceDistanceMeters = (traceCorrected ?: traceRaw)?.let { GeometryUtils.totalDistance(it) },
                            corridorPct = conformity?.pctDist ?: conformity?.pctPts,
                            flightScore = flightResult?.score,
                            isCompetitorMode = isCompetitorMode,
                            onToggleCompetitorMode = { isCompetitorMode = !isCompetitorMode }
                        )
                    }
                ) { innerPadding ->
                    val isLandscape = LocalConfiguration.current.screenWidthDp > 600

                    if (isLandscape) {
                        // Wide landscape layout
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(HighDensityBg)
                        ) {
                            // Left Side: Control Panel & Tabs (Organisateur mode only)
                            if (!isCompetitorMode) {
                                Column(
                                    modifier = Modifier
                                        .width(380.dp)
                                        .fillMaxHeight()
                                        .background(HighDensitySurface)
                                ) {
                                    TabRowHeader(
                                        selectedTab = selectedTab,
                                        onTabSelected = { selectedTab = it }
                                    )
                                    TabContentArea(
                                        selectedTab = selectedTab,
                                        courseData = courseData,
                                        savedCourses = savedCourses,
                                        currentCourseSlug = currentCourseSlug,
                                        toolMode = toolMode,
                                        addPointType = addPointType,
                                        tileProvider = tileProvider,
                                        competitionData = competitionData,
                                        savedCompetitions = savedCompetitions,
                                        currentCompSlug = currentCompSlug,
                                        traceRaw = traceRaw,
                                        traceCorrected = traceCorrected,
                                        conformity = conformity,
                                        flightResult = flightResult,
                                        flightHistory = flightHistory,
                                        viewModel = viewModel,
                                        importGpxLauncher = { importGpxLauncher.launch("*/*") },
                                        importCourseJsonLauncher = { importCourseJsonLauncher.launch("*/*") },
                                        importCompJsonLauncher = { importCompJsonLauncher.launch("*/*") },
                                        importCompetitorGpxLauncher = { mancheId, compId ->
                                            pendingMancheId = mancheId
                                            pendingCompetitorId = compId
                                            importCompetitorGpxLauncher.launch("*/*")
                                        },
                                        shareTextFile = ::shareTextFile
                                    )
                                }
                            }

                            // Right Side: Map Canvas
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                MapCanvas(
                                    modifier = Modifier.fillMaxSize(),
                                    courseData = courseData,
                                    traceRaw = traceRaw,
                                    traceCorrected = traceCorrected,
                                    toolMode = if (isCompetitorMode) MapToolMode.NAVIGATE else toolMode,
                                    addPointType = addPointType,
                                    tileProvider = tileProvider,
                                    onPointAdded = { type, lat, lng -> if (!isCompetitorMode) viewModel.addPoint(type, lat, lng) },
                                    onVertexAdded = { lat, lng -> if (!isCompetitorMode) viewModel.addRouteVertex(lat, lng) },
                                    onVerticesDrawn = { stroke -> if (!isCompetitorMode) viewModel.addDrawnRouteVertices(stroke) },
                                    onVertexInserted = { lat, lng -> if (!isCompetitorMode) viewModel.insertVertexNear(lat, lng) },
                                    onItemDeleted = { lat, lng -> if (!isCompetitorMode) viewModel.deleteNearestItem(lat, lng) },
                                    onSmoothToggled = { lat, lng -> if (!isCompetitorMode) viewModel.toggleSmoothVertex(lat, lng) },
                                    onSimulatedFlightDrawn = { stroke -> if (!isCompetitorMode) viewModel.setSimulatedFlightTrace(stroke, 40.0) },
                                    onPointDragged = { id, lat, lng -> if (!isCompetitorMode) viewModel.dragPoint(id, lat, lng) },
                                    onVertexDragged = { id, lat, lng -> if (!isCompetitorMode) viewModel.dragVertex(id, lat, lng) },
                                    onTileProviderChanged = { provider -> viewModel.setTileProvider(provider) }
                                )

                                Column(modifier = Modifier.align(Alignment.TopCenter)) {
                                    QuickFlightPanel(
                                        courseData = courseData,
                                        isRecordingGps = isRecordingGps,
                                        recordedGpsCount = recordedGpsCount,
                                        flightDurationSeconds = flightDurationSeconds,
                                        currentSpeedKmh = currentSpeedKmh,
                                        flightResult = flightResult,
                                        onImportJsonClick = { importCourseJsonLauncher.launch("*/*") },
                                        onStartGpsClick = { startFlightGps() },
                                        onStopGpsAndAnalyzeClick = { viewModel.stopGpsRecordingAndAnalyze() },
                                        onResetFlightClick = { viewModel.clearTrace() },
                                        declaredTimesMap = declaredTimesMap,
                                        onDeclaredTimeChange = { ptId, sec -> viewModel.setDeclaredTime(ptId, sec) }
                                    )
                                    if (!isCompetitorMode) {
                                        ToolModeBanner(toolMode = toolMode, addPointType = addPointType)
                                    }
                                }
                            }
                        }
                    } else {
                        // Standard portrait layout
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(HighDensityBg)
                        ) {
                            // Interactive Map Canvas
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(if (isCompetitorMode) 1f else 1.2f)
                            ) {
                                MapCanvas(
                                    modifier = Modifier.fillMaxSize(),
                                    courseData = courseData,
                                    traceRaw = traceRaw,
                                    traceCorrected = traceCorrected,
                                    toolMode = if (isCompetitorMode) MapToolMode.NAVIGATE else toolMode,
                                    addPointType = addPointType,
                                    tileProvider = tileProvider,
                                    onPointAdded = { type, lat, lng -> if (!isCompetitorMode) viewModel.addPoint(type, lat, lng) },
                                    onVertexAdded = { lat, lng -> if (!isCompetitorMode) viewModel.addRouteVertex(lat, lng) },
                                    onVerticesDrawn = { stroke -> if (!isCompetitorMode) viewModel.addDrawnRouteVertices(stroke) },
                                    onVertexInserted = { lat, lng -> if (!isCompetitorMode) viewModel.insertVertexNear(lat, lng) },
                                    onItemDeleted = { lat, lng -> if (!isCompetitorMode) viewModel.deleteNearestItem(lat, lng) },
                                    onSmoothToggled = { lat, lng -> if (!isCompetitorMode) viewModel.toggleSmoothVertex(lat, lng) },
                                    onSimulatedFlightDrawn = { stroke -> if (!isCompetitorMode) viewModel.setSimulatedFlightTrace(stroke, 40.0) },
                                    onPointDragged = { id, lat, lng -> if (!isCompetitorMode) viewModel.dragPoint(id, lat, lng) },
                                    onVertexDragged = { id, lat, lng -> if (!isCompetitorMode) viewModel.dragVertex(id, lat, lng) },
                                    onTileProviderChanged = { provider -> viewModel.setTileProvider(provider) }
                                )

                                Column(modifier = Modifier.align(Alignment.TopCenter)) {
                                    QuickFlightPanel(
                                        courseData = courseData,
                                        isRecordingGps = isRecordingGps,
                                        recordedGpsCount = recordedGpsCount,
                                        flightDurationSeconds = flightDurationSeconds,
                                        currentSpeedKmh = currentSpeedKmh,
                                        flightResult = flightResult,
                                        onImportJsonClick = { importCourseJsonLauncher.launch("*/*") },
                                        onStartGpsClick = { startFlightGps() },
                                        onStopGpsAndAnalyzeClick = { viewModel.stopGpsRecordingAndAnalyze() },
                                        onResetFlightClick = { viewModel.clearTrace() },
                                        declaredTimesMap = declaredTimesMap,
                                        onDeclaredTimeChange = { ptId, sec -> viewModel.setDeclaredTime(ptId, sec) }
                                    )
                                    if (!isCompetitorMode) {
                                        ToolModeBanner(toolMode = toolMode, addPointType = addPointType)
                                    }
                                }
                            }

                            // Lower Half: Tabbed Control Panel (Shown only in Organisateur mode)
                            if (!isCompetitorMode) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1.1f)
                                        .background(HighDensitySurface)
                                ) {
                                    TabRowHeader(
                                        selectedTab = selectedTab,
                                        onTabSelected = { selectedTab = it }
                                    )
                                    TabContentArea(
                                        selectedTab = selectedTab,
                                        courseData = courseData,
                                        savedCourses = savedCourses,
                                        currentCourseSlug = currentCourseSlug,
                                        toolMode = toolMode,
                                        addPointType = addPointType,
                                        tileProvider = tileProvider,
                                        competitionData = competitionData,
                                        savedCompetitions = savedCompetitions,
                                        currentCompSlug = currentCompSlug,
                                        traceRaw = traceRaw,
                                        traceCorrected = traceCorrected,
                                        conformity = conformity,
                                        flightResult = flightResult,
                                        flightHistory = flightHistory,
                                        viewModel = viewModel,
                                        importGpxLauncher = { importGpxLauncher.launch("*/*") },
                                        importCourseJsonLauncher = { importCourseJsonLauncher.launch("*/*") },
                                        importCompJsonLauncher = { importCompJsonLauncher.launch("*/*") },
                                        importCompetitorGpxLauncher = { mancheId, compId ->
                                            pendingMancheId = mancheId
                                            pendingCompetitorId = compId
                                            importCompetitorGpxLauncher.launch("*/*")
                                        },
                                        shareTextFile = ::shareTextFile
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val uri: Uri? = intent?.data ?: intent?.getParcelableExtra(Intent.EXTRA_STREAM)
        uri?.let { u ->
            try {
                contentResolver.openInputStream(u)?.use { stream ->
                    val content = stream.bufferedReader().readText()
                    if (content.trim().startsWith("{")) {
                        viewModel.importCourseJson(content)
                        Toast.makeText(this, "Épreuve JSON importée avec succès !", Toast.LENGTH_LONG).show()
                    } else if (content.contains("<gpx", ignoreCase = true)) {
                        contentResolver.openInputStream(u)?.use { gpxStream ->
                            viewModel.loadGpxFromStream(gpxStream)
                            Toast.makeText(this, "Trace GPX importée !", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Erreur lors de l'ouverture du fichier", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
private fun ToolModeBanner(toolMode: MapToolMode, addPointType: String) {
    if (toolMode != MapToolMode.NAVIGATE) {
        val bannerText = when (toolMode) {
            MapToolMode.ADD_POINT -> "📍 Cliquez sur la carte pour placer: ${addPointType.uppercase()}"
            MapToolMode.ADD_ROUTE_VERTEX -> "🖊 Cliquez pour ajouter un point du couloir"
            MapToolMode.DRAW_ROUTE -> "✏️ Glissez sur la carte pour tracer le couloir à main levée"
            MapToolMode.INSERT_VERTEX -> "➕ Cliquez près du couloir pour y insérer un point"
            MapToolMode.DELETE_ITEM -> "🗑 Cliquez sur une porte, balise ou point couloir pour supprimer"
            MapToolMode.TOGGLE_SMOOTH -> "🎨 Cliquez sur un point couloir pour basculer courbe / angle"
            MapToolMode.SIMULATE_FLIGHT -> "🖱️ Glissez pour dessiner un vol simulé"
            else -> ""
        }
        Surface(
            color = HighDensitySurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            shadowElevation = 2.dp,
            modifier = Modifier
                .padding(12.dp)
        ) {
            Text(
                text = bannerText,
                color = PrimaryBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun TabRowHeader(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    Surface(
        color = HighDensityNavBar,
        modifier = Modifier.fillMaxWidth()
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = HighDensityNavBar,
            contentColor = PrimaryBlueDark,
            edgePadding = 8.dp,
            divider = { HorizontalDivider(color = BorderOutline, thickness = 1.dp) }
        ) {
            MainTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                Tab(
                    selected = isSelected,
                    onClick = { onTabSelected(tab) },
                    text = {
                        Surface(
                            color = if (isSelected) PrimaryBlueContainer else androidx.compose.ui.graphics.Color.Transparent,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = tab.title,
                                color = if (isSelected) PrimaryBlueDark else SecondaryText,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun TabContentArea(
    selectedTab: MainTab,
    courseData: CourseData,
    savedCourses: List<Pair<String, String>>,
    currentCourseSlug: String?,
    toolMode: MapToolMode,
    addPointType: String,
    tileProvider: MapTileProvider,
    competitionData: com.example.data.model.CompetitionData,
    savedCompetitions: List<Pair<String, String>>,
    currentCompSlug: String?,
    traceRaw: List<GpxPoint>?,
    traceCorrected: List<GpxPoint>?,
    conformity: com.example.data.model.ConformityStats?,
    flightResult: com.example.data.model.FlightAnalysisResult?,
    flightHistory: List<com.example.data.model.FlightHistoryEntity>,
    viewModel: ParamoteurViewModel,
    importGpxLauncher: () -> Unit,
    importCourseJsonLauncher: () -> Unit,
    importCompJsonLauncher: () -> Unit,
    importCompetitorGpxLauncher: (String, String) -> Unit,
    shareTextFile: (String, String, String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (selectedTab) {
            MainTab.COURSE -> CourseTab(
                courseData = courseData,
                savedCourses = savedCourses,
                currentSlug = currentCourseSlug,
                toolMode = toolMode,
                addPointType = addPointType,
                tileProvider = tileProvider,
                onCourseNameChanged = { viewModel.updateCourseName(it) },
                onSaveCourse = { viewModel.saveCourse() },
                onLoadCourse = { viewModel.loadCourse(it) },
                onDeleteCourse = { viewModel.deleteCourse(it) },
                onExportCourseJson = {
                    val json = viewModel.exportCourseJson()
                    shareTextFile(json, "${courseData.name}.json", "application/json")
                },
                onImportCourseJson = { importCourseJsonLauncher() },
                onToolModeSelected = { viewModel.setToolMode(it) },
                onAddPointTypeSelected = { viewModel.setAddPointType(it) },
                onTileProviderSelected = { viewModel.setTileProvider(it) },
                onCorridorWidthChanged = { viewModel.updateCorridorWidth(it) },
                onUndoLastVertex = { viewModel.undoLastVertex() },
                onClearCorridor = { viewModel.clearCorridor() },
                onClearAll = { viewModel.clearAll() },
                onPointTypeChanged = { id, type -> viewModel.updatePointType(id, type) },
                onPointDimensionChanged = { id, dim -> viewModel.updatePointDimension(id, dim) },
                onMovePoint = { id, dir -> viewModel.movePointOrder(id, dir) },
                onDeletePoint = { id -> viewModel.deletePoint(id) }
            )

            MainTab.PENALTIES -> PenaltiesTab(
                penalties = courseData.penalties,
                onPenaltiesChanged = { viewModel.updatePenalties(it) }
            )

            MainTab.TRACE -> TraceTab(
                traceRaw = traceRaw,
                traceCorrected = traceCorrected,
                conformity = conformity,
                onImportGpxRequested = importGpxLauncher,
                onStartSimulationRequested = { speed ->
                    viewModel.setToolMode(MapToolMode.SIMULATE_FLIGHT)
                },
                onCleanOutliers = { maxSpeed -> viewModel.cleanOutliers(maxSpeed) },
                onApplySimplification = { tol -> viewModel.applySimplification(tol) },
                onResetTrace = { viewModel.resetTrace() },
                onClearTrace = { viewModel.clearTrace() }
            )

            MainTab.ANALYSE -> AnalyseTab(
                courseData = courseData,
                flightResult = flightResult,
                history = flightHistory,
                onAnalyzeFlight = { type, ref, declMap -> viewModel.analyzeFlight(type, ref, declMap) },
                onSaveToHistory = { viewModel.saveFlightToHistory() }
            )

            MainTab.COMPETITION -> CompetitionTab(
                competition = competitionData,
                savedCompetitions = savedCompetitions,
                currentCompSlug = currentCompSlug,
                savedCourses = savedCourses,
                onCompNameChanged = { viewModel.updateCompetitionName(it) },
                onSaveComp = { viewModel.saveCompetition() },
                onLoadComp = { viewModel.loadCompetition(it) },
                onDeleteComp = { viewModel.deleteCompetition(it) },
                onExportCompJson = {
                    val json = viewModel.exportCompetitionJson()
                    shareTextFile(json, "${competitionData.name}.json", "application/json")
                },
                onAddCompetitor = { name -> viewModel.addCompetitor(name) },
                onRemoveCompetitor = { id -> viewModel.removeCompetitor(id) },
                onAddManche = { name, slug, ep -> viewModel.addManche(name, slug, ep) },
                onDeleteManche = { id -> viewModel.deleteManche(id) },
                onSimulateCompetitorFlight = { mancheId, compId, speed ->
                    viewModel.setToolMode(MapToolMode.SIMULATE_FLIGHT)
                },
                onImportCompetitorGpx = { mancheId, compId ->
                    importCompetitorGpxLauncher(mancheId, compId)
                },
                onExportRankingCsv = {
                    val csv = viewModel.exportRankingCsv()
                    shareTextFile(csv, "${competitionData.name}_ranking.csv", "text/csv")
                }
            )

            MainTab.PRINT -> PrintTab(
                courseData = courseData,
                onCleanMapModeToggled = { isClean -> viewModel.setCleanMapMode(isClean) }
            )
        }
    }
}

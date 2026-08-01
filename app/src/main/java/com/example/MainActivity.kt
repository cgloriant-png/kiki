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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ParamoteurViewModel
import com.example.util.GeometryUtils
import com.example.util.LatLng

class MainActivity : ComponentActivity() {

    private val viewModel: ParamoteurViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        try {
            handleIncomingIntent(intent)
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        setContent {
            ParamoteurTheme {
                val context = LocalContext.current

                // ViewModel State collections
                val courseData by viewModel.courseData.collectAsStateWithLifecycle()
                val currentCourseSlug by viewModel.currentCourseSlug.collectAsStateWithLifecycle()
                val savedCourses by viewModel.savedCourses.collectAsStateWithLifecycle()

                val traceRaw by viewModel.traceRaw.collectAsStateWithLifecycle()
                val traceCorrected by viewModel.traceCorrected.collectAsStateWithLifecycle()
                val conformity by viewModel.conformity.collectAsStateWithLifecycle()

                val flightResult by viewModel.flightResult.collectAsStateWithLifecycle()
                val flightHistory by viewModel.flightHistory.collectAsStateWithLifecycle()

                val tileProvider by viewModel.tileProvider.collectAsStateWithLifecycle()

                // GPS Recording state
                val isRecordingGps by viewModel.isRecordingGps.collectAsStateWithLifecycle()
                val recordedGpsCount by viewModel.recordedGpsCount.collectAsStateWithLifecycle()
                val flightDurationSeconds by viewModel.flightDurationSeconds.collectAsStateWithLifecycle()
                val currentSpeedKmh by viewModel.currentSpeedKmh.collectAsStateWithLifecycle()
                val declaredTimesMap by viewModel.declaredTimesMap.collectAsStateWithLifecycle()
                val mapFocusLocation by viewModel.mapFocusLocation.collectAsStateWithLifecycle()

                // Set default mode to Navigate & IGN Map
                LaunchedEffect(Unit) {
                    viewModel.setToolMode(MapToolMode.NAVIGATE)
                    viewModel.setTileProvider(MapTileProvider.IGN_PLAN)
                }

                // Permission Launcher for GPS & Notifications
                val locationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val granted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                            permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    if (granted) {
                        viewModel.startGpsRecording(context)
                        Toast.makeText(context, "Enregistrement GPS du vol démarré en arrière-plan !", Toast.LENGTH_SHORT).show()
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
                        val permsToRequest = mutableListOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            permsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                        locationPermissionLauncher.launch(permsToRequest.toTypedArray())
                    }
                }

                // Activity Launcher for Importing Course JSON
                val importCourseJsonLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.GetContent()
                ) { uri: Uri? ->
                    uri?.let {
                        try {
                            contentResolver.openInputStream(it)?.use { stream ->
                                val json = stream.bufferedReader().readText()
                                viewModel.importCourseJson(json)
                                Toast.makeText(context, "Épreuve importée et enregistrée !", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Erreur import épreuve JSON", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                var selectedTab by remember { mutableIntStateOf(0) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing,
                    topBar = {
                        GaugeHeader(
                            courseName = courseData.name,
                            savedCourses = savedCourses,
                            currentCourseSlug = currentCourseSlug,
                            onSelectCourse = { slug -> viewModel.loadCourse(slug) },
                            onDeleteCourse = { slug -> viewModel.deleteCourse(slug) },
                            onImportJsonClick = { importCourseJsonLauncher.launch("*/*") },
                            pointsCount = courseData.points.size,
                            traceDistanceMeters = (traceCorrected ?: traceRaw)?.let { GeometryUtils.totalDistance(it) },
                            corridorPct = conformity?.pctDist ?: conformity?.pctPts,
                            flightScore = flightResult?.score
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .background(HighDensityBg)
                    ) {
                        // Navigation Tabs (1. Données & Vol / 2. Carte Plein Écran)
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = HighDensitySurface,
                            contentColor = PrimaryBlueDark
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Text("1. Données & Vol", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Text("2. Carte Plein Écran", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            )
                        }

                        if (selectedTab == 0) {
                            // PAGE 1: Données, Réglages et Commandes de Vol
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(bottom = 16.dp)
                            ) {
                                QuickFlightPanel(
                                    courseData = courseData,
                                    savedCourses = savedCourses,
                                    currentCourseSlug = currentCourseSlug,
                                    onSelectCourse = { slug -> viewModel.loadCourse(slug) },
                                    onDeleteCourse = { slug -> viewModel.deleteCourse(slug) },
                                    isRecordingGps = isRecordingGps,
                                    recordedGpsCount = recordedGpsCount,
                                    flightDurationSeconds = flightDurationSeconds,
                                    currentSpeedKmh = currentSpeedKmh,
                                    flightResult = flightResult,
                                    flightHistory = flightHistory,
                                    onImportJsonClick = { importCourseJsonLauncher.launch("*/*") },
                                    onStartGpsClick = { startFlightGps() },
                                    onStopGpsAndAnalyzeClick = { viewModel.stopGpsRecordingAndAnalyze(context) },
                                    onResetFlightClick = { viewModel.clearTrace() },
                                    onLoadHistoryItem = { item -> viewModel.loadHistoryFlight(item) },
                                    onDeleteHistoryItem = { id -> viewModel.deleteHistoryFlight(id) },
                                    declaredTimesMap = declaredTimesMap,
                                    onDeclaredTimeChange = { ptId, sec -> viewModel.setDeclaredTime(ptId, sec) },
                                    onSwitchToMapClick = { selectedTab = 1 },
                                    onFocusFaultClick = { loc ->
                                        viewModel.focusOnMapLocation(loc)
                                        selectedTab = 1
                                    }
                                )
                            }
                        } else {
                            // PAGE 2: Carte Plein Écran sans aucun masque
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                MapCanvas(
                                    modifier = Modifier.fillMaxSize(),
                                    courseData = courseData,
                                    traceRaw = traceRaw,
                                    traceCorrected = traceCorrected,
                                    toolMode = MapToolMode.NAVIGATE,
                                    addPointType = "balise",
                                    tileProvider = tileProvider,
                                    faultPoint = flightResult?.faultPoint,
                                    faultDescription = flightResult?.faultDescription,
                                    focusLocation = mapFocusLocation,
                                    onPointAdded = { _, _, _ -> },
                                    onVertexAdded = { _, _ -> },
                                    onVerticesDrawn = { _ -> },
                                    onVertexInserted = { _, _ -> },
                                    onItemDeleted = { _, _ -> },
                                    onSmoothToggled = { _, _ -> },
                                    onSimulatedFlightDrawn = { _ -> },
                                    onPointDragged = { _, _, _ -> },
                                    onVertexDragged = { _, _, _ -> },
                                    onTileProviderChanged = { provider -> viewModel.setTileProvider(provider) }
                                )

                                // Non-intrusive floating status bar over map
                                Surface(
                                    color = HighDensitySurface.copy(alpha = 0.92f),
                                    shape = RoundedCornerShape(20.dp),
                                    shadowElevation = 6.dp,
                                    border = BorderStroke(1.dp, PrimaryBlueContainer),
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        if (isRecordingGps) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(RedAlert, CircleShape)
                                            )
                                            val min = flightDurationSeconds / 60
                                            val sec = flightDurationSeconds % 60
                                            Text(
                                                text = String.format("%02d:%02d • %.0f km/h (%d pts)", min, sec, currentSpeedKmh, recordedGpsCount),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = HighDensityHeaderTitle
                                            )
                                            Button(
                                                onClick = { viewModel.stopGpsRecordingAndAnalyze() },
                                                colors = ButtonDefaults.buttonColors(containerColor = RedAlert),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("STOP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .background(GreenSuccess, CircleShape)
                                            )
                                            Text(
                                                text = if (courseData.name.isBlank()) "Carte Libre" else "Épreuve: ${courseData.name}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = HighDensityHeaderTitle,
                                                maxLines = 1
                                            )
                                            Button(
                                                onClick = { startFlightGps() },
                                                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("DÉBUTER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }

                                        VerticalDivider(modifier = Modifier.height(18.dp), color = BorderOutline)

                                        IconButton(
                                            onClick = { selectedTab = 0 },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Assignment,
                                                contentDescription = "Données",
                                                tint = PrimaryBlueDark,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
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
        if (intent == null) return
        try {
            val uri: Uri? = intent.data ?: try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    (intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri)
                }
            } catch (e: Throwable) {
                null
            }
            uri?.let { u ->
                contentResolver.openInputStream(u)?.use { stream ->
                    val content = stream.bufferedReader().readText()
                    if (content.trim().startsWith("{")) {
                        viewModel.importCourseJson(content)
                        Toast.makeText(this, "Épreuve JSON importée et enregistrée avec succès !", Toast.LENGTH_LONG).show()
                    } else if (content.contains("<gpx", ignoreCase = true)) {
                        contentResolver.openInputStream(u)?.use { gpxStream ->
                            viewModel.loadGpxFromStream(gpxStream)
                            Toast.makeText(this, "Trace GPX importée !", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

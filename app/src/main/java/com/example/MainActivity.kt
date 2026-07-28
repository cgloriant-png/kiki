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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ParamoteurViewModel
import com.example.util.GeometryUtils

class MainActivity : ComponentActivity() {

    private val viewModel: ParamoteurViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

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

                // Set default mode to Navigate & IGN Map
                LaunchedEffect(Unit) {
                    viewModel.setToolMode(MapToolMode.NAVIGATE)
                    viewModel.setTileProvider(MapTileProvider.IGN_PLAN)
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

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
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
                        // Main Interactive Map Canvas
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

                            // Quick Flight Control & Correction Panel Overlay
                            Column(
                                modifier = Modifier.align(Alignment.TopCenter)
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
                                    onStopGpsAndAnalyzeClick = { viewModel.stopGpsRecordingAndAnalyze() },
                                    onResetFlightClick = { viewModel.clearTrace() },
                                    onLoadHistoryItem = { item -> viewModel.loadHistoryFlight(item) },
                                    onDeleteHistoryItem = { id -> viewModel.deleteHistoryFlight(id) },
                                    declaredTimesMap = declaredTimesMap,
                                    onDeclaredTimeChange = { ptId, sec -> viewModel.setDeclaredTime(ptId, sec) }
                                )
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
                        Toast.makeText(this, "Épreuve JSON importée et enregistrée avec succès !", Toast.LENGTH_LONG).show()
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

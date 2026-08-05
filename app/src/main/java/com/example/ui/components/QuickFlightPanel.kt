package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CourseData
import com.example.data.model.FlightAnalysisResult
import com.example.data.model.FlightHistoryEntity
import com.example.data.model.PointValidationResult
import com.example.ui.theme.*
import com.example.util.LatLng

@Composable
fun QuickFlightPanel(
    courseData: CourseData,
    savedCourses: List<Pair<String, String>>,
    currentCourseSlug: String?,
    onSelectCourse: (slug: String) -> Unit,
    onDeleteCourse: (slug: String) -> Unit,
    isRecordingGps: Boolean,
    recordedGpsCount: Int,
    flightDurationSeconds: Long,
    currentSpeedKmh: Double,
    flightResult: FlightAnalysisResult?,
    flightHistory: List<FlightHistoryEntity>,
    onImportJsonClick: () -> Unit,
    onStartGpsClick: () -> Unit,
    onStopGpsAndAnalyzeClick: () -> Unit,
    onResetFlightClick: () -> Unit,
    onLoadHistoryItem: (FlightHistoryEntity) -> Unit,
    onDeleteHistoryItem: (Long) -> Unit,
    declaredTimesMap: Map<String, Double> = emptyMap(),
    onDeclaredTimeChange: ((pointId: String, seconds: Double) -> Unit)? = null,
    onSwitchToMapClick: (() -> Unit)? = null,
    onFocusFaultClick: ((LatLng) -> Unit)? = null,
    onShareGpxClick: (() -> Unit)? = null,
    onShareHistoryGpxClick: ((FlightHistoryEntity) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(true) }
    var showDeclaredTimesSection by remember { mutableStateOf(false) }
    var showDetailedReportDialog by remember { mutableStateOf(false) }
    var showHistorySection by remember { mutableStateOf(true) }

    Surface(
        color = HighDensitySurface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlueContainer),
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (isRecordingGps) RedAlert.copy(alpha = 0.15f) else PrimaryBlueContainer,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isRecordingGps) Icons.Default.Navigation else Icons.Default.FlightTakeoff,
                            contentDescription = "Vol",
                            tint = if (isRecordingGps) RedAlert else PrimaryBlueDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (isRecordingGps) "VOL EN COURS (GPS ENREGISTREMENT...)" else "Régulateur Vol & Correction",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityHeaderTitle
                        )
                        Text(
                            text = if (courseData.name.isBlank()) "Aucune épreuve sélectionnée" else "Épreuve: ${courseData.name} (${courseData.points.size} portes)",
                            fontSize = 11.sp,
                            color = SecondaryText
                        )
                    }
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Réduire",
                        tint = SecondaryText
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = BorderOutline, thickness = 1.dp)

                    if (onSwitchToMapClick != null) {
                        Button(
                            onClick = onSwitchToMapClick,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🗺️ VOIR LA CARTE PLEIN ÉCRAN",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Main Action Buttons Row: Open JSON / Start Flight GPS / Stop & Correct
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Open JSON Button
                        OutlinedButton(
                            onClick = onImportJsonClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ouvrir JSON",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }

                        // Flight GPS Control Buttons
                        if (!isRecordingGps) {
                            Button(
                                onClick = onStartGpsClick,
                                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "DÉBUTER LE VOL",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Button(
                                onClick = onStopGpsAndAnalyzeClick,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueDark),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.2f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "POSÉ ! (CORRIGER)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        } else {
                            Button(
                                onClick = onStopGpsAndAnalyzeClick,
                                colors = ButtonDefaults.buttonColors(containerColor = RedAlert),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "POSÉ ! (CORRIGER)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Declared Times Toggle (for precision epreuves)
                    if (!isRecordingGps && courseData.points.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showDeclaredTimesSection = !showDeclaredTimesSection },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (declaredTimesMap.isNotEmpty()) PrimaryBlueContainer.copy(alpha = 0.2f) else Color.Transparent
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = PrimaryBlueDark
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (declaredTimesMap.isEmpty()) "⏱️ Entrer mes temps annoncés aux portes" else "⏱️ Temps annoncés (${declaredTimesMap.size} renseignés)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlueDark
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(
                                imageVector = if (showDeclaredTimesSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = SecondaryText
                            )
                        }

                        if (showDeclaredTimesSection) {
                            Surface(
                                color = HighDensityNavBar,
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderOutline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Temps annoncés par porte (en secondes)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityHeaderTitle
                                    )

                                    val timingPoints = courseData.points.filter { 
                                        val t = it.type.lowercase()
                                        val id = it.id.lowercase()
                                        t != "sp" && id != "sp" && (t == "tg" || t == "fp")
                                    }
                                    val pointsToUse = (if (timingPoints.isNotEmpty()) timingPoints else courseData.points).filter {
                                        val t = it.type.lowercase()
                                        val id = it.id.lowercase()
                                        t != "sp" && id != "sp"
                                    }

                                    pointsToUse.forEach { pt ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(PrimaryBlue, CircleShape)
                                                )
                                                Text(
                                                    text = "${pt.type.uppercase()} (${pt.id.take(8)})",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = HighDensityHeaderTitle
                                                )
                                            }

                                            val currentSecs = declaredTimesMap[pt.id] ?: 0.0

                                            OutlinedTextField(
                                                value = if (currentSecs > 0) "${currentSecs.toInt()}" else "",
                                                onValueChange = { inputStr ->
                                                    val totalSec = inputStr.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
                                                    onDeclaredTimeChange?.invoke(pt.id, totalSec)
                                                },
                                                placeholder = { Text("120", fontSize = 11.sp, color = SecondaryText.copy(alpha = 0.5f)) },
                                                trailingIcon = { Text("s", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SecondaryText) },
                                                singleLine = true,
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                                modifier = Modifier.width(115.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Live GPS Recording Telemetry
                    if (isRecordingGps) {
                        Surface(
                            color = RedAlert.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RedAlert.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LiveMetric(
                                    label = "POINTS GPS",
                                    value = "$recordedGpsCount",
                                    icon = Icons.Default.LocationOn
                                )
                                LiveMetric(
                                    label = "DURÉE",
                                    value = formatTime(flightDurationSeconds),
                                    icon = Icons.Default.Timer
                                )
                                LiveMetric(
                                    label = "DISTANCE",
                                    value = com.example.util.GeometryUtils.fmtDist(flightResult?.distMeters ?: 0.0),
                                    icon = Icons.Default.Navigation
                                )
                                LiveMetric(
                                    label = "VITESSE",
                                    value = "%.1f km/h".format(currentSpeedKmh),
                                    icon = Icons.Default.Speed
                                )
                            }
                        }
                    }

                    // Flight Result Banner & Breakdown
                    if (!isRecordingGps && flightResult != null) {
                        Surface(
                            color = PrimaryBlueContainer.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "🏆 CORRECTION & SCORE DU VOL",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryBlueDark
                                        )
                                        Text(
                                            text = "${flightResult.score} pts",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = GreenSuccess
                                        )
                                    }

                                    Row {
                                        IconButton(
                                            onClick = { showDetailedReportDialog = true },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Assessment,
                                                contentDescription = "Détail du vol",
                                                tint = PrimaryBlue
                                            )
                                        }
                                        IconButton(
                                            onClick = onResetFlightClick,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Effacer la trace",
                                                tint = SecondaryText
                                            )
                                        }
                                    }
                                }

                                if (!flightResult.bannerTxt.isNullOrBlank()) {
                                    Text(
                                        text = flightResult.bannerTxt,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = SecondaryText,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                flightResult.breakdown?.let { bd ->
                                    if (bd.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            color = HighDensityNavBar,
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderOutline),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "DÉTAIL DES POINTS (BARÈME) :",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = HighDensityHeaderTitle
                                                )
                                                bd.forEach { (cat, pts) ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(text = "• $cat", fontSize = 11.sp, color = SecondaryText)
                                                        Text(text = "$pts pts", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryBlueDark)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                flightResult.faultPoint?.let { fp ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = {
                                            onFocusFaultClick?.invoke(fp)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RedAlert),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "🚩 VOIR LA FAUTE SUR LA CARTE",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                if (flightResult.results.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    HorizontalDivider(color = BorderOutline, thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(6.dp))

                                    val validatedCount = flightResult.results.count { it.validated }
                                    val totalGates = flightResult.results.size
                                    val totalPenalties = flightResult.breakdown?.get("penalties") ?: 0

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Portes validées : $validatedCount/$totalGates",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GreenSuccess
                                        )
                                        if (totalPenalties > 0) {
                                            Text(
                                                text = "Pénalités : -$totalPenalties pts",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = RedAlert
                                            )
                                        }
                                    }

                                    flightResult.corridorStats?.let { stats ->
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            color = PrimaryBlueContainer.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.AltRoute,
                                                        contentDescription = null,
                                                        tint = PrimaryBlueDark,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = "Présence couloir :",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = HighDensityHeaderTitle
                                                    )
                                                }
                                                Text(
                                                    text = "${stats.pctTime ?: stats.pctDist ?: stats.pctPts}% dans le couloir",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = GreenSuccess
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Button(
                                        onClick = { showDetailedReportDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ListAlt,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "VOIR LE DÉTAIL PORTE PAR PORTE",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    if (onShareGpxClick != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedButton(
                                            onClick = onShareGpxClick,
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenSuccess),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, GreenSuccess),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = null,
                                                tint = GreenSuccess,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "📤 ENVOYER LA TRACE GPX À L'ORGANISATEUR",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = GreenSuccess
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Saved History / Corrections Section
                    if (!isRecordingGps && flightHistory.isNotEmpty()) {
                        Surface(
                            color = HighDensityNavBar,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.History,
                                            contentDescription = null,
                                            tint = PrimaryBlueDark,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "📋 CORRECTIONS & VOLS ENREGISTRÉS (${flightHistory.size})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = HighDensityHeaderTitle
                                        )
                                    }

                                    IconButton(
                                        onClick = { showHistorySection = !showHistorySection },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (showHistorySection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = SecondaryText,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                if (showHistorySection) {
                                    flightHistory.forEach { item ->
                                        Surface(
                                            color = HighDensitySurface,
                                            shape = RoundedCornerShape(8.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderOutline),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Score: ${item.score} pts",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = GreenSuccess
                                                    )
                                                    Text(
                                                        text = "${item.dateIso} - ${item.epreuveType}",
                                                        fontSize = 10.sp,
                                                        color = SecondaryText
                                                    )
                                                }

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Load / View on map
                                                    OutlinedButton(
                                                        onClick = { onLoadHistoryItem(item) },
                                                        shape = RoundedCornerShape(8.dp),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Visibility,
                                                            contentDescription = "Voir",
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Voir", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }

                                                    if (onShareHistoryGpxClick != null) {
                                                        IconButton(
                                                            onClick = { onShareHistoryGpxClick(item) },
                                                            modifier = Modifier.size(32.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Share,
                                                                contentDescription = "Partager GPX",
                                                                tint = GreenSuccess,
                                                                modifier = Modifier.size(18.dp)
                                                            )
                                                        }
                                                    }

                                                    // Delete correction to re-fly
                                                    IconButton(
                                                        onClick = { onDeleteHistoryItem(item.id) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.DeleteOutline,
                                                            contentDescription = "Supprimer correction",
                                                            tint = RedAlert,
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
            }
        }
    }

    // Detailed Gate-By-Gate Flight Results Dialog
    if (showDetailedReportDialog && flightResult != null) {
        AlertDialog(
            onDismissRequest = { showDetailedReportDialog = false },
            confirmButton = {
                Button(
                    onClick = { showDetailedReportDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("FERMER", fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = GreenSuccess
                    )
                    Text(
                        text = "Résultat Détaillé du Vol",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityHeaderTitle
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Total Score Summary Box
                    Surface(
                        color = PrimaryBlueContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "SCORE TOTAL OBTENU",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlueDark
                                )
                                Text(
                                    text = "${flightResult.score} pts",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GreenSuccess
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                val durStr = flightResult.durationSeconds?.let { formatTime(it.toLong()) } ?: "--"
                                Text(text = "Durée: $durStr", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = HighDensityHeaderTitle)
                                val distKm = flightResult.distMeters?.let { "%.2f km".format(it / 1000.0) } ?: "--"
                                Text(text = "Distance: $distKm", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = HighDensityHeaderTitle)
                            }
                        }
                    }

                    // Corridor Conformity Box
                    flightResult.corridorStats?.let { stats ->
                        Surface(
                            color = HighDensityNavBar,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AltRoute,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "POURCENTAGE DANS LE COULOIR",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityHeaderTitle
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "Distance", fontSize = 10.sp, color = SecondaryText)
                                        Text(text = "${stats.pctDist ?: "--"}%", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = GreenSuccess)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "Temps de vol", fontSize = 10.sp, color = SecondaryText)
                                        Text(text = "${stats.pctTime ?: "--"}%", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = GreenSuccess)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "Points GPS", fontSize = 10.sp, color = SecondaryText)
                                        Text(text = "${stats.pctPts}%", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = GreenSuccess)
                                    }
                                }
                            }
                        }
                    }

                    // Breakdown Score Categories
                    flightResult.breakdown?.let { bd ->
                        if (bd.isNotEmpty()) {
                            Text(
                                text = "RÉPARTITION DU SCORE :",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityHeaderTitle
                            )
                            bd.forEach { (cat, pts) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = cat, fontSize = 12.sp, color = SecondaryText)
                                    Text(
                                        text = "$pts pts",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (pts < 0) RedAlert else GreenSuccess
                                    )
                                }
                            }
                            HorizontalDivider(color = BorderOutline, thickness = 1.dp)
                        }
                    }

                    // Gate-by-Gate Table Header
                    Text(
                        text = "DÉTAIL PORTE PAR PORTE :",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityHeaderTitle
                    )

                    flightResult.results.forEachIndexed { idx, res ->
                        GateResultCard(index = idx + 1, result = res)
                    }
                }
            }
        )
    }
}

@Composable
private fun GateResultCard(index: Int, result: PointValidationResult) {
    Surface(
        color = HighDensityNavBar,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (result.validated) GreenSuccess.copy(alpha = 0.5f) else RedAlert.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (result.validated) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (result.validated) GreenSuccess else RedAlert,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "$index. ${result.point.type.uppercase()} (${result.point.id.take(8)})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityHeaderTitle
                    )
                }

                Text(
                    text = if (result.validated) "VALIDÉE" else "NON FRANCHIE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (result.validated) GreenSuccess else RedAlert
                )
            }

            if (result.validated) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val declText = result.declaredS?.let { "${it.toInt()}s (${fmtSecToMinSec(it)})" } ?: "--"
                    val actText = result.actualS?.let { "${it.toInt()}s (${fmtSecToMinSec(it)})" } ?: "--"
                    val ecartText = result.ecartS?.let { "%.1fs".format(it) } ?: "--"

                    Column {
                        Text(text = "Annoncé: $declText", fontSize = 11.sp, color = SecondaryText)
                        Text(text = "Réalisé: $actText", fontSize = 11.sp, color = HighDensityHeaderTitle, fontWeight = FontWeight.SemiBold)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Écart: $ecartText", fontSize = 11.sp, color = SecondaryText)
                        Text(
                            text = "Points: +${result.points ?: 0} pts",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenSuccess
                        )
                    }
                }
            }
        }
    }
}

private fun fmtSecToMinSec(seconds: Double): String {
    val totalSec = seconds.toInt()
    val mins = totalSec / 60
    val secs = totalSec % 60
    return "%02dm %02ds".format(mins, secs)
}

@Composable
private fun LiveMetric(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = RedAlert,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = SecondaryText
            )
        }
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = HighDensityHeaderTitle
        )
    }
}

private fun formatTime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    val hrs = mins / 60
    return if (hrs > 0) {
        "%02d:%02d:%02d".format(hrs, mins % 60, secs)
    } else {
        "%02d:%02d".format(mins, secs)
    }
}

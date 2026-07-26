package com.example.ui.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.ui.theme.*
import com.example.util.GeometryUtils

@Composable
fun QuickFlightPanel(
    courseData: CourseData,
    isRecordingGps: Boolean,
    recordedGpsCount: Int,
    flightDurationSeconds: Long,
    currentSpeedKmh: Double,
    flightResult: FlightAnalysisResult?,
    onImportJsonClick: () -> Unit,
    onStartGpsClick: () -> Unit,
    onStopGpsAndAnalyzeClick: () -> Unit,
    onResetFlightClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(true) }

    Surface(
        color = HighDensitySurface,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlueContainer),
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp)
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
                            text = if (isRecordingGps) "VOL EN COURS (GPS...)" else "Épreuve de Précision",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityHeaderTitle
                        )
                        Text(
                            text = if (courseData.name.isBlank()) "Aucun parcours chargé" else "Épreuve: ${courseData.name} (${courseData.points.size} portes/balises)",
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

                    // Actions Bar: 1. Import JSON, 2. Start/Stop Flight GPS
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Import JSON Button
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
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }

                        // Main Flight GPS Recording Button
                        if (!isRecordingGps) {
                            Button(
                                onClick = onStartGpsClick,
                                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "DÉBUTER LE VOL",
                                    fontSize = 12.sp,
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
                                    text = "POSÉ ! (POINTS)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Live Flight Stats Panel
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
                                    label = "VITESSE",
                                    value = "%.1f km/h".format(currentSpeedKmh),
                                    icon = Icons.Default.Speed
                                )
                            }
                        }
                    }

                    // Result Banner when flight is finished & calculated
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
                                            text = "🏆 RÉSULTAT DU VOL",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryBlueDark
                                        )
                                        Text(
                                            text = "Score : ${flightResult.score} points",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = GreenSuccess
                                        )
                                    }

                                    IconButton(
                                        onClick = onResetFlightClick,
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Effacer la trace",
                                            tint = SecondaryText
                                        )
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

                                if (flightResult.results.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Portes/Balises validées: ${flightResult.results.count { it.validated }}/${flightResult.results.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = PrimaryBlueDark
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

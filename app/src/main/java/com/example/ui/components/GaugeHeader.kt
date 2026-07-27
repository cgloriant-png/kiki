package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.GeometryUtils

@Composable
fun GaugeHeader(
    courseName: String,
    pointsCount: Int,
    traceDistanceMeters: Double?,
    corridorPct: Int?,
    flightScore: Int?,
    isCompetitorMode: Boolean = true,
    onToggleCompetitorMode: (() -> Unit)? = null
) {
    Surface(
        color = HighDensityBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Header Title & Active Subtitle
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isCompetitorMode) "Vol Concurrent (Navigation)" else "Trace Compétition (Éditeur)",
                        color = HighDensityHeaderTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isCompetitorMode) GreenSuccess else PrimaryBlue, CircleShape)
                        )
                        Text(
                            text = if (courseName.isBlank()) "Aucune épreuve chargée" else "Épreuve: $courseName",
                            color = SecondaryText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Mode Selector Toggle Button
                onToggleCompetitorMode?.let { toggle ->
                    FilterChip(
                        selected = isCompetitorMode,
                        onClick = toggle,
                        label = {
                            Text(
                                text = if (isCompetitorMode) "MODE CONCURRENT" else "MODE ORGANISATEUR",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenSuccess.copy(alpha = 0.2f),
                            selectedLabelColor = GreenSuccess
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // High Density Gauges Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GaugeCell(
                    value = pointsCount.toString(),
                    label = "POINTS",
                    valueColor = PrimaryBlue,
                    modifier = Modifier.weight(1f)
                )
                GaugeCell(
                    value = GeometryUtils.fmtDist(traceDistanceMeters),
                    label = "TRACE",
                    valueColor = PrimaryBlue,
                    modifier = Modifier.weight(1f)
                )
                GaugeCell(
                    value = corridorPct?.let { "$it%" } ?: "—",
                    label = "COULOIR",
                    valueColor = if (corridorPct != null && corridorPct >= 80) GreenSuccess else RedAlertText,
                    modifier = Modifier.weight(1f)
                )
                GaugeCell(
                    value = flightScore?.toString() ?: "—",
                    label = "SCORE",
                    valueColor = if (flightScore != null && flightScore > 0) GreenSuccess else PrimaryBlue,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = BorderOutline, thickness = 1.dp)
        }
    }
}

@Composable
private fun GaugeCell(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = HighDensitySurface,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderOutline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = SecondaryText,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = value,
                color = valueColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


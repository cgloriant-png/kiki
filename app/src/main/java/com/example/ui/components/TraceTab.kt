package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConformityStats
import com.example.data.model.GpxPoint
import com.example.ui.theme.*
import com.example.util.GeometryUtils

@Composable
fun TraceTab(
    traceRaw: List<GpxPoint>?,
    traceCorrected: List<GpxPoint>?,
    conformity: ConformityStats?,
    onImportGpxRequested: () -> Unit,
    onStartSimulationRequested: (Double) -> Unit,
    onCleanOutliers: (Double) -> Unit,
    onApplySimplification: (Double) -> Unit,
    onResetTrace: () -> Unit,
    onClearTrace: () -> Unit,
    onExportGpxRequested: (() -> Unit)? = null
) {
    var maxSpeedInput by remember { mutableStateOf("65") }
    var lissageSlider by remember { mutableFloatStateOf(8f) }
    var simSpeedInput by remember { mutableStateOf("40") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Card 1: Import GPX
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkLine)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("IMPORTER UNE TRACE (.GPX)", color = InkDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = onImportGpxRequested,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SkyDim, contentColor = InkText)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Choisir un fichier .gpx")
                    }
                }
            }
        }

        // Card 2: Simulate Track
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkLine)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🖱️ SIMULER UNE TRACE DE TEST", color = SkyBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Pour vérifier que le scoring fonctionne sans vraie trace GPS : dessine un vol au doigt sur la carte à vitesse constante.",
                        color = InkDim,
                        fontSize = 11.sp
                    )
                    OutlinedTextField(
                        value = simSpeedInput,
                        onValueChange = { simSpeedInput = it },
                        label = { Text("Vitesse simulée (km/h)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkPanel2,
                            unfocusedContainerColor = DarkPanel2,
                            focusedTextColor = InkText,
                            unfocusedTextColor = InkText
                        )
                    )
                    Button(
                        onClick = {
                            val speed = simSpeedInput.toDoubleOrNull() ?: 40.0
                            onStartSimulationRequested(speed)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberAccent, contentColor = DarkBg)
                    ) {
                        Text("🖱️ Dessiner une trace simulée", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Card 3: Raw Track Stats
        if (traceRaw != null && traceRaw.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkPanel),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkLine)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("TRACE BRUTE", color = SkyBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        StatRow("Points", traceRaw.size.toString())
                        StatRow("Distance", GeometryUtils.fmtDist(GeometryUtils.totalDistance(traceRaw)))
                        StatRow("Durée", GeometryUtils.fmtDur(GeometryUtils.totalDurationSeconds(traceRaw)))
                    }
                }
            }
        }

        // Card 4: Correction Controls
        if (traceCorrected != null && traceCorrected.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkPanel),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkLine)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("CORRECTION DE LA TRACE", color = SkyBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = maxSpeedInput,
                            onValueChange = { maxSpeedInput = it },
                            label = { Text("Vitesse max plausible (km/h)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkPanel2,
                                unfocusedContainerColor = DarkPanel2,
                                focusedTextColor = InkText,
                                unfocusedTextColor = InkText
                            )
                        )

                        OutlinedButton(
                            onClick = {
                                val speed = maxSpeedInput.toDoubleOrNull() ?: 65.0
                                onCleanOutliers(speed)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = InkText)
                        ) {
                            Text("🧹 Nettoyer les sauts de vitesse")
                        }

                        Text("LISSAGE (TOLÉRANCE M) : ${lissageSlider.toInt()} M", color = InkDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Slider(
                            value = lissageSlider,
                            onValueChange = { lissageSlider = it },
                            valueRange = 0f..60f,
                            colors = SliderDefaults.colors(thumbColor = SkyBlue, activeTrackColor = SkyBlue)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { onApplySimplification(lissageSlider.toDouble()) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = SkyDim, contentColor = InkText)
                            ) {
                                Text("✔️ Appliquer")
                            }
                            OutlinedButton(
                                onClick = onResetTrace,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("↺ Réinit.")
                            }
                        }
                    }
                }
            }

            // Card 5: Corrected Track Stats
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkPanel),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkLine)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("TRACE CORRIGÉE", color = SkyBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        StatRow("Points", traceCorrected.size.toString())
                        StatRow("Distance", GeometryUtils.fmtDist(GeometryUtils.totalDistance(traceCorrected)))
                        StatRow("Durée", GeometryUtils.fmtDur(GeometryUtils.totalDurationSeconds(traceCorrected)))

                        Spacer(modifier = Modifier.height(4.dp))
                        if (onExportGpxRequested != null) {
                            Button(
                                onClick = onExportGpxRequested,
                                colors = ButtonDefaults.buttonColors(containerColor = GreenOk, contentColor = DarkBg),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("📤 Exporter / Partager la trace (.gpx)", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Button(
                            onClick = onClearTrace,
                            colors = ButtonDefaults.buttonColors(containerColor = RedAlert),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🗑 Effacer la trace")
                        }
                    }
                }
            }
        }

        // Card 6: Conformity Stats
        if (conformity != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkPanel),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkLine)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("CONFORMITÉ AU COULOIR", color = SkyBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            ConformityBox("${conformity.pctPts}%", "% POINTS", modifier = Modifier.weight(1f))
                            ConformityBox(conformity.pctDist?.let { "$it%" } ?: "—", "% DISTANCE", modifier = Modifier.weight(1f))
                            ConformityBox(conformity.pctTime?.let { "$it%" } ?: "—", "% TEMPS", modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = InkDim, fontSize = 11.sp)
        Text(value, color = InkText, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun ConformityBox(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        color = DarkPanel2,
        shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkLine),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = SkyBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(label, color = InkDim, fontSize = 8.sp, fontWeight = FontWeight.Bold)
        }
    }
}

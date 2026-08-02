package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.util.GeometryUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyseTab(
    courseData: CourseData,
    flightResult: FlightAnalysisResult?,
    history: List<FlightHistoryEntity>,
    onAnalyzeFlight: (EpreuveType, ScoringRef, Map<String, Double>) -> Unit,
    onSaveToHistory: () -> Unit
) {
    var selectedEpreuve by remember { mutableStateOf(EpreuveType.PURE) }

    // Scoring reference values
    var refMaxTimeMin by remember { mutableStateOf("") }
    var refNbmax by remember { mutableStateOf(courseData.points.count { it.type == "balise" || it.type == "cachee" || it.type == "porte" }.toString()) }
    var refTmin by remember { mutableStateOf("") }
    var refDmax by remember { mutableStateOf("") }
    var refTmax by remember { mutableStateOf("") }

    var wGates by remember(courseData) { mutableStateOf(courseData.scoringRef.wGates.toInt().toString()) }
    var wTime by remember(courseData) { mutableStateOf(courseData.scoringRef.wTime.toInt().toString()) }
    var wSpeed by remember(courseData) { mutableStateOf(courseData.scoringRef.wSpeed.toInt().toString()) }
    var wCouloir by remember(courseData) { mutableStateOf(courseData.scoringRef.wCouloir.toInt().toString()) }

    val tgPoints = remember(courseData.points) {
        courseData.points.filter {
            val t = it.type.lowercase()
            val id = it.id.lowercase()
            t != "sp" && id != "sp" && (t == "tg" || t == "fp")
        }
    }
    val tgDeclarations = remember { mutableStateMapOf<String, String>() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Epreuve Type Selector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkLine)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("TYPE D'ÉPREUVE (FFPLUM)", color = InkDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)

                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value = selectedEpreuve.title,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = DarkPanel2,
                                unfocusedContainerColor = DarkPanel2,
                                focusedTextColor = InkText,
                                unfocusedTextColor = InkText
                            )
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(DarkPanel2)) {
                            EpreuveType.entries.forEach { ep ->
                                DropdownMenuItem(
                                    text = { Text(ep.title, color = InkText, fontSize = 12.sp) },
                                    onClick = {
                                        selectedEpreuve = ep
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Reference values input
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkLine)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("VALEURS DE RÉFÉRENCE", color = SkyBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    when (selectedEpreuve) {
                        EpreuveType.PURE -> {
                            OutlinedTextField(
                                value = refMaxTimeMin,
                                onValueChange = { refMaxTimeMin = it },
                                label = { Text("Temps max autorisé (min, optionnel)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = DarkPanel2, unfocusedContainerColor = DarkPanel2, focusedTextColor = InkText, unfocusedTextColor = InkText)
                            )
                            OutlinedTextField(
                                value = refNbmax,
                                onValueChange = { refNbmax = it },
                                label = { Text("Nb balises max du meilleur pilote (Nbmax)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = DarkPanel2, unfocusedContainerColor = DarkPanel2, focusedTextColor = InkText, unfocusedTextColor = InkText)
                            )
                        }
                        EpreuveType.SNAKE -> {
                            OutlinedTextField(
                                value = refTmin,
                                onValueChange = { refTmin = it },
                                label = { Text("Temps de référence Tmin (s, optionnel)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = DarkPanel2, unfocusedContainerColor = DarkPanel2, focusedTextColor = InkText, unfocusedTextColor = InkText)
                            )
                        }
                        EpreuveType.PRECISION -> {
                            OutlinedTextField(
                                value = refTmin,
                                onValueChange = { refTmin = it },
                                label = { Text("Temps de référence Tmin (s, optionnel)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = DarkPanel2, unfocusedContainerColor = DarkPanel2, focusedTextColor = InkText, unfocusedTextColor = InkText)
                            )
                            Text("Barème personnalisé (Total recommandé = 1000)", color = InkDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = wGates, onValueChange = { wGates = it }, label = { Text("Gates") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = DarkPanel2, unfocusedContainerColor = DarkPanel2, focusedTextColor = InkText, unfocusedTextColor = InkText))
                                OutlinedTextField(value = wTime, onValueChange = { wTime = it }, label = { Text("Temps TG") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = DarkPanel2, unfocusedContainerColor = DarkPanel2, focusedTextColor = InkText, unfocusedTextColor = InkText))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = wSpeed, onValueChange = { wSpeed = it }, label = { Text("Vitesse") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = DarkPanel2, unfocusedContainerColor = DarkPanel2, focusedTextColor = InkText, unfocusedTextColor = InkText))
                                OutlinedTextField(value = wCouloir, onValueChange = { wCouloir = it }, label = { Text("Couloir") }, modifier = Modifier.weight(1f), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = DarkPanel2, unfocusedContainerColor = DarkPanel2, focusedTextColor = InkText, unfocusedTextColor = InkText))
                            }
                        }
                        EpreuveType.ECO_DIST -> {
                            OutlinedTextField(value = refDmax, onValueChange = { refDmax = it }, label = { Text("Distance max Dmax (m, optionnel)") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = DarkPanel2, unfocusedContainerColor = DarkPanel2, focusedTextColor = InkText, unfocusedTextColor = InkText))
                            OutlinedTextField(value = refTmax, onValueChange = { refTmax = it }, label = { Text("Temps max Tmax (s, optionnel)") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = DarkPanel2, unfocusedContainerColor = DarkPanel2, focusedTextColor = InkText, unfocusedTextColor = InkText))
                        }
                        EpreuveType.ECO_PURE -> {
                            OutlinedTextField(value = refTmax, onValueChange = { refTmax = it }, label = { Text("Temps max Tmax (s, optionnel)") }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = DarkPanel2, unfocusedContainerColor = DarkPanel2, focusedTextColor = InkText, unfocusedTextColor = InkText))
                        }
                    }
                }
            }
        }

        // TG Declarations Card if Precision task
        if (selectedEpreuve == EpreuveType.PRECISION && tgPoints.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkPanel),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkLine)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("DÉCLARATION DES TEMPS (TG / FP)", color = ColorTG, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Saisis les temps estimés en secondes depuis SP pour chaque porte (TG, FP...).", color = InkDim, fontSize = 10.sp)

                        tgPoints.forEachIndexed { index, tgPt ->
                            OutlinedTextField(
                                value = tgDeclarations[tgPt.id] ?: "",
                                onValueChange = { tgDeclarations[tgPt.id] = it },
                                label = { Text("${tgPt.type.uppercase()} ${tgPt.id} (secondes)") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = DarkPanel2, unfocusedContainerColor = DarkPanel2, focusedTextColor = InkText, unfocusedTextColor = InkText)
                            )
                        }
                    }
                }
            }
        }

        // Analyze Button
        item {
            Button(
                onClick = {
                    val ref = ScoringRef(
                        maxTimeMin = refMaxTimeMin.toDoubleOrNull(),
                        nbmax = refNbmax.toDoubleOrNull(),
                        tmin = refTmin.toDoubleOrNull(),
                        dmax = refDmax.toDoubleOrNull(),
                        tmax = refTmax.toDoubleOrNull(),
                        wGates = wGates.toDoubleOrNull() ?: 600.0,
                        wTime = wTime.toDoubleOrNull() ?: 300.0,
                        wSpeed = wSpeed.toDoubleOrNull() ?: 100.0,
                        wCouloir = wCouloir.toDoubleOrNull() ?: 0.0
                    )
                    val declMap = tgDeclarations.mapValues { it.value.toDoubleOrNull() ?: -1.0 }
                    onAnalyzeFlight(selectedEpreuve, ref, declMap)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SkyDim, contentColor = InkText)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("▶ Analyser le vol", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        // Results Section
        flightResult?.let { result ->
            if (result.error != null) {
                item {
                    Surface(color = RedAlert.copy(alpha = 0.2f), border = androidx.compose.foundation.BorderStroke(1.dp, RedAlert), shape = MaterialTheme.shapes.small) {
                        Text(result.error, color = RedAlert, modifier = Modifier.padding(10.dp), fontSize = 12.sp)
                    }
                }
            } else {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkPanel2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SkyDim)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(result.score.toString(), color = SkyBlue, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                            Text(result.label, color = InkDim, fontSize = 11.sp)
                            if (result.bannerTxt.isNotBlank()) {
                                Surface(
                                    color = AmberAccent.copy(alpha = 0.1f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberAccent.copy(alpha = 0.3f)),
                                    shape = MaterialTheme.shapes.extraSmall,
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text(result.bannerTxt, color = AmberAccent, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                                }
                            }
                        }
                    }
                }

                // Score Breakdown
                result.breakdown?.let { bd ->
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = DarkPanel), border = androidx.compose.foundation.BorderStroke(1.dp, DarkLine)) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("DÉTAIL DU SCORE", color = SkyBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                bd.forEach { (k, v) ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(k, color = InkDim, fontSize = 11.sp)
                                        Text("$v pts", color = InkText, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Point validation list
                item {
                    Text("DÉTAIL PAR POINT (${result.results.size})", color = SkyBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                itemsIndexed(result.results) { index, pRes ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkPanel2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkLine),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("#${index + 1}", color = InkDim, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text(PointType.fromCode(pRes.point.type).label, color = InkText, fontSize = 12.sp)
                            }
                            Text(
                                if (pRes.validated) "✔ Validé" else "✕ Non franchi",
                                color = if (pRes.validated) GreenOk else RedAlert,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = onSaveToHistory,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = InkText)
                    ) {
                        Icon(Icons.Default.Bookmark, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📌 Enregistrer dans l'historique")
                    }
                }
            }
        }

        // History Table
        if (history.isNotEmpty()) {
            item {
                Text("HISTORIQUE DES VOLS (${history.size})", color = SkyBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            itemsIndexed(history) { _, item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkPanel2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkLine)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(item.dateIso.take(10), color = InkDim, fontSize = 10.sp)
                            Text(item.epreuveType, color = InkText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text("${item.score} pts", color = SkyBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.util.GeometryUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompetitionTab(
    competition: CompetitionData,
    savedCompetitions: List<Pair<String, String>>, // slug to name
    currentCompSlug: String?,
    savedCourses: List<Pair<String, String>>, // slug to name
    onCompNameChanged: (String) -> Unit,
    onSaveComp: () -> Unit,
    onLoadComp: (String) -> Unit,
    onDeleteComp: (String) -> Unit,
    onExportCompJson: () -> Unit,
    onAddCompetitor: (String) -> Unit,
    onRemoveCompetitor: (String) -> Unit,
    onAddManche: (name: String, courseSlug: String, epreuveType: EpreuveType) -> Unit,
    onDeleteManche: (String) -> Unit,
    onSimulateCompetitorFlight: (mancheId: String, competitorId: String, speedKmh: Double) -> Unit,
    onImportCompetitorGpx: (mancheId: String, competitorId: String) -> Unit,
    onExportRankingCsv: () -> Unit
) {
    var newCompetitorName by remember { mutableStateOf("") }
    var newMancheName by remember { mutableStateOf("") }
    var selectedCourseSlug by remember { mutableStateOf("") }
    var selectedEpreuve by remember { mutableStateOf(EpreuveType.PURE) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Card 1: Competition Name & Storage
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkLine)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("NOM DE LA COMPÉTITION", color = InkDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = competition.name,
                        onValueChange = onCompNameChanged,
                        placeholder = { Text("ex : Championnat régional 2026") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkPanel2, unfocusedContainerColor = DarkPanel2, focusedTextColor = InkText, unfocusedTextColor = InkText
                        )
                    )

                    Button(
                        onClick = onSaveComp,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SkyDim, contentColor = InkText)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enregistrer la compétition")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var exp by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = !exp }, modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = savedCompetitions.find { it.first == currentCompSlug }?.second ?: "— Charger —",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exp) },
                                modifier = Modifier.menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = DarkPanel2, unfocusedContainerColor = DarkPanel2, focusedTextColor = InkText, unfocusedTextColor = InkText)
                            )
                            ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }, modifier = Modifier.background(DarkPanel2)) {
                                savedCompetitions.forEach { (slug, name) ->
                                    DropdownMenuItem(
                                        text = { Text(name, color = InkText) },
                                        onClick = {
                                            onLoadComp(slug)
                                            exp = false
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(onClick = { currentCompSlug?.let { onDeleteComp(it) } }, enabled = currentCompSlug != null) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = RedAlert)
                        }
                    }

                    OutlinedButton(onClick = onExportCompJson, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exporter Compétition (.JSON)")
                    }
                }
            }
        }

        // Card 2: Competitors
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkLine)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("CONCURRENTS (${competition.competitors.size})", color = SkyBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newCompetitorName,
                            onValueChange = { newCompetitorName = it },
                            placeholder = { Text("Nom du pilote") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = DarkPanel2, unfocusedContainerColor = DarkPanel2, focusedTextColor = InkText, unfocusedTextColor = InkText)
                        )
                        Button(
                            onClick = {
                                if (newCompetitorName.isNotBlank()) {
                                    onAddCompetitor(newCompetitorName.trim())
                                    newCompetitorName = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SkyDim, contentColor = InkText)
                        ) {
                            Text("+ Ajouter")
                        }
                    }

                    competition.competitors.forEach { comp ->
                        Row(
                            modifier = Modifier.fillMaxWidth().background(DarkPanel2, shape = MaterialTheme.shapes.extraSmall).padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(comp.name, color = InkText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = { onRemoveCompetitor(comp.id) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = RedAlert)
                            }
                        }
                    }
                }
            }
        }

        // Card 3: Add Manche
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkLine)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("AJOUTER UNE MANCHE", color = SkyBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = newMancheName,
                        onValueChange = { newMancheName = it },
                        label = { Text("Nom de la manche") },
                        placeholder = { Text("ex : Manche 1 - Navigation pure") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = DarkPanel2, unfocusedContainerColor = DarkPanel2, focusedTextColor = InkText, unfocusedTextColor = InkText)
                    )

                    Text("PARCOURS ASSOCIÉ", color = InkDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    var expCourse by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expCourse, onExpandedChange = { expCourse = !expCourse }) {
                        OutlinedTextField(
                            value = savedCourses.find { it.first == selectedCourseSlug }?.second ?: "— Choisir un parcours —",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expCourse) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = DarkPanel2, unfocusedContainerColor = DarkPanel2, focusedTextColor = InkText, unfocusedTextColor = InkText)
                        )
                        ExposedDropdownMenu(expanded = expCourse, onDismissRequest = { expCourse = false }, modifier = Modifier.background(DarkPanel2)) {
                            savedCourses.forEach { (slug, name) ->
                                DropdownMenuItem(
                                    text = { Text(name, color = InkText) },
                                    onClick = {
                                        selectedCourseSlug = slug
                                        expCourse = false
                                    }
                                )
                            }
                        }
                    }

                    Text("TYPE D'ÉPREUVE", color = InkDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    var expEpr by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expEpr, onExpandedChange = { expEpr = !expEpr }) {
                        OutlinedTextField(
                            value = selectedEpreuve.title,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expEpr) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = DarkPanel2, unfocusedContainerColor = DarkPanel2, focusedTextColor = InkText, unfocusedTextColor = InkText)
                        )
                        ExposedDropdownMenu(expanded = expEpr, onDismissRequest = { expEpr = false }, modifier = Modifier.background(DarkPanel2)) {
                            EpreuveType.entries.forEach { ep ->
                                DropdownMenuItem(
                                    text = { Text(ep.title, color = InkText) },
                                    onClick = {
                                        selectedEpreuve = ep
                                        expEpr = false
                                    }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (newMancheName.isNotBlank() && selectedCourseSlug.isNotBlank()) {
                                onAddManche(newMancheName.trim(), selectedCourseSlug, selectedEpreuve)
                                newMancheName = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SkyDim, contentColor = InkText)
                    ) {
                        Text("+ Ajouter la manche", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Manches List
        if (competition.manches.isNotEmpty()) {
            item {
                Text("MANCHES DE LA COMPÉTITION", color = SkyBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            itemsIndexed(competition.manches) { _, manche ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkPanel2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkLine),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(manche.name, color = InkText, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${manche.courseLabel} · ${EpreuveType.fromCode(manche.epreuveTypeCode).title}", color = InkDim, fontSize = 10.sp)
                            }
                            IconButton(onClick = { onDeleteManche(manche.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = RedAlert)
                            }
                        }

                        val n = competition.competitors.size
                        val rankedIds = competition.competitors
                            .mapNotNull { cc -> manche.results[cc.id]?.let { r -> Pair(cc.id, r.score) } }
                            .sortedByDescending { it.second }
                            .map { it.first }

                        competition.competitors.forEach { c ->
                            val r = manche.results[c.id]
                            val rankIndex = rankedIds.indexOf(c.id)
                            val rank = if (rankIndex >= 0) rankIndex + 1 else null
                            val pts = if (rank != null) GeometryUtils.championshipPoints(rank, n) else 0

                            Row(
                                modifier = Modifier.fillMaxWidth().background(DarkPanel, shape = MaterialTheme.shapes.extraSmall).padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(c.name, color = InkText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(
                                        if (r != null) "Score: ${r.score} pts · Rang: ${rank ?: "—"} · Pts Barème: $pts" else "Non évalué",
                                        color = if (r != null) GreenOk else InkDim,
                                        fontSize = 10.sp
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OutlinedButton(onClick = { onSimulateCompetitorFlight(manche.id, c.id, 40.0) }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)) {
                                        Text("🖱️ Simuler", fontSize = 10.sp)
                                    }
                                    Button(onClick = { onImportCompetitorGpx(manche.id, c.id) }, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp), colors = ButtonDefaults.buttonColors(containerColor = SkyDim)) {
                                        Text("GPX", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // General Classification Ranking
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkLine)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("CLASSEMENT GÉNÉRAL (BARÈME FFPLUM)", color = SkyBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    val n = competition.competitors.size
                    val totals = competition.competitors.map { c ->
                        var total = 0
                        competition.manches.forEach { manche ->
                            val rankedIds = competition.competitors
                                .mapNotNull { cc -> manche.results[cc.id]?.let { r -> Pair(cc.id, r.score) } }
                                .sortedByDescending { it.second }
                                .map { it.first }
                            val rankIndex = rankedIds.indexOf(c.id)
                            if (rankIndex >= 0) {
                                total += GeometryUtils.championshipPoints(rankIndex + 1, n)
                            }
                        }
                        Pair(c.name, total)
                    }.sortedByDescending { it.second }

                    totals.forEachIndexed { i, (name, pts) ->
                        val medalColor = when (i) {
                            0 -> Color(0xFFFFD54A)
                            1 -> Color(0xFFC9D3DA)
                            2 -> Color(0xFFC9863F)
                            else -> InkDim
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("#${i + 1}", color = medalColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(name, color = InkText, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            }
                            Text("$pts pts", color = SkyBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        if (i < totals.size - 1) HorizontalDivider(color = DarkLine)
                    }

                    OutlinedButton(onClick = onExportRankingCsv, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exporter Classement (CSV)")
                    }
                }
            }
        }
    }
}

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
import com.example.data.model.CourseData
import com.example.data.model.CoursePoint
import com.example.data.model.PointType
import com.example.ui.theme.*
import com.example.util.GeometryUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseTab(
    courseData: CourseData,
    savedCourses: List<Pair<String, String>>, // slug to name
    currentSlug: String?,
    toolMode: MapToolMode,
    addPointType: String,
    tileProvider: MapTileProvider,
    onCourseNameChanged: (String) -> Unit,
    onSaveCourse: () -> Unit,
    onLoadCourse: (String) -> Unit,
    onDeleteCourse: (String) -> Unit,
    onExportCourseJson: () -> Unit,
    onImportCourseJson: (String) -> Unit,
    onToolModeSelected: (MapToolMode) -> Unit,
    onAddPointTypeSelected: (String) -> Unit,
    onTileProviderSelected: (MapTileProvider) -> Unit,
    onCorridorWidthChanged: (Double) -> Unit,
    onUndoLastVertex: () -> Unit,
    onClearCorridor: () -> Unit,
    onClearAll: () -> Unit,
    onPointTypeChanged: (String, String) -> Unit,
    onPointDimensionChanged: (String, Double) -> Unit,
    onMovePoint: (String, Int) -> Unit, // -1 up, +1 down
    onDeletePoint: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Card 1: Course Name & Storage
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkLine)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("NOM DU PARCOURS", color = InkDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = courseData.name,
                        onValueChange = onCourseNameChanged,
                        placeholder = { Text("ex : Manche C2 - Snake") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkPanel2,
                            unfocusedContainerColor = DarkPanel2,
                            focusedBorderColor = SkyBlue,
                            unfocusedBorderColor = DarkLine,
                            focusedTextColor = InkText,
                            unfocusedTextColor = InkText
                        )
                    )

                    Button(
                        onClick = onSaveCourse,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SkyDim, contentColor = InkText)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enregistrer")
                    }

                    // Load / Delete row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = savedCourses.find { it.first == currentSlug }?.second ?: "— Charger —",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkPanel2,
                                    unfocusedContainerColor = DarkPanel2,
                                    focusedBorderColor = DarkLine,
                                    unfocusedBorderColor = DarkLine,
                                    focusedTextColor = InkText,
                                    unfocusedTextColor = InkText
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.background(DarkPanel2)
                            ) {
                                savedCourses.forEach { (slug, name) ->
                                    DropdownMenuItem(
                                        text = { Text(name, color = InkText) },
                                        onClick = {
                                            onLoadCourse(slug)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { currentSlug?.let { onDeleteCourse(it) } },
                            enabled = currentSlug != null
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = RedAlert)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onExportCourseJson,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = InkText)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exporter JSON", fontSize = 11.sp)
                        }
                    }
                    Text(
                        "« Enregistrer » garde le parcours localement. « Exporter » télécharge un fichier .json à partager.",
                        color = InkDim,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Card 2: Fond de carte
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkLine)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("FOND DE CARTE", color = InkDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    MapTileProvider.entries.forEach { provider ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            RadioButton(
                                selected = tileProvider == provider,
                                onClick = { onTileProviderSelected(provider) },
                                colors = RadioButtonDefaults.colors(selectedColor = SkyBlue)
                            )
                            Text(provider.label, color = InkText, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Card 3: Interactive Editing Tools
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkLine)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("OUTILS D'ÉDITION", color = SkyBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                    ToolButtonRow(
                        buttons = listOf(
                            Pair("🖐 Naviguer", MapToolMode.NAVIGATE),
                            Pair("📍 Placer porte/balise", MapToolMode.ADD_POINT)
                        ),
                        activeMode = toolMode,
                        onSelect = onToolModeSelected
                    )

                    ToolButtonRow(
                        buttons = listOf(
                            Pair("🖊 Couloir point/point", MapToolMode.ADD_ROUTE_VERTEX),
                            Pair("✏️ Couloir main levée", MapToolMode.DRAW_ROUTE)
                        ),
                        activeMode = toolMode,
                        onSelect = onToolModeSelected
                    )

                    ToolButtonRow(
                        buttons = listOf(
                            Pair("➕ Insérer point couloir", MapToolMode.INSERT_VERTEX),
                            Pair("🗑 Supprimer (clic)", MapToolMode.DELETE_ITEM)
                        ),
                        activeMode = toolMode,
                        onSelect = onToolModeSelected
                    )

                    ToolButtonRow(
                        buttons = listOf(
                            Pair("🎨 Courbe/droite sur point", MapToolMode.TOGGLE_SMOOTH)
                        ),
                        activeMode = toolMode,
                        onSelect = onToolModeSelected
                    )

                    if (toolMode == MapToolMode.ADD_POINT) {
                        Surface(
                            color = DarkPanel2,
                            shape = MaterialTheme.shapes.small,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AmberAccent)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("👉 Prochain point à placer sur la carte", color = AmberAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                var expType by remember { mutableStateOf(false) }
                                ExposedDropdownMenuBox(expanded = expType, onExpandedChange = { expType = !expType }) {
                                    OutlinedTextField(
                                        value = PointType.fromCode(addPointType).label,
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expType) },
                                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = DarkBg,
                                            unfocusedContainerColor = DarkBg,
                                            focusedTextColor = InkText,
                                            unfocusedTextColor = InkText
                                        )
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expType,
                                        onDismissRequest = { expType = false },
                                        modifier = Modifier.background(DarkPanel2)
                                    ) {
                                        PointType.entries.forEach { pType ->
                                            DropdownMenuItem(
                                                text = { Text(pType.label, color = InkText) },
                                                onClick = {
                                                    onAddPointTypeSelected(pType.name.lowercase())
                                                    expType = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        "Points et couloir déplaçables au doigt. Par défaut le couloir relie les points en ligne droite ; 'Courbe/droite' arrondit certains virages.",
                        color = InkDim,
                        fontSize = 10.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(onClick = onUndoLastVertex, modifier = Modifier.weight(1f)) {
                            Text("↺ Annuler dernier point", fontSize = 10.sp)
                        }
                        OutlinedButton(onClick = onClearCorridor, modifier = Modifier.weight(1f)) {
                            Text("🧹 Effacer couloir", fontSize = 10.sp)
                        }
                    }
                    Button(
                        onClick = onClearAll,
                        colors = ButtonDefaults.buttonColors(containerColor = RedAlert),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("✕ Tout effacer")
                    }
                }
            }
        }

        // Card 4: Largeur couloir & Course length
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkLine)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("LARGEUR DU COULOIR (M)", color = InkDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = courseData.corridorWidth.toInt().toString(),
                        onValueChange = { v ->
                            v.toDoubleOrNull()?.let { onCorridorWidthChanged(it) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkPanel2,
                            unfocusedContainerColor = DarkPanel2,
                            focusedTextColor = InkText,
                            unfocusedTextColor = InkText
                        )
                    )
                    val km = GeometryUtils.courseLengthKm(courseData)
                    Text("Longueur du parcours : ${if (km > 0) String.format("%.2f km", km) else "—"}", color = InkText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Points list header
        item {
            Text("POINTS DU PARCOURS (${courseData.points.size})", color = SkyBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        if (courseData.points.isEmpty()) {
            item {
                Text("Aucun point placé.", color = InkDim, fontSize = 11.sp)
            }
        } else {
            itemsIndexed(courseData.points) { idx, p ->
                PointItemCard(
                    index = idx + 1,
                    point = p,
                    onMoveUp = { onMovePoint(p.id, -1) },
                    onMoveDown = { onMovePoint(p.id, 1) },
                    onDelete = { onDeletePoint(p.id) },
                    onTypeChanged = { newType -> onPointTypeChanged(p.id, newType) },
                    onDimensionChanged = { newDim -> onPointDimensionChanged(p.id, newDim) }
                )
            }
        }
    }
}

@Composable
private fun ToolButtonRow(
    buttons: List<Pair<String, MapToolMode>>,
    activeMode: MapToolMode,
    onSelect: (MapToolMode) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        buttons.forEach { (text, mode) ->
            val isActive = activeMode == mode
            Button(
                onClick = { onSelect(mode) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive) AmberAccent else SkyDim,
                    contentColor = if (isActive) DarkBg else InkText
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PointItemCard(
    index: Int,
    point: CoursePoint,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onTypeChanged: (String) -> Unit,
    onDimensionChanged: (Double) -> Unit
) {
    val isCircle = point.type == "balise" || point.type == "cachee"
    val color = when (point.type.uppercase()) {
        "SP" -> ColorSP
        "FP" -> ColorFP
        "PORTE" -> ColorPorte
        "TG" -> ColorTG
        "BALISE" -> ColorBalise
        "CACHEE" -> ColorCachee
        else -> ColorBalise
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkPanel2),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkLine),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = color, shape = MaterialTheme.shapes.extraSmall) {
                        Text(index.toString(), color = DarkBg, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                    Text(PointType.fromCode(point.type).label, color = InkText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Row {
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Monter", tint = InkDim)
                    }
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Descendre", tint = InkDim)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Supprimer", tint = RedAlert)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                var exp by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = exp, onExpandedChange = { exp = !exp }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = PointType.fromCode(point.type).label,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = exp) },
                        modifier = Modifier.menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkPanel,
                            unfocusedContainerColor = DarkPanel,
                            focusedTextColor = InkText,
                            unfocusedTextColor = InkText
                        )
                    )
                    ExposedDropdownMenu(expanded = exp, onDismissRequest = { exp = false }, modifier = Modifier.background(DarkPanel2)) {
                        PointType.entries.forEach { pType ->
                            DropdownMenuItem(
                                text = { Text(pType.label, color = InkText) },
                                onClick = {
                                    onTypeChanged(pType.name.lowercase())
                                    exp = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = (if (isCircle) point.radius else point.width).toInt().toString(),
                    onValueChange = { v -> v.toDoubleOrNull()?.let { onDimensionChanged(it) } },
                    label = { Text(if (isCircle) "Rayon (m)" else "Largeur (m)", fontSize = 10.sp) },
                    modifier = Modifier.width(110.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkPanel,
                        unfocusedContainerColor = DarkPanel,
                        focusedTextColor = InkText,
                        unfocusedTextColor = InkText
                    )
                )
            }
        }
    }
}

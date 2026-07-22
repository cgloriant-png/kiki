package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CourseData
import com.example.ui.theme.*
import com.example.util.GeometryUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintTab(
    courseData: CourseData,
    onCleanMapModeToggled: (Boolean) -> Unit
) {
    var scale by remember { mutableStateOf("100000") }
    var marginPct by remember { mutableStateOf("15") }
    var includeTrace by remember { mutableStateOf(true) }
    var showMarkers by remember { mutableStateOf(false) }

    val courseKm = GeometryUtils.courseLengthKm(courseData)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkLine)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("IMPRESSION CARTE VECTORIELLE À L'ÉCHELLE EXACTE", color = SkyBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Génère une grille kilométrique et une échelle graphique ultra-précise pour les pilotes.", color = InkDim, fontSize = 11.sp)

                    Text("ÉCHELLE", color = InkDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    var expScale by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expScale, onExpandedChange = { expScale = !expScale }) {
                        OutlinedTextField(
                            value = "1:$scale",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expScale) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = DarkPanel2, unfocusedContainerColor = DarkPanel2, focusedTextColor = InkText, unfocusedTextColor = InkText)
                        )
                        ExposedDropdownMenu(expanded = expScale, onDismissRequest = { expScale = false }, modifier = Modifier.background(DarkPanel2)) {
                            listOf("10000", "25000", "50000", "100000", "200000").forEach { sc ->
                                DropdownMenuItem(
                                    text = { Text("1:$sc", color = InkText) },
                                    onClick = {
                                        scale = sc
                                        expScale = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = marginPct,
                        onValueChange = { marginPct = it },
                        label = { Text("Marge autour du parcours (%)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = DarkPanel2, unfocusedContainerColor = DarkPanel2, focusedTextColor = InkText, unfocusedTextColor = InkText)
                    )

                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = includeTrace, onCheckedChange = { includeTrace = it }, colors = CheckboxDefaults.colors(checkedColor = SkyBlue))
                        Text("Inclure la trace GPS", color = InkText, fontSize = 12.sp)
                    }

                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = showMarkers, onCheckedChange = { showMarkers = it }, colors = CheckboxDefaults.colors(checkedColor = SkyBlue))
                        Text("Afficher les repères numérotés", color = InkText, fontSize = 12.sp)
                    }

                    Text("Infos parcours : ${if (courseKm > 0) String.format("%.2f km", courseKm) else "Parcours non tracé"}", color = InkText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkPanel),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkLine)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MODE CARTE ÉPURÉE (PRÊT POUR IMPRESSION / CONCURRENTS)", color = SkyBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Masque les points d'édition pour afficher uniquement le couloir, les portes et les balises.", color = InkDim, fontSize = 11.sp)

                    var isClean by remember { mutableStateOf(false) }
                    Button(
                        onClick = {
                            isClean = !isClean
                            onCleanMapModeToggled(isClean)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isClean) AmberAccent else SkyDim, contentColor = if (isClean) DarkBg else InkText),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isClean) "🖨️ Mode épuré actif (Cliquer pour désactiver)" else "🖨️ Activer le mode carte épurée")
                    }
                }
            }
        }
    }
}

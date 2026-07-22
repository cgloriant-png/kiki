package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CoursePenalties
import com.example.ui.theme.*

@Composable
fun PenaltiesTab(
    penalties: CoursePenalties,
    onPenaltiesChanged: (CoursePenalties) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            color = AmberAccent.copy(alpha = 0.1f),
            shape = MaterialTheme.shapes.small,
            border = androidx.compose.foundation.BorderStroke(1.dp, AmberAccent.copy(alpha = 0.4f))
        ) {
            Text(
                "Ces règles sont enregistrées avec le parcours (onglet Parcours → Enregistrer) et s'appliquent à chaque correction, solo ou en compétition.",
                color = AmberAccent,
                fontSize = 11.sp,
                modifier = Modifier.padding(10.dp)
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkPanel),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkLine)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = penalties.requireSP,
                        onCheckedChange = { onPenaltiesChanged(penalties.copy(requireSP = it)) },
                        colors = CheckboxDefaults.colors(checkedColor = SkyBlue)
                    )
                    Text("Porte d'entrée (SP) obligatoire — 0 point si non franchie", color = InkText, fontSize = 12.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = penalties.requireFP,
                        onCheckedChange = { onPenaltiesChanged(penalties.copy(requireFP = it)) },
                        colors = CheckboxDefaults.colors(checkedColor = SkyBlue)
                    )
                    Text("Porte de sortie (FP) obligatoire — 0 point si non franchie", color = InkText, fontSize = 12.sp)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkPanel),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkLine)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = penalties.noBacktrack,
                        onCheckedChange = { onPenaltiesChanged(penalties.copy(noBacktrack = it)) },
                        colors = CheckboxDefaults.colors(checkedColor = SkyBlue)
                    )
                    Text("Interdire le retour en arrière (backtracking) dans le couloir — 0 point si détecté", color = InkText, fontSize = 12.sp)
                }

                Text("ANGLE MINIMUM AUTORISÉ SUR LA TRACE (°)", color = InkDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = penalties.backtrackAngleDeg.toInt().toString(),
                    onValueChange = { v ->
                        v.toDoubleOrNull()?.let { onPenaltiesChanged(penalties.copy(backtrackAngleDeg = it)) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkPanel2,
                        unfocusedContainerColor = DarkPanel2,
                        focusedTextColor = InkText,
                        unfocusedTextColor = InkText
                    )
                )
                Text(
                    "Si la trace forme, à l'intérieur du couloir, un angle plus serré que cette valeur entre deux segments consécutifs, c'est considéré comme un retour en arrière.",
                    color = InkDim,
                    fontSize = 10.sp
                )
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircleOutline
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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SchoolOverviewScreen(
    students: List<StudentEntity>,
    slotsWithBookings: List<SlotWithBookings>,
    onToggleAttendance: (Long, Long, Boolean) -> Unit,
    onOpenWhatsAppShare: () -> Unit,
    onQuickGenerateWeekend: () -> Unit,
    onOpenAddSlot: () -> Unit,
    onOpenAddStudent: () -> Unit,
    onSelectStudent: (StudentEntity) -> Unit
) {
    val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(Date()) }
    val todaySlots = remember(slotsWithBookings, todayIso) {
        slotsWithBookings.filter { it.slot.dateIso == todayIso }
    }

    val totalBookings = remember(slotsWithBookings) {
        slotsWithBookings.sumOf { it.confirmedBookings.size }
    }
    val totalCapacity = remember(slotsWithBookings) {
        slotsWithBookings.sumOf { it.slot.maxCapacity }
    }
    val occupancyRate = if (totalCapacity > 0) (totalBookings * 100) / totalCapacity else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HighDensityBg)
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Quick Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: Students
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
                border = BorderStroke(1.dp, BorderOutline.copy(alpha = 0.5f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("👥", fontSize = 20.sp)
                    Text("${students.size}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PrimaryBlueDark)
                    Text("Élèves Actifs", fontSize = 10.sp, color = SecondaryText)
                }
            }

            // Card 2: Slots
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
                border = BorderStroke(1.dp, BorderOutline.copy(alpha = 0.5f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📅", fontSize = 20.sp)
                    Text("${slotsWithBookings.size}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PrimaryBlueDark)
                    Text("Créneaux", fontSize = 10.sp, color = SecondaryText)
                }
            }

            // Card 3: Remplissage
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
                border = BorderStroke(1.dp, BorderOutline.copy(alpha = 0.5f)),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎯", fontSize = 20.sp)
                    Text("$occupancyRate%", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = GreenSuccess)
                    Text("Remplissage", fontSize = 10.sp, color = SecondaryText)
                }
            }
        }

        // Today's Flight Check-in Card (Feuille d'émargement du jour)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
            border = BorderStroke(1.dp, BorderOutline.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("📋", fontSize = 16.sp)
                        Text(
                            "Vols du Jour & Émargement",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = HighDensityHeaderTitle
                        )
                    }
                    Surface(
                        color = PrimaryBlueContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "${todaySlots.size} session(s)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlueDark,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (todaySlots.isEmpty()) {
                    Surface(
                        color = HighDensityNavBar,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Aucun créneau programmé pour aujourd'hui. Créez-en un ou profitez du briefing météo !",
                            fontSize = 11.sp,
                            color = SecondaryText,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                } else {
                    todaySlots.forEach { slotItem ->
                        val slot = slotItem.slot
                        val type = ParamoteurLessonType.fromCode(slot.lessonType)

                        Surface(
                            color = HighDensityNavBar,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "${slot.startTime} - ${slot.endTime} • ${type.emoji} ${slot.title}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = HighDensityHeaderTitle
                                    )
                                    Text(
                                        "${slotItem.confirmedBookings.size}/${slot.maxCapacity}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = PrimaryBlueDark
                                    )
                                }

                                if (slotItem.confirmedBookings.isEmpty()) {
                                    Text(
                                        "Aucun élève inscrit sur ce créneau.",
                                        fontSize = 11.sp,
                                        color = SecondaryText,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    slotItem.confirmedBookings.forEach { bws ->
                                        val s = bws.student
                                        val b = bws.booking
                                        Surface(
                                            color = if (b.attended) GreenSuccessBg else HighDensitySurface,
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(0.5.dp, if (b.attended) GreenSuccess else BorderOutline),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp)
                                                .clickable { onToggleAttendance(b.id, s.id, b.attended) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (b.attended) Icons.Default.CheckCircle else Icons.Outlined.CheckCircleOutline,
                                                        contentDescription = null,
                                                        tint = if (b.attended) GreenSuccess else SecondaryText,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        s.fullName,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = HighDensityHeaderTitle
                                                    )
                                                }
                                                Text(
                                                    if (b.attended) "Présent ✅" else "Pointer présence",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (b.attended) GreenSuccess else PrimaryBlue
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

        // Action Shortcuts Grid
        Text(
            "Actions Rapides École",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = HighDensityHeaderTitle
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onOpenWhatsAppShare,
                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Text("💬", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Planning WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onQuickGenerateWeekend,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+ Pack Week-End", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onOpenAddSlot,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nouveau Créneau", fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = onOpenAddStudent,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Nouvel Élève", fontSize = 11.sp)
            }
        }

        // Students by level breakdown
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
            border = BorderStroke(1.dp, BorderOutline.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "Répartition des Élèves par Niveau",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = HighDensityHeaderTitle
                )
                Spacer(modifier = Modifier.height(8.dp))

                val debutants = students.count { it.level.contains("Débutant", ignoreCase = true) }
                val grandsVols = students.count { it.level.contains("Grands Vols", ignoreCase = true) }
                val autonomes = students.count { it.level.contains("Autonome", ignoreCase = true) }
                val brevetes = students.count { it.level.contains("Breveté", ignoreCase = true) }

                LevelProgressRow("🪂 Débutants & Pente école", debutants, students.size, PrimaryBlue)
                LevelProgressRow("✈️ Premiers Grands Vols", grandsVols, students.size, AmberAccent)
                LevelProgressRow("🧭 Autonomes & Navigation", autonomes, students.size, Color(0xFF0284C7))
                LevelProgressRow("🦅 Brevetés & Perfectionnement", brevetes, students.size, GreenSuccess)
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun LevelProgressRow(label: String, count: Int, total: Int, color: Color) {
    val ratio = if (total > 0) count.toFloat() / total else 0f
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 11.sp, color = HighDensityHeaderTitle, fontWeight = FontWeight.Medium)
            Text("$count élève(s)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
        LinearProgressIndicator(
            progress = { ratio },
            color = color,
            trackColor = BorderOutline.copy(alpha = 0.2f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
                .height(4.dp)
        )
    }
}

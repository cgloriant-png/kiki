package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppUserMode
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PlanningScreen(
    slots: List<SlotWithBookings>,
    userMode: AppUserMode,
    currentStudent: StudentEntity?,
    selectedDateFilter: String?,
    onDateFilterChange: (String?) -> Unit,
    filterOnlyAvailable: Boolean,
    onToggleFilterOnlyAvailable: () -> Unit,
    filterLessonType: String?,
    onLessonTypeFilterChange: (String?) -> Unit,
    filterOnlyMyBookings: Boolean,
    onToggleFilterOnlyMyBookings: () -> Unit,
    onToggleStudentEnrollment: (SlotWithBookings, StudentEntity) -> Unit,
    onOpenInstructorEnroll: (SlotWithBookings) -> Unit,
    onInstructorUnenroll: (Long, Long) -> Unit,
    onToggleAttendance: (Long, Long, Boolean) -> Unit,
    onUpdateWeather: (LessonSlotEntity) -> Unit,
    onEditSlot: (LessonSlotEntity) -> Unit,
    onDeleteSlot: (Long) -> Unit,
    onSelectStudentToView: (StudentEntity) -> Unit,
    onOpenAddSlot: () -> Unit,
    onQuickGenerateWeekend: () -> Unit
) {
    // Group filtered slots by date
    val groupedSlots = remember(slots) {
        slots.groupBy { it.slot.dateIso }
    }

    val dateIn = remember { SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE) }
    val dateOut = remember { SimpleDateFormat("EEEE d MMMM yyyy", Locale.FRANCE) }
    val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(Date()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HighDensityBg)
    ) {
        // Date & Filter Chips Scrollable Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(HighDensitySurface)
                .padding(vertical = 8.dp)
        ) {
            // Row 1: Date Fast Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedDateFilter == "TOUS" || selectedDateFilter == null,
                    onClick = { onDateFilterChange("TOUS") },
                    label = { Text("Tous les jours", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlue,
                        selectedLabelColor = Color.White
                    )
                )

                FilterChip(
                    selected = selectedDateFilter == "TODAY",
                    onClick = { onDateFilterChange("TODAY") },
                    label = { Text("Aujourd'hui", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlue,
                        selectedLabelColor = Color.White
                    )
                )

                FilterChip(
                    selected = selectedDateFilter == "TOMORROW",
                    onClick = { onDateFilterChange("TOMORROW") },
                    label = { Text("Demain", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlue,
                        selectedLabelColor = Color.White
                    )
                )

                FilterChip(
                    selected = selectedDateFilter == "WEEK",
                    onClick = { onDateFilterChange("WEEK") },
                    label = { Text("7 prochains jours", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlue,
                        selectedLabelColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Row 2: Secondary Fast Filters (Places dispo, My bookings, Lesson types)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Filter only available places
                FilterChip(
                    selected = filterOnlyAvailable,
                    onClick = onToggleFilterOnlyAvailable,
                    label = { Text("🟢 Places dispo uniquement", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GreenSuccessBg,
                        selectedLabelColor = GreenSuccess
                    )
                )

                // Filter only my bookings (if in student mode)
                if (userMode == AppUserMode.STUDENT) {
                    FilterChip(
                        selected = filterOnlyMyBookings,
                        onClick = onToggleFilterOnlyMyBookings,
                        label = { Text("✈️ Mes réservations", fontSize = 10.sp, fontWeight = FontWeight.Medium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlueContainer,
                            selectedLabelColor = PrimaryBlueDark
                        )
                    )
                }

                // Lesson type chips
                ParamoteurLessonType.entries.forEach { type ->
                    val isSelected = filterLessonType.equals(type.code, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (isSelected) onLessonTypeFilterChange(null) else onLessonTypeFilterChange(type.code)
                        },
                        label = { Text("${type.emoji} ${type.label}", fontSize = 10.sp) }
                    )
                }
            }
        }

        Divider(color = BorderOutline.copy(alpha = 0.5f))

        // Slots Feed or Empty State
        if (groupedSlots.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = PrimaryBlueContainer.copy(alpha = 0.5f),
                        shape = CircleShape,
                        modifier = Modifier.size(70.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🪂", fontSize = 32.sp)
                        }
                    }
                    Text(
                        "Aucun créneau correspondant",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = HighDensityHeaderTitle
                    )
                    Text(
                        "Aucun créneau de vol ne correspond aux filtres actuels.",
                        fontSize = 13.sp,
                        color = SecondaryText,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    if (userMode == AppUserMode.INSTRUCTOR) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = onOpenAddSlot,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Créer un créneau de vol")
                        }

                        OutlinedButton(
                            onClick = onQuickGenerateWeekend
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = AmberAccent)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Générer les créneaux du week-end")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                groupedSlots.forEach { (dateIso, daySlots) ->
                    val dateObj = try { dateIn.parse(dateIso) } catch (e: Exception) { null }
                    val formattedDate = dateObj?.let { dateOut.format(it).replaceFirstChar { c -> c.uppercase() } } ?: dateIso
                    val isToday = dateIso == todayIso

                    item(key = "header_$dateIso") {
                        Surface(
                            color = if (isToday) PrimaryBlueContainer else HighDensityContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = if (isToday) PrimaryBlue else SecondaryText,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = formattedDate,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isToday) PrimaryBlueDark else HighDensityHeaderTitle
                                    )
                                }

                                if (isToday) {
                                    Surface(
                                        color = PrimaryBlue,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            "AUJOURD'HUI",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    Text(
                                        "${daySlots.size} créneau(x)",
                                        fontSize = 11.sp,
                                        color = SecondaryText
                                    )
                                }
                            }
                        }
                    }

                    items(daySlots, key = { it.slot.id }) { slotItem ->
                        SlotCard(
                            slotItem = slotItem,
                            userMode = userMode,
                            currentStudent = currentStudent,
                            onToggleStudentEnrollment = onToggleStudentEnrollment,
                            onOpenInstructorEnroll = onOpenInstructorEnroll,
                            onInstructorUnenroll = onInstructorUnenroll,
                            onToggleAttendance = onToggleAttendance,
                            onUpdateWeather = onUpdateWeather,
                            onEditSlot = onEditSlot,
                            onDeleteSlot = onDeleteSlot,
                            onSelectStudentToView = onSelectStudentToView
                        )
                    }
                }
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppUserMode

@Composable
fun SlotCard(
    slotItem: SlotWithBookings,
    userMode: AppUserMode,
    currentStudent: StudentEntity?,
    onToggleStudentEnrollment: (SlotWithBookings, StudentEntity) -> Unit,
    onOpenInstructorEnroll: (SlotWithBookings) -> Unit,
    onInstructorUnenroll: (Long, Long) -> Unit, // slotId, studentId
    onToggleAttendance: (Long, Long, Boolean) -> Unit, // bookingId, studentId, currentAttended
    onUpdateWeather: (LessonSlotEntity) -> Unit,
    onEditSlot: (LessonSlotEntity) -> Unit,
    onDeleteSlot: (Long) -> Unit,
    onSelectStudentToView: (StudentEntity) -> Unit
) {
    val slot = slotItem.slot
    val lessonType = ParamoteurLessonType.fromCode(slot.lessonType)
    val weather = SlotWeather.fromCode(slot.weatherStatus)
    val isCancelled = slot.isCancelled || slot.weatherStatus == "CANCELLED"

    val isCurrentStudentEnrolled = currentStudent != null && slotItem.enrolledStudentIds.contains(currentStudent.id)
    val isCurrentStudentOnWaitList = currentStudent != null && slotItem.waitingListBookings.any { it.student.id == currentStudent.id }

    var isExpanded by remember { mutableStateOf(false) }

    // Card border and container color depending on state
    val cardBorderColor = when {
        isCancelled -> RedAlert.copy(alpha = 0.5f)
        slotItem.isFull -> AmberAccent.copy(alpha = 0.4f)
        isCurrentStudentEnrolled -> PrimaryBlue
        else -> BorderOutline.copy(alpha = 0.6f)
    }

    val cardBg = when {
        isCancelled -> RedAlertBg.copy(alpha = 0.35f)
        isCurrentStudentEnrolled && userMode == AppUserMode.STUDENT -> PrimaryBlueContainer.copy(alpha = 0.25f)
        else -> HighDensitySurface
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(if (isCurrentStudentEnrolled && userMode == AppUserMode.STUDENT) 2.dp else 1.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Time window, Lesson badge & Weather chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Time window pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryBlueDark,
                    contentColor = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = Color.White
                        )
                        Text(
                            text = "${slot.startTime} - ${slot.endTime}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }

                // Lesson type badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (lessonType) {
                        ParamoteurLessonType.GONFLAGE -> Color(0xFFE0F2FE)
                        ParamoteurLessonType.GRAND_VOL -> Color(0xFFDCFCE7)
                        ParamoteurLessonType.NAVIGATION -> Color(0xFFF3E8FF)
                        ParamoteurLessonType.PRECISION -> Color(0xFFFEF3C7)
                        ParamoteurLessonType.PERFECTIONNEMENT -> Color(0xFFFFEDD5)
                        ParamoteurLessonType.THEORIE -> Color(0xFFF1F5F9)
                    },
                    border = BorderStroke(0.5.dp, PrimaryBlue.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(lessonType.emoji, fontSize = 12.sp)
                        Text(
                            text = lessonType.label,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            color = HighDensityHeaderTitle
                        )
                    }
                }

                // Weather badge (clickable for instructor)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (weather) {
                        SlotWeather.OPTIMAL -> GreenSuccessBg
                        SlotWeather.TO_CONFIRM -> Color(0xFFFEF9C3)
                        SlotWeather.CANCELLED -> RedAlertBg
                        SlotWeather.COMPLETED -> HighDensityContainer
                    },
                    border = BorderStroke(0.5.dp, when (weather) {
                        SlotWeather.OPTIMAL -> GreenSuccess
                        SlotWeather.TO_CONFIRM -> AmberAccent
                        SlotWeather.CANCELLED -> RedAlert
                        SlotWeather.COMPLETED -> BorderOutline
                    }),
                    modifier = Modifier.then(
                        if (userMode == AppUserMode.INSTRUCTOR) {
                            Modifier.clickable { onUpdateWeather(slot) }
                        } else Modifier
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(weather.iconEmoji, fontSize = 10.sp)
                        Text(
                            text = if (userMode == AppUserMode.INSTRUCTOR) "${weather.label} ▾" else weather.label,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (weather) {
                                SlotWeather.OPTIMAL -> GreenSuccess
                                SlotWeather.TO_CONFIRM -> AmberAccent
                                SlotWeather.CANCELLED -> RedAlert
                                SlotWeather.COMPLETED -> SecondaryText
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title & Location
            Text(
                text = slot.title,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = HighDensityHeaderTitle,
                textDecoration = if (isCancelled) TextDecoration.LineThrough else TextDecoration.None
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = SecondaryText
                )
                Text(
                    text = slot.location,
                    fontSize = 11.sp,
                    color = SecondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (slot.windInfo.isNotBlank()) {
                    Text("•", fontSize = 11.sp, color = BorderOutline)
                    Text(
                        text = "💨 ${slot.windInfo}",
                        fontSize = 11.sp,
                        color = PrimaryBlueDark,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (slot.instructorNotes.isNotBlank()) {
                Surface(
                    color = HighDensityNavBar,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(12.dp), tint = PrimaryBlue)
                        Text(
                            text = slot.instructorNotes,
                            fontSize = 11.sp,
                            color = SecondaryText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Places Gauge & Capacity Bar
            val confirmedCount = slotItem.confirmedBookings.size
            val maxCap = slot.maxCapacity
            val ratio = if (maxCap > 0) (confirmedCount.toFloat() / maxCap).coerceIn(0f, 1f) else 0f

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "$confirmedCount / $maxCap places",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (slotItem.isFull) RedAlert else PrimaryBlueDark
                    )

                    if (isCancelled) {
                        Surface(
                            color = RedAlertBg,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "ANNULÉ MÉTÉO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = RedAlert,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    } else if (slotItem.isFull) {
                        Surface(
                            color = RedAlertBg,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "COMPLET",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = RedAlert,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    } else {
                        Surface(
                            color = GreenSuccessBg,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "${slotItem.availablePlaces} DISPO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GreenSuccess,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (slotItem.waitingListBookings.isNotEmpty()) {
                    Text(
                        text = "⏳ ${slotItem.waitingListBookings.size} en attente",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AmberAccent
                    )
                }
            }

            // Visual linear gauge
            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = when {
                    isCancelled -> RedAlert
                    slotItem.isFull -> RedAlert
                    ratio > 0.65f -> AmberAccent
                    else -> GreenSuccess
                },
                trackColor = BorderOutline.copy(alpha = 0.3f)
            )

            // Inscrits list / Student Avatar Chips
            if (slotItem.confirmedBookings.isNotEmpty() || slotItem.waitingListBookings.isNotEmpty()) {
                Text(
                    text = "Élèves inscrits :",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SecondaryText,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )

                // Chips container
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        slotItem.confirmedBookings.forEach { bookingWithStudent ->
                            val s = bookingWithStudent.student
                            val b = bookingWithStudent.booking

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (b.attended) GreenSuccessBg else HighDensityNavBar,
                                border = BorderStroke(1.dp, if (b.attended) GreenSuccess else BorderOutline.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                        .clickable { onSelectStudentToView(s) },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = PrimaryBlue,
                                            shape = CircleShape,
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(s.initials, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                        Column {
                                            Text(
                                                s.fullName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = HighDensityHeaderTitle
                                            )
                                            Text(
                                                s.level.take(28) + if (s.level.length > 28) "…" else "",
                                                fontSize = 9.sp,
                                                color = SecondaryText
                                            )
                                        }
                                    }

                                    if (userMode == AppUserMode.INSTRUCTOR) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            // Attendance toggle
                                            IconButton(
                                                onClick = { onToggleAttendance(b.id, s.id, b.attended) },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (b.attended) Icons.Default.CheckCircle else Icons.Outlined.CheckCircleOutline,
                                                    contentDescription = "Pointer présence",
                                                    tint = if (b.attended) GreenSuccess else SecondaryText,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            // Remove enrollment
                                            IconButton(
                                                onClick = { onInstructorUnenroll(slot.id, s.id) },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Désinscrire",
                                                    tint = RedAlert,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        if (b.attended) {
                                            Text("✅ Validé", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GreenSuccess)
                                        }
                                    }
                                }
                            }
                        }

                        // Waiting List
                        if (slotItem.waitingListBookings.isNotEmpty()) {
                            Text(
                                text = "Liste d'attente (priorité si désistement) :",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberAccent,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            slotItem.waitingListBookings.forEach { bookingWithStudent ->
                                val s = bookingWithStudent.student
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFEF3C7),
                                    border = BorderStroke(1.dp, AmberAccent.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("⏳", fontSize = 10.sp)
                                            Text(s.fullName, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        if (userMode == AppUserMode.INSTRUCTOR) {
                                            IconButton(
                                                onClick = { onInstructorUnenroll(slot.id, s.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = null, tint = RedAlert, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Surface(
                    color = HighDensityNavBar.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        "Aucun élève inscrit pour le moment. Soyez le premier !",
                        fontSize = 11.sp,
                        color = SecondaryText,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons Row depending on User Mode
            if (userMode == AppUserMode.STUDENT) {
                // Student Mode Action Button
                if (currentStudent != null) {
                    if (isCancelled) {
                        Surface(
                            color = RedAlertBg,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Séance annulée en raison des conditions météo.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = RedAlert,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    } else if (isCurrentStudentEnrolled) {
                        Button(
                            onClick = { onToggleStudentEnrollment(slotItem, currentStudent) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RedAlertBg,
                                contentColor = RedAlert
                            ),
                            border = BorderStroke(1.dp, RedAlert),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Vous êtes inscrit • Annuler ma place", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (isCurrentStudentOnWaitList) {
                        Button(
                            onClick = { onToggleStudentEnrollment(slotItem, currentStudent) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFEF3C7),
                                contentColor = AmberAccent
                            ),
                            border = BorderStroke(1.dp, AmberAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("⏳ Sur liste d'attente • Se retirer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (!slotItem.isFull) {
                        Button(
                            onClick = { onToggleStudentEnrollment(slotItem, currentStudent) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("M'inscrire à ce créneau (${slotItem.availablePlaces} place dispo)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { onToggleStudentEnrollment(slotItem, currentStudent) },
                            colors = ButtonDefaults.buttonColors(containerColor = AmberAccent),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Complet • Rejoindre la liste d'attente", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Instructor Mode Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quick Inscribe Student Button
                    Button(
                        onClick = { onOpenInstructorEnroll(slotItem) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Inscrire un élève", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Edit
                    IconButton(
                        onClick = { onEditSlot(slot) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(HighDensityNavBar, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = PrimaryBlueDark, modifier = Modifier.size(16.dp))
                    }

                    // Delete
                    IconButton(
                        onClick = { onDeleteSlot(slot.id) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(RedAlertBg.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = RedAlert, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

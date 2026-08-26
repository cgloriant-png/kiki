package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudentEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppUserMode

@Composable
fun PlanningHeader(
    userMode: AppUserMode,
    onUserModeChange: (AppUserMode) -> Unit,
    currentStudent: StudentEntity?,
    allStudents: List<StudentEntity>,
    onSelectCurrentStudent: (StudentEntity) -> Unit,
    onOpenWhatsAppShare: () -> Unit,
    onQuickGenerateWeekend: () -> Unit,
    onOpenAddSlot: () -> Unit,
    onOpenAddStudent: () -> Unit
) {
    var showStudentPickerDropdown by remember { mutableStateOf(false) }

    Surface(
        color = HighDensitySurface,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Row 1: Brand & User Mode Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Title with Paramotor Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = PrimaryBlue,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🪂", fontSize = 20.sp)
                        }
                    }
                    Column {
                        Text(
                            text = "Planning Paramoteur",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = HighDensityHeaderTitle
                        )
                        Text(
                            text = "École de pilotage & Réservations",
                            fontSize = 11.sp,
                            color = SecondaryText
                        )
                    }
                }

                // Mode Switcher Pill (Instructeur vs Élève)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PrimaryBlueContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (userMode == AppUserMode.INSTRUCTOR) PrimaryBlue else Color.Transparent,
                            modifier = Modifier
                                .clickable { onUserModeChange(AppUserMode.INSTRUCTOR) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("👨‍✈️", fontSize = 12.sp)
                                Text(
                                    "Instructeur",
                                    fontSize = 11.sp,
                                    fontWeight = if (userMode == AppUserMode.INSTRUCTOR) FontWeight.Bold else FontWeight.Normal,
                                    color = if (userMode == AppUserMode.INSTRUCTOR) Color.White else PrimaryBlueDark
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (userMode == AppUserMode.STUDENT) PrimaryBlue else Color.Transparent,
                            modifier = Modifier
                                .clickable { onUserModeChange(AppUserMode.STUDENT) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("🪂", fontSize = 12.sp)
                                Text(
                                    "Élève",
                                    fontSize = 11.sp,
                                    fontWeight = if (userMode == AppUserMode.STUDENT) FontWeight.Bold else FontWeight.Normal,
                                    color = if (userMode == AppUserMode.STUDENT) Color.White else PrimaryBlueDark
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Mode Banner / Quick Actions
            if (userMode == AppUserMode.STUDENT) {
                // Student selection bar
                Surface(
                    color = PrimaryBlueContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStudentPickerDropdown = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = PrimaryBlue,
                                shape = CircleShape,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        currentStudent?.initials ?: "EL",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Connecté en tant que :",
                                    fontSize = 10.sp,
                                    color = SecondaryText
                                )
                                Text(
                                    text = currentStudent?.fullName ?: "Sélectionner mon profil élève",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityHeaderTitle,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Button(
                            onClick = { showStudentPickerDropdown = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryBlue,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Changer", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                // Instructor Actions Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // WhatsApp Share Button
                    OutlinedButton(
                        onClick = onOpenWhatsAppShare,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, GreenSuccess),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenSuccess),
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("💬", fontSize = 13.sp)
                            Text("Planning WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Weekend quick generation
                    OutlinedButton(
                        onClick = onQuickGenerateWeekend,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp), tint = AmberAccent)
                            Text("+ Week-end", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    // Student Switcher Dialog
    if (showStudentPickerDropdown) {
        AlertDialog(
            onDismissRequest = { showStudentPickerDropdown = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🪂", fontSize = 20.sp)
                    Text("Choisir mon profil élève", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                ) {
                    Text(
                        "Sélectionnez votre nom parmi les ${allStudents.size} élèves de l'école pour vous inscrire ou voir vos vols :",
                        fontSize = 12.sp,
                        color = SecondaryText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(allStudents.size) { index ->
                            val s = allStudents[index]
                            val isSelected = s.id == currentStudent?.id
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) PrimaryBlueContainer else HighDensityNavBar,
                                border = if (isSelected) BorderStroke(1.dp, PrimaryBlue) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectCurrentStudent(s)
                                        showStudentPickerDropdown = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            s.fullName,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            s.level,
                                            fontSize = 11.sp,
                                            color = SecondaryText
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStudentPickerDropdown = false }) {
                    Text("Fermer")
                }
            }
        )
    }
}

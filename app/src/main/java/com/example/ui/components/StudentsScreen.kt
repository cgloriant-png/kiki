package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudentEntity
import com.example.ui.theme.*

@Composable
fun StudentsScreen(
    students: List<StudentEntity>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    levelFilter: String?,
    onLevelFilterChange: (String?) -> Unit,
    onOpenAddStudent: () -> Unit,
    onSelectStudent: (StudentEntity) -> Unit,
    onEditStudent: (StudentEntity) -> Unit,
    onDeleteStudent: (StudentEntity) -> Unit
) {
    val context = LocalContext.current

    val levels = listOf(
        "Débutant",
        "Premiers Grands Vols",
        "Autonome",
        "Breveté"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HighDensityBg)
    ) {
        // Header & Search Area
        Surface(
            color = HighDensitySurface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Title and Add button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Annuaire des Élèves (${students.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = HighDensityHeaderTitle
                        )
                        Text(
                            text = "Gestion des fiches, niveaux et carnet de vol",
                            fontSize = 11.sp,
                            color = SecondaryText
                        )
                    }

                    Button(
                        onClick = onOpenAddStudent,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Élève", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search TextField
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Rechercher un élève, téléphone, matériel...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SecondaryText) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = HighDensityBg,
                        unfocusedContainerColor = HighDensityBg
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Level Filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = levelFilter == null,
                        onClick = { onLevelFilterChange(null) },
                        label = { Text("Tous (${students.size})", fontSize = 11.sp) }
                    )
                    levels.forEach { lvl ->
                        FilterChip(
                            selected = levelFilter == lvl,
                            onClick = {
                                if (levelFilter == lvl) onLevelFilterChange(null) else onLevelFilterChange(lvl)
                            },
                            label = { Text(lvl, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        Divider(color = BorderOutline.copy(alpha = 0.5f))

        // Student List
        if (students.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("👥", fontSize = 36.sp)
                    Text("Aucun élève trouvé", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Modifiez votre recherche ou ajoutez un nouvel élève.", fontSize = 12.sp, color = SecondaryText)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(students, key = { it.id }) { student ->
                    StudentCard(
                        student = student,
                        onSelect = { onSelectStudent(student) },
                        onEdit = { onEditStudent(student) },
                        onDelete = { onDeleteStudent(student) },
                        onCall = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:${student.phone.replace(" ", "")}")
                            }
                            try { context.startActivity(intent) } catch (e: Exception) {
                                Toast.makeText(context, "Impossible de composer le numéro", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onWhatsApp = {
                            val cleanNumber = student.phone.replace(" ", "").replace("^0".toRegex(), "33")
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse("https://wa.me/$cleanNumber")
                            }
                            try { context.startActivity(intent) } catch (e: Exception) {
                                Toast.makeText(context, "WhatsApp non disponible", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StudentCard(
    student: StudentEntity,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
        border = BorderStroke(1.dp, BorderOutline.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Avatar & Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = when {
                            student.level.contains("Breveté", ignoreCase = true) -> Color(0xFF15803D)
                            student.level.contains("Autonome", ignoreCase = true) -> Color(0xFF0284C7)
                            student.level.contains("Grands Vols", ignoreCase = true) -> Color(0xFFD97706)
                            else -> PrimaryBlue
                        },
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                student.initials,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Column {
                        Text(
                            text = student.fullName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = HighDensityHeaderTitle
                        )
                        Text(
                            text = student.level,
                            fontSize = 11.sp,
                            color = SecondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Quick sessions badge
                Surface(
                    color = PrimaryBlueContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${student.completedSessions} vols/séances",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlueDark,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Details: Equipment & Phone
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Flight, contentDescription = null, modifier = Modifier.size(13.dp), tint = PrimaryBlue)
                    Text(
                        text = student.equipment,
                        fontSize = 11.sp,
                        color = SecondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(13.dp), tint = SecondaryText)
                    Text(
                        text = student.phone,
                        fontSize = 11.sp,
                        color = PrimaryBlueDark,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (student.notes.isNotBlank()) {
                Surface(
                    color = HighDensityNavBar,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                ) {
                    Text(
                        text = "📝 ${student.notes}",
                        fontSize = 10.sp,
                        color = SecondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Action Bar (Appeler, WhatsApp, Fiche)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Call Button
                    OutlinedButton(
                        onClick = onCall,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Appeler", fontSize = 10.sp)
                    }

                    // WhatsApp Button
                    OutlinedButton(
                        onClick = onWhatsApp,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, GreenSuccess),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenSuccess),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("💬", fontSize = 10.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("WhatsApp", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifier", tint = PrimaryBlueDark, modifier = Modifier.size(15.dp))
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = RedAlert, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}

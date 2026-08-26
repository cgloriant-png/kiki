package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AddEditSlotDialog(
    slotToEdit: LessonSlotEntity?,
    onDismiss: () -> Unit,
    onSave: (
        dateIso: String,
        startTime: String,
        endTime: String,
        title: String,
        lessonType: String,
        location: String,
        maxCapacity: Int,
        weatherStatus: String,
        windInfo: String,
        instructorNotes: String
    ) -> Unit
) {
    val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(Date()) }

    var dateIso by remember { mutableStateOf(slotToEdit?.dateIso ?: todayIso) }
    var startTime by remember { mutableStateOf(slotToEdit?.startTime ?: "07:00") }
    var endTime by remember { mutableStateOf(slotToEdit?.endTime ?: "09:30") }
    var title by remember { mutableStateOf(slotToEdit?.title ?: "") }
    var selectedLessonType by remember { mutableStateOf(slotToEdit?.lessonType ?: "GRAND_VOL") }
    var location by remember { mutableStateOf(slotToEdit?.location ?: "Base Paramoteur - Piste Principale") }
    var maxCapacity by remember { mutableIntStateOf(slotToEdit?.maxCapacity ?: 3) }
    var weatherStatus by remember { mutableStateOf(slotToEdit?.weatherStatus ?: "OPTIMAL") }
    var windInfo by remember { mutableStateOf(slotToEdit?.windInfo ?: "5-10 km/h Ouest - Laminaire") }
    var instructorNotes by remember { mutableStateOf(slotToEdit?.instructorNotes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(if (slotToEdit == null) "➕ Nouveau Créneau de Vol" else "✏️ Modifier le Créneau", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Type de leçon
                Text("Type de Séance & Activité :", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HighDensityHeaderTitle)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ParamoteurLessonType.entries.take(3).forEach { t ->
                        val isSelected = selectedLessonType == t.code
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) PrimaryBlue else HighDensityNavBar,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedLessonType = t.code
                                    if (title.isBlank()) title = t.label
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(t.emoji, fontSize = 16.sp)
                                Text(
                                    t.name.take(6),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else HighDensityHeaderTitle
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ParamoteurLessonType.entries.drop(3).forEach { t ->
                        val isSelected = selectedLessonType == t.code
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) PrimaryBlue else HighDensityNavBar,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedLessonType = t.code
                                    if (title.isBlank()) title = t.label
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(t.emoji, fontSize = 16.sp)
                                Text(
                                    t.name.take(7),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else HighDensityHeaderTitle
                                )
                            }
                        }
                    }
                }

                // Date & Time
                OutlinedTextField(
                    value = dateIso,
                    onValueChange = { dateIso = it },
                    label = { Text("Date (AAAA-MM-JJ)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Début") },
                        placeholder = { Text("07:00") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("Fin") },
                        placeholder = { Text("09:30") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Capacity Stepper
                Text("Nombre de places limité :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = { if (maxCapacity > 1) maxCapacity-- },
                        modifier = Modifier
                            .size(36.dp)
                            .background(HighDensityNavBar, CircleShape)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        "$maxCapacity places max",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = PrimaryBlueDark
                    )
                    IconButton(
                        onClick = { if (maxCapacity < 10) maxCapacity++ },
                        modifier = Modifier
                            .size(36.dp)
                            .background(HighDensityNavBar, CircleShape)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titre / Intitulé du cours") },
                    placeholder = { Text("Vol du Matin & Tours de Piste") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Location
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Terrain / Lieu") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Wind & Météo
                OutlinedTextField(
                    value = windInfo,
                    onValueChange = { windInfo = it },
                    label = { Text("Vent / Conditions Météo") },
                    placeholder = { Text("5-10 km/h Ouest laminaire") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes
                OutlinedTextField(
                    value = instructorNotes,
                    onValueChange = { instructorNotes = it },
                    label = { Text("Consignes & Notes instructeur") },
                    placeholder = { Text("Radio chargée, gants, briefing 15 min avant") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        dateIso,
                        startTime,
                        endTime,
                        title,
                        selectedLessonType,
                        location,
                        maxCapacity,
                        weatherStatus,
                        windInfo,
                        instructorNotes
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(if (slotToEdit == null) "Créer le créneau" else "Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun AddEditStudentDialog(
    studentToEdit: StudentEntity?,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        firstName: String,
        lastName: String,
        phone: String,
        email: String,
        level: String,
        equipment: String,
        notes: String
    ) -> Unit
) {
    var firstName by remember { mutableStateOf(studentToEdit?.firstName ?: "") }
    var lastName by remember { mutableStateOf(studentToEdit?.lastName ?: "") }
    var phone by remember { mutableStateOf(studentToEdit?.phone ?: "") }
    var email by remember { mutableStateOf(studentToEdit?.email ?: "") }
    var level by remember { mutableStateOf(studentToEdit?.level ?: "Débutant - Pente école & Gonflage") }
    var equipment by remember { mutableStateOf(studentToEdit?.equipment ?: "Matériel École") }
    var notes by remember { mutableStateOf(studentToEdit?.notes ?: "") }

    val levelsList = listOf(
        "Débutant - Pente école & Gonflage",
        "Premiers Grands Vols (Lâcher solo)",
        "Autonome - Navigation GPS & Cross",
        "Breveté - Perfectionnement & Maniabilité"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (studentToEdit == null) "➕ Nouvel Élève Paramoteur" else "✏️ Modifier la Fiche Élève", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("Prénom") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Nom") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Téléphone (pour WhatsApp)") },
                    placeholder = { Text("06 12 34 56 78") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Niveau de formation :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                levelsList.forEach { lvl ->
                    val isSelected = level == lvl
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) PrimaryBlueContainer else HighDensityNavBar,
                        border = if (isSelected) BorderStroke(1.dp, PrimaryBlue) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { level = lvl }
                    ) {
                        Text(
                            lvl,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = equipment,
                    onValueChange = { equipment = it },
                    label = { Text("Matériel / Équipement") },
                    placeholder = { Text("Matériel École ou Voile Dudek + Moster 185") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Remarques / Progression") },
                    placeholder = { Text("Très bon feeling gonflage face voile...") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (firstName.isNotBlank() || lastName.isNotBlank()) {
                        onSave(
                            studentToEdit?.id ?: 0L,
                            firstName,
                            lastName,
                            phone,
                            email,
                            level,
                            equipment,
                            notes
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(if (studentToEdit == null) "Ajouter l'élève" else "Enregistrer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun InstructorEnrollDialog(
    slotItem: SlotWithBookings,
    allStudents: List<StudentEntity>,
    onDismiss: () -> Unit,
    onEnroll: (studentId: Long, isWaitingList: Boolean) -> Unit
) {
    val enrolledIds = slotItem.enrolledStudentIds
    val availableStudents = allStudents.filter { !enrolledIds.contains(it.id) }
    val isFull = slotItem.isFull

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Inscrire un élève au créneau", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "${slotItem.slot.startTime}-${slotItem.slot.endTime} • ${slotItem.slot.title}",
                    fontSize = 12.sp,
                    color = SecondaryText
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
            ) {
                if (isFull) {
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            "⚠️ Créneau complet (${slotItem.slot.maxCapacity} places). L'élève sera ajouté en liste d'attente.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AmberAccent,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                if (availableStudents.isEmpty()) {
                    Text("Tous les élèves sont déjà inscrits à ce créneau.", fontSize = 12.sp, color = SecondaryText)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(availableStudents) { student ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = HighDensityNavBar,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onEnroll(student.id, isFull)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(student.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(student.level, fontSize = 10.sp, color = SecondaryText)
                                    }
                                    Icon(
                                        Icons.Default.PersonAdd,
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
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

@Composable
fun WeatherUpdateDialog(
    slot: LessonSlotEntity,
    onDismiss: () -> Unit,
    onConfirm: (weatherStatus: String, windInfo: String) -> Unit
) {
    var status by remember { mutableStateOf(slot.weatherStatus) }
    var wind by remember { mutableStateOf(slot.windInfo) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("🌤️ Statut Météo du Créneau", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Modifier les conditions pour : ${slot.startTime}-${slot.endTime} (${slot.title})", fontSize = 12.sp, color = SecondaryText)

                SlotWeather.entries.forEach { w ->
                    val isSelected = status == w.code
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) PrimaryBlueContainer else HighDensityNavBar,
                        border = if (isSelected) BorderStroke(1.dp, PrimaryBlue) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { status = w.code }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(w.iconEmoji, fontSize = 16.sp)
                            Text(
                                w.label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = wind,
                    onValueChange = { wind = it },
                    label = { Text("Détail vent / conditions") },
                    placeholder = { Text("8 km/h OSO - Vent laminaire") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(status, wind) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Mettre à jour")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

@Composable
fun WhatsAppShareDialog(
    content: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("💬", fontSize = 20.sp)
                Text("Partage Planning WhatsApp", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)
            ) {
                Text(
                    "Message récapitulatif généré pour le groupe WhatsApp des élèves :",
                    fontSize = 11.sp,
                    color = SecondaryText,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Surface(
                    color = HighDensityNavBar,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BorderOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text(
                        text = content,
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        modifier = Modifier
                            .padding(10.dp)
                            .verticalScroll(rememberScrollState())
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, content)
                    }
                    try {
                        context.startActivity(Intent.createChooser(intent, "Partager le planning"))
                        onDismiss()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Impossible de partager", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess)
            ) {
                Text("Partager / WhatsApp", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText("Planning Paramoteur", content))
                    Toast.makeText(context, "Planning copié dans le presse-papiers !", Toast.LENGTH_SHORT).show()
                    onDismiss()
                }
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copier le texte")
            }
        }
    )
}

@Composable
fun StudentDetailDialog(
    student: StudentEntity,
    allSlotsWithBookings: List<SlotWithBookings>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit
) {
    // Find all slots this student is registered to
    val studentSlots = remember(allSlotsWithBookings, student) {
        allSlotsWithBookings.filter { it.enrolledStudentIds.contains(student.id) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = PrimaryBlue,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(student.initials, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                Column {
                    Text(student.fullName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(student.level, fontSize = 11.sp, color = SecondaryText)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Info Grid
                Surface(
                    color = HighDensityNavBar,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row {
                            Text("📞 Téléphone : ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(student.phone, fontSize = 11.sp)
                        }
                        if (student.email.isNotBlank()) {
                            Row {
                                Text("✉️ Email : ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(student.email, fontSize = 11.sp)
                            }
                        }
                        Row {
                            Text("🪂 Matériel : ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(student.equipment, fontSize = 11.sp)
                        }
                        Row {
                            Text("📊 Vols effectués : ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("${student.completedSessions} séances (${student.totalFlightHours} h de vol)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = PrimaryBlueDark)
                        }
                    }
                }

                if (student.notes.isNotBlank()) {
                    Text("Carnet de progression & Notes :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Surface(
                        color = HighDensityContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            student.notes,
                            fontSize = 11.sp,
                            color = HighDensityHeaderTitle,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Text("Prochains créneaux réservés (${studentSlots.size}) :", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                if (studentSlots.isEmpty()) {
                    Text("Aucun créneau programmé actuellement pour cet élève.", fontSize = 11.sp, color = SecondaryText)
                } else {
                    studentSlots.forEach { s ->
                        Surface(
                            color = HighDensityNavBar,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "${s.slot.dateIso} • ${s.slot.startTime}-${s.slot.endTime}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(s.slot.title, fontSize = 10.sp, color = SecondaryText)
                                }
                                Text("Inscrit ✅", fontSize = 10.sp, color = GreenSuccess, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onCall,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Appeler", fontSize = 11.sp)
                }

                Button(
                    onClick = onWhatsApp,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("💬 WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

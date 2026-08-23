package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.util.LicenseManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseAdminDialog(
    onDismiss: () -> Unit,
    onStatusChanged: () -> Unit
) {
    val context = LocalContext.current
    val currentStatus = remember { LicenseManager.checkStatus(context) }
    val thisDeviceId = remember { LicenseManager.getDeviceId(context) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Generator, 1 = Pilots List
    var issuedLicenses by remember { mutableStateOf(LicenseManager.getIssuedLicenses(context)) }

    var targetDeviceId by remember { mutableStateOf("") }
    var targetPilotName by remember { mutableStateOf("") }
    var selectedDuration by remember { mutableStateOf(LicenseManager.LicenseDuration.THIRTY_DAYS) }
    var generatedKey by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun copyText(text: String, label: String) {
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copié !", Toast.LENGTH_SHORT).show()
    }

    fun shareKey(key: String, pilotName: String, durationLabel: String) {
        val pilotInfo = if (pilotName.isNotBlank()) " pour $pilotName" else ""
        val text = "Bonjour$pilotInfo,\nVoici votre clé d'activation pour l'application Eagles Academy ($durationLabel) :\n\n$key\n\nBons vols !"
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Envoyer la clé au pilote"))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 20.dp),
        content = {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = HighDensityBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = PrimaryBlueDark)
                            Text(
                                text = "Centre de Protection & Licences",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = HighDensityHeaderTitle
                            )
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Fermer")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tab Selector
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = HighDensitySurface,
                        contentColor = PrimaryBlueDark,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Générateur", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                issuedLicenses = LicenseManager.getIssuedLicenses(context)
                                selectedTab = 1
                            },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Mes Pilotes (${issuedLicenses.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (selectedTab == 0) {
                        // TAB 1: GENERATOR & STATUS

                        // Statut de cet appareil
                        Card(
                            colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "STATUT SUR CET APPAREIL CONCEPTEUR",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SecondaryText
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Surface(
                                        color = if (currentStatus.isActivated) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = currentStatus.licenseTypeLabel,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (currentStatus.isActivated) Color(0xFF16A34A) else Color(0xFFDC2626),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "ID: $thisDeviceId",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        color = PrimaryBlueDark
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Générateur
                        Card(
                            colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "CRÉER UNE CLÉ POUR UN PILOTE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityHeaderTitle
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = targetDeviceId,
                                    onValueChange = { targetDeviceId = it.trim().uppercase() },
                                    label = { Text("ID Appareil du Pilote") },
                                    placeholder = { Text("Ex: PM-7A4B-91C2") },
                                    leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            val clip = clipboard.primaryClip
                                            if (clip != null && clip.itemCount > 0) {
                                                targetDeviceId = clip.getItemAt(0).text.toString().trim().uppercase()
                                            }
                                        }) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = "Coller l'ID")
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = targetPilotName,
                                    onValueChange = { targetPilotName = it },
                                    label = { Text("Nom du Pilote") },
                                    placeholder = { Text("Ex: Jean-Luc, Marc...") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "DURÉE DE VALIDITÉ :",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SecondaryText
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    LicenseManager.LicenseDuration.values().forEach { duration ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            RadioButton(
                                                selected = selectedDuration == duration,
                                                onClick = { selectedDuration = duration }
                                            )
                                            Text(
                                                text = duration.label,
                                                fontSize = 13.sp,
                                                fontWeight = if (selectedDuration == duration) FontWeight.Bold else FontWeight.Normal,
                                                color = HighDensityHeaderTitle
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        if (targetDeviceId.isBlank()) {
                                            Toast.makeText(context, "Veuillez renseigner l'ID appareil du pilote !", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val key = LicenseManager.generateKey(targetDeviceId, selectedDuration)
                                            generatedKey = key
                                            LicenseManager.recordIssuedLicense(
                                                context = context,
                                                pilotName = targetPilotName,
                                                deviceId = targetDeviceId,
                                                duration = selectedDuration,
                                                generatedKey = key
                                            )
                                            issuedLicenses = LicenseManager.getIssuedLicenses(context)
                                            Toast.makeText(context, "Clé générée et enregistrée dans vos pilotes !", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueDark),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Générer & Enregistrer la Clé", fontWeight = FontWeight.Bold)
                                }

                                // Affichage de la clé générée
                                generatedKey?.let { key ->
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Surface(
                                        color = Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "CLÉ PRÊTE POUR LE PILOTE :",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SecondaryText
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = key,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFF0F172A),
                                                textAlign = TextAlign.Center
                                            )

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = { copyText(key, "Clé d'activation") },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Copier", fontSize = 12.sp)
                                                }

                                                Button(
                                                    onClick = { shareKey(key, targetPilotName, selectedDuration.label) },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(6.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                                                ) {
                                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Partager", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // TAB 2: PILOTES ENREGISTRÉS & HISTORIQUE DES CLÉS
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Search bar if list not empty
                            if (issuedLicenses.isNotEmpty()) {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    label = { Text("Rechercher un pilote ou un ID") },
                                    placeholder = { Text("Nom ou ID appareil...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }

                            val filtered = if (searchQuery.isBlank()) {
                                issuedLicenses
                            } else {
                                issuedLicenses.filter {
                                    it.pilotName.contains(searchQuery, ignoreCase = true) ||
                                            it.deviceId.contains(searchQuery, ignoreCase = true) ||
                                            it.generatedKey.contains(searchQuery, ignoreCase = true)
                                }
                            }

                            if (filtered.isEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, CardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.PeopleOutline,
                                            contentDescription = null,
                                            tint = SecondaryText,
                                            modifier = Modifier.size(44.dp)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = if (issuedLicenses.isEmpty()) "Aucun pilote enregistré pour l'instant" else "Aucun résultat trouvé",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = HighDensityHeaderTitle
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Générez une première clé dans l'onglet 'Générateur' pour ajouter automatiquement le pilote à votre registre.",
                                            fontSize = 12.sp,
                                            color = SecondaryText,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Button(
                                            onClick = { selectedTab = 0 },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueDark),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Générer une clé pilote")
                                        }
                                    }
                                }
                            } else {
                                filtered.forEach { pilot ->
                                    val isExp = pilot.isExpired
                                    val daysLeft = pilot.daysRemaining
                                    val statusColor = if (isExp) Color(0xFFDC2626) else Color(0xFF16A34A)
                                    val statusBg = if (isExp) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)
                                    val statusLabel = if (isExp) "Expiré" else if (daysLeft > 365) "Illimité" else "$daysLeft j restants"

                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, if (isExp) Color(0xFFFCA5A5) else CardBorder),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            // Top info row
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlueDark, modifier = Modifier.size(18.dp))
                                                    Text(
                                                        text = pilot.pilotName,
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 14.sp,
                                                        color = HighDensityHeaderTitle
                                                    )
                                                }

                                                Surface(
                                                    color = statusBg,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = statusLabel,
                                                        color = statusColor,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            // Device ID & Duration
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "ID: ${pilot.deviceId}",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = SecondaryText
                                                )
                                                Text(
                                                    text = pilot.durationLabel,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = SecondaryText
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            // Key Box
                                            Surface(
                                                color = Color(0xFFF8FAFC),
                                                shape = RoundedCornerShape(6.dp),
                                                border = BorderStroke(0.5.dp, Color(0xFFE2E8F0)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = pilot.generatedKey,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF1E293B),
                                                    modifier = Modifier.padding(6.dp),
                                                    textAlign = TextAlign.Center
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Actions
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = { copyText(pilot.generatedKey, "Clé de ${pilot.pilotName}") },
                                                    modifier = Modifier.weight(1f),
                                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text("Copier", fontSize = 11.sp)
                                                }

                                                Button(
                                                    onClick = { shareKey(pilot.generatedKey, pilot.pilotName, pilot.durationLabel) },
                                                    modifier = Modifier.weight(1f),
                                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                                    shape = RoundedCornerShape(6.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                                                ) {
                                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text("Partager", fontSize = 11.sp)
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        // Pre-fill generator to renew
                                                        targetDeviceId = pilot.deviceId
                                                        targetPilotName = pilot.pilotName
                                                        selectedTab = 0
                                                    },
                                                    modifier = Modifier.weight(1.1f),
                                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                                    shape = RoundedCornerShape(6.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlueDark)
                                                ) {
                                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text("Prolonger", fontSize = 11.sp)
                                                }

                                                IconButton(
                                                    onClick = {
                                                        LicenseManager.deleteIssuedLicense(context, pilot.id)
                                                        issuedLicenses = LicenseManager.getIssuedLicenses(context)
                                                        Toast.makeText(context, "Pilote retiré de la liste.", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ACTIONS DE TEST DU CONCEPTEUR
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                LicenseManager.resetActivation(context)
                                Toast.makeText(context, "Application reverrouillée pour tester l'écran d'activation !", Toast.LENGTH_SHORT).show()
                                onStatusChanged()
                                onDismiss()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verrouiller App", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                LicenseManager.activate(context, "PARAMASTER2026", "Concepteur Master")
                                Toast.makeText(context, "Mode Développeur Master réactivé !", Toast.LENGTH_SHORT).show()
                                onStatusChanged()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueDark)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Déverrouiller Master", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    )
}

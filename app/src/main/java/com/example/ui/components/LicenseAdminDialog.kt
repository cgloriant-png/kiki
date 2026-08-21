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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    var targetDeviceId by remember { mutableStateOf("") }
    var targetPilotName by remember { mutableStateOf("") }
    var selectedDuration by remember { mutableStateOf(LicenseManager.LicenseDuration.THIRTY_DAYS) }
    var generatedKey by remember { mutableStateOf<String?>(null) }

    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun copyText(text: String, label: String) {
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "$label copié !", Toast.LENGTH_SHORT).show()
    }

    fun shareKey(key: String, durationLabel: String) {
        val pilotInfo = if (targetPilotName.isNotBlank()) " pour $targetPilotName" else ""
        val text = "Bonjour$pilotInfo,\nVoici votre clé d'activation pour l'application Eagles Academy Paramoteur ($durationLabel) :\n\n$key\n\nBons vols !"
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Envoyer la clé au pilote"))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f),
        content = {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = HighDensityBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // SECTION 1: Statut sur cet appareil
                    Card(
                        colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "STATUT DE CET APPAREIL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SecondaryText
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = currentStatus.licenseTypeLabel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (currentStatus.isActivated) Color(0xFF16A34A) else Color(0xFFDC2626)
                                )
                                Text(
                                    text = thisDeviceId,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = PrimaryBlueDark
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // SECTION 2: GÉNÉRATEUR DE CLÉS PILOTES
                    Card(
                        colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.VpnKey, contentDescription = null, tint = PrimaryBlueDark, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "GÉNÉRATEUR DE CLÉ POUR UN PILOTE",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityHeaderTitle
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

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
                                label = { Text("Nom du Pilote (repère)") },
                                placeholder = { Text("Ex: Jean-Luc") },
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

                            // Duration Chips
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                        generatedKey = LicenseManager.generateKey(targetDeviceId, selectedDuration)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlueDark),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Générer la Clé d'Activation", fontWeight = FontWeight.Bold)
                            }

                            // Display Generated Key
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
                                            text = "CLÉ GÉNÉRÉE :",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SecondaryText
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = key,
                                            fontSize = 15.sp,
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
                                                onClick = { shareKey(key, selectedDuration.label) },
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // SECTION 3: ACTIONS DE TEST
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                LicenseManager.resetActivation(context)
                                Toast.makeText(context, "Application reverrouillée pour test !", Toast.LENGTH_SHORT).show()
                                onStatusChanged()
                                onDismiss()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verrouiller", fontSize = 12.sp)
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
                            Text("Déverrouiller", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    )
}

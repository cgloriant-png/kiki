package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import com.example.util.GeometryUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GaugeHeader(
    courseName: String,
    savedCourses: List<Pair<String, String>>,
    currentCourseSlug: String?,
    onSelectCourse: (slug: String) -> Unit,
    onDeleteCourse: (slug: String) -> Unit,
    onImportJsonClick: () -> Unit,
    onOpenLicenseAdmin: () -> Unit,
    licenseStatusLabel: String? = null,
    pointsCount: Int,
    traceDistanceMeters: Double?,
    corridorPct: Int?,
    flightScore: Int?
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    Surface(
        color = HighDensityBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 8.dp)
        ) {
            // Top Eagles Academy Brand Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(7.dp),
                        color = Color.White,
                        shadowElevation = 1.dp,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_eagles_logo_1787304896446),
                            contentDescription = "Logo Eagles Academy",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Text(
                        text = "EAGLES ACADEMY",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = HighDensityHeaderTitle,
                        letterSpacing = 0.5.sp
                    )
                }

                if (!licenseStatusLabel.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = PrimaryBlue.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, PrimaryBlue.copy(alpha = 0.35f)),
                        modifier = Modifier.clickable { onOpenLicenseAdmin() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = licenseStatusLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        }
                    }
                }
            }

            // Header Bar with Dropdown Selector for Saved Courses
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Dropdown / Active Course Selector Box
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = HighDensitySurface,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dropdownExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(GreenSuccess, CircleShape)
                                )
                                Column {
                                    Text(
                                        text = "ÉPREUVE SÉLECTIONNÉE :",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SecondaryText
                                    )
                                    Text(
                                        text = if (courseName.isBlank()) "Cliquer pour choisir / importer une épreuve" else courseName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityHeaderTitle,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Sélecteur d'épreuves",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Dropdown Menu Listing Saved Courses
                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier
                            .width(320.dp)
                            .background(HighDensitySurface)
                    ) {
                        Text(
                            text = "📌 Mes Épreuves Enregistrées (${savedCourses.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = PrimaryBlueDark,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                        HorizontalDivider(color = BorderOutline, thickness = 1.dp)

                        if (savedCourses.isEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Aucune épreuve enregistrée",
                                        fontSize = 12.sp,
                                        color = SecondaryText
                                    )
                                },
                                onClick = { }
                            )
                        } else {
                            savedCourses.forEach { (slug, name) ->
                                val isSelected = currentCourseSlug == slug
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.GolfCourse,
                                                    contentDescription = null,
                                                    tint = if (isSelected) GreenSuccess else SecondaryText,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = name,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) GreenSuccess else HighDensityHeaderTitle,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    onDeleteCourse(slug)
                                                    dropdownExpanded = false
                                                },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Supprimer épreuve",
                                                    tint = RedAlert,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        onSelectCourse(slug)
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }

                        HorizontalDivider(color = BorderOutline, thickness = 1.dp)

                        // Action: Import new JSON
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Ouvrir / Importer nouveau JSON",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue
                                    )
                                }
                            },
                            onClick = {
                                dropdownExpanded = false
                                onImportJsonClick()
                            }
                        )

                        HorizontalDivider(color = BorderOutline, thickness = 1.dp)

                        // Action: Security & License
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = PrimaryBlueDark,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Protection & Clés Pilotes",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HighDensityHeaderTitle
                                        )
                                        if (!licenseStatusLabel.isNullOrBlank()) {
                                            Text(
                                                text = licenseStatusLabel,
                                                fontSize = 10.sp,
                                                color = SecondaryText
                                            )
                                        }
                                    }
                                }
                            },
                            onClick = {
                                dropdownExpanded = false
                                onOpenLicenseAdmin()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // License / Security Quick Button
                IconButton(
                    onClick = onOpenLicenseAdmin,
                    modifier = Modifier
                        .size(38.dp)
                        .background(HighDensitySurface, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Protection & Licences",
                        tint = PrimaryBlueDark,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Import JSON Quick Button
                Button(
                    onClick = onImportJsonClick,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Ouvrir",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "JSON",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Gauges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GaugeCell(
                    value = pointsCount.toString(),
                    label = "PORTES/BALISES",
                    valueColor = PrimaryBlue,
                    modifier = Modifier.weight(1f)
                )
                GaugeCell(
                    value = GeometryUtils.fmtDist(traceDistanceMeters),
                    label = "TRACE",
                    valueColor = PrimaryBlue,
                    modifier = Modifier.weight(1f)
                )
                GaugeCell(
                    value = corridorPct?.let { "$it%" } ?: "—",
                    label = "COULOIR",
                    valueColor = if (corridorPct != null && corridorPct >= 80) GreenSuccess else RedAlertText,
                    modifier = Modifier.weight(1f)
                )
                GaugeCell(
                    value = flightScore?.toString() ?: "—",
                    label = "SCORE",
                    valueColor = if (flightScore != null && flightScore > 0) GreenSuccess else PrimaryBlue,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = BorderOutline, thickness = 1.dp)
        }
    }
}

@Composable
private fun GaugeCell(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = HighDensitySurface,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderOutline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = SecondaryText,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
            Text(
                text = value,
                color = valueColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

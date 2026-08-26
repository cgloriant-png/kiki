package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppUserMode
import com.example.ui.viewmodel.PlanningViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PlanningViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        setContent {
            ParamoteurTheme {
                val context = LocalContext.current

                // State from ViewModel
                val userMode by viewModel.userMode.collectAsStateWithLifecycle()
                val currentStudent by viewModel.currentStudent.collectAsStateWithLifecycle()
                val allStudents by viewModel.allStudents.collectAsStateWithLifecycle()
                val slotsWithBookings by viewModel.slotsWithBookings.collectAsStateWithLifecycle()
                val filteredSlots by viewModel.filteredSlots.collectAsStateWithLifecycle()
                val filteredStudents by viewModel.filteredStudents.collectAsStateWithLifecycle()

                // Filter States
                val selectedDateFilter by viewModel.selectedDateFilter.collectAsStateWithLifecycle()
                val filterOnlyAvailable by viewModel.filterOnlyAvailable.collectAsStateWithLifecycle()
                val filterLessonType by viewModel.filterLessonType.collectAsStateWithLifecycle()
                val filterOnlyMyBookings by viewModel.filterOnlyMyBookings.collectAsStateWithLifecycle()
                val studentSearchQuery by viewModel.studentSearchQuery.collectAsStateWithLifecycle()
                val studentLevelFilter by viewModel.studentLevelFilter.collectAsStateWithLifecycle()

                // Dialog States
                var showAddSlotDialog by remember { mutableStateOf(false) }
                var slotToEdit by remember { mutableStateOf<LessonSlotEntity?>(null) }
                var showAddStudentDialog by remember { mutableStateOf(false) }
                var studentToEdit by remember { mutableStateOf<StudentEntity?>(null) }
                var studentToViewDetail by remember { mutableStateOf<StudentEntity?>(null) }
                var slotToInstructorEnroll by remember { mutableStateOf<SlotWithBookings?>(null) }
                var slotToUpdateWeather by remember { mutableStateOf<LessonSlotEntity?>(null) }
                var showWhatsAppShareDialog by remember { mutableStateOf(false) }
                var whatsAppContent by remember { mutableStateOf("") }

                // Navigation Bar Tab
                var currentTab by remember { mutableIntStateOf(0) }

                // Snackbar Host State
                val snackbarHostState = remember { SnackbarHostState() }

                // Collect Feedback Messages
                LaunchedEffect(Unit) {
                    viewModel.feedbackMessage.collect { message ->
                        snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        PlanningHeader(
                            userMode = userMode,
                            onUserModeChange = { mode -> viewModel.setUserMode(mode) },
                            currentStudent = currentStudent,
                            allStudents = allStudents,
                            onSelectCurrentStudent = { s -> viewModel.setCurrentStudent(s) },
                            onOpenWhatsAppShare = {
                                whatsAppContent = viewModel.getWhatsAppText()
                                showWhatsAppShareDialog = true
                            },
                            onQuickGenerateWeekend = { viewModel.quickGenerateWeekendSlots() },
                            onOpenAddSlot = {
                                slotToEdit = null
                                showAddSlotDialog = true
                            },
                            onOpenAddStudent = {
                                studentToEdit = null
                                showAddStudentDialog = true
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = HighDensitySurface,
                            tonalElevation = 6.dp,
                            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            NavigationBarItem(
                                selected = currentTab == 0,
                                onClick = { currentTab = 0 },
                                icon = {
                                    Icon(
                                        if (currentTab == 0) Icons.Default.CalendarMonth else Icons.Outlined.CalendarMonth,
                                        contentDescription = "Planning"
                                    )
                                },
                                label = { Text("Planning", fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrimaryBlue,
                                    selectedTextColor = PrimaryBlue,
                                    indicatorColor = PrimaryBlueContainer
                                )
                            )

                            NavigationBarItem(
                                selected = currentTab == 1,
                                onClick = { currentTab = 1 },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (allStudents.isNotEmpty()) {
                                                Badge { Text("${allStudents.size}") }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            if (currentTab == 1) Icons.Default.People else Icons.Outlined.People,
                                            contentDescription = "Élèves"
                                        )
                                    }
                                },
                                label = { Text("Élèves", fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrimaryBlue,
                                    selectedTextColor = PrimaryBlue,
                                    indicatorColor = PrimaryBlueContainer
                                )
                            )

                            NavigationBarItem(
                                selected = currentTab == 2,
                                onClick = { currentTab = 2 },
                                icon = {
                                    Icon(
                                        if (currentTab == 2) Icons.Default.Assessment else Icons.Outlined.Assessment,
                                        contentDescription = "École"
                                    )
                                },
                                label = { Text("Vols du Jour", fontWeight = if (currentTab == 2) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrimaryBlue,
                                    selectedTextColor = PrimaryBlue,
                                    indicatorColor = PrimaryBlueContainer
                                )
                            )
                        }
                    },
                    floatingActionButton = {
                        if (userMode == AppUserMode.INSTRUCTOR) {
                            if (currentTab == 0) {
                                ExtendedFloatingActionButton(
                                    onClick = {
                                        slotToEdit = null
                                        showAddSlotDialog = true
                                    },
                                    containerColor = PrimaryBlue,
                                    contentColor = Color.White,
                                    elevation = FloatingActionButtonDefaults.elevation(6.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Créer un Créneau", fontWeight = FontWeight.Bold)
                                }
                            } else if (currentTab == 1) {
                                FloatingActionButton(
                                    onClick = {
                                        studentToEdit = null
                                        showAddStudentDialog = true
                                    },
                                    containerColor = PrimaryBlue,
                                    contentColor = Color.White
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = "Ajouter un élève")
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentTab) {
                            0 -> PlanningScreen(
                                slots = filteredSlots,
                                userMode = userMode,
                                currentStudent = currentStudent,
                                selectedDateFilter = selectedDateFilter,
                                onDateFilterChange = { df -> viewModel.setDateFilter(df) },
                                filterOnlyAvailable = filterOnlyAvailable,
                                onToggleFilterOnlyAvailable = { viewModel.toggleFilterOnlyAvailable() },
                                filterLessonType = filterLessonType,
                                onLessonTypeFilterChange = { tf -> viewModel.setLessonTypeFilter(tf) },
                                filterOnlyMyBookings = filterOnlyMyBookings,
                                onToggleFilterOnlyMyBookings = { viewModel.toggleFilterOnlyMyBookings() },
                                onToggleStudentEnrollment = { slot, student ->
                                    viewModel.toggleStudentEnrollment(slot, student)
                                },
                                onOpenInstructorEnroll = { slotItem ->
                                    slotToInstructorEnroll = slotItem
                                },
                                onInstructorUnenroll = { slotId, studentId ->
                                    viewModel.instructorUnenroll(slotId, studentId)
                                },
                                onToggleAttendance = { bookingId, studentId, attended ->
                                    viewModel.toggleAttendance(bookingId, studentId, attended)
                                },
                                onUpdateWeather = { slot ->
                                    slotToUpdateWeather = slot
                                },
                                onEditSlot = { slot ->
                                    slotToEdit = slot
                                    showAddSlotDialog = true
                                },
                                onDeleteSlot = { slotId ->
                                    viewModel.deleteSlot(slotId)
                                },
                                onSelectStudentToView = { student ->
                                    studentToViewDetail = student
                                },
                                onOpenAddSlot = {
                                    slotToEdit = null
                                    showAddSlotDialog = true
                                },
                                onQuickGenerateWeekend = {
                                    viewModel.quickGenerateWeekendSlots()
                                }
                            )

                            1 -> StudentsScreen(
                                students = filteredStudents,
                                searchQuery = studentSearchQuery,
                                onSearchQueryChange = { q -> viewModel.setStudentSearchQuery(q) },
                                levelFilter = studentLevelFilter,
                                onLevelFilterChange = { l -> viewModel.setStudentLevelFilter(l) },
                                onOpenAddStudent = {
                                    studentToEdit = null
                                    showAddStudentDialog = true
                                },
                                onSelectStudent = { s ->
                                    studentToViewDetail = s
                                },
                                onEditStudent = { s ->
                                    studentToEdit = s
                                    showAddStudentDialog = true
                                },
                                onDeleteStudent = { s ->
                                    viewModel.deleteStudent(s)
                                }
                            )

                            2 -> SchoolOverviewScreen(
                                students = allStudents,
                                slotsWithBookings = slotsWithBookings,
                                onToggleAttendance = { bookingId, studentId, attended ->
                                    viewModel.toggleAttendance(bookingId, studentId, attended)
                                },
                                onOpenWhatsAppShare = {
                                    whatsAppContent = viewModel.getWhatsAppText()
                                    showWhatsAppShareDialog = true
                                },
                                onQuickGenerateWeekend = {
                                    viewModel.quickGenerateWeekendSlots()
                                },
                                onOpenAddSlot = {
                                    slotToEdit = null
                                    showAddSlotDialog = true
                                },
                                onOpenAddStudent = {
                                    studentToEdit = null
                                    showAddStudentDialog = true
                                },
                                onSelectStudent = { s ->
                                    studentToViewDetail = s
                                }
                            )
                        }
                    }
                }

                // Dialog: Add / Edit Slot
                if (showAddSlotDialog) {
                    AddEditSlotDialog(
                        slotToEdit = slotToEdit,
                        onDismiss = {
                            showAddSlotDialog = false
                            slotToEdit = null
                        },
                        onSave = { dateIso, startTime, endTime, title, lessonType, location, maxCapacity, weatherStatus, windInfo, instructorNotes ->
                            if (slotToEdit == null) {
                                viewModel.createSlot(
                                    dateIso = dateIso,
                                    startTime = startTime,
                                    endTime = endTime,
                                    title = title,
                                    lessonType = lessonType,
                                    location = location,
                                    maxCapacity = maxCapacity,
                                    weatherStatus = weatherStatus,
                                    windInfo = windInfo,
                                    instructorNotes = instructorNotes
                                )
                            } else {
                                val updated = slotToEdit!!.copy(
                                    dateIso = dateIso,
                                    startTime = startTime,
                                    endTime = endTime,
                                    title = title,
                                    lessonType = lessonType,
                                    location = location,
                                    maxCapacity = maxCapacity,
                                    weatherStatus = weatherStatus,
                                    windInfo = windInfo,
                                    instructorNotes = instructorNotes
                                )
                                viewModel.updateSlot(updated)
                            }
                            showAddSlotDialog = false
                            slotToEdit = null
                        }
                    )
                }

                // Dialog: Add / Edit Student
                if (showAddStudentDialog) {
                    AddEditStudentDialog(
                        studentToEdit = studentToEdit,
                        onDismiss = {
                            showAddStudentDialog = false
                            studentToEdit = null
                        },
                        onSave = { id, firstName, lastName, phone, email, level, equipment, notes ->
                            viewModel.saveStudent(
                                id = id,
                                firstName = firstName,
                                lastName = lastName,
                                phone = phone,
                                email = email,
                                level = level,
                                equipment = equipment,
                                notes = notes
                            )
                            showAddStudentDialog = false
                            studentToEdit = null
                        }
                    )
                }

                // Dialog: Instructor Enroll Student
                slotToInstructorEnroll?.let { slotItem ->
                    InstructorEnrollDialog(
                        slotItem = slotItem,
                        allStudents = allStudents,
                        onDismiss = { slotToInstructorEnroll = null },
                        onEnroll = { studentId, isWaitingList ->
                            viewModel.instructorEnroll(slotItem.slot.id, studentId, isWaitingList)
                            slotToInstructorEnroll = null
                        }
                    )
                }

                // Dialog: Update Slot Weather
                slotToUpdateWeather?.let { slot ->
                    WeatherUpdateDialog(
                        slot = slot,
                        onDismiss = { slotToUpdateWeather = null },
                        onConfirm = { status, wind ->
                            viewModel.setWeather(slot.id, status, wind)
                            slotToUpdateWeather = null
                        }
                    )
                }

                // Dialog: WhatsApp Share
                if (showWhatsAppShareDialog) {
                    WhatsAppShareDialog(
                        content = whatsAppContent,
                        onDismiss = { showWhatsAppShareDialog = false }
                    )
                }

                // Dialog: Student Detail Sheet
                studentToViewDetail?.let { student ->
                    StudentDetailDialog(
                        student = student,
                        allSlotsWithBookings = slotsWithBookings,
                        onDismiss = { studentToViewDetail = null },
                        onEdit = {
                            studentToEdit = student
                            studentToViewDetail = null
                            showAddStudentDialog = true
                        },
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

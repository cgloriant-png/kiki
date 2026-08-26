package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.PlanningRepository
import com.example.data.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class AppUserMode {
    INSTRUCTOR,
    STUDENT
}

class PlanningViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PlanningRepository

    // Base data flows
    val allStudents: StateFlow<List<StudentEntity>>
    val slotsWithBookings: StateFlow<List<SlotWithBookings>>

    // User mode & current identity
    private val _userMode = MutableStateFlow(AppUserMode.INSTRUCTOR)
    val userMode: StateFlow<AppUserMode> = _userMode.asStateFlow()

    private val _currentStudent = MutableStateFlow<StudentEntity?>(null)
    val currentStudent: StateFlow<StudentEntity?> = _currentStudent.asStateFlow()

    // Filters
    private val _selectedDateFilter = MutableStateFlow<String?>("TOUS") // "TOUS", "TODAY", "TOMORROW", "WEEK", or "YYYY-MM-DD"
    val selectedDateFilter: StateFlow<String?> = _selectedDateFilter.asStateFlow()

    private val _filterOnlyAvailable = MutableStateFlow(false)
    val filterOnlyAvailable: StateFlow<Boolean> = _filterOnlyAvailable.asStateFlow()

    private val _filterLessonType = MutableStateFlow<String?>(null)
    val filterLessonType: StateFlow<String?> = _filterLessonType.asStateFlow()

    private val _filterOnlyMyBookings = MutableStateFlow(false)
    val filterOnlyMyBookings: StateFlow<Boolean> = _filterOnlyMyBookings.asStateFlow()

    private val _studentSearchQuery = MutableStateFlow("")
    val studentSearchQuery: StateFlow<String> = _studentSearchQuery.asStateFlow()

    private val _studentLevelFilter = MutableStateFlow<String?>(null)
    val studentLevelFilter: StateFlow<String?> = _studentLevelFilter.asStateFlow()

    // Toast / Feedback message
    private val _feedbackMessage = MutableSharedFlow<String>()
    val feedbackMessage: SharedFlow<String> = _feedbackMessage.asSharedFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = PlanningRepository(db.planningDao())

        allStudents = repository.allStudents
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        slotsWithBookings = repository.slotsWithBookings
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        viewModelScope.launch {
            repository.initializeSampleDataIfNeeded()
            // Default selected student to first student if available
            allStudents.collect { list ->
                if (_currentStudent.value == null && list.isNotEmpty()) {
                    _currentStudent.value = list.first()
                }
            }
        }
    }

    fun setUserMode(mode: AppUserMode) {
        _userMode.value = mode
        if (mode == AppUserMode.INSTRUCTOR) {
            _filterOnlyMyBookings.value = false
        }
    }

    fun setCurrentStudent(student: StudentEntity) {
        _currentStudent.value = student
    }

    fun setDateFilter(filter: String?) {
        _selectedDateFilter.value = filter
    }

    fun toggleFilterOnlyAvailable() {
        _filterOnlyAvailable.value = !_filterOnlyAvailable.value
    }

    fun setLessonTypeFilter(typeCode: String?) {
        _filterLessonType.value = typeCode
    }

    fun toggleFilterOnlyMyBookings() {
        _filterOnlyMyBookings.value = !_filterOnlyMyBookings.value
    }

    fun setStudentSearchQuery(query: String) {
        _studentSearchQuery.value = query
    }

    fun setStudentLevelFilter(level: String?) {
        _studentLevelFilter.value = level
    }

    private data class SlotFilters(
        val dateFilter: String?,
        val onlyAvailable: Boolean,
        val typeFilter: String?,
        val onlyMine: Boolean,
        val student: StudentEntity?
    )

    private val _slotFilters: Flow<SlotFilters> = combine(
        _selectedDateFilter,
        _filterOnlyAvailable,
        _filterLessonType,
        _filterOnlyMyBookings,
        _currentStudent
    ) { dateFilter, onlyAvailable, typeFilter, onlyMine, student ->
        SlotFilters(dateFilter, onlyAvailable, typeFilter, onlyMine, student)
    }

    // Filtered slots calculation
    val filteredSlots: StateFlow<List<SlotWithBookings>> = combine(
        slotsWithBookings,
        _slotFilters
    ) { slots, filters ->
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(Date())
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowIso = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, 6)
        val endOfWeekIso = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE).format(cal.time)

        slots.filter { item ->
            // Date filter
            val dateMatches = when (filters.dateFilter) {
                null, "TOUS" -> true
                "TODAY" -> item.slot.dateIso == todayIso
                "TOMORROW" -> item.slot.dateIso == tomorrowIso
                "WEEK" -> item.slot.dateIso in todayIso..endOfWeekIso
                else -> item.slot.dateIso == filters.dateFilter
            }

            // Availability filter
            val availableMatches = if (filters.onlyAvailable) !item.isFull && !item.slot.isCancelled else true

            // Type filter
            val typeMatches = if (filters.typeFilter != null) item.slot.lessonType.equals(filters.typeFilter, ignoreCase = true) else true

            // Only mine filter
            val mineMatches = if (filters.onlyMine && filters.student != null) {
                item.enrolledStudentIds.contains(filters.student.id)
            } else true

            dateMatches && availableMatches && typeMatches && mineMatches
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered students list
    val filteredStudents: StateFlow<List<StudentEntity>> = combine(
        allStudents,
        _studentSearchQuery,
        _studentLevelFilter
    ) { students, query, level ->
        students.filter { student ->
            val matchesQuery = query.isBlank() ||
                    student.fullName.contains(query, ignoreCase = true) ||
                    student.phone.contains(query) ||
                    student.equipment.contains(query, ignoreCase = true)

            val matchesLevel = level == null || student.level.contains(level, ignoreCase = true)

            matchesQuery && matchesLevel
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Slot Operations ---
    fun createSlot(
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
    ) {
        viewModelScope.launch {
            val slot = LessonSlotEntity(
                dateIso = dateIso,
                startTime = startTime,
                endTime = endTime,
                title = title.ifBlank { "${ParamoteurLessonType.fromCode(lessonType).label} ($startTime-$endTime)" },
                lessonType = lessonType,
                location = location.ifBlank { "Base Paramoteur - Piste Principale" },
                maxCapacity = maxCapacity.coerceAtLeast(1),
                weatherStatus = weatherStatus,
                windInfo = windInfo.ifBlank { "Vent calme laminaire" },
                instructorNotes = instructorNotes
            )
            repository.createSlot(slot)
            _feedbackMessage.emit("Nouveau créneau créé avec succès !")
        }
    }

    fun updateSlot(slot: LessonSlotEntity) {
        viewModelScope.launch {
            repository.updateSlot(slot)
            _feedbackMessage.emit("Créneau mis à jour !")
        }
    }

    fun deleteSlot(slotId: Long) {
        viewModelScope.launch {
            repository.deleteSlot(slotId)
            _feedbackMessage.emit("Créneau supprimé")
        }
    }

    fun setWeather(slotId: Long, status: String, windInfo: String? = null) {
        viewModelScope.launch {
            repository.setSlotWeather(slotId, status, windInfo)
            _feedbackMessage.emit("Statut météo mis à jour (${SlotWeather.fromCode(status).label})")
        }
    }

    // --- Booking Operations ---
    fun toggleStudentEnrollment(slot: SlotWithBookings, student: StudentEntity) {
        viewModelScope.launch {
            val isAlreadyEnrolled = slot.enrolledStudentIds.contains(student.id)
            if (isAlreadyEnrolled) {
                repository.unenrollStudent(slot.slot.id, student.id)
                _feedbackMessage.emit("Désinscription confirmée pour ${student.firstName}")
            } else {
                val isFull = slot.isFull
                val success = repository.enrollStudent(
                    slotId = slot.slot.id,
                    studentId = student.id,
                    isWaitingList = isFull
                )
                if (success) {
                    if (isFull) {
                        _feedbackMessage.emit("${student.firstName} ajouté en liste d'attente !")
                    } else {
                        _feedbackMessage.emit("Inscription confirmée pour ${student.firstName} !")
                    }
                }
            }
        }
    }

    fun instructorEnroll(slotId: Long, studentId: Long, isWaitingList: Boolean = false) {
        viewModelScope.launch {
            val success = repository.enrollStudent(slotId, studentId, isWaitingList)
            if (success) {
                _feedbackMessage.emit("Élève inscrit au créneau !")
            } else {
                _feedbackMessage.emit("Cet élève est déjà inscrit à ce créneau.")
            }
        }
    }

    fun instructorUnenroll(slotId: Long, studentId: Long) {
        viewModelScope.launch {
            repository.unenrollStudent(slotId, studentId)
            _feedbackMessage.emit("Inscription annulée.")
        }
    }

    fun toggleAttendance(bookingId: Long, studentId: Long, currentAttended: Boolean) {
        viewModelScope.launch {
            repository.toggleAttendance(bookingId, studentId, !currentAttended)
            if (!currentAttended) {
                _feedbackMessage.emit("Présence validée (+1 séance enregistrée) !")
            }
        }
    }

    // --- Student Operations ---
    fun saveStudent(
        id: Long = 0,
        firstName: String,
        lastName: String,
        phone: String,
        email: String,
        level: String,
        equipment: String,
        notes: String
    ) {
        viewModelScope.launch {
            val student = StudentEntity(
                id = id,
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                phone = phone.trim(),
                email = email.trim(),
                level = level,
                equipment = equipment.trim().ifBlank { "Matériel École" },
                notes = notes.trim()
            )
            if (id == 0L) {
                repository.createStudent(student)
                _feedbackMessage.emit("Nouvel élève ${student.fullName} enregistré !")
            } else {
                repository.updateStudent(student)
                _feedbackMessage.emit("Fiche élève ${student.fullName} mise à jour !")
            }
        }
    }

    fun deleteStudent(student: StudentEntity) {
        viewModelScope.launch {
            repository.deleteStudent(student)
            _feedbackMessage.emit("Élève ${student.fullName} supprimé.")
        }
    }

    fun quickGenerateWeekendSlots() {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            // Find next Saturday
            while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
            val satIso = dateFormat.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val sunIso = dateFormat.format(cal.time)

            val slots = listOf(
                LessonSlotEntity(
                    dateIso = satIso,
                    startTime = "06:30",
                    endTime = "09:30",
                    title = "Samedi Matin : Grands Vols & Radioguidage",
                    lessonType = "GRAND_VOL",
                    location = "Base Paramoteur",
                    maxCapacity = 3,
                    weatherStatus = "OPTIMAL",
                    windInfo = "4-8 km/h Est",
                    instructorNotes = "Briefing tour de piste à 06h15"
                ),
                LessonSlotEntity(
                    dateIso = satIso,
                    startTime = "18:00",
                    endTime = "20:30",
                    title = "Samedi Soir : Pente École & Gonflage",
                    lessonType = "GONFLAGE",
                    location = "Pente Nord",
                    maxCapacity = 4,
                    weatherStatus = "OPTIMAL",
                    windInfo = "10 km/h Ouest laminaire",
                    instructorNotes = "Face voile et décollage dos"
                ),
                LessonSlotEntity(
                    dateIso = sunIso,
                    startTime = "07:00",
                    endTime = "10:30",
                    title = "Dimanche Matin : Navigation GPS & Cross",
                    lessonType = "NAVIGATION",
                    location = "Base Paramoteur",
                    maxCapacity = 3,
                    weatherStatus = "OPTIMAL",
                    windInfo = "5-9 km/h Sud",
                    instructorNotes = "Parcours 35 km - Apportez tablettes et batteries"
                ),
                LessonSlotEntity(
                    dateIso = sunIso,
                    startTime = "17:30",
                    endTime = "20:30",
                    title = "Dimanche Soir : Précision & Coucher de Soleil",
                    lessonType = "PRECISION",
                    location = "Base Paramoteur",
                    maxCapacity = 3,
                    weatherStatus = "OPTIMAL",
                    windInfo = "6 km/h Ouest doux",
                    instructorNotes = "Cible de précision 10m"
                )
            )

            for (s in slots) {
                repository.createSlot(s)
            }
            _feedbackMessage.emit("4 créneaux de week-end générés avec succès !")
        }
    }

    fun getWhatsAppText(): String {
        return repository.generateWhatsAppPlanningText(slotsWithBookings.value)
    }
}

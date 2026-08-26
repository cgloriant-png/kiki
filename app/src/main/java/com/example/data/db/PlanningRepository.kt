package com.example.data.db

import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.text.SimpleDateFormat
import java.util.*

class PlanningRepository(private val planningDao: PlanningDao) {

    val allStudents: Flow<List<StudentEntity>> = planningDao.getAllStudents()
    val allSlots: Flow<List<LessonSlotEntity>> = planningDao.getAllSlots()
    val allBookings: Flow<List<BookingEntity>> = planningDao.getAllBookings()

    // Combined Flow: Slots with enrolled students
    val slotsWithBookings: Flow<List<SlotWithBookings>> = combine(
        allSlots,
        allBookings,
        allStudents
    ) { slots, bookings, students ->
        val studentMap = students.associateBy { it.id }
        val bookingBySlotId = bookings.groupBy { it.slotId }

        slots.map { slot ->
            val slotBookings = bookingBySlotId[slot.id].orEmpty().mapNotNull { booking ->
                studentMap[booking.studentId]?.let { student ->
                    BookingWithStudent(booking, student)
                }
            }
            SlotWithBookings(slot, slotBookings)
        }
    }

    suspend fun initializeSampleDataIfNeeded() {
        val count = planningDao.getStudentsCount()
        if (count == 0) {
            val sampleStudents = listOf(
                StudentEntity(
                    id = 1,
                    firstName = "Julien",
                    lastName = "Mercier",
                    phone = "06 12 34 56 78",
                    email = "julien.mercier@email.fr",
                    level = "Débutant - Pente école & Gonflage",
                    equipment = "Matériel École (Atom 80 + Voile Mojo 24)",
                    completedSessions = 3,
                    totalFlightHours = 0.5,
                    notes = "Très bon sens du vent. Gonflage face voile en cours d'acquisition."
                ),
                StudentEntity(
                    id = 2,
                    firstName = "Sophie",
                    lastName = "Bernard",
                    phone = "06 23 45 67 89",
                    email = "sophie.bernard@email.fr",
                    level = "Premiers Grands Vols (Lâcher solo)",
                    equipment = "Perso : Voile Dudek Universal 26 + Vittorazi Moster 185",
                    completedSessions = 8,
                    totalFlightHours = 3.2,
                    notes = "Décollages dos parfaits. Travailler le palier d'arrondi à l'atterrissage."
                ),
                StudentEntity(
                    id = 3,
                    firstName = "Thomas",
                    lastName = "Laurent",
                    phone = "06 34 56 78 90",
                    email = "thomas.laurent@email.fr",
                    level = "Autonome - Navigation GPS & Cross",
                    equipment = "Perso : Ozone Spyder 3 + Thor 202",
                    completedSessions = 15,
                    totalFlightHours = 14.5,
                    notes = "Prêt pour l'emport passager. Navigation triangulaire 40 km validée."
                ),
                StudentEntity(
                    id = 4,
                    firstName = "Lucas",
                    lastName = "Dubois",
                    phone = "06 45 67 89 01",
                    email = "lucas.dubois@email.fr",
                    level = "Premiers Grands Vols (Lâcher solo)",
                    equipment = "Matériel École (Moster 185 Plus + ITV Boxer 2)",
                    completedSessions = 6,
                    totalFlightHours = 2.0,
                    notes = "Bonne écoute radio. Bien tenir l'axe de décollage vent de face."
                ),
                StudentEntity(
                    id = 5,
                    firstName = "Émilie",
                    lastName = "Moreau",
                    phone = "06 56 78 90 12",
                    email = "emilie.moreau@email.fr",
                    level = "Débutant - Pente école & Gonflage",
                    equipment = "Matériel École",
                    completedSessions = 4,
                    totalFlightHours = 0.0,
                    notes = "Course d'élan dynamique. À entraîner sur le centrage voile."
                ),
                StudentEntity(
                    id = 6,
                    firstName = "Maxime",
                    lastName = "Petit",
                    phone = "06 67 89 01 23",
                    email = "maxime.petit@email.fr",
                    level = "Autonome - Navigation GPS & Cross",
                    equipment = "Perso : Dudek Nucleon 4 + Polini Thor 190",
                    completedSessions = 18,
                    totalFlightHours = 22.0,
                    notes = "Travail sur la gestion carburant et optimisation de vitesse."
                ),
                StudentEntity(
                    id = 7,
                    firstName = "Alexandre",
                    lastName = "Martin",
                    phone = "06 78 90 12 34",
                    email = "alex.martin@email.fr",
                    level = "Breveté - Perfectionnement & Maniabilité",
                    equipment = "Perso : BGD Echo 2 + Moster 185 EFI",
                    completedSessions = 24,
                    totalFlightHours = 35.0,
                    notes = "Pilotage sellette acquis. Décrochages parachutaux maîtrisés."
                ),
                StudentEntity(
                    id = 8,
                    firstName = "Camille",
                    lastName = "Roux",
                    phone = "06 89 01 23 45",
                    email = "camille.roux@email.fr",
                    level = "Débutant - Pente école & Gonflage",
                    equipment = "Matériel École",
                    completedSessions = 2,
                    totalFlightHours = 0.0,
                    notes = "Découverte des commandes et sécurité sol."
                ),
                StudentEntity(
                    id = 9,
                    firstName = "Nicolas",
                    lastName = "Fournier",
                    phone = "06 90 12 34 56",
                    email = "nicolas.fournier@email.fr",
                    level = "Premiers Grands Vols (Lâcher solo)",
                    equipment = "Matériel École",
                    completedSessions = 7,
                    totalFlightHours = 2.8,
                    notes = "Très régulier en tour de piste. Respecte parfaitement les altitudes."
                ),
                StudentEntity(
                    id = 10,
                    firstName = "Antoine",
                    lastName = "Girard",
                    phone = "06 01 23 45 67",
                    email = "antoine.girard@email.fr",
                    level = "Autonome - Navigation GPS & Cross",
                    equipment = "Perso : Ozone Roadster 3 + Moster Silent",
                    completedSessions = 14,
                    totalFlightHours = 12.0,
                    notes = "Excellente gestion des espaces aériens et zones R."
                ),
                StudentEntity(
                    id = 11,
                    firstName = "Cédric",
                    lastName = "Lefebvre",
                    phone = "06 11 22 33 44",
                    email = "cedric.lefebvre@email.fr",
                    level = "Débutant - Pente école & Gonflage",
                    equipment = "Matériel École",
                    completedSessions = 1,
                    totalFlightHours = 0.0,
                    notes = "Première séance théorique validée."
                ),
                StudentEntity(
                    id = 12,
                    firstName = "Marie",
                    lastName = "Guillot",
                    phone = "06 22 33 44 55",
                    email = "marie.guillot@email.fr",
                    level = "Premiers Grands Vols (Lâcher solo)",
                    equipment = "Perso : Voile MacPara Charger 2 + Atom 80",
                    completedSessions = 9,
                    totalFlightHours = 4.1,
                    notes = "Très douce aux commandes. Prête pour les vols thermiques matinaux."
                ),
                StudentEntity(
                    id = 13,
                    firstName = "Romain",
                    lastName = "Bonnet",
                    phone = "06 33 44 55 66",
                    email = "romain.bonnet@email.fr",
                    level = "Breveté - Perfectionnement & Maniabilité",
                    equipment = "Perso : Flow Cosmos Power + Thor 200",
                    completedSessions = 20,
                    totalFlightHours = 28.0,
                    notes = "Entraînement aux pannes moteur simulées en finale."
                ),
                StudentEntity(
                    id = 14,
                    firstName = "Pierre",
                    lastName = "Dupont",
                    phone = "06 44 55 66 77",
                    email = "pierre.dupont@email.fr",
                    level = "Débutant - Pente école & Gonflage",
                    equipment = "Matériel École",
                    completedSessions = 3,
                    totalFlightHours = 0.0,
                    notes = "Gonflage dos régulier. Passage sous portique moteur prévu."
                ),
                StudentEntity(
                    id = 15,
                    firstName = "Pauline",
                    lastName = "Fontaine",
                    phone = "06 55 66 77 88",
                    email = "pauline.fontaine@email.fr",
                    level = "Premiers Grands Vols (Lâcher solo)",
                    equipment = "Matériel École",
                    completedSessions = 5,
                    totalFlightHours = 1.5,
                    notes = "Gestion du gaz progressive. Atterrissages debout impeccables."
                ),
                StudentEntity(
                    id = 16,
                    firstName = "Guillaume",
                    lastName = "Chevalier",
                    phone = "06 66 77 88 99",
                    email = "guillaume.c@email.fr",
                    level = "Autonome - Navigation GPS & Cross",
                    equipment = "Perso : Apco Lift 2EZ + Moster 185",
                    completedSessions = 16,
                    totalFlightHours = 18.0,
                    notes = "Navigation au compas et GPS validée."
                ),
                StudentEntity(
                    id = 17,
                    firstName = "David",
                    lastName = "Lambert",
                    phone = "06 77 88 99 00",
                    email = "david.lambert@email.fr",
                    level = "Débutant - Pente école & Gonflage",
                    equipment = "Matériel École",
                    completedSessions = 2,
                    totalFlightHours = 0.0,
                    notes = "Bon feeling de l'aile. Affiner la temporisation."
                ),
                StudentEntity(
                    id = 18,
                    firstName = "Hugo",
                    lastName = "Garnier",
                    phone = "06 88 99 00 11",
                    email = "hugo.garnier@email.fr",
                    level = "Premiers Grands Vols (Lâcher solo)",
                    equipment = "Matériel École",
                    completedSessions = 10,
                    totalFlightHours = 5.0,
                    notes = "Autonome sur la check-list pré-vol. Prêt pour brevet théorique."
                ),
                StudentEntity(
                    id = 19,
                    firstName = "Benjamin",
                    lastName = "Faure",
                    phone = "06 99 00 11 22",
                    email = "benjamin.faure@email.fr",
                    level = "Breveté - Perfectionnement & Maniabilité",
                    equipment = "Perso : Ozone Viper 5 + Moster 185 Factory",
                    completedSessions = 30,
                    totalFlightHours = 52.0,
                    notes = "Slalom et virages engagés à 360° bien gérés."
                ),
                StudentEntity(
                    id = 20,
                    firstName = "Clara",
                    lastName = "Vidal",
                    phone = "06 00 11 22 33",
                    email = "clara.vidal@email.fr",
                    level = "Débutant - Pente école & Gonflage",
                    equipment = "Matériel École",
                    completedSessions = 4,
                    totalFlightHours = 0.0,
                    notes = "Décollage face voile propre, prête pour les premières tractions moteur."
                )
            )
            planningDao.insertStudents(sampleStudents)

            // Seed initial calendar slots for the next 5 days
            val cal = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)

            val slots = mutableListOf<LessonSlotEntity>()
            val bookings = mutableListOf<BookingEntity>()

            // Day 1 (Aujourd'hui)
            val d0 = dateFormat.format(cal.time)
            slots.add(
                LessonSlotEntity(
                    id = 1,
                    dateIso = d0,
                    startTime = "07:00",
                    endTime = "09:30",
                    title = "Vol du Matin & Tours de Piste",
                    lessonType = "GRAND_VOL",
                    location = "Base Paramoteur - Piste Principale",
                    maxCapacity = 3,
                    weatherStatus = "OPTIMAL",
                    windInfo = "5 km/h Est - Ciel dégagé, air très calme",
                    instructorNotes = "Radio VHF 143.9875 MHz - Briefing sécurité à 06h45"
                )
            )
            slots.add(
                LessonSlotEntity(
                    id = 2,
                    dateIso = d0,
                    startTime = "18:00",
                    endTime = "20:30",
                    title = "Session Gonflage & Pente École du Soir",
                    lessonType = "GONFLAGE",
                    location = "Pente Nord - Terrain Herbe",
                    maxCapacity = 4,
                    weatherStatus = "OPTIMAL",
                    windInfo = "10 km/h Ouest régulier",
                    instructorNotes = "Casques et gants obligatoires"
                )
            )

            // Day 2 (Demain)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val d1 = dateFormat.format(cal.time)
            slots.add(
                LessonSlotEntity(
                    id = 3,
                    dateIso = d1,
                    startTime = "06:45",
                    endTime = "09:15",
                    title = "Grands Vols & Perfectionnement Décollage",
                    lessonType = "GRAND_VOL",
                    location = "Base Paramoteur",
                    maxCapacity = 3,
                    weatherStatus = "OPTIMAL",
                    windInfo = "6 km/h Sud-Est",
                    instructorNotes = "Prise de terrain en U et approche finale moteur ralenti"
                )
            )
            slots.add(
                LessonSlotEntity(
                    id = 4,
                    dateIso = d1,
                    startTime = "18:15",
                    endTime = "20:45",
                    title = "Navigation GPS & Cross Paramoteur",
                    lessonType = "NAVIGATION",
                    location = "Base Paramoteur - Piste Principale",
                    maxCapacity = 3,
                    weatherStatus = "TO_CONFIRM",
                    windInfo = "12 km/h OSO - Évolution brise à surveiller",
                    instructorNotes = "Apportez vos instruments / smartphones chargés"
                )
            )

            // Day 3 (Après-demain)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val d2 = dateFormat.format(cal.time)
            slots.add(
                LessonSlotEntity(
                    id = 5,
                    dateIso = d2,
                    startTime = "07:00",
                    endTime = "10:00",
                    title = "Session Gonflage Dos & Face Voile",
                    lessonType = "GONFLAGE",
                    location = "Pente École Ouest",
                    maxCapacity = 4,
                    weatherStatus = "OPTIMAL",
                    windInfo = "8-12 km/h Ouest laminaire",
                    instructorNotes = "Idéal débutants et perfectionnement commande dynamique"
                )
            )
            slots.add(
                LessonSlotEntity(
                    id = 6,
                    dateIso = d2,
                    startTime = "18:00",
                    endTime = "20:30",
                    title = "Précision d'Atterrissage & Exercices Panne",
                    lessonType = "PRECISION",
                    location = "Base Paramoteur - Cible Centrale",
                    maxCapacity = 3,
                    weatherStatus = "OPTIMAL",
                    windInfo = "7 km/h Nord-Ouest",
                    instructorNotes = "Toucher cible au cône des 10m"
                )
            )

            // Day 4 (Week-end Matin)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val d3 = dateFormat.format(cal.time)
            slots.add(
                LessonSlotEntity(
                    id = 7,
                    dateIso = d3,
                    startTime = "06:30",
                    endTime = "10:00",
                    title = "Grand Stage Week-End : Vols & Radioguidage",
                    lessonType = "GRAND_VOL",
                    location = "Base Paramoteur",
                    maxCapacity = 4,
                    weatherStatus = "OPTIMAL",
                    windInfo = "4 km/h Sud - Conditions vol parfaites",
                    instructorNotes = "3 passages radio par élève - Café offert à 06h15"
                )
            )
            slots.add(
                LessonSlotEntity(
                    id = 8,
                    dateIso = d3,
                    startTime = "17:30",
                    endTime = "20:30",
                    title = "Vol Coucher de Soleil & Vol Rando",
                    lessonType = "PERFECTIONNEMENT",
                    location = "Base Paramoteur",
                    maxCapacity = 3,
                    weatherStatus = "OPTIMAL",
                    windInfo = "8 km/h Ouest doux",
                    instructorNotes = "Vol de groupe sécurisé autour de la vallée"
                )
            )

            planningDao.insertSlots(slots)

            // Sample registrations for active slots
            bookings.add(BookingEntity(slotId = 1, studentId = 2, isWaitingList = false)) // Sophie
            bookings.add(BookingEntity(slotId = 1, studentId = 4, isWaitingList = false)) // Lucas
            bookings.add(BookingEntity(slotId = 2, studentId = 1, isWaitingList = false)) // Julien
            bookings.add(BookingEntity(slotId = 2, studentId = 5, isWaitingList = false)) // Emilie
            bookings.add(BookingEntity(slotId = 2, studentId = 8, isWaitingList = false)) // Camille
            bookings.add(BookingEntity(slotId = 3, studentId = 9, isWaitingList = false)) // Nicolas
            bookings.add(BookingEntity(slotId = 3, studentId = 12, isWaitingList = false)) // Marie
            bookings.add(BookingEntity(slotId = 4, studentId = 3, isWaitingList = false)) // Thomas
            bookings.add(BookingEntity(slotId = 4, studentId = 6, isWaitingList = false)) // Maxime
            bookings.add(BookingEntity(slotId = 4, studentId = 10, isWaitingList = false)) // Antoine (Complet!)
            bookings.add(BookingEntity(slotId = 4, studentId = 16, isWaitingList = true)) // Guillaume (Liste d'attente)
            bookings.add(BookingEntity(slotId = 7, studentId = 2, isWaitingList = false)) // Sophie
            bookings.add(BookingEntity(slotId = 7, studentId = 4, isWaitingList = false)) // Lucas
            bookings.add(BookingEntity(slotId = 7, studentId = 15, isWaitingList = false)) // Pauline
            bookings.add(BookingEntity(slotId = 7, studentId = 18, isWaitingList = false)) // Hugo (Complet!)
            bookings.add(BookingEntity(slotId = 8, studentId = 7, isWaitingList = false)) // Alex
            bookings.add(BookingEntity(slotId = 8, studentId = 13, isWaitingList = false)) // Romain

            planningDao.insertBookings(bookings)
        }
    }

    // --- Slot Actions ---
    suspend fun createSlot(slot: LessonSlotEntity): Long = planningDao.insertSlot(slot)

    suspend fun updateSlot(slot: LessonSlotEntity) = planningDao.updateSlot(slot)

    suspend fun deleteSlot(slotId: Long) = planningDao.deleteSlotById(slotId)

    suspend fun setSlotWeather(slotId: Long, weatherStatus: String, windInfo: String? = null) {
        val slot = planningDao.getSlotById(slotId) ?: return
        val updated = slot.copy(
            weatherStatus = weatherStatus,
            windInfo = windInfo ?: slot.windInfo,
            isCancelled = (weatherStatus == "CANCELLED")
        )
        planningDao.updateSlot(updated)
    }

    // --- Student Actions ---
    suspend fun createStudent(student: StudentEntity): Long = planningDao.insertStudent(student)

    suspend fun updateStudent(student: StudentEntity) = planningDao.updateStudent(student)

    suspend fun deleteStudent(student: StudentEntity) = planningDao.deleteStudent(student)

    suspend fun getStudentById(studentId: Long): StudentEntity? = planningDao.getStudentById(studentId)

    // --- Booking Actions ---
    suspend fun enrollStudent(slotId: Long, studentId: Long, isWaitingList: Boolean = false): Boolean {
        try {
            planningDao.insertBooking(
                BookingEntity(
                    slotId = slotId,
                    studentId = studentId,
                    isWaitingList = isWaitingList
                )
            )
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    suspend fun unenrollStudent(slotId: Long, studentId: Long) {
        planningDao.deleteBookingBySlotAndStudent(slotId, studentId)
    }

    suspend fun toggleAttendance(bookingId: Long, studentId: Long, attended: Boolean) {
        planningDao.updateAttendance(bookingId, attended)
        if (attended) {
            planningDao.incrementCompletedSessions(studentId)
        }
    }

    // Generate formatted WhatsApp message for quick broadcast to students
    fun generateWhatsAppPlanningText(slotsWithBookings: List<SlotWithBookings>): String {
        val sb = StringBuilder()
        sb.append("🪂 *ÉCOLE PARAMOTEUR - PLANNING DES COURS & CRÉNEAUX* 🪂\n")
        sb.append("Bonjour à tous ! Voici les créneaux de vol ouverts pour la semaine :\n\n")

        val groupedByDate = slotsWithBookings.groupBy { it.slot.dateIso }
        val dateIn = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
        val dateOut = SimpleDateFormat("EEEE d MMMM", Locale.FRANCE)

        groupedByDate.forEach { (dateIso, daySlots) ->
            val formattedDate = try {
                dateIn.parse(dateIso)?.let { dateOut.format(it).replaceFirstChar { c -> c.uppercase() } } ?: dateIso
            } catch (e: Exception) {
                dateIso
            }

            sb.append("📅 *").append(formattedDate).append("*\n")

            daySlots.forEach { item ->
                val slot = item.slot
                val type = ParamoteurLessonType.fromCode(slot.lessonType)
                val weather = SlotWeather.fromCode(slot.weatherStatus)

                sb.append("  • *").append(slot.startTime).append(" - ").append(slot.endTime).append("* | ")
                sb.append(type.emoji).append(" ").append(type.label).append("\n")
                sb.append("    📍 ").append(slot.location).append("\n")
                sb.append("    🌤️ Météo : ").append(weather.iconEmoji).append(" ").append(slot.windInfo).append("\n")

                val enrolled = item.confirmedBookings.map { it.student.firstName }
                val dispo = item.availablePlaces

                if (slot.isCancelled) {
                    sb.append("    ⚠️ *CRÉNEAU ANNULÉ MÉTÉO*\n")
                } else if (item.isFull) {
                    sb.append("    🔴 *COMPLET* (").append(enrolled.joinToString(", ")).append(")\n")
                    if (item.waitingListBookings.isNotEmpty()) {
                        val waitList = item.waitingListBookings.map { it.student.firstName }
                        sb.append("    ⏳ Liste d'attente : ").append(waitList.joinToString(", ")).append("\n")
                    }
                } else {
                    val names = if (enrolled.isNotEmpty()) " (Inscrits : ${enrolled.joinToString(", ")})" else ""
                    sb.append("    🟢 *").append(dispo).append(" place(s) dispo* / ").append(slot.maxCapacity).append(" max").append(names).append("\n")
                }

                if (slot.instructorNotes.isNotBlank()) {
                    sb.append("    💬 _").append(slot.instructorNotes).append("_\n")
                }
                sb.append("\n")
            }
        }

        sb.append("👉 Inscrivez-vous vite dans l'application pour réserver votre créneau !")
        return sb.toString()
    }
}

package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val firstName: String,
    val lastName: String,
    val phone: String,
    val email: String,
    val level: String, // "Débutant - Pente école & Gonflage", "Premiers Grands Vols", "Autonome & Navigation", "Breveté Perfectionnement"
    val equipment: String = "Matériel École",
    val wingModel: String = "",
    val motorModel: String = "",
    val completedSessions: Int = 0,
    val totalFlightHours: Double = 0.0,
    val notes: String = "",
    val emergencyContact: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val fullName: String get() = "$firstName $lastName".trim()
    val initials: String get() = "${firstName.take(1)}${lastName.take(1)}".uppercase()
}

@Entity(tableName = "lesson_slots")
data class LessonSlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateIso: String, // "YYYY-MM-DD"
    val startTime: String, // "07:00"
    val endTime: String, // "09:30"
    val title: String, // e.g. "Session Matin Calme - Pente École"
    val lessonType: String, // GONFLAGE, GRAND_VOL, NAVIGATION, PRECISION, THEORIE, PERFECTIONNEMENT
    val location: String = "Base Paramoteur - Piste Principale",
    val maxCapacity: Int = 3,
    val weatherStatus: String = "OPTIMAL", // OPTIMAL, TO_CONFIRM, CANCELLED, COMPLETED
    val windInfo: String = "5-10 km/h Ouest",
    val instructorNotes: String = "",
    val isCancelled: Boolean = false,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "bookings",
    foreignKeys = [
        ForeignKey(
            entity = LessonSlotEntity::class,
            parentColumns = ["id"],
            childColumns = ["slotId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("slotId"),
        Index("studentId"),
        Index(value = ["slotId", "studentId"], unique = true)
    ]
)
data class BookingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val slotId: Long,
    val studentId: Long,
    val registeredAt: Long = System.currentTimeMillis(),
    val isWaitingList: Boolean = false,
    val attended: Boolean = false,
    val debriefNotes: String = ""
)

data class BookingWithStudent(
    val booking: BookingEntity,
    val student: StudentEntity
)

data class SlotWithBookings(
    val slot: LessonSlotEntity,
    val bookings: List<BookingWithStudent> = emptyList()
) {
    val confirmedBookings: List<BookingWithStudent> get() = bookings.filter { !it.booking.isWaitingList }
    val waitingListBookings: List<BookingWithStudent> get() = bookings.filter { it.booking.isWaitingList }
    val availablePlaces: Int get() = (slot.maxCapacity - confirmedBookings.size).coerceAtLeast(0)
    val isFull: Boolean get() = availablePlaces == 0
    val enrolledStudentIds: Set<Long> get() = bookings.map { it.student.id }.toSet()
}

data class BookingWithSlot(
    val booking: BookingEntity,
    val slot: LessonSlotEntity
)

data class StudentWithBookings(
    val student: StudentEntity,
    val bookings: List<BookingWithSlot> = emptyList()
)

enum class ParamoteurLessonType(val code: String, val label: String, val emoji: String, val defaultCapacity: Int) {
    GONFLAGE("GONFLAGE", "Gonflage & Pente École", "🪂", 4),
    GRAND_VOL("GRAND_VOL", "Grands Vols Guidés Radio", "✈️", 2),
    NAVIGATION("NAVIGATION", "Navigation GPS & Cross", "🧭", 3),
    PRECISION("PRECISION", "Précision Atterrissage / Panne", "🎯", 3),
    THEORIE("THEORIE", "Briefing Météo & Théorie", "📖", 8),
    PERFECTIONNEMENT("PERFECTIONNEMENT", "Perfectionnement & Maniabilité", "🦅", 3);

    companion object {
        fun fromCode(code: String): ParamoteurLessonType {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: GONFLAGE
        }
    }
}

enum class SlotWeather(val code: String, val label: String, val iconEmoji: String) {
    OPTIMAL("OPTIMAL", "Conditions Idéales", "🟢"),
    TO_CONFIRM("TO_CONFIRM", "À Confirmer (Brise/Vent)", "🟡"),
    CANCELLED("CANCELLED", "Annulé Météo", "🔴"),
    COMPLETED("COMPLETED", "Séance Terminée", "✅");

    companion object {
        fun fromCode(code: String): SlotWeather {
            return entries.find { it.code.equals(code, ignoreCase = true) } ?: OPTIMAL
        }
    }
}

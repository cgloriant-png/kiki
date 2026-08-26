package com.example.data.db

import androidx.room.*
import com.example.data.model.BookingEntity
import com.example.data.model.LessonSlotEntity
import com.example.data.model.StudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanningDao {

    // --- Students ---
    @Query("SELECT * FROM students ORDER BY lastName ASC, firstName ASC")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getStudentById(id: Long): StudentEntity?

    @Query("SELECT COUNT(*) FROM students")
    suspend fun getStudentsCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>)

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Delete
    suspend fun deleteStudent(student: StudentEntity)

    // --- Slots ---
    @Query("SELECT * FROM lesson_slots ORDER BY dateIso ASC, startTime ASC")
    fun getAllSlots(): Flow<List<LessonSlotEntity>>

    @Query("SELECT * FROM lesson_slots WHERE dateIso = :dateIso ORDER BY startTime ASC")
    fun getSlotsByDate(dateIso: String): Flow<List<LessonSlotEntity>>

    @Query("SELECT * FROM lesson_slots WHERE id = :slotId LIMIT 1")
    suspend fun getSlotById(slotId: Long): LessonSlotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: LessonSlotEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlots(slots: List<LessonSlotEntity>)

    @Update
    suspend fun updateSlot(slot: LessonSlotEntity)

    @Delete
    suspend fun deleteSlot(slot: LessonSlotEntity)

    @Query("DELETE FROM lesson_slots WHERE id = :slotId")
    suspend fun deleteSlotById(slotId: Long)

    // --- Bookings ---
    @Query("SELECT * FROM bookings WHERE slotId = :slotId")
    fun getBookingsForSlot(slotId: Long): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE studentId = :studentId")
    fun getBookingsForStudent(studentId: Long): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings")
    fun getAllBookings(): Flow<List<BookingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooking(booking: BookingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookings(bookings: List<BookingEntity>)

    @Update
    suspend fun updateBooking(booking: BookingEntity)

    @Delete
    suspend fun deleteBooking(booking: BookingEntity)

    @Query("DELETE FROM bookings WHERE slotId = :slotId AND studentId = :studentId")
    suspend fun deleteBookingBySlotAndStudent(slotId: Long, studentId: Long)

    @Query("UPDATE bookings SET attended = :attended WHERE id = :bookingId")
    suspend fun updateAttendance(bookingId: Long, attended: Boolean)

    @Query("UPDATE students SET completedSessions = completedSessions + 1 WHERE id = :studentId")
    suspend fun incrementCompletedSessions(studentId: Long)
}

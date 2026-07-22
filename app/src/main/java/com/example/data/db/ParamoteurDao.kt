package com.example.data.db

import androidx.room.*
import com.example.data.model.CompetitionEntity
import com.example.data.model.CourseEntity
import com.example.data.model.FlightHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParamoteurDao {
    // Courses
    @Query("SELECT * FROM courses ORDER BY updatedAt DESC")
    fun getAllCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE slug = :slug LIMIT 1")
    suspend fun getCourseBySlug(slug: String): CourseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: CourseEntity)

    @Query("DELETE FROM courses WHERE slug = :slug")
    suspend fun deleteCourseBySlug(slug: String)

    // Competitions
    @Query("SELECT * FROM competitions ORDER BY updatedAt DESC")
    fun getAllCompetitions(): Flow<List<CompetitionEntity>>

    @Query("SELECT * FROM competitions WHERE slug = :slug LIMIT 1")
    suspend fun getCompetitionBySlug(slug: String): CompetitionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompetition(comp: CompetitionEntity)

    @Query("DELETE FROM competitions WHERE slug = :slug")
    suspend fun deleteCompetitionBySlug(slug: String)

    // Flight History
    @Query("SELECT * FROM flight_history WHERE courseSlug = :courseSlug ORDER BY id DESC")
    fun getHistoryForCourse(courseSlug: String): Flow<List<FlightHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: FlightHistoryEntity)
}

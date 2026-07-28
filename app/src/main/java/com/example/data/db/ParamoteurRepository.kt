package com.example.data.db

import com.example.data.model.*
import com.example.util.GeometryUtils
import com.example.util.JsonExportUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ParamoteurRepository(private val dao: ParamoteurDao) {

    val allCourses: Flow<List<CourseEntity>> = dao.getAllCourses()
    val allCompetitions: Flow<List<CompetitionEntity>> = dao.getAllCompetitions()

    suspend fun saveCourse(course: CourseData, slugOverride: String? = null): String {
        val slug = slugOverride ?: GeometryUtils.slugify(course.name)
        val jsonStr = JsonExportUtils.serializeCourse(course)
        val entity = CourseEntity(slug = slug, name = course.name, jsonContent = jsonStr)
        dao.insertCourse(entity)
        return slug
    }

    suspend fun loadCourse(slug: String): CourseData? {
        val entity = dao.getCourseBySlug(slug) ?: return null
        return try {
            JsonExportUtils.deserializeCourse(entity.jsonContent)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteCourse(slug: String) {
        dao.deleteCourseBySlug(slug)
    }

    suspend fun saveCompetition(competition: CompetitionData, slugOverride: String? = null): String {
        val slug = slugOverride ?: GeometryUtils.slugify(competition.name)
        val jsonStr = JsonExportUtils.serializeCompetition(competition)
        val entity = CompetitionEntity(slug = slug, name = competition.name, jsonContent = jsonStr)
        dao.insertCompetition(entity)
        return slug
    }

    suspend fun loadCompetition(slug: String): CompetitionData? {
        val entity = dao.getCompetitionBySlug(slug) ?: return null
        return try {
            JsonExportUtils.deserializeCompetition(entity.jsonContent)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteCompetition(slug: String) {
        dao.deleteCompetitionBySlug(slug)
    }

    fun getHistoryForCourse(slug: String): Flow<List<FlightHistoryEntity>> {
        return dao.getHistoryForCourse(slug)
    }

    suspend fun addHistoryItem(
        slug: String,
        epreuveType: String,
        score: Int,
        dateIso: String,
        resultJson: String? = null,
        traceJson: String? = null
    ) {
        val entity = FlightHistoryEntity(
            courseSlug = slug,
            dateIso = dateIso,
            epreuveType = epreuveType,
            score = score,
            resultJson = resultJson,
            traceJson = traceJson
        )
        dao.insertHistory(entity)
    }

    suspend fun deleteHistoryById(id: Long) {
        dao.deleteHistoryById(id)
    }
}

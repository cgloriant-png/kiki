package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.util.LatLng

enum class PointType(val label: String, val isCircle: Boolean) {
    SP("Porte d'entrée (SP)", false),
    FP("Porte de sortie (FP)", false),
    PORTE("Porte intermédiaire", false),
    TG("Porte de temps (TG)", false),
    BALISE("Balise simple", true),
    CACHEE("Balise cachée", true);

    companion object {
        fun fromCode(code: String): PointType {
            return when (code.lowercase()) {
                "sp" -> SP
                "fp" -> FP
                "porte" -> PORTE
                "tg" -> TG
                "balise" -> BALISE
                "cachee" -> CACHEE
                else -> BALISE
            }
        }
    }
}

data class CoursePoint(
    val id: String,
    val type: String, // SP, FP, porte, tg, balise, cachee
    var lat: Double,
    var lng: Double,
    var radius: Double = 100.0,
    var width: Double = 150.0
)

data class RouteVertex(
    val id: String,
    var lat: Double,
    var lng: Double,
    var smooth: Boolean = false
)

data class CoursePenalties(
    var requireSP: Boolean = true,
    var requireFP: Boolean = true,
    var noBacktrack: Boolean = true,
    var backtrackAngleDeg: Double = 45.0
)

data class CourseData(
    var name: String = "",
    var points: MutableList<CoursePoint> = mutableListOf(),
    var routeVertices: MutableList<RouteVertex> = mutableListOf(),
    var corridorWidth: Double = 200.0,
    var penalties: CoursePenalties = CoursePenalties()
)

data class GpxPoint(
    val lat: Double,
    val lng: Double,
    val ele: Double? = null,
    val time: Long? = null // Timestamp in ms
)

enum class EpreuveType(val code: String, val title: String) {
    PURE("pure", "B/C — Navigation pure (max balises)"),
    SNAKE("snake", "C — Navigation imposée (snake)"),
    PRECISION("precision", "C — Navigation précision"),
    ECO_DIST("ecoDist", "A — Économie distance"),
    ECO_PURE("ecoPure", "A — Économie pure (temps de vol)");

    companion object {
        fun fromCode(code: String): EpreuveType {
            return entries.find { it.code == code } ?: PURE
        }
    }
}

data class ScoringRef(
    var maxTimeMin: Double? = null,
    var nbmax: Double? = null,
    var tmin: Double? = null,
    var dmax: Double? = null,
    var tmax: Double? = null,
    var wGates: Double = 600.0,
    var wTime: Double = 300.0,
    var wSpeed: Double = 100.0,
    var wCouloir: Double = 0.0
)

data class PointValidationResult(
    val point: CoursePoint,
    val validated: Boolean,
    val traceIndex: Int? = null,
    val time: Long? = null,
    var declaredS: Double? = null,
    var actualS: Double? = null,
    var ecartS: Double? = null,
    var hi: Double? = null,
    var points: Int? = null
)

data class FlightAnalysisResult(
    val score: Int,
    val label: String,
    val bannerTxt: String,
    val results: List<PointValidationResult>,
    val distMeters: Double,
    val durationSeconds: Double?,
    val breakdown: Map<String, Int>? = null,
    val corridorStats: ConformityStats? = null,
    val error: String? = null,
    val faultPoint: LatLng? = null,
    val faultDescription: String? = null
)

data class ConformityStats(
    val pctPts: Int,
    val pctDist: Int?,
    val pctTime: Int?
)

data class Competitor(
    val id: String,
    var name: String
)

data class MancheResult(
    val score: Int,
    val distMeters: Double,
    val durationSeconds: Double?,
    val pctDist: Int?,
    val dateIso: String,
    val simulated: Boolean = false,
    val breakdown: Map<String, Int>? = null
)

data class Manche(
    val id: String,
    var name: String,
    var courseSlug: String,
    var courseLabel: String,
    var epreuveTypeCode: String,
    var refMaxTimeMin: Double? = null,
    var refNbmax: Double? = null,
    var refTmin: Double? = null,
    var refDmax: Double? = null,
    var refTmax: Double? = null,
    var refWGates: Double = 600.0,
    var refWTime: Double = 300.0,
    var refWSpeed: Double = 100.0,
    var refWCouloir: Double = 0.0,
    var results: MutableMap<String, MancheResult> = mutableMapOf(),
    var courseData: CourseData? = null
)

data class CompetitionData(
    var name: String = "",
    var competitors: MutableList<Competitor> = mutableListOf(),
    var manches: MutableList<Manche> = mutableListOf()
)

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val slug: String,
    val name: String,
    val jsonContent: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "competitions")
data class CompetitionEntity(
    @PrimaryKey val slug: String,
    val name: String,
    val jsonContent: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "flight_history")
data class FlightHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseSlug: String,
    val dateIso: String,
    val epreuveType: String,
    val score: Int,
    val resultJson: String? = null,
    val traceJson: String? = null
)

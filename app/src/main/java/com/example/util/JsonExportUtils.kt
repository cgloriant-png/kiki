package com.example.util

import com.example.data.model.*
import org.json.JSONArray
import org.json.JSONObject

object JsonExportUtils {

    fun serializeCourse(course: CourseData): String {
        val root = JSONObject()
        root.put("name", course.name)
        root.put("corridorWidth", course.corridorWidth)

        val ptsArr = JSONArray()
        course.points.forEach { p ->
            val pObj = JSONObject()
            pObj.put("id", p.id)
            pObj.put("type", p.type)
            pObj.put("lat", p.lat)
            pObj.put("lng", p.lng)
            pObj.put("radius", p.radius)
            pObj.put("width", p.width)
            ptsArr.put(pObj)
        }
        root.put("points", ptsArr)

        val vertsArr = JSONArray()
        course.routeVertices.forEach { v ->
            val vObj = JSONObject()
            vObj.put("id", v.id)
            vObj.put("lat", v.lat)
            vObj.put("lng", v.lng)
            vObj.put("smooth", v.smooth)
            vertsArr.put(vObj)
        }
        root.put("routeVertices", vertsArr)

        val penObj = JSONObject()
        penObj.put("requireSP", course.penalties.requireSP)
        penObj.put("requireFP", course.penalties.requireFP)
        penObj.put("noBacktrack", course.penalties.noBacktrack)
        penObj.put("backtrackAngleDeg", course.penalties.backtrackAngleDeg)
        root.put("penalties", penObj)

        return root.toString(2)
    }

    fun deserializeCourse(jsonStr: String): CourseData {
        val root = JSONObject(jsonStr)
        val name = root.optString("name", "")
        val corridorWidth = root.optDouble("corridorWidth", 200.0)

        val points = mutableListOf<CoursePoint>()
        val ptsArr = root.optJSONArray("points")
        if (ptsArr != null) {
            for (i in 0 until ptsArr.length()) {
                val pObj = ptsArr.getJSONObject(i)
                val type = pObj.optString("type", "balise")
                val isCircle = (type == "balise" || type == "cachee")
                points.add(
                    CoursePoint(
                        id = pObj.optString("id", "p$i"),
                        type = type,
                        lat = pObj.getDouble("lat"),
                        lng = pObj.getDouble("lng"),
                        radius = pObj.optDouble("radius", if (type == "cachee") 250.0 else 100.0),
                        width = pObj.optDouble("width", if (type == "tg") 200.0 else 150.0)
                    )
                )
            }
        }

        val verts = mutableListOf<RouteVertex>()
        val vertsArr = root.optJSONArray("routeVertices")
        if (vertsArr != null) {
            for (i in 0 until vertsArr.length()) {
                val vObj = vertsArr.getJSONObject(i)
                verts.add(
                    RouteVertex(
                        id = vObj.optString("id", "v$i"),
                        lat = vObj.getDouble("lat"),
                        lng = vObj.getDouble("lng"),
                        smooth = vObj.optBoolean("smooth", false)
                    )
                )
            }
        }

        val penObj = root.optJSONObject("penalties")
        val penalties = CoursePenalties(
            requireSP = penObj?.optBoolean("requireSP", true) ?: true,
            requireFP = penObj?.optBoolean("requireFP", true) ?: true,
            noBacktrack = penObj?.optBoolean("noBacktrack", true) ?: true,
            backtrackAngleDeg = penObj?.optDouble("backtrackAngleDeg", 45.0) ?: 45.0
        )

        return CourseData(name, points, verts, corridorWidth, penalties)
    }

    fun serializeCompetition(comp: CompetitionData): String {
        val root = JSONObject()
        root.put("name", comp.name)

        val compArr = JSONArray()
        comp.competitors.forEach { c ->
            val cObj = JSONObject()
            cObj.put("id", c.id)
            cObj.put("name", c.name)
            compArr.put(cObj)
        }
        root.put("competitors", compArr)

        val manchesArr = JSONArray()
        comp.manches.forEach { m ->
            val mObj = JSONObject()
            mObj.put("id", m.id)
            mObj.put("name", m.name)
            mObj.put("courseSlug", m.courseSlug)
            mObj.put("courseLabel", m.courseLabel)
            mObj.put("epreuveTypeCode", m.epreuveTypeCode)
            mObj.put("refMaxTimeMin", m.refMaxTimeMin ?: JSONObject.NULL)
            mObj.put("refNbmax", m.refNbmax ?: JSONObject.NULL)
            mObj.put("refTmin", m.refTmin ?: JSONObject.NULL)
            mObj.put("refDmax", m.refDmax ?: JSONObject.NULL)
            mObj.put("refTmax", m.refTmax ?: JSONObject.NULL)
            mObj.put("refWGates", m.refWGates)
            mObj.put("refWTime", m.refWTime)
            mObj.put("refWSpeed", m.refWSpeed)
            mObj.put("refWCouloir", m.refWCouloir)

            if (m.courseData != null) {
                mObj.put("courseData", JSONObject(serializeCourse(m.courseData!!)))
            }

            val resObj = JSONObject()
            m.results.forEach { (compId, r) ->
                val rObj = JSONObject()
                rObj.put("score", r.score)
                rObj.put("distMeters", r.distMeters)
                rObj.put("durationSeconds", r.durationSeconds ?: JSONObject.NULL)
                rObj.put("pctDist", r.pctDist ?: JSONObject.NULL)
                rObj.put("dateIso", r.dateIso)
                rObj.put("simulated", r.simulated)
                if (r.breakdown != null) {
                    val bdObj = JSONObject()
                    r.breakdown.forEach { (k, v) -> bdObj.put(k, v) }
                    rObj.put("breakdown", bdObj)
                }
                resObj.put(compId, rObj)
            }
            mObj.put("results", resObj)
            manchesArr.put(mObj)
        }
        root.put("manches", manchesArr)

        return root.toString(2)
    }

    fun deserializeCompetition(jsonStr: String): CompetitionData {
        val root = JSONObject(jsonStr)
        val name = root.optString("name", "")

        val competitors = mutableListOf<Competitor>()
        val compArr = root.optJSONArray("competitors")
        if (compArr != null) {
            for (i in 0 until compArr.length()) {
                val cObj = compArr.getJSONObject(i)
                competitors.add(Competitor(cObj.getString("id"), cObj.getString("name")))
            }
        }

        val manches = mutableListOf<Manche>()
        val manchesArr = root.optJSONArray("manches")
        if (manchesArr != null) {
            for (i in 0 until manchesArr.length()) {
                val mObj = manchesArr.getJSONObject(i)
                val mCourseData = if (mObj.has("courseData") && !mObj.isNull("courseData")) {
                    deserializeCourse(mObj.getJSONObject("courseData").toString())
                } else null

                val resultsMap = mutableMapOf<String, MancheResult>()
                val resObj = mObj.optJSONObject("results")
                if (resObj != null) {
                    resObj.keys().forEach { compId ->
                        val rObj = resObj.getJSONObject(compId)
                        val bdObj = rObj.optJSONObject("breakdown")
                        val bd = if (bdObj != null) {
                            val map = mutableMapOf<String, Int>()
                            bdObj.keys().forEach { k -> map[k] = bdObj.getInt(k) }
                            map
                        } else null

                        resultsMap[compId] = MancheResult(
                            score = rObj.getInt("score"),
                            distMeters = rObj.getDouble("distMeters"),
                            durationSeconds = if (rObj.isNull("durationSeconds")) null else rObj.getDouble("durationSeconds"),
                            pctDist = if (rObj.isNull("pctDist")) null else rObj.getInt("pctDist"),
                            dateIso = rObj.optString("dateIso", ""),
                            simulated = rObj.optBoolean("simulated", false),
                            breakdown = bd
                        )
                    }
                }

                manches.add(
                    Manche(
                        id = mObj.optString("id", "m$i"),
                        name = mObj.optString("name", ""),
                        courseSlug = mObj.optString("courseSlug", ""),
                        courseLabel = mObj.optString("courseLabel", ""),
                        epreuveTypeCode = mObj.optString("epreuveTypeCode", "pure"),
                        refMaxTimeMin = if (mObj.isNull("refMaxTimeMin")) null else mObj.optDouble("refMaxTimeMin"),
                        refNbmax = if (mObj.isNull("refNbmax")) null else mObj.optDouble("refNbmax"),
                        refTmin = if (mObj.isNull("refTmin")) null else mObj.optDouble("refTmin"),
                        refDmax = if (mObj.isNull("refDmax")) null else mObj.optDouble("refDmax"),
                        refTmax = if (mObj.isNull("refTmax")) null else mObj.optDouble("refTmax"),
                        refWGates = mObj.optDouble("refWGates", 600.0),
                        refWTime = mObj.optDouble("refWTime", 300.0),
                        refWSpeed = mObj.optDouble("refWSpeed", 100.0),
                        refWCouloir = mObj.optDouble("refWCouloir", 0.0),
                        results = resultsMap,
                        courseData = mCourseData
                    )
                )
            }
        }

        return CompetitionData(name, competitors, manches)
    }

    fun buildRankingCsv(competition: CompetitionData): String {
        val n = competition.competitors.size
        val totals = competition.competitors.map { c ->
            var total = 0
            competition.manches.forEach { manche ->
                val rankedIds = competition.competitors
                    .mapNotNull { cc -> manche.results[cc.id]?.let { r -> Pair(cc.id, r.score) } }
                    .sortedByDescending { it.second }
                    .map { it.first }
                val rankIndex = rankedIds.indexOf(c.id)
                if (rankIndex >= 0) {
                    total += GeometryUtils.championshipPoints(rankIndex + 1, n)
                }
            }
            Pair(c.name, total)
        }.sortedByDescending { it.second }

        val sb = StringBuilder()
        sb.append("Rang;Pilote;Points\n")
        totals.forEachIndexed { i, (name, pts) ->
            sb.append("${i + 1};$name;$pts\n")
        }
        return sb.toString()
    }
}

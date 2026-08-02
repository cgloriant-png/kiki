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

        val scObj = JSONObject()
        scObj.put("wGates", course.scoringRef.wGates)
        scObj.put("wTime", course.scoringRef.wTime)
        scObj.put("wSpeed", course.scoringRef.wSpeed)
        scObj.put("wCouloir", course.scoringRef.wCouloir)
        course.scoringRef.tmin?.let { scObj.put("tmin", it) }
        course.scoringRef.tmax?.let { scObj.put("tmax", it) }
        course.scoringRef.dmax?.let { scObj.put("dmax", it) }
        root.put("scoringRef", scObj)

        return root.toString(2)
    }

    private fun parseScoringRef(root: JSONObject): ScoringRef {
        val candidates = listOfNotNull(
            root.optJSONObject("scoringRef"),
            root.optJSONObject("bareme"),
            root.optJSONObject("baremeEpreuve"),
            root.optJSONObject("scoring"),
            root.optJSONObject("points"),
            root.optJSONObject("weights"),
            root.optJSONObject("ref"),
            root
        )

        fun findDouble(keys: List<String>): Double? {
            for (obj in candidates) {
                for (key in keys) {
                    if (obj.has(key) && !obj.isNull(key)) {
                        val value = obj.get(key)
                        if (value is Number) {
                            val d = value.toDouble()
                            if (!d.isNaN() && !d.isInfinite()) return d
                        } else if (value is String) {
                            val d = value.toDoubleOrNull()
                            if (d != null && !d.isNaN() && !d.isInfinite()) return d
                        }
                    }
                }
            }
            return null
        }

        // Keys for Couloir / Respect du couloir / Parcours
        val couloirKeys = listOf(
            "wCouloir", "refWCouloir", "couloir", "wCourse", "parcours",
            "ptsCouloir", "ptsParcours", "ptsRespectCouloir", "respectCouloir",
            "pts_couloir", "pts_respect_couloir", "pts_parcours",
            "couloirPts", "parcoursPts", "weightCouloir", "weightParcours"
        )

        // Keys for Portes cachées / Gates
        val gatesKeys = listOf(
            "wGates", "refWGates", "gates", "portes", "ptsPortes",
            "portesCachees", "ptsPortesCachees", "ptsCachees", "wHidden",
            "hiddenGates", "pts_portes", "pts_portes_cachees", "pts_cachees",
            "gatesPts", "portesPts", "weightGates", "weightPortes"
        )

        // Keys for Temps déclarés / Portes temps / Time
        val timeKeys = listOf(
            "wTime", "refWTime", "time", "temps", "portesTemps", "ptsTemps",
            "tempsDeclares", "ptsTempsDeclares", "declaredTime", "wDeclared",
            "pts_temps", "pts_temps_declares", "pts_time", "timePts", "tempsPts",
            "weightTime", "weightTemps"
        )

        // Keys for Vitesse / Speed
        val speedKeys = listOf(
            "wSpeed", "refWSpeed", "speed", "vitesse", "ptsVitesse",
            "pts_vitesse", "pts_speed", "speedPts", "vitessePts",
            "weightSpeed", "weightVitesse"
        )

        val parsedCouloir = findDouble(couloirKeys)
        val parsedGates = findDouble(gatesKeys)
        val parsedTime = findDouble(timeKeys)
        val parsedSpeed = findDouble(speedKeys)

        val wCouloir = parsedCouloir ?: 0.0

        val defaultGates = if (wCouloir > 0.0) 0.0 else 600.0
        val defaultTime = if (wCouloir > 0.0) 200.0 else 300.0

        val wGates = parsedGates ?: defaultGates
        val wTime = parsedTime ?: defaultTime
        val wSpeed = parsedSpeed ?: 0.0

        val maxTimeMin = findDouble(listOf("maxTimeMin", "refMaxTimeMin"))
        val nbmax = findDouble(listOf("nbmax", "refNbmax"))
        val tmin = findDouble(listOf("tmin", "refTmin"))
        val dmax = findDouble(listOf("dmax", "refDmax"))
        val tmax = findDouble(listOf("tmax", "refTmax"))

        return ScoringRef(
            maxTimeMin = maxTimeMin,
            nbmax = nbmax,
            tmin = tmin,
            dmax = dmax,
            tmax = tmax,
            wGates = wGates,
            wTime = wTime,
            wSpeed = wSpeed,
            wCouloir = wCouloir
        )
    }

    fun deserializeCourse(jsonStr: String): CourseData {
        val root = JSONObject(jsonStr)
        val name = root.optString("name", "")
        val corridorWidth = root.optDouble("corridorWidth", 200.0)

        val points = mutableListOf<CoursePoint>()
        val ptsArr = root.optJSONArray("points")
            ?: root.optJSONArray("portes")
            ?: root.optJSONArray("gates")
            ?: root.optJSONArray("balises")
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

        val scoringRef = parseScoringRef(root)

        return CourseData(name, points, verts, corridorWidth, penalties, scoringRef)
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

    fun serializeFlightResult(res: FlightAnalysisResult): String {
        val root = JSONObject()
        root.put("score", res.score)
        root.put("label", res.label)
        root.put("bannerTxt", res.bannerTxt)
        root.put("distMeters", res.distMeters)
        root.put("durationSeconds", res.durationSeconds ?: JSONObject.NULL)
        if (res.corridorStats != null) {
            val cs = JSONObject()
            cs.put("pctPts", res.corridorStats.pctPts)
            cs.put("pctDist", res.corridorStats.pctDist ?: JSONObject.NULL)
            cs.put("pctTime", res.corridorStats.pctTime ?: JSONObject.NULL)
            root.put("corridorStats", cs)
        }
        if (res.breakdown != null) {
            val bd = JSONObject()
            res.breakdown.forEach { (k, v) -> bd.put(k, v) }
            root.put("breakdown", bd)
        }
        val resArr = JSONArray()
        res.results.forEach { g ->
            val gObj = JSONObject()
            g.point.let { p ->
                val pObj = JSONObject()
                pObj.put("id", p.id)
                pObj.put("type", p.type)
                pObj.put("lat", p.lat)
                pObj.put("lng", p.lng)
                pObj.put("radius", p.radius)
                pObj.put("width", p.width)
                gObj.put("point", pObj)
            }
            gObj.put("validated", g.validated)
            gObj.put("declaredS", g.declaredS ?: JSONObject.NULL)
            gObj.put("actualS", g.actualS ?: JSONObject.NULL)
            gObj.put("ecartS", g.ecartS ?: JSONObject.NULL)
            gObj.put("points", g.points ?: JSONObject.NULL)
            resArr.put(gObj)
        }
        root.put("results", resArr)
        return root.toString()
    }

    fun deserializeFlightResult(jsonStr: String): FlightAnalysisResult {
        val root = JSONObject(jsonStr)
        val score = root.getInt("score")
        val label = root.optString("label", "")
        val bannerTxt = root.optString("bannerTxt", "")
        val distMeters = root.optDouble("distMeters", 0.0)
        val durationSeconds = if (root.isNull("durationSeconds")) null else root.optDouble("durationSeconds")

        var corridorStats: ConformityStats? = null
        val csObj = root.optJSONObject("corridorStats")
        if (csObj != null) {
            corridorStats = ConformityStats(
                pctPts = csObj.optInt("pctPts", 0),
                pctDist = if (csObj.isNull("pctDist")) null else csObj.optInt("pctDist"),
                pctTime = if (csObj.isNull("pctTime")) null else csObj.optInt("pctTime")
            )
        }

        val breakdownMap = mutableMapOf<String, Int>()
        val bdObj = root.optJSONObject("breakdown")
        if (bdObj != null) {
            bdObj.keys().forEach { k -> breakdownMap[k] = bdObj.getInt(k) }
        }

        val resultsList = mutableListOf<PointValidationResult>()
        val resArr = root.optJSONArray("results")
        if (resArr != null) {
            for (i in 0 until resArr.length()) {
                val gObj = resArr.getJSONObject(i)
                val pObj = gObj.getJSONObject("point")
                val pt = CoursePoint(
                    id = pObj.optString("id", "p$i"),
                    type = pObj.optString("type", "balise"),
                    lat = pObj.getDouble("lat"),
                    lng = pObj.getDouble("lng"),
                    radius = pObj.optDouble("radius", 100.0),
                    width = pObj.optDouble("width", 150.0)
                )
                resultsList.add(
                    PointValidationResult(
                        point = pt,
                        validated = gObj.optBoolean("validated", false),
                        declaredS = if (gObj.isNull("declaredS")) null else gObj.optDouble("declaredS"),
                        actualS = if (gObj.isNull("actualS")) null else gObj.optDouble("actualS"),
                        ecartS = if (gObj.isNull("ecartS")) null else gObj.optDouble("ecartS"),
                        points = if (gObj.isNull("points")) null else gObj.optInt("points")
                    )
                )
            }
        }

        return FlightAnalysisResult(
            score = score,
            label = label,
            bannerTxt = bannerTxt,
            results = resultsList,
            distMeters = distMeters,
            durationSeconds = durationSeconds,
            breakdown = if (breakdownMap.isNotEmpty()) breakdownMap else null,
            corridorStats = corridorStats
        )
    }

    fun serializeTrace(points: List<GpxPoint>): String {
        val arr = JSONArray()
        points.forEach { p ->
            val obj = JSONObject()
            obj.put("lat", p.lat)
            obj.put("lng", p.lng)
            if (p.ele != null) obj.put("ele", p.ele)
            if (p.time != null) obj.put("time", p.time)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializeTrace(jsonStr: String): List<GpxPoint> {
        val list = mutableListOf<GpxPoint>()
        val arr = JSONArray(jsonStr)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                GpxPoint(
                    lat = obj.getDouble("lat"),
                    lng = obj.getDouble("lng"),
                    ele = if (obj.has("ele") && !obj.isNull("ele")) obj.getDouble("ele") else null,
                    time = if (obj.has("time") && !obj.isNull("time")) obj.getLong("time") else null
                )
            )
        }
        return list
    }
}

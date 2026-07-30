package com.example.util

import com.example.data.model.*
import java.util.Date
import kotlin.math.*

data class Point2D(val x: Double, val y: Double)
data class LatLng(val lat: Double, val lng: Double)

object GeometryUtils {
    private const val EARTH_RADIUS = 6371000.0

    fun toRad(deg: Double): Double = deg * Math.PI / 180.0
    fun toDeg(rad: Double): Double = rad * 180.0 / Math.PI

    fun haversine(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Double {
        val dLat = toRad(bLat - aLat)
        val dLng = toRad(bLng - aLng)
        val s = sin(dLat / 2).pow(2) + cos(toRad(aLat)) * cos(toRad(bLat)) * sin(dLng / 2).pow(2)
        return 2 * EARTH_RADIUS * asin(sqrt(s))
    }

    fun haversine(a: LatLng, b: LatLng): Double = haversine(a.lat, a.lng, b.lat, b.lng)
    fun haversine(a: CoursePoint, b: CoursePoint): Double = haversine(a.lat, a.lng, b.lat, b.lng)

    fun toXY(pLat: Double, pLng: Double, originLat: Double, originLng: Double): Point2D {
        val x = toRad(pLng - originLng) * cos(toRad(originLat)) * EARTH_RADIUS
        val y = toRad(pLat - originLat) * EARTH_RADIUS
        return Point2D(x, y)
    }

    fun toXY(p: LatLng, origin: LatLng): Point2D = toXY(p.lat, p.lng, origin.lat, origin.lng)

    fun toLatLng(xy: Point2D, origin: LatLng): LatLng {
        val lat = origin.lat + toDeg(xy.y / EARTH_RADIUS)
        val lng = origin.lng + toDeg(xy.x / (EARTH_RADIUS * cos(toRad(origin.lat))))
        return LatLng(lat, lng)
    }

    fun distToSegment(p: Point2D, a: Point2D, b: Point2D): Double {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val len2 = dx * dx + dy * dy
        if (len2 == 0.0) return hypot(p.x - a.x, p.y - a.y)
        var t = ((p.x - a.x) * dx + (p.y - a.y) * dy) / len2
        t = max(0.0, min(1.0, t))
        return hypot(p.x - (a.x + t * dx), p.y - (a.y + t * dy))
    }

    fun distToPolyline(p: Point2D, pts: List<Point2D>): Double {
        if (pts.size < 2) return Double.POSITIVE_INFINITY
        var m = Double.POSITIVE_INFINITY
        for (i in 1 until pts.size) {
            m = min(m, distToSegment(p, pts[i - 1], pts[i]))
        }
        return m
    }

    fun nearestTangent(p: Point2D, pts: List<Point2D>): Point2D {
        if (pts.size < 2) return Point2D(1.0, 0.0)
        var minD = Double.POSITIVE_INFINITY
        var seg = 1
        for (i in 1 until pts.size) {
            val d = distToSegment(p, pts[i - 1], pts[i])
            if (d < minD) {
                minD = d
                seg = i
            }
        }
        val a = pts[seg - 1]
        val b = pts[seg]
        val dx = b.x - a.x
        val dy = b.y - a.y
        val len = hypot(dx, dy)
        return if (len == 0.0) Point2D(1.0, 0.0) else Point2D(dx / len, dy / len)
    }

    fun bufferPolyline(pts: List<Point2D>, halfWidth: Double): List<Point2D> {
        if (pts.size < 2) return emptyList()
        val left = mutableListOf<Point2D>()
        val right = mutableListOf<Point2D>()

        for (i in pts.indices) {
            val nx: Double
            val ny: Double
            if (i == 0) {
                val dx = pts[1].x - pts[0].x
                val dy = pts[1].y - pts[0].y
                val len = hypot(dx, dy).coerceAtLeast(1e-6)
                nx = -dy / len
                ny = dx / len
            } else if (i == pts.size - 1) {
                val dx = pts[i].x - pts[i - 1].x
                val dy = pts[i].y - pts[i - 1].y
                val len = hypot(dx, dy).coerceAtLeast(1e-6)
                nx = -dy / len
                ny = dx / len
            } else {
                val dx1 = pts[i].x - pts[i - 1].x
                val dy1 = pts[i].y - pts[i - 1].y
                val l1 = hypot(dx1, dy1).coerceAtLeast(1e-6)
                val dx2 = pts[i + 1].x - pts[i].x
                val dy2 = pts[i + 1].y - pts[i].y
                val l2 = hypot(dx2, dy2).coerceAtLeast(1e-6)
                val n1x = -dy1 / l1
                val n1y = dx1 / l1
                val n2x = -dy2 / l2
                val n2y = dx2 / l2
                var sumX = n1x + n2x
                var sumY = n1y + n2y
                val len = hypot(sumX, sumY).coerceAtLeast(1e-6)
                sumX /= len
                sumY /= len
                nx = sumX
                ny = sumY
            }
            left.add(Point2D(pts[i].x + nx * halfWidth, pts[i].y + ny * halfWidth))
            right.add(Point2D(pts[i].x - nx * halfWidth, pts[i].y - ny * halfWidth))
        }
        return left + right.reversed()
    }

    fun mixedLocalPath(vertsLocal: List<Point2D>, smoothFlags: List<Boolean>, perSeg: Int = 10): List<Point2D> {
        if (vertsLocal.size < 2) return vertsLocal.toList()
        val out = mutableListOf(vertsLocal[0])
        for (i in 0 until vertsLocal.size - 1) {
            val segSmooth = (smoothFlags.getOrElse(i) { false } || smoothFlags.getOrElse(i + 1) { false })
            if (segSmooth) {
                val p0 = vertsLocal.getOrElse(i - 1) { vertsLocal[i] }
                val p1 = vertsLocal[i]
                val p2 = vertsLocal[i + 1]
                val p3 = vertsLocal.getOrElse(i + 2) { vertsLocal[i + 1] }
                for (s in 1..perSeg) {
                    val t = s.toDouble() / perSeg
                    val t2 = t * t
                    val t3 = t2 * t
                    val x = 0.5 * ((2 * p1.x) + (-p0.x + p2.x) * t + (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2 + (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3)
                    val y = 0.5 * ((2 * p1.y) + (-p0.y + p2.y) * t + (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2 + (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3)
                    out.add(Point2D(x, y))
                }
            } else {
                out.add(vertsLocal[i + 1])
            }
        }
        return out
    }

    fun simplifyDP(points: List<GpxPoint>, tol: Double): List<GpxPoint> {
        if (points.size < 3 || tol <= 0) return points.toList()
        val origin = LatLng(points[0].lat, points[0].lng)
        val xy = points.map { toXY(LatLng(it.lat, it.lng), origin) }

        fun rdp(s: Int, e: Int): List<Int> {
            var maxD = 0.0
            var idx = -1
            val a = xy[s]
            val b = xy[e]
            val dx = b.x - a.x
            val dy = b.y - a.y
            val len = hypot(dx, dy).coerceAtLeast(1e-9)
            for (i in (s + 1) until e) {
                val p = xy[i]
                val d = abs((p.x - a.x) * dy - (p.y - a.y) * dx) / len
                if (d > maxD) {
                    maxD = d
                    idx = i
                }
            }
            return if (maxD > tol && idx != -1) {
                rdp(s, idx).dropLast(1) + rdp(idx, e)
            } else {
                listOf(s, e)
            }
        }

        val indices = rdp(0, points.size - 1)
        return indices.map { points[it] }
    }

    fun removeOutliers(points: List<GpxPoint>, maxKmh: Double = 250.0): List<GpxPoint> {
        if (points.size < 3) return points.toList()
        val result = mutableListOf(points[0])
        var consecutiveSkips = 0

        for (i in 1 until points.size) {
            val prev = result.last()
            val cur = points[i]
            if (prev.time != null && cur.time != null) {
                val dt = (cur.time - prev.time) / 1000.0
                if (dt <= 0) {
                    result.add(cur)
                    consecutiveSkips = 0
                    continue
                }
                val speed = (haversine(prev.lat, prev.lng, cur.lat, cur.lng) / dt) * 3.6
                if (speed > maxKmh && consecutiveSkips < 2) {
                    if (i + 1 < points.size && points[i + 1].time != null) {
                        val dtNext = (points[i + 1].time!! - prev.time) / 1000.0
                        if (dtNext > 0) {
                            val speedNext = (haversine(prev.lat, prev.lng, points[i + 1].lat, points[i + 1].lng) / dtNext) * 3.6
                            if (speedNext <= maxKmh) {
                                consecutiveSkips++
                                continue
                            }
                        }
                    }
                }
            }
            result.add(cur)
            consecutiveSkips = 0
        }
        return result
    }

    fun totalDistance(pts: List<GpxPoint>): Double {
        var d = 0.0
        for (i in 1 until pts.size) {
            d += haversine(pts[i - 1].lat, pts[i - 1].lng, pts[i].lat, pts[i].lng)
        }
        return d
    }

    fun totalDurationSeconds(pts: List<GpxPoint>): Double? {
        if (pts.size < 2) return null
        val a = pts.first().time
        val b = pts.last().time
        return if (a != null && b != null) (b - a) / 1000.0 else null
    }

    fun courseOrigin(courseData: CourseData, defaultCenter: LatLng = LatLng(46.6, 2.2)): LatLng {
        val allPts = courseData.points.map { LatLng(it.lat, it.lng) } + courseData.routeVertices.map { LatLng(it.lat, it.lng) }
        if (allPts.isEmpty()) return defaultCenter
        val sumLat = allPts.sumOf { it.lat }
        val sumLng = allPts.sumOf { it.lng }
        return LatLng(sumLat / allPts.size, sumLng / allPts.size)
    }

    fun centerlineDenseGeo(courseData: CourseData, origin: LatLng): List<LatLng> {
        if (courseData.routeVertices.size >= 2) {
            val local = courseData.routeVertices.map { toXY(LatLng(it.lat, it.lng), origin) }
            val flags = courseData.routeVertices.map { it.smooth }
            val dense = mixedLocalPath(local, flags)
            return dense.map { toLatLng(it, origin) }
        }
        if (courseData.points.size >= 2) {
            return courseData.points.map { LatLng(it.lat, it.lng) }
        }
        return emptyList()
    }

    fun courseTotalLengthKm(courseData: CourseData): Double {
        val origin = courseOrigin(courseData)
        val cl = centerlineDenseGeo(courseData, origin)
        if (cl.size < 2) return 0.0
        var d = 0.0
        for (i in 1 until cl.size) {
            d += haversine(cl[i - 1], cl[i])
        }
        return d / 1000.0
    }

    fun courseLengthKm(courseData: CourseData): Double = courseTotalLengthKm(courseData)

    data class GateFrame(val dir: Point2D, val perp: Point2D, val local: Point2D)

    fun gateFrameFor(p: CoursePoint, courseData: CourseData, origin: LatLng): GateFrame {
        val pLocal = toXY(LatLng(p.lat, p.lng), origin)
        val routeVerts = courseData.routeVertices

        val isSpOrFp = p.type.equals("SP", true) || p.type.equals("FP", true)
        if (isSpOrFp && routeVerts.size >= 2) {
            val isSp = p.type.equals("SP", true)
            val a = if (isSp) routeVerts[0] else routeVerts[routeVerts.size - 2]
            val b = if (isSp) routeVerts[1] else routeVerts.last()
            val aXy = toXY(LatLng(a.lat, a.lng), origin)
            val bXy = toXY(LatLng(b.lat, b.lng), origin)
            val dx = bXy.x - aXy.x
            val dy = bXy.y - aXy.y
            val len = hypot(dx, dy).coerceAtLeast(1e-6)
            val dir = Point2D(dx / len, dy / len)
            val perp = Point2D(-dir.y, dir.x)
            return GateFrame(dir, perp, pLocal)
        }

        val pts = courseData.points
        val idx = pts.indexOfFirst { it.id == p.id }
        var inDir: Point2D? = null
        var outDir: Point2D? = null

        if (idx > 0) {
            val prev = toXY(LatLng(pts[idx - 1].lat, pts[idx - 1].lng), origin)
            val dx = pLocal.x - prev.x
            val dy = pLocal.y - prev.y
            val len = hypot(dx, dy).coerceAtLeast(1e-6)
            inDir = Point2D(dx / len, dy / len)
        }

        if (idx in 0 until (pts.size - 1)) {
            val next = toXY(LatLng(pts[idx + 1].lat, pts[idx + 1].lng), origin)
            val dx = next.x - pLocal.x
            val dy = next.y - pLocal.y
            val len = hypot(dx, dy).coerceAtLeast(1e-6)
            outDir = Point2D(dx / len, dy / len)
        }

        val dir = if (inDir != null && outDir != null) {
            val sx = inDir.x + outDir.x
            val sy = inDir.y + outDir.y
            val len = hypot(sx, sy)
            if (len > 1e-6) Point2D(sx / len, sy / len) else inDir
        } else inDir ?: outDir ?: run {
            val cl = centerlineDenseGeo(courseData, origin)
            if (cl.size >= 2) nearestTangent(pLocal, cl.map { toXY(it, origin) }) else Point2D(1.0, 0.0)
        }

        val perp = Point2D(-dir.y, dir.x)
        return GateFrame(dir, perp, pLocal)
    }

    fun gateEndpointsLocal(p: CoursePoint, courseData: CourseData, origin: LatLng): Pair<Point2D, Point2D> {
        val frame = gateFrameFor(p, courseData, origin)
        val half = p.width / 2.0
        val a = Point2D(frame.local.x + frame.perp.x * half, frame.local.y + frame.perp.y * half)
        val b = Point2D(frame.local.x - frame.perp.x * half, frame.local.y - frame.perp.y * half)
        return Pair(a, b)
    }

    fun validateAgainstCourse(
        trace: List<GpxPoint>,
        courseData: CourseData,
        origin: LatLng
    ): List<PointValidationResult> {
        var pointer = 0
        val results = mutableListOf<PointValidationResult>()
        val traceLocal = trace.map { toXY(LatLng(it.lat, it.lng), origin) }

        courseData.points.forEach { p ->
            var foundIdx: Int? = null
            var foundTime: Long? = null
            val pLocal = toXY(LatLng(p.lat, p.lng), origin)
            val isCircle = p.type.equals("balise", true) || p.type.equals("cachee", true)

            if (isCircle) {
                val searchRadius = if (p.radius > 0) p.radius else 150.0
                for (i in pointer until traceLocal.size) {
                    val d = hypot(traceLocal[i].x - pLocal.x, traceLocal[i].y - pLocal.y)
                    if (d <= searchRadius) {
                        foundIdx = i
                        foundTime = trace[i].time
                        break
                    }
                }
            } else {
                val frame = gateFrameFor(p, courseData, origin)
                val halfWidth = (p.width / 2.0).coerceAtLeast(50.0)

                for (i in max(pointer, 1) until traceLocal.size) {
                    val a = traceLocal[i - 1]
                    val b = traceLocal[i]
                    val dA = (a.x - frame.local.x) * frame.dir.x + (a.y - frame.local.y) * frame.dir.y
                    val dB = (b.x - frame.local.x) * frame.dir.x + (b.y - frame.local.y) * frame.dir.y

                    val crossed = (dA < 0 && dB >= 0) || (dA >= 0 && dB <= 0)
                    if (crossed) {
                        val denominator = (dA - dB)
                        val t = if (abs(denominator) > 1e-6) (dA / denominator).coerceIn(0.0, 1.0) else 0.5
                        val xCross = Point2D(a.x + t * (b.x - a.x), a.y + t * (b.y - a.y))
                        val offset = (xCross.x - frame.local.x) * frame.perp.x + (xCross.y - frame.local.y) * frame.perp.y
                        if (abs(offset) <= halfWidth) {
                            foundIdx = i
                            val timeA = trace[i - 1].time
                            val timeB = trace[i].time
                            if (timeA != null && timeB != null && timeB >= timeA) {
                                foundTime = (timeA + t * (timeB - timeA)).toLong()
                            } else {
                                foundTime = timeB ?: timeA
                            }
                            break
                        }
                    }

                    // Backup check: distance from segment to gate center
                    val dx = b.x - a.x
                    val dy = b.y - a.y
                    val len2 = dx * dx + dy * dy
                    val projT = if (len2 > 1e-6) (((pLocal.x - a.x) * dx + (pLocal.y - a.y) * dy) / len2).coerceIn(0.0, 1.0) else 0.5
                    val projX = a.x + projT * dx
                    val projY = a.y + projT * dy
                    val distToCenter = hypot(projX - pLocal.x, projY - pLocal.y)

                    if (distToCenter <= halfWidth) {
                        foundIdx = i
                        val timeA = trace[i - 1].time
                        val timeB = trace[i].time
                        if (timeA != null && timeB != null && timeB >= timeA) {
                            foundTime = (timeA + projT * (timeB - timeA)).toLong()
                        } else {
                            foundTime = timeB ?: timeA
                        }
                        break
                    }
                }
            }

            if (foundIdx != null) {
                results.add(
                    PointValidationResult(
                        point = p,
                        validated = true,
                        traceIndex = foundIdx,
                        time = foundTime ?: trace[foundIdx].time
                    )
                )
                pointer = foundIdx
            } else {
                results.add(
                    PointValidationResult(
                        point = p,
                        validated = false,
                        traceIndex = null,
                        time = null
                    )
                )
            }
        }
        return results
    }

    fun conformity(courseData: CourseData, trace: List<GpxPoint>): ConformityStats? {
        val origin = courseOrigin(courseData)
        val cl = centerlineDenseGeo(courseData, origin)
        if (trace.isEmpty() || cl.size < 2) return null
        val clLocal = cl.map { toXY(it, origin) }
        val half = courseData.corridorWidth / 2.0
        val traceLocal = trace.map { toXY(LatLng(it.lat, it.lng), origin) }

        var insidePts = 0
        var insideDist = 0.0
        var totalDist = 0.0
        var insideTime = 0.0
        var totalTime = 0.0

        for (i in traceLocal.indices) {
            if (distToPolyline(traceLocal[i], clLocal) <= half) insidePts++
        }

        for (i in 1 until trace.size) {
            val segDist = haversine(trace[i - 1].lat, trace[i - 1].lng, trace[i].lat, trace[i].lng)
            val dA = distToPolyline(traceLocal[i - 1], clLocal)
            val dB = distToPolyline(traceLocal[i], clLocal)
            val bothIn = (dA <= half && dB <= half)
            totalDist += segDist
            if (bothIn) insideDist += segDist

            if (trace[i - 1].time != null && trace[i].time != null) {
                val dt = (trace[i].time!! - trace[i - 1].time!!) / 1000.0
                totalTime += dt
                if (bothIn) insideTime += dt
            }
        }

        return ConformityStats(
            pctPts = (100.0 * insidePts / trace.size).roundToInt(),
            pctDist = if (totalDist > 0) (100.0 * insideDist / totalDist).roundToInt() else null,
            pctTime = if (totalTime > 0) (100.0 * insideTime / totalTime).roundToInt() else null
        )
    }

    fun detectBacktracking(courseData: CourseData, trace: List<GpxPoint>, thresholdDeg: Double): Boolean {
        val origin = courseOrigin(courseData)
        val cl = centerlineDenseGeo(courseData, origin)
        if (cl.size < 2 || trace.size < 3) return false
        val clLocal = cl.map { toXY(it, origin) }
        val half = courseData.corridorWidth / 2.0
        val traceLocal = trace.map { toXY(LatLng(it.lat, it.lng), origin) }
        val thresholdCos = cos(toRad(thresholdDeg))

        for (i in 1 until (traceLocal.size - 1)) {
            if (distToPolyline(traceLocal[i], clLocal) > half) continue
            val v1 = Point2D(traceLocal[i].x - traceLocal[i - 1].x, traceLocal[i].y - traceLocal[i - 1].y)
            val v2 = Point2D(traceLocal[i + 1].x - traceLocal[i].x, traceLocal[i + 1].y - traceLocal[i].y)
            val l1 = hypot(v1.x, v1.y)
            val l2 = hypot(v2.x, v2.y)
            if (l1 < 1e-6 || l2 < 1e-6) continue
            val cosInterior = -(v1.x * v2.x + v1.y * v2.y) / (l1 * l2)
            if (cosInterior > thresholdCos) return true
        }
        return false
    }

    fun scoreFlight(
        courseData: CourseData,
        trace: List<GpxPoint>,
        epreuveType: EpreuveType,
        ref: ScoringRef,
        declMap: Map<String, Double> = emptyMap()
    ): FlightAnalysisResult {
        val origin = courseOrigin(courseData)
        val dist = totalDistance(trace)
        val dur = totalDurationSeconds(trace)

        if (courseData.points.isEmpty() && (epreuveType == EpreuveType.PURE || epreuveType == EpreuveType.SNAKE || epreuveType == EpreuveType.PRECISION)) {
            return FlightAnalysisResult(
                score = 0,
                label = "",
                bannerTxt = "",
                results = emptyList(),
                distMeters = dist,
                durationSeconds = dur,
                error = "Cette manche n'a pas de portes/balises définies."
            )
        }

        val results = validateAgainstCourse(trace, courseData, origin)
        var score = 0.0
        var label = ""
        var bannerTxt = ""
        var breakdown: Map<String, Int>? = null

        when (epreuveType) {
            EpreuveType.PURE -> {
                val cand = results.filter { it.point.type == "balise" || it.point.type == "cachee" || it.point.type == "porte" }
                val nbp = cand.count { it.validated }
                val nbmax = if ((ref.nbmax ?: 0.0) > 0) ref.nbmax!! else cand.size.toDouble().coerceAtLeast(1.0)
                var q = 1000.0 * (nbp / nbmax)
                var penTxt = ""
                if ((ref.maxTimeMin ?: 0.0) > 0 && dur != null) {
                    val over = (dur - ref.maxTimeMin!! * 60) / 60.0
                    var pen = 0.0
                    if (over > 10) pen = 1.0 else if (over > 5) pen = 0.8 else if (over > 2) pen = 0.4 else if (over > 1) pen = 0.2 else if (over > 0) pen = 0.1
                    if (pen > 0) {
                        q *= (1.0 - pen)
                        penTxt = " · Pénalité temps : -${(pen * 100).roundToInt()}%"
                    }
                }
                score = q
                label = "Navigation pure — Q=1000×(Nbp/Nbmax)"
                bannerTxt = "Balises validées : $nbp/${cand.size} (Nbmax=${nbmax.toInt()}).$penTxt"
            }
            EpreuveType.SNAKE -> {
                val hidden = results.filter { it.point.type.equals("porte", true) || it.point.type.equals("tg", true) }
                val hCount = hidden.count { it.validated }
                val nh = hidden.size.coerceAtLeast(1)
                val qh = 400.0 * (hCount.toDouble() / nh)
                var qv = 0.0
                var sTxt = ""
                if ((ref.tmin ?: 0.0) > 0 && dur != null) {
                    qv = min(200.0, 200.0 * (ref.tmin!! / dur))
                    sTxt = " · Vitesse : ${qv.roundToInt()}/200"
                }
                score = (qh + qv) * (1000.0 / 600.0)
                label = "Navigation imposée — normalisé sur 1000 (portes cachées + vitesse)"
                bannerTxt = "Portes franchies : $hCount/${hidden.size}.$sTxt"
            }
            EpreuveType.PRECISION -> {
                val hidden = results.filter { 
                    val t = it.point.type.lowercase()
                    t == "porte" || t == "tg" || t == "balise" || t == "cachee"
                }
                val tc = hidden.count { it.validated }
                val ntc = hidden.size.coerceAtLeast(1)
                val gatesRatio = if (hidden.isNotEmpty()) tc.toDouble() / ntc else 0.0

                val tgResults = results.filter { 
                    val t = it.point.type.lowercase()
                    val id = it.point.id.lowercase()
                    t != "sp" && id != "sp" && t != "fp" && id != "fp"
                }
                val sp = results.find { it.point.type.equals("SP", true) || it.point.id.equals("SP", true) }
                val spTime = if (sp != null && sp.validated && sp.time != null) sp.time else trace.firstOrNull()?.time

                var sumH = 0.0
                tgResults.forEach { r ->
                    val declared = declMap[r.point.id]
                    r.declaredS = declared
                    if (r.validated && declared != null && declared >= 0 && spTime != null && r.time != null) {
                        val actual = (r.time - spTime) / 1000.0
                        val ei = min(180.0, max(0.0, abs(declared - actual)))
                        val hi = max(0.0, 180.0 - ei)
                        r.actualS = actual
                        r.ecartS = actual - declared
                        r.hi = hi
                        sumH += hi
                    }
                }
                val timeRatio = if (tgResults.isNotEmpty()) sumH / (180.0 * tgResults.size) else 0.0

                var speedRatio = 0.0
                var sTxt = ""
                if ((ref.tmin ?: 0.0) > 0 && dur != null) {
                    speedRatio = min(1.0, ref.tmin!! / dur)
                    sTxt = " · Vitesse : ${(speedRatio * 100).roundToInt()}%"
                }

                val conf = conformity(courseData, trace)
                val couloirRatio = if (conf?.pctDist != null) conf.pctDist / 100.0 else 0.0

                val wGates = ref.wGates
                val wTime = ref.wTime
                val wSpeed = ref.wSpeed
                val wCouloir = ref.wCouloir

                val gatesPts = (wGates * gatesRatio).roundToInt()
                val timePts = (wTime * timeRatio).roundToInt()
                val speedPts = (wSpeed * speedRatio).roundToInt()
                val couloirPts = (wCouloir * couloirRatio).roundToInt()

                score = (gatesPts + timePts + speedPts + couloirPts).toDouble()

                tgResults.forEach { r ->
                    r.points = if (tgResults.isNotEmpty() && r.hi != null) {
                        (wTime * (r.hi!! / 180.0) / tgResults.size).roundToInt()
                    } else if (r.hi != null) 0 else null
                }

                breakdown = mapOf(
                    "Portes cachées" to gatesPts,
                    "Temps déclarés" to timePts,
                    "Vitesse" to speedPts,
                    "Couloir" to couloirPts
                )
                label = "Navigation précision — barème (portes ${wGates.toInt()} + temps ${wTime.toInt()} + vitesse ${wSpeed.toInt()} + couloir ${wCouloir.toInt()})"
                bannerTxt = "Portes franchies : $tc/${hidden.size}. Portes mesurées : ${tgResults.size}.$sTxt" +
                        if (conf?.pctDist != null) " · Couloir : ${conf.pctDist}%" else ""
            }
            EpreuveType.ECO_DIST -> {
                val dmax = if ((ref.dmax ?: 0.0) > 0) ref.dmax!! else dist
                val tmax = if ((ref.tmax ?: 0.0) > 0) ref.tmax!! else (dur ?: 1.0)
                score = min(1000.0, 800.0 * ((dur ?: 0.0) / tmax) + 200.0 * (dist / dmax))
                label = "Économie distance — Q=800×(Tp/Tmax)+200×(dp/dmax)"
                bannerTxt = "Distance : ${fmtDist(dist)} · Temps : ${fmtDur(dur)}."
            }
            EpreuveType.ECO_PURE -> {
                val tmax = if ((ref.tmax ?: 0.0) > 0) ref.tmax!! else (dur ?: 1.0)
                score = min(1000.0, 1000.0 * ((dur ?: 0.0) / tmax))
                label = "Économie pure — Q=1000×(Tp/Tmax)"
                bannerTxt = "Temps de vol : ${fmtDur(dur)}."
            }
        }

        val pen = courseData.penalties
        val spResult = results.find { it.point.type.equals("SP", true) || it.point.id.equals("SP", true) }
        val fpResult = results.find { it.point.type.equals("FP", true) || it.point.id.equals("FP", true) }
        var mandatoryMsg = ""

        if (pen.requireSP && spResult != null && !spResult.validated) {
            mandatoryMsg += "⚠ Porte d'entrée (SP) non franchie — score = 0. "
        }
        if (pen.requireFP && fpResult != null && !fpResult.validated) {
            mandatoryMsg += "⚠ Porte de sortie (FP) non franchie — score = 0. "
        }
        if (pen.noBacktrack && detectBacktracking(courseData, trace, pen.backtrackAngleDeg)) {
            mandatoryMsg += "⚠ Retour en arrière détecté dans le couloir (angle < ${pen.backtrackAngleDeg.toInt()}°) — score = 0. "
        }

        if (mandatoryMsg.isNotEmpty()) {
            score = 0.0
            bannerTxt = mandatoryMsg + bannerTxt
        }

        val confStats = conformity(courseData, trace)
        val finalScore = max(0, min(1000, score.roundToInt()))

        return FlightAnalysisResult(
            score = finalScore,
            label = label,
            bannerTxt = bannerTxt,
            results = results,
            distMeters = dist,
            durationSeconds = dur,
            breakdown = breakdown,
            corridorStats = confStats
        )
    }

    fun championshipPoints(pos: Int, N: Int): Int {
        if (pos > N || pos < 1) return 0
        val intercept = (0.8 * N + 6).roundToInt()
        val bonus = when (pos) {
            1 -> 7
            2 -> 3
            3 -> 1
            else -> 0
        }
        return max(2, intercept - pos + bonus)
    }

    fun fmtDist(m: Double?): String {
        if (m == null) return "—"
        return if (m >= 1000) String.format("%.2f km", m / 1000) else "${m.roundToInt()} m"
    }

    fun fmtDur(seconds: Double?): String {
        if (seconds == null) return "—"
        val s = seconds.roundToInt()
        val h = s / 3600
        val m = (s % 3600) / 60
        val sec = s % 60
        return if (h > 0) String.format("%d:%02d:%02d", h, m, sec) else String.format("%d:%02d", m, sec)
    }

    fun buildSimulatedTrace(rawPoints: List<LatLng>, speedKmh: Double): List<GpxPoint> {
        if (rawPoints.size < 2) return emptyList()
        val speedMs = speedKmh * 1000.0 / 3600.0
        val stepSeconds = 2.0
        val stepDist = max(3.0, speedMs * stepSeconds)

        val segDists = mutableListOf(0.0)
        for (i in 1 until rawPoints.size) {
            segDists.add(segDists.last() + haversine(rawPoints[i - 1], rawPoints[i]))
        }
        val total = segDists.last()
        if (total <= 0) return emptyList()

        val startTime = System.currentTimeMillis()
        val trace = mutableListOf<GpxPoint>()
        var segIdx = 1

        var d = 0.0
        while (d < total) {
            while (segIdx < segDists.size - 1 && segDists[segIdx] < d) segIdx++
            val d0 = segDists[segIdx - 1]
            val d1 = segDists[segIdx]
            val t = if (d1 > d0) (d - d0) / (d1 - d0) else 0.0
            val a = rawPoints[segIdx - 1]
            val b = rawPoints[segIdx]
            val lat = a.lat + (b.lat - a.lat) * t
            val lng = a.lng + (b.lng - a.lng) * t
            val time = (startTime + (d / speedMs) * 1000).toLong()
            trace.add(GpxPoint(lat, lng, time = time))
            d += stepDist
        }

        val last = rawPoints.last()
        trace.add(GpxPoint(last.lat, last.lng, time = (startTime + (total / speedMs) * 1000).toLong()))
        return trace
    }

    fun slugify(s: String?): String {
        if (s.isNullOrBlank()) return "sans_nom"
        return s.lowercase()
            .replace(Regex("[áàâä]"), "a")
            .replace(Regex("[éèêë]"), "e")
            .replace(Regex("[íìîï]"), "i")
            .replace(Regex("[óòôö]"), "o")
            .replace(Regex("[úùûü]"), "u")
            .replace(Regex("[ç]"), "c")
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
            .ifEmpty { "sans_nom" }
    }
}

private fun String.isNull_Or_Blank(): Boolean = this.trim().isEmpty()

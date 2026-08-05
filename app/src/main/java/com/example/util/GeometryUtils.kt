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

    fun removeOutliers(points: List<GpxPoint>, maxKmh: Double = 110.0): List<GpxPoint> {
        if (points.size < 3) return points.toList()

        var currentList = points.filter { p -> (p.lat != 0.0 || p.lng != 0.0) && abs(p.lat) <= 90.0 && abs(p.lng) <= 180.0 }
        if (currentList.size < 3) return currentList

        val maxAllowedSpeed = if (maxKmh > 0) maxKmh else 110.0

        // Remove initial aberrant points (early GPS fix jumps before lock)
        while (currentList.size >= 3) {
            val p0 = currentList[0]
            val p1 = currentList[1]
            val p2 = currentList[2]

            var isP0Spike = false
            if (p0.time != null && p1.time != null) {
                val dt = (p1.time - p0.time) / 1000.0
                if (dt > 0) {
                    val speed01 = (haversine(p0.lat, p0.lng, p1.lat, p1.lng) / dt) * 3.6
                    if (speed01 > maxAllowedSpeed) isP0Spike = true
                } else if (dt == 0.0) {
                    val dist01 = haversine(p0.lat, p0.lng, p1.lat, p1.lng)
                    if (dist01 > 30.0) isP0Spike = true
                }
            } else {
                val dist01 = haversine(p0.lat, p0.lng, p1.lat, p1.lng)
                val dist12 = haversine(p1.lat, p1.lng, p2.lat, p2.lng)
                if (dist01 > 200.0 && dist12 < 100.0) isP0Spike = true
            }

            if (isP0Spike) {
                currentList = currentList.drop(1)
            } else {
                break
            }
        }

        if (currentList.size < 3) return currentList

        var pass = 0
        val maxPasses = 5
        var changed = true

        while (changed && pass < maxPasses && currentList.size >= 3) {
            pass++
            changed = false
            val filtered = mutableListOf<GpxPoint>()
            filtered.add(currentList.first())

            var i = 1
            while (i < currentList.size) {
                val prev = filtered.last()
                val cur = currentList[i]
                val next = if (i + 1 < currentList.size) currentList[i + 1] else null

                var isSpike = false

                // 1. Check speed jump
                if (prev.time != null && cur.time != null) {
                    val dt = (cur.time - prev.time) / 1000.0
                    if (dt > 0) {
                        val speedKmh = (haversine(prev.lat, prev.lng, cur.lat, cur.lng) / dt) * 3.6
                        if (speedKmh > maxAllowedSpeed) {
                            var resolved = false
                            for (lookahead in 1..4) {
                                if (i + lookahead < currentList.size) {
                                    val candidateNext = currentList[i + lookahead]
                                    if (candidateNext.time != null) {
                                        val dtNext = (candidateNext.time - prev.time) / 1000.0
                                        if (dtNext > 0) {
                                            val speedNext = (haversine(prev.lat, prev.lng, candidateNext.lat, candidateNext.lng) / dtNext) * 3.6
                                            if (speedNext <= maxAllowedSpeed) {
                                                isSpike = true
                                                resolved = true
                                                break
                                            }
                                        }
                                    }
                                }
                            }
                            if (!resolved && speedKmh > 180.0) {
                                isSpike = true
                            }
                        }
                    }
                }

                // 2. Check sharp geometric triangle spike (out and back jump / lateral detour)
                if (!isSpike && next != null) {
                    val d1 = haversine(prev.lat, prev.lng, cur.lat, cur.lng)
                    val d2 = haversine(cur.lat, cur.lng, next.lat, next.lng)
                    val dDirect = haversine(prev.lat, prev.lng, next.lat, next.lng)

                    if (d1 >= 3.0 && d2 >= 3.0) {
                        if (dDirect < 1.5 && (d1 + d2) > 6.0) {
                            isSpike = true
                        } else if (dDirect >= 1.5) {
                            val ratio = (d1 + d2) / dDirect

                            val origin = LatLng(prev.lat, prev.lng)
                            val pPrev = toXY(origin, origin)
                            val pCur = toXY(LatLng(cur.lat, cur.lng), origin)
                            val pNext = toXY(LatLng(next.lat, next.lng), origin)

                            val dx = pNext.x - pPrev.x
                            val dy = pNext.y - pPrev.y
                            val len2 = dx * dx + dy * dy
                            val perpDist = if (len2 > 1e-6) {
                                val projT = (((pCur.x - pPrev.x) * dx + (pCur.y - pPrev.y) * dy) / len2).coerceIn(0.0, 1.0)
                                val projX = pPrev.x + projT * dx
                                val projY = pPrev.y + projT * dy
                                hypot(pCur.x - projX, pCur.y - projY)
                            } else {
                                hypot(pCur.x, pCur.y)
                            }

                            if ((ratio > 1.45 && perpDist > 8.0) || ratio > 2.2 || perpDist > 30.0) {
                                isSpike = true
                            }
                        }
                    }
                }

                // 3. 2-Point consecutive lateral jump check
                if (!isSpike && next != null && i + 2 < currentList.size) {
                    val next2 = currentList[i + 2]
                    val d1 = haversine(prev.lat, prev.lng, cur.lat, cur.lng)
                    val d2 = haversine(cur.lat, cur.lng, next.lat, next.lng)
                    val d3 = haversine(next.lat, next.lng, next2.lat, next2.lng)
                    val dDirect2 = haversine(prev.lat, prev.lng, next2.lat, next2.lng)

                    if (dDirect2 >= 2.0) {
                        val ratio2 = (d1 + d2 + d3) / dDirect2
                        val origin = LatLng(prev.lat, prev.lng)
                        val pPrev = toXY(origin, origin)
                        val pCur = toXY(LatLng(cur.lat, cur.lng), origin)
                        val pNext2 = toXY(LatLng(next2.lat, next2.lng), origin)

                        val dx = pNext2.x - pPrev.x
                        val dy = pNext2.y - pPrev.y
                        val len2 = dx * dx + dy * dy
                        val perpDist = if (len2 > 1e-6) {
                            val projT = (((pCur.x - pPrev.x) * dx + (pCur.y - pPrev.y) * dy) / len2).coerceIn(0.0, 1.0)
                            val projX = pPrev.x + projT * dx
                            val projY = pPrev.y + projT * dy
                            hypot(pCur.x - projX, pCur.y - projY)
                        } else {
                            hypot(pCur.x, pCur.y)
                        }

                        if (ratio2 > 1.5 && perpDist > 10.0) {
                            isSpike = true
                        }
                    }
                }

                if (isSpike) {
                    changed = true
                    i++
                } else {
                    filtered.add(cur)
                    i++
                }
            }

            if (filtered.size < currentList.size) {
                currentList = filtered
            } else {
                changed = false
            }
        }

        return currentList
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
        val results = mutableListOf<PointValidationResult>()
        val traceLocal = trace.map { toXY(LatLng(it.lat, it.lng), origin) }

        var spTraceIndex: Int? = null

        val spPoint = courseData.points.find { it.type.equals("SP", true) || it.id.equals("SP", true) }
        if (spPoint != null) {
            val frame = gateFrameFor(spPoint, courseData, origin)
            val halfWidth = (spPoint.width / 2.0).coerceAtLeast(50.0)
            val pLocal = toXY(LatLng(spPoint.lat, spPoint.lng), origin)

            val spCandidates = mutableListOf<Pair<Int, Double>>()

            for (i in 1 until traceLocal.size) {
                val a = traceLocal[i - 1]
                val b = traceLocal[i]
                val dA = (a.x - frame.local.x) * frame.dir.x + (a.y - frame.local.y) * frame.dir.y
                val dB = (b.x - frame.local.x) * frame.dir.x + (b.y - frame.local.y) * frame.dir.y
                val crossed = (dA < 0 && dB >= 0) || (dA >= 0 && dB <= 0)
                var matched = false

                if (crossed) {
                    val denominator = (dA - dB)
                    val t = if (abs(denominator) > 1e-6) (dA / denominator).coerceIn(0.0, 1.0) else 0.5
                    val xCross = Point2D(a.x + t * (b.x - a.x), a.y + t * (b.y - a.y))
                    val offset = (xCross.x - frame.local.x) * frame.perp.x + (xCross.y - frame.local.y) * frame.perp.y
                    if (abs(offset) <= halfWidth) {
                        matched = true
                    }
                }
                if (!matched) {
                    val dx = b.x - a.x
                    val dy = b.y - a.y
                    val len2 = dx * dx + dy * dy
                    val projT = if (len2 > 1e-6) (((pLocal.x - a.x) * dx + (pLocal.y - a.y) * dy) / len2).coerceIn(0.0, 1.0) else 0.5
                    val projX = a.x + projT * dx
                    val projY = a.y + projT * dy
                    if (hypot(projX - pLocal.x, projY - pLocal.y) <= halfWidth) {
                        matched = true
                    }
                }

                if (matched) {
                    var speedKmh = 20.0
                    val tA = trace[i - 1].time
                    val tB = trace[i].time
                    if (tA != null && tB != null && tB > tA) {
                        speedKmh = (haversine(trace[i - 1].lat, trace[i - 1].lng, trace[i].lat, trace[i].lng) / ((tB - tA) / 1000.0)) * 3.6
                    }
                    spCandidates.add(Pair(i, speedKmh))
                }
            }

            if (spCandidates.isNotEmpty()) {
                val flightCandidates = spCandidates.filter { it.second >= 5.0 }
                val chosen = flightCandidates.lastOrNull() ?: spCandidates.first()
                spTraceIndex = chosen.first
            }
        }

        var lastGateTraceIndex = spTraceIndex ?: 0

        courseData.points.forEach { p ->
            var foundIdx: Int? = null
            var foundTime: Long? = null
            val pLocal = toXY(LatLng(p.lat, p.lng), origin)
            val isCircle = p.type.equals("balise", true) || p.type.equals("cachee", true)

            val candidates = mutableListOf<Pair<Int, Long?>>()

            if (isCircle) {
                val searchRadius = if (p.radius > 0) p.radius else 150.0
                for (i in 0 until traceLocal.size) {
                    val d = hypot(traceLocal[i].x - pLocal.x, traceLocal[i].y - pLocal.y)
                    if (d <= searchRadius) {
                        candidates.add(Pair(i, trace[i].time))
                    }
                }
            } else {
                val frame = gateFrameFor(p, courseData, origin)
                val halfWidth = (p.width / 2.0).coerceAtLeast(50.0)

                for (i in 1 until traceLocal.size) {
                    val a = traceLocal[i - 1]
                    val b = traceLocal[i]
                    val dA = (a.x - frame.local.x) * frame.dir.x + (a.y - frame.local.y) * frame.dir.y
                    val dB = (b.x - frame.local.x) * frame.dir.x + (b.y - frame.local.y) * frame.dir.y

                    val crossed = (dA < 0 && dB >= 0) || (dA >= 0 && dB <= 0)
                    var matched = false
                    var tMatch = 0.5

                    if (crossed) {
                        val denominator = (dA - dB)
                        tMatch = if (abs(denominator) > 1e-6) (dA / denominator).coerceIn(0.0, 1.0) else 0.5
                        val xCross = Point2D(a.x + tMatch * (b.x - a.x), a.y + tMatch * (b.y - a.y))
                        val offset = (xCross.x - frame.local.x) * frame.perp.x + (xCross.y - frame.local.y) * frame.perp.y
                        if (abs(offset) <= halfWidth) {
                            matched = true
                        }
                    }

                    if (!matched) {
                        val dx = b.x - a.x
                        val dy = b.y - a.y
                        val len2 = dx * dx + dy * dy
                        val projT = if (len2 > 1e-6) (((pLocal.x - a.x) * dx + (pLocal.y - a.y) * dy) / len2).coerceIn(0.0, 1.0) else 0.5
                        val projX = a.x + projT * dx
                        val projY = a.y + projT * dy
                        if (hypot(projX - pLocal.x, projY - pLocal.y) <= halfWidth) {
                            matched = true
                            tMatch = projT
                        }
                    }

                    if (matched) {
                        val timeA = trace[i - 1].time
                        val timeB = trace[i].time
                        val cTime = if (timeA != null && timeB != null && timeB >= timeA) {
                            (timeA + tMatch * (timeB - timeA)).toLong()
                        } else {
                            timeB ?: timeA
                        }
                        candidates.add(Pair(i, cTime))
                    }
                }
            }

            if (candidates.isNotEmpty()) {
                val best = candidates.find { it.first >= lastGateTraceIndex }
                    ?: (if (spTraceIndex != null) candidates.find { it.first >= spTraceIndex } else null)
                    ?: candidates.first()

                foundIdx = best.first
                foundTime = best.second

                if (!isCircle) {
                    lastGateTraceIndex = max(lastGateTraceIndex, foundIdx)
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

    fun conformity(
        courseData: CourseData,
        trace: List<GpxPoint>,
        spIndex: Int? = null,
        fpIndex: Int? = null
    ): ConformityStats? {
        val origin = courseOrigin(courseData)
        val cl = centerlineDenseGeo(courseData, origin)
        if (trace.isEmpty() || cl.size < 2) return null

        // Determine start index (entry gate SP)
        var startIdx: Int? = spIndex
        if (startIdx == null) {
            val spPoint = courseData.points.find { it.type.equals("SP", true) || it.id.equals("SP", true) }
            if (spPoint != null) {
                var minDist = Double.MAX_VALUE
                var bestIdx = 0
                for (i in trace.indices) {
                    val d = haversine(trace[i].lat, trace[i].lng, spPoint.lat, spPoint.lng)
                    if (d < minDist) {
                        minDist = d
                        bestIdx = i
                    }
                }
                startIdx = bestIdx
            } else {
                startIdx = 0
            }
        }
        startIdx = startIdx.coerceIn(0, trace.size - 1)

        // Determine end index (exit gate FP or last gate)
        var endIdx: Int? = fpIndex
        if (endIdx == null) {
            val lastGate = courseData.points.find { it.type.equals("FP", true) || it.id.equals("FP", true) }
                ?: courseData.points.lastOrNull { p ->
                    val t = p.type.lowercase()
                    val id = p.id.lowercase()
                    t != "sp" && id != "sp"
                }

            if (lastGate != null) {
                var minDist = Double.MAX_VALUE
                var bestIdx = trace.size - 1
                for (i in startIdx until trace.size) {
                    val d = haversine(trace[i].lat, trace[i].lng, lastGate.lat, lastGate.lng)
                    if (d < minDist) {
                        minDist = d
                        bestIdx = i
                    }
                }
                endIdx = bestIdx
            } else {
                endIdx = trace.size - 1
            }
        }
        endIdx = endIdx.coerceIn(startIdx, trace.size - 1)

        val activeTrace = if (endIdx > startIdx) trace.subList(startIdx, endIdx + 1) else trace
        if (activeTrace.size < 2) return null

        val clLocal = cl.map { toXY(it, origin) }
        val half = courseData.corridorWidth / 2.0
        val traceLocal = activeTrace.map { toXY(LatLng(it.lat, it.lng), origin) }

        var insidePts = 0
        var insideDist = 0.0
        var totalDist = 0.0
        var insideTime = 0.0
        var totalTime = 0.0

        for (i in traceLocal.indices) {
            if (distToPolyline(traceLocal[i], clLocal) <= half) insidePts++
        }

        for (i in 1 until activeTrace.size) {
            val segDist = haversine(activeTrace[i - 1].lat, activeTrace[i - 1].lng, activeTrace[i].lat, activeTrace[i].lng)
            val dA = distToPolyline(traceLocal[i - 1], clLocal)
            val dB = distToPolyline(traceLocal[i], clLocal)

            val frac = when {
                dA <= half && dB <= half -> 1.0
                dA > half && dB > half -> 0.0
                else -> {
                    val dMin = min(dA, dB)
                    val dMax = max(dA, dB)
                    if (dMax > dMin) ((half - dMin) / (dMax - dMin)).coerceIn(0.0, 1.0) else 0.5
                }
            }

            totalDist += segDist
            insideDist += segDist * frac

            if (activeTrace[i - 1].time != null && activeTrace[i].time != null) {
                val dt = (activeTrace[i].time!! - activeTrace[i - 1].time!!) / 1000.0
                if (dt > 0) {
                    totalTime += dt
                    insideTime += dt * frac
                }
            }
        }

        val pctTimeVal = if (totalTime > 0) (100.0 * insideTime / totalTime).roundToInt() else null
        val pctDistVal = if (totalDist > 0) (100.0 * insideDist / totalDist).roundToInt() else null
        val pctPtsVal = (100.0 * insidePts / activeTrace.size).roundToInt()

        return ConformityStats(
            pctPts = pctPtsVal,
            pctDist = pctDistVal,
            pctTime = pctTimeVal
        )
    }

    data class BacktrackResult(
        val hasBacktrack: Boolean,
        val location: LatLng? = null,
        val description: String? = null
    )

    fun detectBacktracking(
        courseData: CourseData,
        trace: List<GpxPoint>,
        thresholdDeg: Double,
        spIndex: Int? = null,
        fpIndex: Int? = null
    ): BacktrackResult {
        val origin = courseOrigin(courseData)
        val cl = centerlineDenseGeo(courseData, origin)
        if (cl.size < 2 || trace.size < 3) return BacktrackResult(false)
        val clLocal = cl.map { toXY(it, origin) }
        val half = courseData.corridorWidth / 2.0
        val traceLocal = trace.map { toXY(LatLng(it.lat, it.lng), origin) }
        val thresholdCos = cos(toRad(thresholdDeg))

        val startIdx = (spIndex ?: 1).coerceAtLeast(1)
        val endIdx = (fpIndex ?: (traceLocal.size - 2)).coerceAtMost(traceLocal.size - 2)

        if (startIdx >= endIdx) return BacktrackResult(false)

        // 1. Centerline distance accumulation array
        val sCenterline = DoubleArray(clLocal.size)
        sCenterline[0] = 0.0
        for (j in 1 until clLocal.size) {
            val dx = clLocal[j].x - clLocal[j - 1].x
            val dy = clLocal[j].y - clLocal[j - 1].y
            sCenterline[j] = sCenterline[j - 1] + hypot(dx, dy)
        }

        fun getCenterlineS(p: Point2D): Pair<Double, Double> {
            var minDist = Double.MAX_VALUE
            var bestS = 0.0
            for (j in 0 until clLocal.size - 1) {
                val a = clLocal[j]
                val b = clLocal[j + 1]
                val dx = b.x - a.x
                val dy = b.y - a.y
                val len2 = dx * dx + dy * dy
                val t = if (len2 > 1e-6) (((p.x - a.x) * dx + (p.y - a.y) * dy) / len2).coerceIn(0.0, 1.0) else 0.0
                val projX = a.x + t * dx
                val projY = a.y + t * dy
                val d = hypot(p.x - projX, p.y - projY)
                if (d < minDist) {
                    minDist = d
                    bestS = sCenterline[j] + t * sqrt(len2)
                }
            }
            return Pair(bestS, minDist)
        }

        var maxS = 0.0
        var insideCount = 0

        for (i in startIdx..endIdx) {
            val pCur = traceLocal[i]
            val (sVal, dToCL) = getCenterlineS(pCur)

            val isInside = dToCL <= half

            if (isInside) {
                if (insideCount == 0) {
                    maxS = sVal
                } else {
                    if (sVal > maxS) {
                        maxS = sVal
                    } else if (maxS - sVal > 25.0) {
                        val loc = LatLng(trace[i].lat, trace[i].lng)
                        val distBack = (maxS - sVal).roundToInt()
                        return BacktrackResult(
                            hasBacktrack = true,
                            location = loc,
                            description = "Demi-tour / Retour en arrière dans le couloir (recul de ${distBack}m)"
                        )
                    }
                }
                insideCount++

                // 3-point angle check strictly INSIDE corridor
                if (i >= 1 && i + 1 < traceLocal.size) {
                    val pPrev = traceLocal[i - 1]
                    val pNext = traceLocal[i + 1]

                    val v1 = Point2D(pCur.x - pPrev.x, pCur.y - pPrev.y)
                    val v2 = Point2D(pNext.x - pCur.x, pNext.y - pCur.y)
                    val l1 = hypot(v1.x, v1.y)
                    val l2 = hypot(v2.x, v2.y)

                    if (l1 >= 8.0 && l2 >= 8.0) {
                        val cosInterior = -(v1.x * v2.x + v1.y * v2.y) / (l1 * l2)
                        if (cosInterior > thresholdCos) {
                            val loc = LatLng(trace[i].lat, trace[i].lng)
                            return BacktrackResult(
                                hasBacktrack = true,
                                location = loc,
                                description = "Demi-tour / Angle aigu dans le couloir (< ${thresholdDeg.toInt()}°)"
                            )
                        }
                    }

                    // Windowed check for smooth U-turns inside corridor
                    if (i >= 3 && i + 3 < traceLocal.size) {
                        val pPrevWin = traceLocal[i - 3]
                        val pNextWin = traceLocal[i + 3]
                        val vw1 = Point2D(pCur.x - pPrevWin.x, pCur.y - pPrevWin.y)
                        val vw2 = Point2D(pNextWin.x - pCur.x, pNextWin.y - pCur.y)
                        val lw1 = hypot(vw1.x, vw1.y)
                        val lw2 = hypot(vw2.x, vw2.y)
                        if (lw1 >= 15.0 && lw2 >= 15.0) {
                            val cosWin = -(vw1.x * vw2.x + vw1.y * vw2.y) / (lw1 * lw2)
                            if (cosWin > cos(toRad(110.0))) {
                                val loc = LatLng(trace[i].lat, trace[i].lng)
                                return BacktrackResult(
                                    hasBacktrack = true,
                                    location = loc,
                                    description = "Demi-tour / Virage en boucle dans le couloir"
                                )
                            }
                        }
                    }
                }
            } else {
                // Outside corridor: reset inside state so turns made outside the corridor never trigger a backtrack fault
                insideCount = 0
            }
        }
        return BacktrackResult(false)
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
        val spResult = results.find { it.point.type.equals("SP", true) || it.point.id.equals("SP", true) }
        val fpResult = results.find { it.point.type.equals("FP", true) || it.point.id.equals("FP", true) }

        var score = 0.0
        var label = ""
        var bannerTxt = ""
        var breakdown: Map<String, Int>? = null

        val confStats = conformity(courseData, trace, spResult?.traceIndex, fpResult?.traceIndex)

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
                val hidden = results.filter {
                    val t = it.point.type.lowercase()
                    val id = it.point.id.lowercase()
                    t != "sp" && id != "sp" && (t == "porte" || t == "cachee" || t == "balise")
                }
                val corridorPct = (confStats?.pctTime ?: confStats?.pctDist ?: confStats?.pctPts ?: 0).toDouble()

                if (hidden.isEmpty()) {
                    score = 1000.0 * (corridorPct / 100.0)
                    label = "Navigation imposée — % du parcours dans le couloir ($corridorPct%)"
                    bannerTxt = "Conformité du couloir : ${corridorPct.toInt()}% (Score : ${score.roundToInt()}/1000 pts)"
                } else {
                    val hCount = hidden.count { it.validated }
                    val nh = hidden.size.coerceAtLeast(1)
                    val qGates = 400.0 * (hCount.toDouble() / nh)
                    val qCorridor = 400.0 * (corridorPct / 100.0)
                    var qSpeed = 0.0
                    var sTxt = ""
                    if ((ref.tmin ?: 0.0) > 0 && dur != null) {
                        qSpeed = min(200.0, 200.0 * (ref.tmin!! / dur))
                        sTxt = " · Vitesse : ${qSpeed.roundToInt()}/200"
                    }
                    val maxPossible = 400.0 + 400.0 + (if ((ref.tmin ?: 0.0) > 0) 200.0 else 0.0)
                    score = (qGates + qCorridor + qSpeed) * (1000.0 / maxPossible)
                    label = "Navigation imposée — Portes ($hCount/$nh) + Couloir (${corridorPct.toInt()}%)"
                    bannerTxt = "Portes : $hCount/${hidden.size} · Couloir : ${corridorPct.toInt()}%$sTxt"
                }
            }
            EpreuveType.PRECISION -> {
                val hidden = results.filter { 
                    val t = it.point.type.lowercase()
                    val id = it.point.id.lowercase()
                    t != "sp" && id != "sp" && t != "fp" && id != "fp" && t != "tg" && (t == "porte" || t == "balise" || t == "cachee")
                }
                val tc = hidden.count { it.validated }
                val ntc = hidden.size.coerceAtLeast(1)
                val gatesRatio = if (hidden.isNotEmpty()) tc.toDouble() / ntc else 0.0

                val tgResults = results.filter { 
                    val t = it.point.type.lowercase()
                    val id = it.point.id.lowercase()
                    t != "sp" && id != "sp" && (t == "tg" || t == "fp")
                }
                val spTime = if (spResult != null && spResult.validated && spResult.time != null) spResult.time else trace.firstOrNull()?.time

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

                val couloirRatio = if (confStats?.pctTime != null) {
                    confStats.pctTime / 100.0
                } else if (confStats?.pctDist != null) {
                    confStats.pctDist / 100.0
                } else if (confStats?.pctPts != null) {
                    confStats.pctPts / 100.0
                } else {
                    0.0
                }

                val wGates = if (hidden.isNotEmpty()) ref.wGates else 0.0
                val wTime = if (tgResults.isNotEmpty()) ref.wTime else 0.0
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

                val bdMap = mutableMapOf<String, Int>()
                if (wCouloir > 0 || couloirPts > 0) {
                    bdMap["Parcours (Couloir)"] = couloirPts
                }
                if (wTime > 0 || timePts > 0) {
                    bdMap["Portes de temps"] = timePts
                }
                if (wGates > 0 || gatesPts > 0) {
                    bdMap["Portes franchies"] = gatesPts
                }
                if (wSpeed > 0 || speedPts > 0) {
                    bdMap["Vitesse"] = speedPts
                }
                breakdown = bdMap

                val barDetails = mutableListOf<String>()
                if (wCouloir > 0) barDetails.add("parcours ${wCouloir.toInt()}")
                if (wGates > 0) barDetails.add("portes ${wGates.toInt()}")
                if (wTime > 0) barDetails.add("temps ${wTime.toInt()}")
                if (wSpeed > 0) barDetails.add("vitesse ${wSpeed.toInt()}")
                label = "Barème : ${barDetails.joinToString(" + ")}"

                val bannerParts = mutableListOf<String>()
                if (wCouloir > 0) {
                    val pct = confStats?.pctTime ?: confStats?.pctDist ?: confStats?.pctPts ?: 0
                    bannerParts.add("Parcours couloir : $pct% ($couloirPts/${wCouloir.toInt()} pts)")
                }
                if (wGates > 0) {
                    bannerParts.add("Portes franchies : $tc/${hidden.size} ($gatesPts/${wGates.toInt()} pts)")
                } else if (hidden.isNotEmpty()) {
                    bannerParts.add("Portes franchies : $tc/${hidden.size}")
                }
                if (wTime > 0) {
                    if (declMap.isEmpty() && tgResults.isNotEmpty()) {
                        bannerParts.add("Portes temps : $timePts/${wTime.toInt()} pts (Saisissez vos temps annoncés pour comptabiliser)")
                    } else {
                        bannerParts.add("Portes temps : $timePts/${wTime.toInt()} pts")
                    }
                }
                if (wSpeed > 0) {
                    bannerParts.add("Vitesse : $speedPts/${wSpeed.toInt()} pts")
                }

                bannerTxt = bannerParts.joinToString(" · ")
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
        var mandatoryMsg = ""
        var faultPoint: LatLng? = null
        var faultDesc: String? = null

        if (pen.requireSP && spResult != null && !spResult.validated) {
            mandatoryMsg += "⚠ Porte d'entrée (SP) non franchie — score = 0. "
            faultPoint = LatLng(spResult.point.lat, spResult.point.lng)
            faultDesc = "Porte SP non franchie"
        }
        if (pen.requireFP && fpResult != null && !fpResult.validated) {
            mandatoryMsg += "⚠ Porte de sortie (FP) non franchie — score = 0. "
            if (faultPoint == null) {
                faultPoint = LatLng(fpResult.point.lat, fpResult.point.lng)
                faultDesc = "Porte FP non franchie"
            }
        }

        if (pen.noBacktrack) {
            val backtrackRes = detectBacktracking(courseData, trace, pen.backtrackAngleDeg, spResult?.traceIndex, fpResult?.traceIndex)
            if (backtrackRes.hasBacktrack) {
                mandatoryMsg += "⚠ Demi-tour / Retour en arrière détecté dans le couloir (angle < ${pen.backtrackAngleDeg.toInt()}°) — score = 0. "
                if (faultPoint == null) {
                    faultPoint = backtrackRes.location
                    faultDesc = backtrackRes.description ?: "Demi-tour / Retour en arrière"
                }
            }
        }

        if (mandatoryMsg.isNotEmpty()) {
            score = 0.0
            bannerTxt = mandatoryMsg + bannerTxt
        }

        val finalScore = max(0, min(1000, score.roundToInt()))

        return FlightAnalysisResult(
            score = finalScore,
            label = label,
            bannerTxt = bannerTxt,
            results = results,
            distMeters = dist,
            durationSeconds = dur,
            breakdown = breakdown,
            corridorStats = confStats,
            faultPoint = faultPoint,
            faultDescription = faultDesc
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

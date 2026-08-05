package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.util.GeometryUtils
import com.example.util.LatLng
import com.example.util.Point2D
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import kotlin.math.*

enum class MapToolMode {
    NAVIGATE,
    ADD_POINT,
    ADD_ROUTE_VERTEX,
    DRAW_ROUTE,
    INSERT_VERTEX,
    DELETE_ITEM,
    TOGGLE_SMOOTH,
    SIMULATE_FLIGHT
}

enum class MapTileProvider(val label: String, val urlTemplate: String, val maxZoom: Int) {
    OSM("OpenStreetMap", "https://tile.openstreetmap.org/{z}/{x}/{y}.png", 19),
    IGN_PLAN("IGN Plan V2", "https://data.geopf.fr/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=GEOGRAPHICALGRIDSYSTEMS.PLANIGNV2&STYLE=normal&FORMAT=image/png&TILEMATRIXSET=PM&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}", 19),
    TOPO("OpenTopoMap", "https://tile.opentopomap.org/{z}/{x}/{y}.png", 17)
}

class TileCache(private val cacheDir: File) {
    private val memoryCache = java.util.Collections.synchronizedMap(mutableMapOf<String, ImageBitmap>())

    suspend fun getTile(url: String): ImageBitmap? {
        memoryCache[url]?.let { return it }

        val fileName = url.hashCode().toString() + ".png"
        val diskFile = File(cacheDir, fileName)

        if (diskFile.exists()) {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    BitmapFactory.decodeFile(diskFile.absolutePath)
                } catch (e: Exception) {
                    diskFile.delete()
                    null
                }
            }
            if (bitmap != null) {
                val imageBitmap = bitmap.asImageBitmap()
                memoryCache[url] = imageBitmap
                return imageBitmap
            }
        }

        return withContext(Dispatchers.IO) {
            try {
                val connection = (URL(url).openConnection() as java.net.HttpURLConnection).apply {
                    setRequestProperty("User-Agent", "ParamoteurScoring/1.0 (Android; Mobile; Paramoteur)")
                    setRequestProperty("Accept", "image/png,image/jpeg,image/*;q=0.8")
                    connectTimeout = 5000
                    readTimeout = 5000
                }
                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val inputStream = connection.inputStream
                    val bytes = inputStream.readBytes()
                    inputStream.close()
                    if (bytes.isNotEmpty()) {
                        diskFile.writeBytes(bytes)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bitmap != null) {
                            val imageBitmap = bitmap.asImageBitmap()
                            memoryCache[url] = imageBitmap
                            imageBitmap
                        } else null
                    } else null
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}

@Composable
fun MapCanvas(
    modifier: Modifier = Modifier,
    courseData: CourseData,
    traceRaw: List<GpxPoint>?,
    traceCorrected: List<GpxPoint>?,
    toolMode: MapToolMode,
    addPointType: String,
    tileProvider: MapTileProvider,
    faultPoint: LatLng? = null,
    faultDescription: String? = null,
    focusLocation: LatLng? = null,
    onPointAdded: (String, Double, Double) -> Unit,
    onVertexAdded: (Double, Double) -> Unit,
    onVerticesDrawn: (List<LatLng>) -> Unit,
    onVertexInserted: (Double, Double) -> Unit,
    onItemDeleted: (Double, Double) -> Unit,
    onSmoothToggled: (Double, Double) -> Unit,
    onSimulatedFlightDrawn: (List<LatLng>) -> Unit,
    onPointDragged: (String, Double, Double) -> Unit,
    onVertexDragged: (String, Double, Double) -> Unit,
    onTileProviderChanged: ((MapTileProvider) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val tileCache = remember { TileCache(File(context.cacheDir, "tile_cache").apply { mkdirs() }) }

    // Map viewport state
    var centerLat by remember { mutableStateOf(46.6) }
    var centerLng by remember { mutableStateOf(2.2) }
    var zoomLevel by remember { mutableFloatStateOf(11f) }

    // Sync initial center with course points if available
    LaunchedEffect(courseData.points.size, courseData.routeVertices.size) {
        val origin = GeometryUtils.courseOrigin(courseData)
        if (courseData.points.isNotEmpty() || courseData.routeVertices.isNotEmpty()) {
            centerLat = origin.lat
            centerLng = origin.lng
        }
    }

    LaunchedEffect(focusLocation) {
        focusLocation?.let { loc ->
            centerLat = loc.lat
            centerLng = loc.lng
            zoomLevel = 15f
        }
    }

    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var freehandStroke by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var simStroke by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var draggedPointId by remember { mutableStateOf<String?>(null) }
    var draggedVertexId by remember { mutableStateOf<String?>(null) }
    var isDraggingPoint by remember { mutableStateOf(false) }

    val loadedTiles = remember { mutableStateMapOf<String, ImageBitmap>() }
    val pendingTiles = remember { mutableSetOf<String>() }

    // Coordinate conversion functions
    fun latLngToScreen(lat: Double, lng: Double): Offset {
        val n = 2.0.pow(zoomLevel.toDouble())
        val worldX = (lng + 180.0) / 360.0 * n * 256.0
        val latRad = Math.toRadians(lat)
        val worldY = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / Math.PI) / 2.0 * n * 256.0

        val centerWorldX = (centerLng + 180.0) / 360.0 * n * 256.0
        val centerLatRad = Math.toRadians(centerLat)
        val centerWorldY = (1.0 - ln(tan(centerLatRad) + 1.0 / cos(centerLatRad)) / Math.PI) / 2.0 * n * 256.0

        val screenX = canvasSize.width / 2f + (worldX - centerWorldX).toFloat()
        val screenY = canvasSize.height / 2f + (worldY - centerWorldY).toFloat()
        return Offset(screenX, screenY)
    }

    fun screenToLatLng(screenOffset: Offset): LatLng {
        val n = 2.0.pow(zoomLevel.toDouble())
        val centerWorldX = (centerLng + 180.0) / 360.0 * n * 256.0
        val centerLatRad = Math.toRadians(centerLat)
        val centerWorldY = (1.0 - ln(tan(centerLatRad) + 1.0 / cos(centerLatRad)) / Math.PI) / 2.0 * n * 256.0

        val worldX = centerWorldX + (screenOffset.x - canvasSize.width / 2f)
        val worldY = centerWorldY + (screenOffset.y - canvasSize.height / 2f)

        val lng = worldX / (n * 256.0) * 360.0 - 180.0
        val latRad = atan(sinh(Math.PI * (1.0 - 2.0 * worldY / (n * 256.0))))
        val lat = Math.toDegrees(latRad)
        return LatLng(lat, lng)
    }

    Box(modifier = modifier.background(DarkBg)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(toolMode) {
                    if (toolMode == MapToolMode.NAVIGATE) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            if (zoom != 1f) {
                                zoomLevel = (zoomLevel * zoom).coerceIn(4f, 18f)
                            }
                            if (pan != Offset.Zero) {
                                val targetCenterScreen = Offset(canvasSize.width / 2f - pan.x, canvasSize.height / 2f - pan.y)
                                val newCenter = screenToLatLng(targetCenterScreen)
                                centerLat = newCenter.lat
                                centerLng = newCenter.lng
                            }
                        }
                    }
                }
                .pointerInput(toolMode) {
                    detectTapGestures { offset ->
                        val latLng = screenToLatLng(offset)
                        when (toolMode) {
                            MapToolMode.ADD_POINT -> onPointAdded(addPointType, latLng.lat, latLng.lng)
                            MapToolMode.ADD_ROUTE_VERTEX -> onVertexAdded(latLng.lat, latLng.lng)
                            MapToolMode.INSERT_VERTEX -> onVertexInserted(latLng.lat, latLng.lng)
                            MapToolMode.DELETE_ITEM -> onItemDeleted(latLng.lat, latLng.lng)
                            MapToolMode.TOGGLE_SMOOTH -> onSmoothToggled(latLng.lat, latLng.lng)
                            else -> {}
                        }
                    }
                }
                .pointerInput(toolMode) {
                    if (toolMode == MapToolMode.DRAW_ROUTE || toolMode == MapToolMode.SIMULATE_FLIGHT) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val latLng = screenToLatLng(offset)
                                when (toolMode) {
                                    MapToolMode.DRAW_ROUTE -> freehandStroke = listOf(latLng)
                                    MapToolMode.SIMULATE_FLIGHT -> simStroke = listOf(latLng)
                                    else -> {}
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val latLng = screenToLatLng(change.position)
                                when (toolMode) {
                                    MapToolMode.DRAW_ROUTE -> {
                                        if (freehandStroke.isEmpty() || GeometryUtils.haversine(freehandStroke.last(), latLng) > 6) {
                                            freehandStroke = freehandStroke + latLng
                                        }
                                    }
                                    MapToolMode.SIMULATE_FLIGHT -> {
                                        if (simStroke.isEmpty() || GeometryUtils.haversine(simStroke.last(), latLng) > 8) {
                                            simStroke = simStroke + latLng
                                        }
                                    }
                                    else -> {}
                                }
                            },
                            onDragEnd = {
                                when (toolMode) {
                                    MapToolMode.DRAW_ROUTE -> {
                                        if (freehandStroke.size > 1) {
                                            onVerticesDrawn(freehandStroke)
                                        }
                                        freehandStroke = emptyList()
                                    }
                                    MapToolMode.SIMULATE_FLIGHT -> {
                                        if (simStroke.size > 1) {
                                            onSimulatedFlightDrawn(simStroke)
                                        }
                                        simStroke = emptyList()
                                    }
                                    else -> {}
                                }
                            }
                        )
                    }
                }
        ) {
            canvasSize = size

            // 1. Draw Map Tiles
            val z = zoomLevel.toInt().coerceIn(1, tileProvider.maxZoom)
            val numTiles = 2.0.pow(z.toDouble())

            val centerWorldX = (centerLng + 180.0) / 360.0 * numTiles * 256.0
            val centerLatRad = Math.toRadians(centerLat)
            val centerWorldY = (1.0 - ln(tan(centerLatRad) + 1.0 / cos(centerLatRad)) / Math.PI) / 2.0 * numTiles * 256.0

            val minWorldX = centerWorldX - size.width / 2f
            val maxWorldX = centerWorldX + size.width / 2f
            val minWorldY = centerWorldY - size.height / 2f
            val maxWorldY = centerWorldY + size.height / 2f

            val minTileX = floor(minWorldX / 256.0).toInt()
            val maxTileX = floor(maxWorldX / 256.0).toInt()
            val minTileY = floor(minWorldY / 256.0).toInt()
            val maxTileY = floor(maxWorldY / 256.0).toInt()

            for (tileX in minTileX..maxTileX) {
                for (tileY in minTileY..maxTileY) {
                    val tileUrl = tileProvider.urlTemplate
                        .replace("{z}", z.toString())
                        .replace("{x}", tileX.toString())
                        .replace("{y}", tileY.toString())

                    val screenX = (tileX * 256.0 - minWorldX).toFloat()
                    val screenY = (tileY * 256.0 - minWorldY).toFloat()

                    val bitmap = loadedTiles[tileUrl]
                    if (bitmap != null) {
                        drawImage(
                            image = bitmap,
                            dstOffset = androidx.compose.ui.unit.IntOffset(screenX.toInt(), screenY.toInt()),
                            dstSize = androidx.compose.ui.unit.IntSize(256, 256)
                        )
                    } else {
                        if (!pendingTiles.contains(tileUrl)) {
                            pendingTiles.add(tileUrl)
                            coroutineScope.launch {
                                try {
                                    val tileBitmap = tileCache.getTile(tileUrl)
                                    if (tileBitmap != null) {
                                        loadedTiles[tileUrl] = tileBitmap
                                    }
                                } finally {
                                    pendingTiles.remove(tileUrl)
                                }
                            }
                        }
                    }
                }
            }

            val origin = GeometryUtils.courseOrigin(courseData)

            // 2. Draw Corridor
            val denseCl = GeometryUtils.centerlineDenseGeo(courseData, origin)
            if (denseCl.size >= 2) {
                val clLocal = denseCl.map { GeometryUtils.toXY(it, origin) }
                val halfWidth = courseData.corridorWidth / 2.0
                val bufferedLocal = GeometryUtils.bufferPolyline(clLocal, halfWidth)
                val polygonGeo = bufferedLocal.map { GeometryUtils.toLatLng(it, origin) }
                val screenPoly = polygonGeo.map { latLngToScreen(it.lat, it.lng) }

                if (screenPoly.size > 2) {
                    val path = Path().apply {
                        moveTo(screenPoly[0].x, screenPoly[0].y)
                        for (i in 1 until screenPoly.size) {
                            lineTo(screenPoly[i].x, screenPoly[i].y)
                        }
                        close()
                    }
                    drawPath(path, color = SkyBlue.copy(alpha = 0.15f))
                    drawPath(path, color = SkyDim.copy(alpha = 0.6f), style = Stroke(width = 2.dp.toPx()))
                }

                // Draw centerline
                val screenCl = denseCl.map { latLngToScreen(it.lat, it.lng) }
                for (i in 1 until screenCl.size) {
                    drawLine(
                        color = SkyDim,
                        start = screenCl[i - 1],
                        end = screenCl[i],
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                    )
                }
            }

            // 3. Draw Corridor Vertices
            courseData.routeVertices.forEach { v ->
                val pos = latLngToScreen(v.lat, v.lng)
                val color = if (v.smooth) Color(0xFF4FD6C9) else AmberAccent
                drawCircle(color = color, radius = 6.dp.toPx(), center = pos)
                drawCircle(color = Color.White, radius = 6.dp.toPx(), center = pos, style = Stroke(width = 1.5.dp.toPx()))
            }

            // Freehand Corridor Drawing Preview
            if (freehandStroke.size > 1) {
                val screenStroke = freehandStroke.map { latLngToScreen(it.lat, it.lng) }
                for (i in 1 until screenStroke.size) {
                    drawLine(color = AmberAccent, start = screenStroke[i - 1], end = screenStroke[i], strokeWidth = 3.dp.toPx())
                }
            }

            // 4. Draw Gates and Turnpoints
            courseData.points.forEachIndexed { index, p ->
                val pos = latLngToScreen(p.lat, p.lng)
                val color = when (p.type.uppercase()) {
                    "SP" -> ColorSP
                    "FP" -> ColorFP
                    "PORTE" -> ColorPorte
                    "TG" -> ColorTG
                    "BALISE" -> ColorBalise
                    "CACHEE" -> ColorCachee
                    else -> ColorBalise
                }

                if (p.type == "balise" || p.type == "cachee") {
                    // Circle turnpoint
                    val edgeLatLng = GeometryUtils.toLatLng(Point2D(0.0, p.radius), LatLng(p.lat, p.lng))
                    val edgeScreen = latLngToScreen(edgeLatLng.lat, edgeLatLng.lng)
                    val radiusPx = hypot((edgeScreen.x - pos.x).toDouble(), (edgeScreen.y - pos.y).toDouble()).toFloat()

                    drawCircle(color = color.copy(alpha = 0.08f), radius = radiusPx, center = pos)
                    drawCircle(color = color, radius = radiusPx, center = pos, style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)))
                    drawCircle(color = color, radius = 7.dp.toPx(), center = pos)
                    drawCircle(color = Color.White, radius = 7.dp.toPx(), center = pos, style = Stroke(width = 2.dp.toPx()))
                } else {
                    // Line Gate
                    val (aLocal, bLocal) = GeometryUtils.gateEndpointsLocal(p, courseData, origin)
                    val aGeo = GeometryUtils.toLatLng(aLocal, origin)
                    val bGeo = GeometryUtils.toLatLng(bLocal, origin)
                    val aScreen = latLngToScreen(aGeo.lat, aGeo.lng)
                    val bScreen = latLngToScreen(bGeo.lat, bGeo.lng)

                    drawLine(color = color, start = aScreen, end = bScreen, strokeWidth = 6.dp.toPx())
                    drawCircle(color = color, radius = 7.dp.toPx(), center = pos)
                    drawCircle(color = Color.White, radius = 7.dp.toPx(), center = pos, style = Stroke(width = 2.dp.toPx()))
                }

                // Draw Label
                drawContext.canvas.nativeCanvas.drawText(
                    "${index + 1}. ${p.type.uppercase()}",
                    pos.x,
                    pos.y - 14.dp.toPx(),
                    AndroidPaint().apply {
                        setColor(android.graphics.Color.WHITE)
                        textSize = 12.sp.toPx()
                        textAlign = AndroidPaint.Align.CENTER
                        isFakeBoldText = true
                        setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
                    }
                )
            }

            // 5. Draw GPX Track
            // If both traceCorrected and traceRaw exist, draw traceRaw underneath as a faint reference
            if (traceCorrected != null && traceRaw != null && traceRaw.size > 1) {
                val screenRaw = traceRaw.map { latLngToScreen(it.lat, it.lng) }
                for (i in 1 until screenRaw.size) {
                    drawLine(color = InkDim.copy(alpha = 0.35f), start = screenRaw[i - 1], end = screenRaw[i], strokeWidth = 1.5.dp.toPx())
                }
            }

            val mainTrace = traceCorrected ?: traceRaw
            mainTrace?.let { pts ->
                if (pts.isNotEmpty()) {
                    val screenPts = pts.map { latLngToScreen(it.lat, it.lng) }
                    if (pts.size == 1) {
                        val pos = screenPts.first()
                        val cl = GeometryUtils.centerlineDenseGeo(courseData, origin)
                        val clLocal = if (cl.size >= 2) cl.map { GeometryUtils.toXY(it, origin) } else null
                        val half = courseData.corridorWidth / 2.0
                        val ptLocal = GeometryUtils.toXY(LatLng(pts[0].lat, pts[0].lng), origin)
                        val isInside = if (clLocal != null) GeometryUtils.distToPolyline(ptLocal, clLocal) <= half else true
                        val col = if (isInside) Color(0xFF16A34A) else Color(0xFFDC2626)

                        drawCircle(color = col.copy(alpha = 0.3f), radius = 16.dp.toPx(), center = pos)
                        drawCircle(color = col, radius = 8.dp.toPx(), center = pos)
                        drawCircle(color = Color.White, radius = 8.dp.toPx(), center = pos, style = Stroke(width = 2.dp.toPx()))
                    } else {
                        val cl = GeometryUtils.centerlineDenseGeo(courseData, origin)
                        val clLocal = if (cl.size >= 2) cl.map { GeometryUtils.toXY(it, origin) } else null
                        val half = courseData.corridorWidth / 2.0
                        val tl = pts.map { GeometryUtils.toXY(LatLng(it.lat, it.lng), origin) }

                        val bgWidthPx = 5.5.dp.toPx()
                        val fgWidthPx = 3.5.dp.toPx()

                        // Dark outline pass for high contrast against IGN/OSM tiles
                        for (i in 1 until screenPts.size) {
                            drawLine(
                                color = Color.Black.copy(alpha = 0.35f),
                                start = screenPts[i - 1],
                                end = screenPts[i],
                                strokeWidth = bgWidthPx
                            )
                        }

                        // Colored pass: Green inside corridor, Red outside corridor
                        for (i in 1 until screenPts.size) {
                            var inside = true
                            if (clLocal != null) {
                                val p1Inside = GeometryUtils.distToPolyline(tl[i - 1], clLocal) <= half
                                val p2Inside = GeometryUtils.distToPolyline(tl[i], clLocal) <= half
                                inside = p1Inside && p2Inside
                            }
                            val color = if (inside) Color(0xFF16A34A) else Color(0xFFDC2626)
                            drawLine(
                                color = color,
                                start = screenPts[i - 1],
                                end = screenPts[i],
                                strokeWidth = fgWidthPx
                            )
                        }

                        // Draw live current position / endpoint marker on the last point
                        val lastPos = screenPts.last()
                        val lastInside = if (clLocal != null && tl.isNotEmpty()) GeometryUtils.distToPolyline(tl.last(), clLocal) <= half else true
                        val headColor = if (lastInside) Color(0xFF16A34A) else Color(0xFFDC2626)

                        drawCircle(color = headColor.copy(alpha = 0.35f), radius = 14.dp.toPx(), center = lastPos)
                        drawCircle(color = headColor, radius = 7.dp.toPx(), center = lastPos)
                        drawCircle(color = Color.White, radius = 7.dp.toPx(), center = lastPos, style = Stroke(width = 2.dp.toPx()))
                    }
                }
            }

            // Draw Simulated Flight Stroke Preview
            if (simStroke.size > 1) {
                val screenStroke = simStroke.map { latLngToScreen(it.lat, it.lng) }
                for (i in 1 until screenStroke.size) {
                    drawLine(
                        color = Color(0xFFFF66CC),
                        start = screenStroke[i - 1],
                        end = screenStroke[i],
                        strokeWidth = 3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )
                }
            }

            // Draw fault marker (Red Flag & Pulsing Target) if faultPoint is set
            faultPoint?.let { fp ->
                val fpScreen = latLngToScreen(fp.lat, fp.lng)
                val pulseRadius = 24.dp.toPx()
                val innerRadius = 12.dp.toPx()

                // Red translucent outer target ring
                drawCircle(color = RedAlert.copy(alpha = 0.35f), radius = pulseRadius, center = fpScreen)
                drawCircle(color = RedAlert, radius = innerRadius, center = fpScreen)
                drawCircle(color = Color.White, radius = innerRadius, center = fpScreen, style = Stroke(width = 3.dp.toPx()))
                drawCircle(color = RedAlert, radius = 5.dp.toPx(), center = fpScreen)

                val labelText = "🚩 FAUTE : ${faultDescription ?: "Pénalité / Anomalie"}"
                drawContext.canvas.nativeCanvas.drawText(
                    labelText,
                    fpScreen.x,
                    fpScreen.y - 22.dp.toPx(),
                    AndroidPaint().apply {
                        color = android.graphics.Color.RED
                        textSize = 13.sp.toPx()
                        textAlign = AndroidPaint.Align.CENTER
                        isFakeBoldText = true
                        setShadowLayer(6f, 0f, 0f, android.graphics.Color.WHITE)
                    }
                )
            }

            // 6. Scale bar
            val scaleMeters = when {
                zoomLevel >= 15 -> 100.0
                zoomLevel >= 13 -> 500.0
                zoomLevel >= 11 -> 2000.0
                zoomLevel >= 9 -> 10000.0
                else -> 50000.0
            }
            val startLatLng = screenToLatLng(Offset(40f, size.height - 40f))
            val endLatLng = GeometryUtils.toLatLng(Point2D(scaleMeters, 0.0), startLatLng)
            val endScreen = latLngToScreen(endLatLng.lat, endLatLng.lng)
            val barLengthPx = abs(endScreen.x - 40f)

            if (barLengthPx >= 10f && barLengthPx <= 400f) {
                val startX = 40f
                val startY = size.height - 40f
                drawLine(color = InkText, start = Offset(startX, startY), end = Offset(startX + barLengthPx, startY), strokeWidth = 3.dp.toPx())
                drawLine(color = InkText, start = Offset(startX, startY - 8f), end = Offset(startX, startY + 8f), strokeWidth = 3.dp.toPx())
                drawLine(color = InkText, start = Offset(startX + barLengthPx, startY - 8f), end = Offset(startX + barLengthPx, startY + 8f), strokeWidth = 3.dp.toPx())

                val scaleLabel = if (scaleMeters >= 1000) "${(scaleMeters / 1000).toInt()} km" else "${scaleMeters.toInt()} m"
                drawContext.canvas.nativeCanvas.drawText(
                    scaleLabel,
                    startX + barLengthPx / 2f,
                    startY - 10f,
                    AndroidPaint().apply {
                        setColor(android.graphics.Color.BLACK)
                        textSize = 11.sp.toPx()
                        textAlign = AndroidPaint.Align.CENTER
                        isFakeBoldText = true
                        setShadowLayer(4f, 0f, 0f, android.graphics.Color.WHITE)
                    }
                )
            }
        }

        // Floating Map Controls (Layers, Zoom +, Zoom -, Recenter)
        var tileMenuExpanded by remember { mutableStateOf(false) }

        // Trace Legend Indicator (if trace is present)
        val activeTrace = traceCorrected ?: traceRaw
        if (activeTrace != null && activeTrace.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 12.dp)
                    .border(1.dp, BorderOutline, androidx.compose.foundation.shape.RoundedCornerShape(20.dp)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                color = HighDensitySurface.copy(alpha = 0.92f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFF16A34A), CircleShape)
                        )
                        Text("Dans couloir", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighDensityHeaderTitle)
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(12.dp)
                            .background(BorderOutline)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFFDC2626), CircleShape)
                        )
                        Text("Hors couloir", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HighDensityHeaderTitle)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box {
                FloatingActionButton(
                    onClick = { tileMenuExpanded = true },
                    modifier = Modifier
                        .size(44.dp)
                        .border(1.dp, if (tileProvider == MapTileProvider.IGN_PLAN) GreenSuccess else BorderOutline, CircleShape),
                    containerColor = HighDensitySurface,
                    contentColor = if (tileProvider == MapTileProvider.IGN_PLAN) GreenSuccess else PrimaryBlue,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(Icons.Default.Layers, contentDescription = "Fond de carte")
                }

                DropdownMenu(
                    expanded = tileMenuExpanded,
                    onDismissRequest = { tileMenuExpanded = false }
                ) {
                    MapTileProvider.entries.forEach { provider ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (provider == MapTileProvider.IGN_PLAN) "🇫🇷 ${provider.label} (Conseillé)" else provider.label,
                                    fontWeight = if (provider == tileProvider) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = if (provider == tileProvider) GreenSuccess else HighDensityHeaderTitle
                                )
                            },
                            onClick = {
                                tileMenuExpanded = false
                                onTileProviderChanged?.invoke(provider)
                            }
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = { zoomLevel = (zoomLevel + 0.5f).coerceAtMost(18f) },
                modifier = Modifier
                    .size(44.dp)
                    .border(1.dp, BorderOutline, CircleShape),
                containerColor = HighDensitySurface,
                contentColor = PrimaryBlue,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom +")
            }
            FloatingActionButton(
                onClick = { zoomLevel = (zoomLevel - 0.5f).coerceAtLeast(4f) },
                modifier = Modifier
                    .size(44.dp)
                    .border(1.dp, BorderOutline, CircleShape),
                containerColor = HighDensitySurface,
                contentColor = PrimaryBlue,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom -")
            }
            FloatingActionButton(
                onClick = {
                    val origin = GeometryUtils.courseOrigin(courseData)
                    centerLat = origin.lat
                    centerLng = origin.lng
                    zoomLevel = 12f
                },
                modifier = Modifier
                    .size(44.dp)
                    .border(1.dp, BorderOutline, CircleShape),
                containerColor = HighDensitySurface,
                contentColor = PrimaryBlue,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Recentrer")
            }
        }
    }
}

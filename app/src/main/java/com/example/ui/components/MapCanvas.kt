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
    private val memoryCache = mutableMapOf<String, ImageBitmap>()

    suspend fun getTile(url: String): ImageBitmap? {
        memoryCache[url]?.let { return it }

        val fileName = url.hashCode().toString() + ".png"
        val diskFile = File(cacheDir, fileName)

        if (diskFile.exists()) {
            val bitmap = withContext(Dispatchers.IO) {
                BitmapFactory.decodeFile(diskFile.absolutePath)
            }
            if (bitmap != null) {
                val imageBitmap = bitmap.asImageBitmap()
                memoryCache[url] = imageBitmap
                return imageBitmap
            }
        }

        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection()
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                val inputStream = connection.getInputStream()
                val bytes = inputStream.readBytes()
                inputStream.close()
                diskFile.writeBytes(bytes)

                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    val imageBitmap = bitmap.asImageBitmap()
                    synchronized(memoryCache) {
                        memoryCache[url] = imageBitmap
                    }
                    imageBitmap
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

    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var freehandStroke by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var simStroke by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var draggedPointId by remember { mutableStateOf<String?>(null) }
    var draggedVertexId by remember { mutableStateOf<String?>(null) }

    val loadedTiles = remember { mutableStateMapOf<String, ImageBitmap>() }

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
                .pointerInput(toolMode, zoomLevel, centerLat, centerLng) {
                    if (toolMode == MapToolMode.NAVIGATE) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newZoom = (zoomLevel * zoom).coerceIn(4f, 18f)
                            zoomLevel = newZoom

                            val currentCenterScreen = Offset(canvasSize.width / 2f, canvasSize.height / 2f)
                            val targetCenterScreen = currentCenterScreen - pan
                            val newCenter = screenToLatLng(targetCenterScreen)
                            centerLat = newCenter.lat
                            centerLng = newCenter.lng
                        }
                    }
                }
                .pointerInput(toolMode, zoomLevel, centerLat, centerLng) {
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
                .pointerInput(toolMode, zoomLevel, centerLat, centerLng) {
                    if (toolMode == MapToolMode.DRAW_ROUTE || toolMode == MapToolMode.SIMULATE_FLIGHT || toolMode == MapToolMode.NAVIGATE) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val latLng = screenToLatLng(offset)
                                when (toolMode) {
                                    MapToolMode.DRAW_ROUTE -> freehandStroke = listOf(latLng)
                                    MapToolMode.SIMULATE_FLIGHT -> simStroke = listOf(latLng)
                                    MapToolMode.NAVIGATE -> {
                                        // Check if near a gate or vertex to start dragging
                                        val origin = GeometryUtils.courseOrigin(courseData)
                                        val clickLocal = GeometryUtils.toXY(latLng, origin)
                                        var closestPtId: String? = null
                                        var closestVertId: String? = null
                                        var minDist = 250.0

                                        courseData.points.forEach { p ->
                                            val pLocal = GeometryUtils.toXY(LatLng(p.lat, p.lng), origin)
                                            val d = hypot(pLocal.x - clickLocal.x, pLocal.y - clickLocal.y)
                                            if (d < minDist) {
                                                minDist = d
                                                closestPtId = p.id
                                            }
                                        }

                                        courseData.routeVertices.forEach { v ->
                                            val vLocal = GeometryUtils.toXY(LatLng(v.lat, v.lng), origin)
                                            val d = hypot(vLocal.x - clickLocal.x, vLocal.y - clickLocal.y)
                                            if (d < minDist) {
                                                minDist = d
                                                closestVertId = v.id
                                                closestPtId = null
                                            }
                                        }

                                        draggedPointId = closestPtId
                                        draggedVertexId = closestVertId
                                    }
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
                                    MapToolMode.NAVIGATE -> {
                                        draggedPointId?.let { id ->
                                            onPointDragged(id, latLng.lat, latLng.lng)
                                        }
                                        draggedVertexId?.let { id ->
                                            onVertexDragged(id, latLng.lat, latLng.lng)
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
                                    MapToolMode.NAVIGATE -> {
                                        draggedPointId = null
                                        draggedVertexId = null
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
                        coroutineScope.launch {
                            val tileBitmap = tileCache.getTile(tileUrl)
                            if (tileBitmap != null) {
                                loadedTiles[tileUrl] = tileBitmap
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
            traceRaw?.let { pts ->
                if (pts.size > 1) {
                    val screenPts = pts.map { latLngToScreen(it.lat, it.lng) }
                    for (i in 1 until screenPts.size) {
                        drawLine(color = InkDim.copy(alpha = 0.4f), start = screenPts[i - 1], end = screenPts[i], strokeWidth = 1.5.dp.toPx())
                    }
                }
            }

            traceCorrected?.let { pts ->
                if (pts.size > 1) {
                    val cl = GeometryUtils.centerlineDenseGeo(courseData, origin)
                    val clLocal = if (cl.size >= 2) cl.map { GeometryUtils.toXY(it, origin) } else null
                    val half = courseData.corridorWidth / 2.0
                    val tl = pts.map { GeometryUtils.toXY(LatLng(it.lat, it.lng), origin) }
                    val screenPts = pts.map { latLngToScreen(it.lat, it.lng) }

                    for (i in 1 until screenPts.size) {
                        var inside = true
                        if (clLocal != null) {
                            inside = GeometryUtils.distToPolyline(tl[i - 1], clLocal) <= half && GeometryUtils.distToPolyline(tl[i], clLocal) <= half
                        }
                        val color = if (inside) GreenOk else RedAlert
                        drawLine(color = color, start = screenPts[i - 1], end = screenPts[i], strokeWidth = 3.5.dp.toPx())
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

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                FloatingActionButton(
                    onClick = { tileMenuExpanded = true },
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, if (tileProvider == MapTileProvider.IGN_PLAN) GreenSuccess else BorderOutline, CircleShape),
                    containerColor = HighDensitySurface,
                    contentColor = if (tileProvider == MapTileProvider.IGN_PLAN) GreenSuccess else PrimaryBlue,
                    elevation = FloatingActionButtonDefaults.elevation(2.dp)
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
                    .size(40.dp)
                    .border(1.dp, BorderOutline, CircleShape),
                containerColor = HighDensitySurface,
                contentColor = PrimaryBlue,
                elevation = FloatingActionButtonDefaults.elevation(2.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom +")
            }
            FloatingActionButton(
                onClick = { zoomLevel = (zoomLevel - 0.5f).coerceAtLeast(4f) },
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, BorderOutline, CircleShape),
                containerColor = HighDensitySurface,
                contentColor = PrimaryBlue,
                elevation = FloatingActionButtonDefaults.elevation(2.dp)
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
                    .size(40.dp)
                    .border(1.dp, BorderOutline, CircleShape),
                containerColor = HighDensitySurface,
                contentColor = PrimaryBlue,
                elevation = FloatingActionButtonDefaults.elevation(2.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Recentrer")
            }
        }
    }
}

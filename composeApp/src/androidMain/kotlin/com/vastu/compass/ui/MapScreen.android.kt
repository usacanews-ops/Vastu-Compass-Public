package com.vastu.compass.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.location.Location
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.vastu.compass.Saffron
import com.vastu.compass.VastuGreen
import com.vastu.compass.sensor.getCompassSensor
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.*

const val METERS_TO_FEET = 3.28084
const val FEET_TO_METERS = 0.3048
const val SQ_METERS_TO_SQ_FEET = 10.7639
const val EARTH_RADIUS_METERS = 6371000.0

fun calculateDistanceMeters(p1: LatLng, p2: LatLng): Float {
    val results = FloatArray(1)
    Location.distanceBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude, results)
    return results[0]
}

fun calculateBearing(from: LatLng, to: LatLng): Float {
    val dLng = Math.toRadians(to.longitude - from.longitude)
    val lat1 = Math.toRadians(from.latitude)
    val lat2 = Math.toRadians(to.latitude)
    val y = sin(dLng) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(dLng)
    val bearing = Math.toDegrees(atan2(y, x))
    return ((bearing + 360) % 360).toFloat()
}

/**
 * Calculates a target destination LatLng given starting origin, distance in meters, and bearing.
 */
fun pointAtDistanceAndBearing(origin: LatLng, distanceMeters: Double, bearingDegrees: Double): LatLng {
    val distRatio = distanceMeters / EARTH_RADIUS_METERS
    val brngRad = Math.toRadians(bearingDegrees)
    val lat1Rad = Math.toRadians(origin.latitude)
    val lon1Rad = Math.toRadians(origin.longitude)

    val lat2Rad = asin(sin(lat1Rad) * cos(distRatio) + cos(lat1Rad) * sin(distRatio) * cos(brngRad))
    val lon2Rad = lon1Rad + atan2(sin(brngRad) * sin(distRatio) * cos(lat1Rad), cos(distRatio) - sin(lat1Rad) * sin(lat2Rad))

    return LatLng(Math.toDegrees(lat2Rad), Math.toDegrees(lon2Rad))
}

fun calculateCentroidAndArea(points: List<LatLng>): Pair<LatLng, Double> {
    if (points.isEmpty()) return Pair(LatLng(0.0, 0.0), 0.0)
    if (points.size < 3) {
        val avgLat = points.map { it.latitude }.average()
        val avgLng = points.map { it.longitude }.average()
        return Pair(LatLng(avgLat, avgLng), 0.0)
    }

    val refLat = points[0].latitude
    val refLng = points[0].longitude
    val rad = PI / 180.0
    val meanLatRad = refLat * rad
    val metersPerDegLat = 111132.92 - 559.82 * cos(2 * meanLatRad) + 1.175 * cos(4 * meanLatRad)
    val metersPerDegLng = 111412.84 * cos(meanLatRad) - 93.5 * cos(3 * meanLatRad)

    val xyPoints = points.map { pt ->
        val x = (pt.longitude - refLng) * metersPerDegLng
        val y = (pt.latitude - refLat) * metersPerDegLat
        Pair(x, y)
    }

    var signedArea = 0.0
    var cxMeters = 0.0
    var cyMeters = 0.0
    val n = xyPoints.size

    for (i in 0 until n) {
        val (xi, yi) = xyPoints[i]
        val (xi1, yi1) = xyPoints[(i + 1) % n]
        val cross = (xi * yi1) - (xi1 * yi)
        signedArea += cross
        cxMeters += (xi + xi1) * cross
        cyMeters += (yi + yi1) * cross
    }

    signedArea *= 0.5
    val areaSqMeters = abs(signedArea)

    if (areaSqMeters < 1e-7) {
        val avgLat = points.map { it.latitude }.average()
        val avgLng = points.map { it.longitude }.average()
        return Pair(LatLng(avgLat, avgLng), 0.0)
    }

    cxMeters /= (6.0 * signedArea)
    cyMeters /= (6.0 * signedArea)

    val centroidLat = refLat + (cyMeters / metersPerDegLat)
    val centroidLng = refLng + (cxMeters / metersPerDegLng)
    return Pair(LatLng(centroidLat, centroidLng), areaSqMeters * SQ_METERS_TO_SQ_FEET)
}

fun createDistanceBadge(context: Context, text: String, isOpaque: Boolean): BitmapDescriptor {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        textSize = 34f
        isFakeBoldText = true
        alpha = if (isOpaque) 128 else 255
    }
    val bounds = Rect()
    paint.getTextBounds(text, 0, text.length, bounds)

    val paddingH = 22
    val paddingV = 14
    val width = bounds.width() + paddingH * 2
    val height = bounds.height() + paddingV * 2

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
        alpha = if (isOpaque) 128 else 245
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 3f
        alpha = if (isOpaque) 128 else 255
    }

    val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
    canvas.drawRoundRect(rect, 14f, 14f, bgPaint)
    canvas.drawRoundRect(rect, 14f, 14f, strokePaint)

    val textX = (width - bounds.width()) / 2f - bounds.left
    val textY = (height + bounds.height()) / 2f - bounds.bottom
    canvas.drawText(text, textX, textY, paint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

// Device Storage Helpers
private const val PREFS_NAME = "vastu_map_prefs"
private const val KEY_PLOT_DATA = "saved_plot_json"

fun savePlotToStorage(context: Context, points: List<LatLng>, centroid: LatLng?, areaSqFt: Double) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    if (points.isEmpty()) {
        prefs.edit().remove(KEY_PLOT_DATA).apply()
        return
    }
    val obj = JSONObject()
    val arr = JSONArray()
    points.forEach { pt ->
        val p = JSONObject()
        p.put("lat", pt.latitude)
        p.put("lng", pt.longitude)
        arr.put(p)
    }
    obj.put("points", arr)
    centroid?.let {
        obj.put("cLat", it.latitude)
        obj.put("cLng", it.longitude)
    }
    obj.put("areaSqFt", areaSqFt)
    prefs.edit().putString(KEY_PLOT_DATA, obj.toString()).apply()
}

fun exportKml(context: Context, points: List<LatLng>, centroid: LatLng?, areaSqFt: Double) {
    if (points.isEmpty()) return

    val kmlCoordinates = StringBuilder()
    points.forEach { pt ->
        kmlCoordinates.append("%.9f,%.9f,0\n".format(pt.longitude, pt.latitude))
    }
    // Close polygon loop
    kmlCoordinates.append("%.9f,%.9f,0\n".format(points[0].longitude, points[0].latitude))

    val kmlContent = """
        <?xml version="1.0" encoding="UTF-8"?>
        <kml xmlns="http://www.opengis.net/kml/2.2">
          <Document>
            <name>Vastu Property Layout</name>
            <description>Total Area: ${"%.2f".format(areaSqFt)} sq ft</description>
            <Placemark>
              <name>Property Boundary</name>
              <Polygon>
                <outerBoundaryIs>
                  <LinearRing>
                    <coordinates>
                      $kmlCoordinates
                    </coordinates>
                  </LinearRing>
                </outerBoundaryIs>
              </Polygon>
            </Placemark>
            ${if (centroid != null) """
            <Placemark>
              <name>Brahmasthan (Centroid)</name>
              <Point>
                <coordinates>${"%.9f,%.9f,0".format(centroid.longitude, centroid.latitude)}</coordinates>
              </Point>
            </Placemark>
            """ else ""}
          </Document>
        </kml>
    """.trimIndent()

    try {
        val file = File(context.cacheDir, "vastu_property_${System.currentTimeMillis()}.kml")
        file.writeText(kmlContent)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.google-earth.kml+xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Vastu Property Boundary & Centroid Data")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export KML"))
    } catch (e: Exception) {
        Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

@SuppressLint("MissingPermission")
@Composable
actual fun MapScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val compassSensor = remember { getCompassSensor() }
    val deviceAzimuth by compassSensor.azimuthDegrees.collectAsState(initial = 0f)

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasLocationPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    DisposableEffect(hasLocationPermission) {
        if (!hasLocationPermission) return@DisposableEffect onDispose {}

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    userLocation = LatLng(loc.latitude, loc.longitude)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(28.6139, 77.2090), 19f)
    }

    val corners = remember { mutableStateListOf<LatLng>() }
    // Keeps distance constraints: Key = Marker index, Value = locked distance in meters from previous marker (index - 1)
    val distanceConstraints = remember { mutableStateMapOf<Int, Double>() }

    var centroid by remember { mutableStateOf<LatLng?>(null) }
    var totalAreaSqFt by remember { mutableStateOf(0.0) }
    var isGuidingMode by remember { mutableStateOf(false) }
    var mapType by remember { mutableStateOf(MapType.HYBRID) }

    // Dialog state for editing edge length
    var editingEdgeIndex by remember { mutableStateOf<Int?>(null) }
    var editDistanceInput by remember { mutableStateOf("") }

    // Input state for adding next fixed marker
    var nextMarkerDistanceText by remember { mutableStateOf("") }

    // Persistent storage auto-restore
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedData = prefs.getString(KEY_PLOT_DATA, null)
        if (!savedData.isNullOrBlank()) {
            try {
                val obj = JSONObject(savedData)
                val pts = obj.getJSONArray("points")
                val loadedCorners = mutableListOf<LatLng>()
                for (i in 0 until pts.length()) {
                    val p = pts.getJSONObject(i)
                    loadedCorners.add(LatLng(p.getDouble("lat"), p.getDouble("lng")))
                }
                if (loadedCorners.isNotEmpty()) {
                    corners.clear()
                    corners.addAll(loadedCorners)
                    if (obj.has("cLat") && obj.has("cLng")) {
                        centroid = LatLng(obj.getDouble("cLat"), obj.getDouble("cLng"))
                    }
                    totalAreaSqFt = obj.optDouble("areaSqFt", 0.0)
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(loadedCorners.first(), 19.5f)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Centering on initial user GPS
    var hasCenteredFirstTime by remember { mutableStateOf(false) }
    LaunchedEffect(userLocation) {
        if (!hasCenteredFirstTime && userLocation != null && corners.isEmpty()) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLocation!!, 20f))
            hasCenteredFirstTime = true
        }
    }

    // Direction arrow: GPS -> Brahmasthan
    val distanceToCentroid = remember(userLocation, centroid) {
        if (userLocation != null && centroid != null) calculateDistanceMeters(userLocation!!, centroid!!) else null
    }
    val targetBearing = remember(userLocation, centroid) {
        if (userLocation != null && centroid != null) calculateBearing(userLocation!!, centroid!!) else 0f
    }
    val relativeArrowAngle = remember(targetBearing, deviceAzimuth) {
        (targetBearing - deviceAzimuth + 360f) % 360f
    }
    val isAtCenter = (distanceToCentroid ?: 999f) <= 1.2f

    val isCalculated = centroid != null
    val lineAlpha = if (isCalculated) 0.5f else 1.0f
    val markerAlpha = if (isCalculated) 0.5f else 1.0f

    fun getPointLabel(index: Int): String {
        return if (index < 26) ('A' + index).toString() else "P${index + 1}"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = mapType,
                isMyLocationEnabled = hasLocationPermission,
                maxZoomPreference = 22f,
                minZoomPreference = 3f
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false,
                compassEnabled = true
            ),
            onMapClick = { latLng ->
                if (!isGuidingMode && centroid == null) {
                    corners.add(latLng)
                }
            }
        ) {
            // 1. Draggable Markers (Works for any A, B, C, D... with radial constraint enforcement)
            corners.forEachIndexed { index, point ->
                val markerState = rememberMarkerState(
                    key = "marker_${index}",
                    position = point
                )

                // Sync marker position when dragged
                LaunchedEffect(markerState.dragState) {
                    if (markerState.dragState == DragState.DRAG) {
                        val draggedPos = markerState.position

                        if (distanceConstraints.containsKey(index) && index > 0) {
                            // Enforce fixed radius around previous marker
                            val prevMarker = corners[index - 1]
                            val currentBearing = calculateBearing(prevMarker, draggedPos)
                            val constrainedDistance = distanceConstraints[index]!!
                            val newConstrainedPos = pointAtDistanceAndBearing(prevMarker, constrainedDistance, currentBearing.toDouble())

                            corners[index] = newConstrainedPos
                            markerState.position = newConstrainedPos
                        } else {
                            corners[index] = draggedPos
                        }

                        if (centroid != null) {
                            val (newCentroid, newArea) = calculateCentroidAndArea(corners)
                            centroid = newCentroid
                            totalAreaSqFt = newArea
                        }
                    }
                }

                Marker(
                    state = markerState,
                    title = "Marker ${getPointLabel(index)} (Hold & drag to move)",
                    draggable = !isGuidingMode,
                    alpha = markerAlpha
                )
            }

            // 2. Lines & Interactive Distance Badges (Clickable to edit distance)
            if (corners.size >= 2) {
                val edgeCount = if (corners.size >= 3) corners.size else 1
                for (i in 0 until edgeCount) {
                    val p1 = corners[i]
                    val p2 = corners[(i + 1) % corners.size]

                    Polyline(
                        points = listOf(p1, p2),
                        color = Color(0xFFFFD600).copy(alpha = lineAlpha),
                        width = 8f
                    )

                    val midLat = (p1.latitude + p2.latitude) / 2.0
                    val midLng = (p1.longitude + p2.longitude) / 2.0
                    val distanceFeet = calculateDistanceMeters(p1, p2) * METERS_TO_FEET
                    val badgeText = "${"%.1f".format(distanceFeet)} ft"

                    Marker(
                        state = rememberMarkerState(key = "edge_${i}_$badgeText", position = LatLng(midLat, midLng)),
                        icon = createDistanceBadge(context, badgeText, isCalculated),
                        anchor = Offset(0.5f, 0.5f),
                        flat = true,
                        alpha = lineAlpha,
                        onClick = {
                            if (!isGuidingMode) {
                                editingEdgeIndex = i
                                editDistanceInput = "%.1f".format(distanceFeet)
                            }
                            true
                        }
                    )
                }
            }

            // 3. Shaded Polygon
            if (corners.size >= 3) {
                Polygon(
                    points = corners.toList(),
                    fillColor = Color(0xFFFFEB3B).copy(alpha = if (isCalculated) 0.18f else 0.35f),
                    strokeColor = Color(0xFFFFD600).copy(alpha = lineAlpha),
                    strokeWidth = 4f
                )
            }

            // 4. Centroid Marker
            centroid?.let { centerPoint ->
                Marker(
                    state = rememberMarkerState(key = "centroid", position = centerPoint),
                    title = "Centroid (Brahmasthan)",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )
            }
        }

        // Top Control Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 12.dp, end = 12.dp),
            shape = RoundedCornerShape(14.dp),
            backgroundColor = Color.White.copy(alpha = 0.95f),
            elevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("PROPERTY PLOTTER", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF1E293B))
                    if (centroid != null) {
                        Text(
                            text = "Area: ${"%,.1f".format(totalAreaSqFt)} sq. ft.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = VastuGreen
                        )
                    } else {
                        Text(
                            text = if (corners.isEmpty()) "Tap corners on map" else "${corners.size} points placed (Tap distance badge to edit)",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (centroid != null) {
                        IconButton(onClick = {
                            savePlotToStorage(context, corners, centroid, totalAreaSqFt)
                            Toast.makeText(context, "Plot & Centroid saved to device", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Save, contentDescription = "Save Plot", tint = VastuGreen)
                        }

                        IconButton(onClick = {
                            exportKml(context, corners, centroid, totalAreaSqFt)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Export KML", tint = Saffron)
                        }
                    }

                    IconButton(onClick = {
                        mapType = if (mapType == MapType.HYBRID) MapType.NORMAL else MapType.HYBRID
                    }) {
                        Icon(
                            if (mapType == MapType.HYBRID) Icons.Default.Layers else Icons.Default.Satellite,
                            contentDescription = "Map Style",
                            tint = Color(0xFF334155)
                        )
                    }

                    if (corners.isNotEmpty()) {
                        IconButton(onClick = {
                            corners.clear()
                            distanceConstraints.clear()
                            centroid = null
                            totalAreaSqFt = 0.0
                            isGuidingMode = false
                            savePlotToStorage(context, emptyList(), null, 0.0)
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color(0xFFDC2626))
                        }
                    }
                }
            }
        }

        // Fixed-Length Next Marker Input Bar (Works for any next marker)
        if (corners.isNotEmpty() && centroid == null) {
            val nextIndex = corners.size
            val prevLabel = getPointLabel(nextIndex - 1)
            val nextLabel = getPointLabel(nextIndex)

            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 86.dp, start = 16.dp, end = 16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                backgroundColor = Color.White,
                elevation = 5.dp
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = nextMarkerDistanceText,
                        onValueChange = { nextMarkerDistanceText = it },
                        label = { Text("Length $prevLabel->$nextLabel (ft)", fontSize = 11.sp) },
                        placeholder = { Text("e.g. 43.5") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).height(52.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val ft = nextMarkerDistanceText.toDoubleOrNull()
                            if (ft != null && ft > 0) {
                                val meters = ft * FEET_TO_METERS
                                val origin = corners.last()
                                val newPos = pointAtDistanceAndBearing(origin, meters, 90.0)
                                distanceConstraints[nextIndex] = meters
                                corners.add(newPos)
                                nextMarkerDistanceText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Saffron, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Add $nextLabel", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Tap-to-Edit Distance Dialog
        if (editingEdgeIndex != null) {
            val edgeIdx = editingEdgeIndex!!
            val p1Idx = edgeIdx
            val p2Idx = (edgeIdx + 1) % corners.size

            AlertDialog(
                onDismissRequest = { editingEdgeIndex = null },
                title = { Text("Edit Length ${getPointLabel(p1Idx)} - ${getPointLabel(p2Idx)}", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Enter new distance in feet. Marker ${getPointLabel(p2Idx)} will adjust along the same line:", fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editDistanceInput,
                            onValueChange = { editDistanceInput = it },
                            label = { Text("Distance (ft)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val newFt = editDistanceInput.toDoubleOrNull()
                            if (newFt != null && newFt > 0) {
                                val newMeters = newFt * FEET_TO_METERS
                                val origin = corners[p1Idx]
                                val currentBearing = calculateBearing(origin, corners[p2Idx])
                                val updatedPos = pointAtDistanceAndBearing(origin, newMeters, currentBearing.toDouble())

                                corners[p2Idx] = updatedPos
                                distanceConstraints[p2Idx] = newMeters

                                if (centroid != null) {
                                    val (newCentroid, newArea) = calculateCentroidAndArea(corners)
                                    centroid = newCentroid
                                    totalAreaSqFt = newArea
                                }
                            }
                            editingEdgeIndex = null
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = VastuGreen, contentColor = Color.White)
                    ) {
                        Text("Update Distance")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingEdgeIndex = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Floating My Location Button
        FloatingActionButton(
            onClick = {
                userLocation?.let { loc ->
                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(loc, 20f))
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            backgroundColor = Color.White,
            contentColor = Color(0xFF1E293B)
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Center Location")
        }

        // Bottom Action Controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 76.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (centroid == null) {
                Button(
                    onClick = {
                        if (corners.size >= 3) {
                            val (center, area) = calculateCentroidAndArea(corners)
                            centroid = center
                            totalAreaSqFt = area
                            savePlotToStorage(context, corners, center, area)
                        }
                    },
                    enabled = corners.size >= 3,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Saffron, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Adjust, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (corners.size >= 3) "CALCULATE CENTER" else "PLACE AT LEAST 3 CORNERS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            } else if (!isGuidingMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            centroid = null
                            totalAreaSqFt = 0.0
                        },
                        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF334155))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ReCalculate", fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    }

                    Button(
                        onClick = { isGuidingMode = true },
                        colors = ButtonDefaults.buttonColors(backgroundColor = VastuGreen, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.2f).height(48.dp)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mark Center", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // Live Guidance Overlay (Points toward the Brahmasthan)
        if (isGuidingMode) {
            val dotColor by animateColorAsState(if (isAtCenter) Color(0xFF16A34A) else Color(0xFFDC2626))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(180.dp)) {
                    val c = Offset(size.width / 2, size.height / 2)
                    drawLine(Color.White.copy(alpha = 0.5f), Offset(c.x, 0f), Offset(c.x, size.height), strokeWidth = 2.dp.toPx())
                    drawLine(Color.White.copy(alpha = 0.5f), Offset(0f, c.y), Offset(size.width, c.y), strokeWidth = 2.dp.toPx())
                }

                Surface(
                    shape = CircleShape,
                    color = dotColor,
                    elevation = 6.dp,
                    modifier = Modifier.size(if (isAtCenter) 44.dp else 28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isAtCenter) {
                            Icon(Icons.Default.Check, contentDescription = "Arrived", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                }

                if (!isAtCenter) {
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .rotate(relativeArrowAngle),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Follow pointer",
                            tint = Color(0xFFFFEB3B),
                            modifier = Modifier.size(50.dp)
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = Color.White,
                    elevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isAtCenter) "EXACT CENTER REACHED!" else "Now follow the pointer to reach the center",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = if (isAtCenter) VastuGreen else Saffron
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isAtCenter) {
                                "You are at the Brahmasthan (${"%,.1f".format(totalAreaSqFt)} sq. ft.)"
                            } else if (distanceToCentroid != null) {
                                val distFeet = distanceToCentroid * METERS_TO_FEET
                                "Distance: ${"%.1f".format(distanceToCentroid)} m (${"%.1f".format(distFeet)} ft)"
                            } else {
                                "Acquiring GPS fix..."
                            },
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { isGuidingMode = false },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Exit Guide Mode")
                        }
                    }
                }
            }
        }
    }
}

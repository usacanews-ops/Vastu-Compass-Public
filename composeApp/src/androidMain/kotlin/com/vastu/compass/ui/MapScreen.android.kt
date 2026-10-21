package com.vastu.compass.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
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
import androidx.activity.result.IntentSenderRequest
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
import com.google.android.gms.common.api.ResolvableApiException
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
import java.util.Locale
import kotlin.math.*

const val METERS_TO_FEET = 3.28084
const val FEET_TO_METERS = 0.3048
const val SQ_METERS_TO_SQ_FEET = 10.7639
const val EARTH_RADIUS_METERS = 6378137.0 // WGS84 exact equatorial radius

fun formatDec(value: Double, digits: Int = 1): String {
    return String.format(Locale.US, "%.${digits}f", value)
}

fun formatArea(value: Double): String {
    return String.format(Locale.US, "%,.1f", value)
}

fun calculateDistanceMeters(p1: LatLng, p2: LatLng): Float {
    val results = FloatArray(1)
    Location.distanceBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude, results)
    return results[0]
}

/**
 * Exact forward azimuth (bearing) between two LatLng points.
 */
fun calculateBearing(from: LatLng, to: LatLng): Float {
    val lat1 = Math.toRadians(from.latitude)
    val lon1 = Math.toRadians(from.longitude)
    val lat2 = Math.toRadians(to.latitude)
    val lon2 = Math.toRadians(to.longitude)
    val dLon = lon2 - lon1

    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    val bearing = Math.toDegrees(atan2(y, x))
    return ((bearing + 360.0) % 360.0).toFloat()
}

/**
 * Projects a point strictly along the geodesic ray from origin at exact distance and bearing.
 */
fun pointAtDistanceAndBearing(origin: LatLng, distanceMeters: Double, bearingDegrees: Double): LatLng {
    val distRatio = distanceMeters / EARTH_RADIUS_METERS
    val brngRad = Math.toRadians(bearingDegrees)
    val lat1Rad = Math.toRadians(origin.latitude)
    val lon1Rad = Math.toRadians(origin.longitude)

    val sinLat1 = sin(lat1Rad)
    val cosLat1 = cos(lat1Rad)
    val sinDist = sin(distRatio)
    val cosDist = cos(distRatio)

    val lat2Rad = asin(sinLat1 * cosDist + cosLat1 * sinDist * cos(brngRad))
    val lon2Rad = lon1Rad + atan2(
        sin(brngRad) * sinDist * cosLat1,
        cosDist - sinLat1 * sin(lat2Rad)
    )

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

/**
 * Large, easily clickable distance badge with 70% visibility (179 alpha) when calculated.
 */
fun createDistanceBadge(context: Context, text: String, isCalculated: Boolean, isSelected: Boolean): BitmapDescriptor {
    val alphaInt = if (isCalculated) 179 else 255 // 70% visible = 179/255

    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isSelected) android.graphics.Color.WHITE else android.graphics.Color.BLACK
        textSize = 44f
        isFakeBoldText = true
        alpha = alphaInt
    }
    val bounds = Rect()
    paint.getTextBounds(text, 0, text.length, bounds)

    val paddingH = 34
    val paddingV = 22
    val width = bounds.width() + paddingH * 2
    val height = bounds.height() + paddingV * 2

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (isSelected) android.graphics.Color.parseColor("#FF8F00") else android.graphics.Color.WHITE
        style = Paint.Style.FILL
        alpha = alphaInt
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#1E293B")
        style = Paint.Style.STROKE
        strokeWidth = 5f
        alpha = alphaInt
    }

    val rect = RectF(3f, 3f, width.toFloat() - 3f, height.toFloat() - 3f)
    canvas.drawRoundRect(rect, 20f, 20f, bgPaint)
    canvas.drawRoundRect(rect, 20f, 20f, strokePaint)

    val textX = (width - bounds.width()) / 2f - bounds.left
    val textY = (height + bounds.height()) / 2f - bounds.bottom
    canvas.drawText(text, textX, textY, paint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

// Persistent Storage Helpers (Uses applicationContext & synchronous commit)
private const val PREFS_NAME = "vastu_map_prefs"
private const val KEY_PLOT_DATA = "saved_plot_json"

data class SavedPlotState(
    val points: List<LatLng>,
    val centroid: LatLng?,
    val areaSqFt: Double
)

fun savePlotToStorage(context: Context, points: List<LatLng>, centroid: LatLng?, areaSqFt: Double) {
    try {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (points.isEmpty()) {
            prefs.edit().remove(KEY_PLOT_DATA).commit()
            return
        }
        val obj = JSONObject()
        val arr = JSONArray()
        points.forEach { pt ->
            if (!pt.latitude.isNaN() && !pt.longitude.isNaN()) {
                val p = JSONObject()
                p.put("lat", pt.latitude)
                p.put("lng", pt.longitude)
                arr.put(p)
            }
        }
        obj.put("points", arr)
        if (centroid != null && !centroid.latitude.isNaN() && !centroid.longitude.isNaN()) {
            obj.put("cLat", centroid.latitude)
            obj.put("cLng", centroid.longitude)
        }
        val safeArea = if (areaSqFt.isNaN() || areaSqFt.isInfinite()) 0.0 else areaSqFt
        obj.put("areaSqFt", safeArea)
        prefs.edit().putString(KEY_PLOT_DATA, obj.toString()).commit()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun loadPlotFromStorage(context: Context): SavedPlotState? {
    return try {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedData = prefs.getString(KEY_PLOT_DATA, null)
        if (savedData.isNullOrBlank()) return null

        val obj = JSONObject(savedData)
        val pts = obj.getJSONArray("points")
        val loadedCorners = mutableListOf<LatLng>()
        for (i in 0 until pts.length()) {
            val p = pts.getJSONObject(i)
            loadedCorners.add(LatLng(p.getDouble("lat"), p.getDouble("lng")))
        }
        if (loadedCorners.isEmpty()) return null

        val loadedCentroid = if (obj.has("cLat") && obj.has("cLng")) {
            LatLng(obj.getDouble("cLat"), obj.getDouble("cLng"))
        } else null

        val loadedArea = obj.optDouble("areaSqFt", 0.0)
        SavedPlotState(loadedCorners, loadedCentroid, loadedArea)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun exportKml(context: Context, points: List<LatLng>, centroid: LatLng?, areaSqFt: Double) {
    if (points.isEmpty()) return

    val kmlCoordinates = StringBuilder()
    points.forEach { pt ->
        kmlCoordinates.append("${formatDec(pt.longitude, 9)},${formatDec(pt.latitude, 9)},0\n")
    }
    kmlCoordinates.append("${formatDec(points[0].longitude, 9)},${formatDec(points[0].latitude, 9)},0\n")

    val centroidPlacemark = if (centroid != null) {
        """
            <Placemark>
              <name>Brahmasthan (Centroid)</name>
              <Point>
                <coordinates>${formatDec(centroid.longitude, 9)},${formatDec(centroid.latitude, 9)},0</coordinates>
              </Point>
            </Placemark>
        """.trimIndent()
    } else ""

    val kmlContent = """
        <?xml version="1.0" encoding="UTF-8"?>
        <kml xmlns="http://www.opengis.net/kml/2.2">
          <Document>
            <name>Vastu Property Layout</name>
            <description>Total Area: ${formatDec(areaSqFt, 2)} sq ft</description>
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
            $centroidPlacemark
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

    // Load saved plot SYNCHRONOUSLY before first frame renders
    val initialSavedPlot = remember { loadPlotFromStorage(context) }

    val corners = remember {
        mutableStateListOf<LatLng>().apply {
            initialSavedPlot?.points?.let { addAll(it) }
        }
    }
    var centroid by remember { mutableStateOf(initialSavedPlot?.centroid) }
    var totalAreaSqFt by remember { mutableStateOf(initialSavedPlot?.areaSqFt ?: 0.0) }

    val cameraPositionState = rememberCameraPositionState {
        val startTarget = initialSavedPlot?.centroid
            ?: initialSavedPlot?.points?.firstOrNull()
            ?: LatLng(28.6139, 77.2090)
        position = CameraPosition.fromLatLngZoom(startTarget, 20f)
    }

    // Undo History Stack
    val undoStack = remember { mutableStateListOf<List<LatLng>>() }
    fun recordStateForUndo() {
        undoStack.add(corners.toList())
        if (undoStack.size > 30) undoStack.removeAt(0)
    }

    var isGuidingMode by remember { mutableStateOf(false) }
    var mapType by remember { mutableStateOf(MapType.HYBRID) }
    var distanceInputText by remember { mutableStateOf("") }
    var editingEdgeIndex by remember { mutableStateOf<Int?>(null) }

    // Check specifically for FINE (Precise) location permission
    var hasFineLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var gpsAccuracyFeet by remember { mutableStateOf<Float?>(null) }

    val resolutionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(context, "High-Precision Location Enabled", Toast.LENGTH_SHORT).show()
        }
    }

    fun promptEnableHighAccuracyLocation() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500L)
            .setGranularity(Granularity.GRANULARITY_FINE)
            .setWaitForAccurateLocation(true)
            .build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)

        val settingsClient = LocationServices.getSettingsClient(context)
        settingsClient.checkLocationSettings(builder.build())
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                        resolutionLauncher.launch(intentSenderRequest)
                    } catch (_: IntentSender.SendIntentException) {}
                }
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        hasFineLocationPermission = fineGranted
        if (fineGranted) {
            promptEnableHighAccuracyLocation()
        } else {
            Toast.makeText(context, "Please enable PRECISE location for inch-level accuracy", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasFineLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            promptEnableHighAccuracyLocation()
        }
    }

    DisposableEffect(hasFineLocationPermission) {
        if (!hasFineLocationPermission) return@DisposableEffect onDispose {}

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 400L)
            .setMinUpdateIntervalMillis(200L)
            .setMinUpdateDistanceMeters(0f)
            .setGranularity(Granularity.GRANULARITY_FINE)
            .setWaitForAccurateLocation(true)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    userLocation = LatLng(loc.latitude, loc.longitude)
                    gpsAccuracyFeet = if (loc.hasAccuracy()) (loc.accuracy * METERS_TO_FEET.toFloat()) else null
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        onDispose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    // Only auto-center on live GPS if there was NO saved plot loaded
    var hasCenteredFirstTime by remember { mutableStateOf(initialSavedPlot != null) }
    LaunchedEffect(userLocation) {
        if (!hasCenteredFirstTime && userLocation != null && corners.isEmpty()) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLocation!!, 20.5f))
            hasCenteredFirstTime = true
        }
    }

    // Exact Geodesic Bearing from User Location -> Calculated Centroid
    val distanceToCentroid = remember(userLocation, centroid) {
        if (userLocation != null && centroid != null) calculateDistanceMeters(userLocation!!, centroid!!) else null
    }
    val targetBearing = remember(userLocation, centroid) {
        if (userLocation != null && centroid != null) calculateBearing(userLocation!!, centroid!!) else 0f
    }
    val relativeArrowAngle = remember(targetBearing, deviceAzimuth) {
        (targetBearing - deviceAzimuth + 360f) % 360f
    }
    val isAtCenter = (distanceToCentroid ?: 999f) <= 1.0f

    val isCalculated = centroid != null
    // 70% visible (30% transparent) when calculated
    val lineAlpha = if (isCalculated) 0.7f else 1.0f
    val markerAlpha = if (isCalculated) 0.7f else 1.0f

    fun getPointLabel(index: Int): String {
        return if (index < 26) ('A' + index).toString() else "P${index + 1}"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = mapType,
                isMyLocationEnabled = hasFineLocationPermission,
                maxZoomPreference = 22f,
                minZoomPreference = 3f
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false,
                compassEnabled = false,
                tiltGesturesEnabled = false
            ),
            onMapClick = { latLng ->
                if (!isGuidingMode && centroid == null) {
                    recordStateForUndo()
                    corners.add(latLng)
                    editingEdgeIndex = null
                    distanceInputText = ""
                    savePlotToStorage(context, corners, centroid, totalAreaSqFt)
                }
            }
        ) {
            // 1. Draggable Corner Markers
            corners.forEachIndexed { index, point ->
                val markerState = rememberMarkerState(key = "marker_${index}", position = point)

                LaunchedEffect(point) {
                    if (markerState.position != point) {
                        markerState.position = point
                    }
                }

                LaunchedEffect(markerState) {
                    snapshotFlow { markerState.position }.collect { livePos ->
                        if (index < corners.size && corners[index] != livePos) {
                            corners[index] = livePos
                            if (centroid != null) {
                                val (newCentroid, newArea) = calculateCentroidAndArea(corners)
                                centroid = newCentroid
                                totalAreaSqFt = newArea
                            }
                            savePlotToStorage(context, corners, centroid, totalAreaSqFt)
                        }
                    }
                }

                Marker(
                    state = markerState,
                    title = "Corner ${getPointLabel(index)} (Hold & drag)",
                    draggable = !isGuidingMode,
                    alpha = markerAlpha
                )
            }

            // 2. Connecting Lines & Large Clickable Length Badges
            if (corners.size >= 2) {
                val edgeCount = if (corners.size >= 3) corners.size else 1
                for (i in 0 until edgeCount) {
                    val p1 = corners[i]
                    val p2 = corners[(i + 1) % corners.size]

                    Polyline(
                        points = listOf(p1, p2),
                        color = Color(0xFFFFD600).copy(alpha = lineAlpha),
                        width = 9f,
                        clickable = true,
                        onClick = {
                            if (!isGuidingMode && centroid == null) {
                                val dFeet = calculateDistanceMeters(p1, p2) * METERS_TO_FEET
                                editingEdgeIndex = i
                                distanceInputText = formatDec(dFeet, 1)
                            }
                        }
                    )

                    val midLat = (p1.latitude + p2.latitude) / 2.0
                    val midLng = (p1.longitude + p2.longitude) / 2.0
                    val distanceFeet = calculateDistanceMeters(p1, p2) * METERS_TO_FEET
                    val badgeText = "${formatDec(distanceFeet, 1)} ft"
                    val isThisEdgeSelected = editingEdgeIndex == i

                    val badgeState = rememberMarkerState(
                        key = "edge_${i}_${badgeText}_${isCalculated}_$isThisEdgeSelected",
                        position = LatLng(midLat, midLng)
                    )
                    LaunchedEffect(midLat, midLng) {
                        badgeState.position = LatLng(midLat, midLng)
                    }

                    Marker(
                        state = badgeState,
                        icon = createDistanceBadge(context, badgeText, isCalculated, isThisEdgeSelected),
                        anchor = Offset(0.5f, 0.5f),
                        flat = true,
                        zIndex = 2f,
                        onClick = {
                            if (!isGuidingMode && centroid == null) {
                                editingEdgeIndex = i
                                distanceInputText = formatDec(distanceFeet, 1)
                            }
                            true
                        }
                    )
                }
            }

            // 3. Enclosed Property Polygon (70% visible border when calculated)
            if (corners.size >= 3) {
                Polygon(
                    points = corners.toList(),
                    fillColor = Color(0xFFFFEB3B).copy(alpha = if (isCalculated) 0.25f else 0.38f),
                    strokeColor = Color(0xFFFFD600).copy(alpha = lineAlpha),
                    strokeWidth = 5f
                )
            }

            // 4. Calculated Centroid (Brahmasthan) Marker
            centroid?.let { centerPoint ->
                val centroidState = rememberMarkerState(key = "centroid_${centerPoint.latitude}_${centerPoint.longitude}", position = centerPoint)
                Marker(
                    state = centroidState,
                    title = "Brahmasthan (Center)",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                    zIndex = 3f
                )
            }
        }

        // Sub-Inch Precision Aiming Crosshair in Center of Map (While Plotting)
        if (!isGuidingMode && centroid == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(36.dp)) {
                    val c = Offset(size.width / 2, size.height / 2)
                    drawCircle(Color.Black.copy(alpha = 0.5f), radius = 4.dp.toPx(), center = c)
                    drawCircle(Color(0xFFFFEB3B), radius = 2.5.dp.toPx(), center = c)
                    drawLine(Color(0xFFFFEB3B), Offset(c.x - 14.dp.toPx(), c.y), Offset(c.x + 14.dp.toPx(), c.y), strokeWidth = 1.5.dp.toPx())
                    drawLine(Color(0xFFFFEB3B), Offset(c.x, c.y - 14.dp.toPx()), Offset(c.x, c.y + 14.dp.toPx()), strokeWidth = 1.5.dp.toPx())
                }
            }
        }

        // Top-Left Bar: 70% Width, Stuck to Top-Left (`Alignment.TopStart`)
        Card(
            modifier = Modifier
                .fillMaxWidth(0.70f)
                .align(Alignment.TopStart)
                .padding(top = 14.dp, start = 10.dp),
            shape = RoundedCornerShape(14.dp),
            backgroundColor = Color.White.copy(alpha = 0.95f),
            elevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("MARK MAP", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFF1E293B))
                    if (centroid != null) {
                        Text(
                            text = "${formatArea(totalAreaSqFt)} sq. ft.",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = VastuGreen
                        )
                    } else {
                        val accText = gpsAccuracyFeet?.let { "±${formatDec(it.toDouble(), 0)}ft" } ?: "GPS..."
                        Text(
                            text = if (corners.isEmpty()) "Tap map ($accText)" else "${corners.size} pts ($accText)",
                            fontSize = 10.5.sp,
                            color = Color(0xFF475569)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // One-Step Back (Undo) Icon
                    if (corners.isNotEmpty() && !isGuidingMode) {
                        IconButton(
                            onClick = {
                                if (centroid != null) {
                                    centroid = null
                                    totalAreaSqFt = 0.0
                                    savePlotToStorage(context, corners, null, 0.0)
                                } else if (undoStack.isNotEmpty()) {
                                    val prev = undoStack.removeAt(undoStack.lastIndex)
                                    corners.clear()
                                    corners.addAll(prev)
                                    editingEdgeIndex = null
                                    distanceInputText = ""
                                    savePlotToStorage(context, corners, centroid, totalAreaSqFt)
                                } else {
                                    corners.removeAt(corners.lastIndex)
                                    savePlotToStorage(context, corners, centroid, totalAreaSqFt)
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Undo, contentDescription = "Step Back", tint = Color(0xFF1E293B), modifier = Modifier.size(19.dp))
                        }
                    }

                    if (centroid != null) {
                        IconButton(
                            onClick = {
                                savePlotToStorage(context, corners, centroid, totalAreaSqFt)
                                Toast.makeText(context, "Plot saved to device", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Save", tint = VastuGreen, modifier = Modifier.size(19.dp))
                        }

                        IconButton(
                            onClick = { exportKml(context, corners, centroid, totalAreaSqFt) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Export", tint = Saffron, modifier = Modifier.size(19.dp))
                        }
                    }

                    IconButton(
                        onClick = { mapType = if (mapType == MapType.HYBRID) MapType.NORMAL else MapType.HYBRID },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (mapType == MapType.HYBRID) Icons.Default.Layers else Icons.Default.Satellite,
                            contentDescription = "Map Style",
                            tint = Color(0xFF334155),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    if (corners.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                recordStateForUndo()
                                corners.clear()
                                centroid = null
                                totalAreaSqFt = 0.0
                                isGuidingMode = false
                                editingEdgeIndex = null
                                distanceInputText = ""
                                savePlotToStorage(context, emptyList(), null, 0.0)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset All", tint = Color(0xFFDC2626), modifier = Modifier.size(19.dp))
                        }
                    }
                }
            }
        }

        // Box 1: Length Input & Collinear Distance Updater
        if (corners.isNotEmpty() && centroid == null) {
            val isEditing = editingEdgeIndex != null
            val nextIndex = corners.size

            val boxLabel = if (isEditing) {
                val p1 = getPointLabel(editingEdgeIndex!!)
                val p2 = getPointLabel((editingEdgeIndex!! + 1) % corners.size)
                "Modify Line $p1 ->$p2 (ft)"
            } else {
                val prev = getPointLabel(nextIndex - 1)
                val nxt = getPointLabel(nextIndex)
                "Length $prev ->$nxt (ft)"
            }

            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 74.dp, start = 10.dp, end = 10.dp)
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
                        value = distanceInputText,
                        onValueChange = { distanceInputText = it },
                        label = { Text(boxLabel, fontSize = 11.sp) },
                        placeholder = { Text("e.g. 40.2") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f).height(54.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    if (isEditing) {
                        Button(
                            onClick = {
                                val ft = distanceInputText.toDoubleOrNull()
                                if (ft != null && ft > 0 && editingEdgeIndex != null) {
                                    val edge = editingEdgeIndex!!
                                    val p1 = corners[edge]
                                    val p2Idx = (edge + 1) % corners.size
                                    val p2 = corners[p2Idx]

                                    recordStateForUndo()
                                    val exactLineBearing = calculateBearing(p1, p2)
                                    val updatedPos = pointAtDistanceAndBearing(
                                        origin = p1,
                                        distanceMeters = ft * FEET_TO_METERS,
                                        bearingDegrees = exactLineBearing.toDouble()
                                    )
                                    corners[p2Idx] = updatedPos
                                    savePlotToStorage(context, corners, centroid, totalAreaSqFt)
                                }
                                editingEdgeIndex = null
                                distanceInputText = ""
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = VastuGreen, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Update", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        IconButton(onClick = {
                            editingEdgeIndex = null
                            distanceInputText = ""
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.Gray)
                        }
                    } else {
                        Button(
                            onClick = {
                                val ft = distanceInputText.toDoubleOrNull()
                                if (ft != null && ft > 0) {
                                    recordStateForUndo()
                                    val meters = ft * FEET_TO_METERS
                                    val origin = corners.last()
                                    val targetCenter = cameraPositionState.position.target
                                    val distToTarget = calculateDistanceMeters(origin, targetCenter)
                                    val spawnAngle = if (distToTarget > 0.5f) {
                                        calculateBearing(origin, targetCenter).toDouble()
                                    } else {
                                        deviceAzimuth.toDouble()
                                    }
                                    val newPos = pointAtDistanceAndBearing(origin, meters, spawnAngle)
                                    corners.add(newPos)
                                    distanceInputText = ""
                                    savePlotToStorage(context, corners, centroid, totalAreaSqFt)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Saffron, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Add ${getPointLabel(nextIndex)}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Right-Side Floating Buttons
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (!isGuidingMode && centroid == null) {
                ExtendedFloatingActionButton(
                    text = { Text("Add Crosshair", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        recordStateForUndo()
                        corners.add(cameraPositionState.position.target)
                        editingEdgeIndex = null
                        distanceInputText = ""
                        savePlotToStorage(context, corners, centroid, totalAreaSqFt)
                    },
                    backgroundColor = VastuGreen,
                    contentColor = Color.White,
                    modifier = Modifier.height(40.dp)
                )

                ExtendedFloatingActionButton(
                    text = { Text("Add My GPS", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.AddLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    onClick = {
                        if (!hasFineLocationPermission) {
                            permissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                        } else {
                            promptEnableHighAccuracyLocation()
                            userLocation?.let { loc ->
                                recordStateForUndo()
                                corners.add(loc)
                                editingEdgeIndex = null
                                distanceInputText = ""
                                savePlotToStorage(context, corners, centroid, totalAreaSqFt)
                            } ?: Toast.makeText(context, "Waiting for Precise GPS...", Toast.LENGTH_SHORT).show()
                        }
                    },
                    backgroundColor = Saffron,
                    contentColor = Color.White,
                    modifier = Modifier.height(40.dp)
                )
            }

            FloatingActionButton(
                onClick = {
                    if (!hasFineLocationPermission) {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    } else {
                        promptEnableHighAccuracyLocation()
                        userLocation?.let { loc ->
                            coroutineScope.launch {
                                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(loc, 21f))
                            }
                        }
                    }
                },
                backgroundColor = Color.White,
                contentColor = Color(0xFF1E293B),
                modifier = Modifier.size(46.dp).align(Alignment.End)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "My Location", modifier = Modifier.size(22.dp))
            }
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
                            editingEdgeIndex = null
                            distanceInputText = ""
                            savePlotToStorage(context, corners, center, area)
                            Toast.makeText(context, "Center calculated & saved", Toast.LENGTH_SHORT).show()
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
                            savePlotToStorage(context, corners, null, 0.0)
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

        // Live Guidance Overlay: Points strictly from Current Location -> Marked Center
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
                            modifier = Modifier.size(56.dp)
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
                                "You are at the Brahmasthan (${formatArea(totalAreaSqFt)} sq. ft.)"
                            } else if (distanceToCentroid != null) {
                                val distFeet = distanceToCentroid * METERS_TO_FEET
                                "Distance: ${formatDec(distFeet, 1)} ft (${formatDec(distanceToCentroid.toDouble(), 2)} m)"
                            } else {
                                "Acquiring Precise GPS..."
                            },
                            fontSize = 14.sp,
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

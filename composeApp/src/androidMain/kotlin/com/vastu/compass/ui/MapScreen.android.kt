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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.vastu.compass.data.VastuRepository
import com.vastu.compass.model.VastuZone
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
 * Exact forward azimuth (bearing) between two LatLng points on the WGS84 ellipsoid.
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

fun createDistanceBadge(context: Context, text: String, isCalculated: Boolean, isSelected: Boolean): BitmapDescriptor {
    val alphaInt = if (isCalculated) 179 else 255

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

fun createChakraLabelBadge(context: Context, text: String): BitmapDescriptor {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#0F172A")
        textSize = 28f
        isFakeBoldText = true
    }
    val bounds = Rect()
    paint.getTextBounds(text, 0, text.length, bounds)

    val paddingH = 14
    val paddingV = 8
    val width = bounds.width() + paddingH * 2
    val height = bounds.height() + paddingV * 2

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#F8FAFC")
        style = Paint.Style.FILL
        alpha = 235
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#CBD5E1")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    val rect = RectF(1f, 1f, width.toFloat() - 1f, height.toFloat() - 1f)
    canvas.drawRoundRect(rect, 10f, 10f, bgPaint)
    canvas.drawRoundRect(rect, 10f, 10f, strokePaint)

    val textX = (width - bounds.width()) / 2f - bounds.left
    val textY = (height + bounds.height()) / 2f - bounds.bottom
    canvas.drawText(text, textX, textY, paint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

/**
 * Solid circular dot termination badge (3x line width).
 */
fun createCircleDotBadge(context: Context, colorHex: String, radiusDp: Float): BitmapDescriptor {
    val px = (radiusDp * context.resources.displayMetrics.density).toInt().coerceAtLeast(6)
    val size = px * 2
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)

    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor(colorHex)
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    canvas.drawCircle(px.toFloat(), px.toFloat(), (px - 2).toFloat(), fillPaint)
    canvas.drawCircle(px.toFloat(), px.toFloat(), (px - 2).toFloat(), strokePaint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

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

// 32 Vastu Pada Sector Angles
private val PADA_32_LIST = listOf(
    Pair("N1", 348.75f), Pair("N2", 360.0f), Pair("N3", 11.25f), Pair("N4", 22.5f),
    Pair("N5", 33.75f), Pair("N6", 45.0f), Pair("N7", 56.25f), Pair("N8", 67.5f),
    Pair("E1", 78.75f), Pair("E2", 90.0f), Pair("E3", 101.25f), Pair("E4", 112.5f),
    Pair("E5", 123.75f), Pair("E6", 135.0f), Pair("E7", 146.25f), Pair("E8", 157.5f),
    Pair("S1", 168.75f), Pair("S2", 180.0f), Pair("S3", 191.25f), Pair("S4", 202.5f),
    Pair("S5", 213.75f), Pair("S6", 225.0f), Pair("S7", 236.25f), Pair("S8", 247.5f),
    Pair("W1", 258.75f), Pair("W2", 270.0f), Pair("W3", 281.25f), Pair("W4", 292.5f),
    Pair("W5", 303.75f), Pair("W6", 315.0f), Pair("W7", 326.25f), Pair("W8", 337.5f)
)

fun getVastuColorForPada(padaId: String): Color {
    return when {
        padaId.startsWith("N") -> Color(0xFF64B5F6) // Water / Blue
        padaId.startsWith("E") -> Color(0xFF81C784) // Wood / Green
        padaId.startsWith("S") -> Color(0xFFE57373) // Fire / Red
        padaId.startsWith("W") -> Color(0xFFFFF176) // Space / Yellow
        else -> Color(0xFFBDBDBD)
    }
}

@SuppressLint("MissingPermission")
@Composable
actual fun MapScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val compassSensor = remember { getCompassSensor() }
    val deviceAzimuth by compassSensor.azimuthDegrees.collectAsState(initial = 0f)

    val vastuPkg by VastuRepository.dataFlow.collectAsState()

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

    val undoStack = remember { mutableStateListOf<List<LatLng>>() }
    fun recordStateForUndo() {
        undoStack.add(corners.toList())
        if (undoStack.size > 30) undoStack.removeAt(0)
    }

    var isGuidingMode by remember { mutableStateOf(false) }
    var isVastuChakraMode by remember { mutableStateOf(false) }
    var vastuRotationOffset by remember { mutableStateOf(0f) }
    var mapType by remember { mutableStateOf(MapType.HYBRID) }
    var distanceInputText by remember { mutableStateOf("") }
    var editingEdgeIndex by remember { mutableStateOf<Int?>(null) }

    var selectedVastuZone by remember { mutableStateOf<VastuZone?>(null) }

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

    var hasCenteredFirstTime by remember { mutableStateOf(initialSavedPlot != null) }
    LaunchedEffect(userLocation) {
        if (!hasCenteredFirstTime && userLocation != null && corners.isEmpty()) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLocation!!, 20.5f))
            hasCenteredFirstTime = true
        }
    }

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
    val lineAlpha = if (isCalculated && !isVastuChakraMode) 0.7f else 1.0f
    val markerAlpha = if (isCalculated && !isVastuChakraMode) 0.7f else 1.0f

    fun getPointLabel(index: Int): String {
        return if (index < 26) ('A' + index).toString() else "P${index + 1}"
    }

    val chakraRadiusMeters = remember(centroid, corners) {
        if (centroid != null && corners.isNotEmpty()) {
            corners.maxOf { calculateDistanceMeters(centroid!!, it).toDouble() }.coerceAtLeast(5.0)
        } else {
            15.0
        }
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
                if (!isGuidingMode && !isVastuChakraMode && centroid == null) {
                    recordStateForUndo()
                    corners.add(latLng)
                    editingEdgeIndex = null
                    distanceInputText = ""
                    savePlotToStorage(context, corners, centroid, totalAreaSqFt)
                }
            }
        ) {
            // 1. Draggable Corner Markers (Red points at 100% visibility when Chakra is drawn)
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
                    title = "Corner ${getPointLabel(index)}",
                    draggable = !isGuidingMode && !isVastuChakraMode,
                    alpha = if (isVastuChakraMode) 1.0f else markerAlpha
                )
            }

            // 2. Connecting Lines & Distance Badges (Hidden when VastuChakra is drawn)
            if (corners.size >= 2 && !isVastuChakraMode) {
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

            // 3. Property Polygon
            if (corners.size >= 3) {
                Polygon(
                    points = corners.toList(),
                    fillColor = Color(0xFFFFEB3B).copy(alpha = if (isCalculated && !isVastuChakraMode) 0.25f else 0.38f),
                    strokeColor = Color(0xFFFFD600).copy(alpha = lineAlpha),
                    strokeWidth = 5f
                )
            }

            // 4. Centroid Marker (Hidden in VastuChakra mode in favor of the blue dot)
            centroid?.let { centerPoint ->
                if (!isVastuChakraMode) {
                    val centroidState = rememberMarkerState(key = "centroid_${centerPoint.latitude}_${centerPoint.longitude}", position = centerPoint)
                    Marker(
                        state = centroidState,
                        title = "Brahmasthan (Center)",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                        zIndex = 3f
                    )
                }
            }

            // 5. Vastu Chakra: 32 Sectors (30% Opacity), Calibrated Dynamic Labels, and Slim Alignment Lines
            if (isVastuChakraMode && centroid != null) {
                val center = centroid!!

                // A. 32 Vastu Wedges
                PADA_32_LIST.forEach { (padaName, startAngle) ->
                    val adjustedStart = (startAngle + vastuRotationOffset + 360f) % 360f
                    val sweepAngle = 11.25f
                    val endAngle = (adjustedStart + sweepAngle) % 360f
                    val midAngle = (adjustedStart + (sweepAngle / 2f)) % 360f

                    val wedgePoints = mutableListOf<LatLng>().apply {
                        add(center)
                        for (step in 0..15) {
                            val ang = (adjustedStart + (sweepAngle * step / 15f)) % 360f
                            add(pointAtDistanceAndBearing(center, chakraRadiusMeters, ang.toDouble()))
                        }
                        add(center)
                    }

                    val zoneColor = getVastuColorForPada(padaName)

                    Polygon(
                        points = wedgePoints,
                        fillColor = zoneColor.copy(alpha = 0.30f), // Exactly 30% fill opacity
                        strokeColor = Color(0xFF1E293B).copy(alpha = 0.5f),
                        strokeWidth = 1.5f,
                        clickable = true,
                        onClick = {
                            val baseZoneKey = when {
                                padaName.startsWith("N") -> "N"
                                padaName.startsWith("E") -> "E"
                                padaName.startsWith("S") -> "S"
                                padaName.startsWith("W") -> "W"
                                else -> "N"
                            }
                            selectedVastuZone = vastuPkg?.zones?.firstOrNull { it.id.equals(baseZoneKey, ignoreCase = true) }
                                ?: VastuZone(
                                    id = baseZoneKey,
                                    name = baseZoneKey,
                                    element = "Vastu",
                                    startDeg = adjustedStart,
                                    endDeg = endAngle,
                                    attributes = "Vastu Chakra sector $padaName",
                                    pada = padaName,
                                    effects = "Harmonizes energy across $padaName pada.",
                                    remedies = "Maintain cleanliness and appropriate color balance."
                                )
                        }
                    )

                    // Direction labels dynamically rotate around the perimeter in lockstep with slider calibration
                    val labelPos = pointAtDistanceAndBearing(center, chakraRadiusMeters * 1.12, midAngle.toDouble())
                    Marker(
                        state = rememberMarkerState(
                            key = "lbl_${padaName}_${formatDec(adjustedStart.toDouble(), 2)}",
                            position = labelPos
                        ),
                        icon = createChakraLabelBadge(context, padaName),
                        anchor = Offset(0.5f, 0.5f),
                        flat = true,
                        zIndex = 4f
                    )
                }

                // B. LINE 1: RED SLIM LINE (Magnetic Compass North Pole)
                // In geographical coordinates on Google Maps, Magnetic North is at (360 - deviceAzimuth + cameraBearing).
                // This counter-rotates with the device so this line always points to the physical Magnetic North Pole.
                val cameraHeading = cameraPositionState.position.bearing
                val trueCompassBearing = (360.0 - deviceAzimuth + cameraHeading + 360.0) % 360.0
                val compassNorthRadius = chakraRadiusMeters * 1.20
                val compassNorthEnd = pointAtDistanceAndBearing(center, compassNorthRadius, trueCompassBearing)

                Polyline(
                    points = listOf(center, compassNorthEnd),
                    color = Color(0xFFDC2626), // Solid Red
                    width = 3f, // Slim 1px line
                    zIndex = 6f
                )

                // C. LINE 2: BLUE SLIM LINE (Google Maps True North with Calibration Offset)
                // True North on Google Maps is 0.0° geographic bearing. When calibrated, it rotates by vastuRotationOffset.
                val vastuNorthAngle = (0.0 + vastuRotationOffset + 360.0) % 360.0
                val vastuNorthRadius = chakraRadiusMeters * 0.90
                val vastuNorthEnd = pointAtDistanceAndBearing(center, vastuNorthRadius, vastuNorthAngle)

                Polyline(
                    points = listOf(center, vastuNorthEnd),
                    color = Color(0xFF1E3A8A), // Solid Deep Blue
                    width = 3f, // Slim 1px line
                    zIndex = 7f
                )

                // Blue line termination dot (3x the line width)
                Marker(
                    state = rememberMarkerState(
                        key = "blue_dot_${formatDec(vastuNorthAngle, 2)}",
                        position = vastuNorthEnd
                    ),
                    icon = createCircleDotBadge(context, "#1E3A8A", radiusDp = 5f),
                    anchor = Offset(0.5f, 0.5f),
                    flat = true,
                    zIndex = 8f,
                    title = "Chakra North (0°)"
                )

                // D. Center Blue Dot (Terminating Center for both lines, 100% visible)
                Marker(
                    state = rememberMarkerState(key = "center_blue_dot", position = center),
                    icon = createCircleDotBadge(context, "#1E3A8A", radiusDp = 7f),
                    anchor = Offset(0.5f, 0.5f),
                    flat = true,
                    zIndex = 9f,
                    title = "Brahmasthan (Center)"
                )
            }
        }

        // Sub-Inch Precision Aiming Crosshair in Center of Map
        if (!isGuidingMode && !isVastuChakraMode && centroid == null) {
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
                    Text(if (isVastuChakraMode) "VASTU CHAKRA" else "MARK MAP", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFF1E293B))
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
                    if (corners.isNotEmpty() && !isGuidingMode && !isVastuChakraMode) {
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
                                isVastuChakraMode = false
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

        // Vastu Chakra Rotation Calibration Card with dedicated Reset button
        if (isVastuChakraMode && centroid != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 74.dp, start = 10.dp, end = 10.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                backgroundColor = Color.White,
                elevation = 5.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("North Calibration Offset: ", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = Color(0xFF1E293B))
                            Text("${formatDec(vastuRotationOffset.toDouble(), 1)}°", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Saffron)
                        }

                        // Reset Offset Button
                        IconButton(
                            onClick = { vastuRotationOffset = 0f },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = "Reset Offset", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                        }
                    }
                    Slider(
                        value = vastuRotationOffset,
                        onValueChange = { vastuRotationOffset = it },
                        valueRange = -45f..45f,
                        steps = 89,
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }

        // Box 1: Length Input & Collinear Distance Updater (Hidden when VastuChakra is active)
        if (corners.isNotEmpty() && centroid == null) {
            val isEditing = editingEdgeIndex != null
            val nextIndex = corners.size

            val boxLabel = if (isEditing) {
                val p1 = getPointLabel(editingEdgeIndex!!)
                val p2 = getPointLabel((editingEdgeIndex!! + 1) % corners.size)
                "Modify Line $p1 -> $p2 (ft)"
            } else {
                val prev = getPointLabel(nextIndex - 1)
                val nxt = getPointLabel(nextIndex)
                "Length $prev -> $nxt (ft)"
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

        // Right-Side Floating Buttons (including Set Map to North button)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.End
        ) {
            // "Set Map to North" Button: Rotates Google Map to True North (0° bearing)
            FloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        val currentTarget = cameraPositionState.position.target
                        val currentZoom = cameraPositionState.position.zoom
                        cameraPositionState.animate(
                            CameraUpdateFactory.newCameraPosition(
                                CameraPosition.builder()
                                    .target(currentTarget)
                                    .zoom(currentZoom)
                                    .bearing(0f) // Exact True North Alignment
                                    .tilt(0f)
                                    .build()
                            )
                        )
                        Toast.makeText(context, "Map aligned to True North (0°)", Toast.LENGTH_SHORT).show()
                    }
                },
                backgroundColor = Color.White,
                contentColor = Color(0xFF1E3A8A),
                modifier = Modifier.size(46.dp)
            ) {
                Icon(Icons.Default.Explore, contentDescription = "Set Map to North", modifier = Modifier.size(24.dp))
            }

            if (!isGuidingMode && !isVastuChakraMode && centroid == null) {
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
                modifier = Modifier.size(46.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "My Location", modifier = Modifier.size(22.dp))
            }
        }

        // Bottom Action Controls: ReCalculate, Go to Center, VastuChakra
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            centroid = null
                            isVastuChakraMode = false
                            totalAreaSqFt = 0.0
                            savePlotToStorage(context, corners, null, 0.0)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("ReCalculate", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    }

                    Button(
                        onClick = {
                            isVastuChakraMode = false
                            isGuidingMode = true
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = VastuGreen, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("Go to Center", fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    Button(
                        onClick = { isVastuChakraMode = !isVastuChakraMode },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (isVastuChakraMode) Color(0xFF1E3A8A) else Saffron,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.2f).height(44.dp)
                    ) {
                        Icon(Icons.Default.DonutLarge, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isVastuChakraMode) "Hide Chakra" else "VastuChakra", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        // Live Guidance Overlay
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

        // Rich Vastu Zone Details Modal Dialog
        if (selectedVastuZone != null) {
            val zone = selectedVastuZone!!
            AlertDialog(
                onDismissRequest = { selectedVastuZone = null },
                title = {
                    Column {
                        Text(text = "${zone.name} (${zone.id})", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF1E293B))
                        if (zone.sanskritName.isNotBlank()) {
                            Text(text = "Sanskrit: ${zone.sanskritName}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Saffron)
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Divider(color = Color(0xFFE2E8F0))

                        InfoRow("Element:", zone.element)
                        InfoRow("Degrees:", "${zone.startDeg}° - ${zone.endDeg}°")
                        InfoRow("Pada / Entrance:", zone.pada)
                        InfoRow("Shapes:", zone.shape)
                        InfoRow("Good Colors:", zone.goodColor)
                        InfoRow("Bad Colors:", zone.badColor)
                        InfoRow("Good Metals:", zone.goodMetal)
                        InfoRow("Bad Metals:", zone.badMetal)

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Attributes & Effects:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF334155))
                        Text(zone.effects.ifBlank { zone.attributes }, fontSize = 12.5.sp, color = Color(0xFF64748B))

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Strictly Avoid:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFDC2626))
                        Text(zone.strictlyAvoid.ifBlank { "None specified" }, fontSize = 12.5.sp, color = Color(0xFF991B1B))

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Recommended Remedies:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = VastuGreen)
                        Text(zone.remedies.ifBlank { "Keep clean and well-balanced." }, fontSize = 12.5.sp, color = Color(0xFF065F46))

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Good Businesses:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF334155))
                        Text(zone.goodBusiness.ifBlank { "General commerce" }, fontSize = 12.5.sp, color = Color(0xFF64748B))
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { selectedVastuZone = null },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Saffron, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF475569), modifier = Modifier.weight(0.4f))
        Text(text = value, fontSize = 12.sp, color = Color(0xFF1E293B), modifier = Modifier.weight(0.6f))
    }
}

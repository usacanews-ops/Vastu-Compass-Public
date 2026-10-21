package com.vastu.compass.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.location.Location
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
import kotlin.math.*

// Converts meters to feet
const val METERS_TO_FEET = 3.28084
// Converts sq. meters to sq. feet
const val SQ_METERS_TO_SQ_FEET = 10.7639

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
 * High-accuracy planar projection centroid and polygon area calculation.
 */
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
 * Creates a clean text badge bitmap for displaying line lengths in feet.
 */
fun createDistanceBadge(context: Context, text: String): BitmapDescriptor {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        textSize = 34f
        isFakeBoldText = true
    }
    val bounds = Rect()
    paint.getTextBounds(text, 0, text.length, bounds)

    val paddingH = 20
    val paddingV = 12
    val width = bounds.width() + paddingH * 2
    val height = bounds.height() + paddingV * 2

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.DKGRAY
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
    canvas.drawRoundRect(rect, 14f, 14f, bgPaint)
    canvas.drawRoundRect(rect, 14f, 14f, strokePaint)

    val textX = (width - bounds.width()) / 2f - bounds.left
    val textY = (height + bounds.height()) / 2f - bounds.bottom
    canvas.drawText(text, textX, textY, paint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
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

    // Continuous Real-Time GPS Tracking
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

    // Auto-focus camera on user location when first detected
    var hasCenteredFirstTime by remember { mutableStateOf(false) }
    LaunchedEffect(userLocation) {
        if (!hasCenteredFirstTime && userLocation != null) {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(userLocation!!, 20f))
            hasCenteredFirstTime = true
        }
    }

    val corners = remember { mutableStateListOf<LatLng>() }
    var centroid by remember { mutableStateOf<LatLng?>(null) }
    var totalAreaSqFt by remember { mutableStateOf(0.0) }
    var isGuidingMode by remember { mutableStateOf(false) }
    var mapType by remember { mutableStateOf(MapType.HYBRID) }

    // Live distance & target direction
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

    // Marker labels: A, B, C, D, ...
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
                myLocationButtonEnabled = false, // Handled via custom floating action button
                zoomControlsEnabled = false,
                compassEnabled = true
            ),
            onMapClick = { latLng ->
                if (!isGuidingMode && centroid == null) {
                    corners.add(latLng)
                }
            }
        ) {
            // 1. Draggable Corner Markers
            corners.forEachIndexed { index, point ->
                val markerState = rememberMarkerState(key = "marker_${index}_${point.latitude}_${point.longitude}", position = point)
                
                LaunchedEffect(markerState.position) {
                    if (markerState.position != corners[index]) {
                        corners[index] = markerState.position
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
                    draggable = !isGuidingMode
                )
            }

            // 2. Connecting Lines and Distance Badges in Feet
            if (corners.size >= 2) {
                val edgeCount = if (corners.size >= 3) corners.size else 1
                for (i in 0 until edgeCount) {
                    val p1 = corners[i]
                    val p2 = corners[(i + 1) % corners.size]

                    // Connecting Line
                    Polyline(
                        points = listOf(p1, p2),
                        color = Color(0xFFFFD600),
                        width = 8f
                    )

                    // Line midpoint badge showing feet
                    val midLat = (p1.latitude + p2.latitude) / 2.0
                    val midLng = (p1.longitude + p2.longitude) / 2.0
                    val distanceFeet = calculateDistanceMeters(p1, p2) * METERS_TO_FEET
                    val badgeText = "${"%.1f".format(distanceFeet)} ft"

                    Marker(
                        state = rememberMarkerState(key = "edge_${i}_$badgeText", position = LatLng(midLat, midLng)),
                        icon = createDistanceBadge(context, badgeText),
                        anchor = Offset(0.5f, 0.5f),
                        flat = true
                    )
                }
            }

            // 3. Shaded Yellow Property Polygon
            if (corners.size >= 3) {
                Polygon(
                    points = corners.toList(),
                    fillColor = Color(0x55FFEB3B),
                    strokeColor = Color(0xFFFFD600),
                    strokeWidth = 4f
                )
            }

            // 4. Centroid (Brahmasthan) Marker
            centroid?.let { centerPoint ->
                Marker(
                    state = rememberMarkerState(key = "centroid", position = centerPoint),
                    title = "Centroid (Brahmasthan)",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )
            }
        }

        // Top Control Bar: Status, Area display, Reset
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
                            text = if (corners.isEmpty()) "Tap corners A, B, C on map" else "${corners.size} markers placed (Drag to adjust)",
                            fontSize = 11.5.sp,
                            color = Color.Gray
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
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
                            centroid = null
                            totalAreaSqFt = 0.0
                            isGuidingMode = false
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Restart Marking", tint = Color(0xFFDC2626))
                        }
                    }
                }
            }
        }

        // Floating "My Location" Button (Right Side)
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
            Icon(Icons.Default.MyLocation, contentDescription = "Center My Location")
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
                        text = if (corners.size >= 3) "FIND CENTER (CENTROID)" else "PLACE AT LEAST 3 CORNERS",
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

        // Live Guidance Overlay
        if (isGuidingMode) {
            val dotColor by animateColorAsState(if (isAtCenter) Color(0xFF16A34A) else Color(0xFFDC2626))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                // Crosshairs
                Canvas(modifier = Modifier.size(180.dp)) {
                    val c = Offset(size.width / 2, size.height / 2)
                    drawLine(Color.White.copy(alpha = 0.5f), Offset(c.x, 0f), Offset(c.x, size.height), strokeWidth = 2.dp.toPx())
                    drawLine(Color.White.copy(alpha = 0.5f), Offset(0f, c.y), Offset(size.width, c.y), strokeWidth = 2.dp.toPx())
                }

                // Center Dot (Red -> Green)
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

                // Rotating Directional Arrow
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

                // Guidance HUD
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
                                "Acquiring high-precision GPS lock..."
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

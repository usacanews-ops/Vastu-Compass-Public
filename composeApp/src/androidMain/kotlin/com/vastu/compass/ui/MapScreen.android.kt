package com.vastu.compass.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.vastu.compass.Saffron
import com.vastu.compass.VastuGreen
import com.vastu.compass.sensor.getCompassSensor
import kotlin.math.*

/**
 * Millimeter-accurate polygon centroid calculation via local planar metric projection.
 */
fun calculateMillimeterCentroid(points: List<LatLng>): LatLng {
    if (points.isEmpty()) return LatLng(0.0, 0.0)
    if (points.size < 3) {
        val avgLat = points.map { it.latitude }.average()
        val avgLng = points.map { it.longitude }.average()
        return LatLng(avgLat, avgLng)
    }

    val refLat = points[0].latitude
    val refLng = points[0].longitude
    val rad = PI / 180.0
    val meanLatRad = refLat * rad
    val metersPerDegLat = 111132.92 - 559.82 * cos(2 * meanLatRad) + 1.175 * cos(4 * meanLatRad)
    val metersPerDegLng = 111412.84 * cos(meanLatRad) - 93.5 * cos(3 * meanLatRad)

    // Project Lat/Lng to local metric Cartesian coordinates (x, y in meters)
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
    if (abs(signedArea) < 1e-7) {
        val avgLat = points.map { it.latitude }.average()
        val avgLng = points.map { it.longitude }.average()
        return LatLng(avgLat, avgLng)
    }

    cxMeters /= (6.0 * signedArea)
    cyMeters /= (6.0 * signedArea)

    // Convert back from meters to Lat/Lng
    val centroidLat = refLat + (cyMeters / metersPerDegLat)
    val centroidLng = refLng + (cxMeters / metersPerDegLng)
    return LatLng(centroidLat, centroidLng)
}

fun calculateBearing(from: LatLng, to: LatLng): Float {
    val dLng = Math.toRadians(to.longitude - from.longitude)
    val lat1 = Math.toRadians(from.latitude)
    val lat2 = Math.toRadians(to.latitude)
    val y = sin(dLng) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLng)
    val bearing = Math.toDegrees(atan2(y, x))
    return ((bearing + 360) % 360).toFloat()
}

fun calculateDistanceMeters(p1: LatLng, p2: LatLng): Float {
    val results = FloatArray(1)
    Location.distanceBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude, results)
    return results[0]
}

@SuppressLint("MissingPermission")
@Composable
actual fun MapScreen() {
    val context = LocalContext.current
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

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) userLocation = LatLng(loc.latitude, loc.longitude)
            }
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation ?: LatLng(28.6139, 77.2090), 19f)
    }

    val corners = remember { mutableStateListOf<LatLng>() }
    var centroid by remember { mutableStateOf<LatLng?>(null) }
    var isGuidingMode by remember { mutableStateOf(false) }
    var mapType by remember { mutableStateOf(MapType.HYBRID) }

    // Live distance & target tracking in Guiding Mode
    val distanceToCentroid = remember(userLocation, centroid) {
        if (userLocation != null && centroid != null) calculateDistanceMeters(userLocation!!, centroid!!) else 999f
    }
    val targetBearing = remember(userLocation, centroid) {
        if (userLocation != null && centroid != null) calculateBearing(userLocation!!, centroid!!) else 0f
    }
    val relativeArrowAngle = remember(targetBearing, deviceAzimuth) {
        (targetBearing - deviceAzimuth + 360f) % 360f
    }
    val isAtCenter = distanceToCentroid <= 1.0f

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = mapType,
                isMyLocationEnabled = hasLocationPermission,
                maxZoomPreference = 22f, // Maximum zoom
                minZoomPreference = 3f
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = true,
                zoomControlsEnabled = false,
                compassEnabled = true
            ),
            onMapClick = { latLng ->
                if (!isGuidingMode && centroid == null) {
                    corners.add(latLng)
                }
            }
        ) {
            // Draw Corners
            corners.forEachIndexed { index, point ->
                Marker(
                    state = MarkerState(position = point),
                    title = "Corner ${index + 1}"
                )
            }

            // Draw Property Shape in Yellow
            if (corners.size >= 3) {
                Polygon(
                    points = corners.toList(),
                    fillColor = Color(0x66FFEB3B), // Transparent Yellow
                    strokeColor = Color(0xFFFFD600), // Solid Yellow outline
                    strokeWidth = 6f
                )
            }

            // Draw Centroid Marker
            centroid?.let { centerPoint ->
                Marker(
                    state = MarkerState(position = centerPoint),
                    title = "Centroid (Brahmasthan)"
                )
            }
        }

        // Top Control Bar: Satellite toggle, Clear, Point count
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 12.dp, end = 12.dp),
            shape = RoundedCornerShape(14.dp),
            backgroundColor = Color.White.copy(alpha = 0.95f),
            elevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("PROPERTY PLOTTER", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF1E293B))
                    Text(
                        text = if (centroid != null) "Centroid calculated" else "Tap corners on map (${corners.size} marked)",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
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

                    if (corners.isNotEmpty() && !isGuidingMode) {
                        IconButton(onClick = {
                            corners.clear()
                            centroid = null
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear", tint = Color(0xFFDC2626))
                        }
                    }
                }
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
                // "Find Center" Button
                Button(
                    onClick = {
                        if (corners.size >= 3) {
                            centroid = calculateMillimeterCentroid(corners)
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
                        text = if (corners.size >= 3) "FIND CENTER (CENTROID)" else "MARK AT LEAST 3 CORNERS",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            } else if (!isGuidingMode) {
                // Two Buttons: "ReCalculate" and "Mark Center"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            centroid = null
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
                        onClick = {
                            isGuidingMode = true
                        },
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

        // Live Guidance Overlay (When "Mark Center" is active)
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

                // Dynamic Center Dot (Turns from Red to Green on exact match)
                Surface(
                    shape = CircleShape,
                    color = dotColor,
                    elevation = 6.dp,
                    modifier = Modifier.size(if (isAtCenter) 42.dp else 28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isAtCenter) {
                            Icon(Icons.Default.Check, contentDescription = "Arrived", tint = Color.White, modifier = Modifier.size(26.dp))
                        }
                    }
                }

                // Directional Guidance Arrow (Points toward Centroid)
                if (!isAtCenter) {
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .rotate(relativeArrowAngle),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Walk this way",
                            tint = Color(0xFFFFEB3B),
                            modifier = Modifier.size(46.dp)
                        )
                    }
                }

                // Bottom HUD with Distance, Status, and Exit button
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp, start = 20.dp, end = 20.dp)
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
                            text = if (isAtCenter) "EXACT CENTER REACHED!" else "WALK TOWARDS ARROW",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = if (isAtCenter) VastuGreen else Saffron
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isAtCenter) "You are standing on the Brahmasthan." else "Distance: ${"%.2f".format(distanceToCentroid)} meters",
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

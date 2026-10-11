package com.vastu.compass.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vastu.compass.data.VastuRepository
import com.vastu.compass.model.VastuZone
import com.vastu.compass.sensor.getCompassSensor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CompassScreen() {
    val sensor = remember { getCompassSensor() }
    val rawDegrees by sensor.azimuthDegrees.collectAsState(initial = 0f)
    val animatedDegrees by animateFloatAsState(
        targetValue = rawDegrees,
        animationSpec = tween(durationMillis = 200)
    )

    val pkg by VastuRepository.dataFlow.collectAsState()

    val currentZone = remember(rawDegrees, pkg) {
        val zones = pkg?.zones ?: emptyList()
        val norm = (rawDegrees % 360f + 360f) % 360f
        zones.firstOrNull { z ->
            if (z.startDeg > z.endDeg) norm >= z.startDeg || norm < z.endDeg
            else norm >= z.startDeg && norm < z.endDeg
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("VASTU COMPASS", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B), letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(24.dp))

        // Premium Compass UI
        Box(modifier = Modifier.size(320.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension / 2 - 16.dp.toPx()

                // Dial rotation
                rotate(-animatedDegrees, pivot = center) {
                    // Outer Ring
                    drawCircle(Color(0xFFE2E8F0), radius = radius + 8.dp.toPx(), style = Stroke(width = 4.dp.toPx()))
                    drawCircle(Color.White, radius = radius)
                    
                    // Zone Dividers & Markers
                    for (i in 0 until 16) {
                        val angleDeg = i * 22.5 - 11.25
                        val angleRad = angleDeg * (PI / 180.0)
                        val p1 = Offset(
                            (center.x + (radius - 16.dp.toPx()) * cos(angleRad)).toFloat(),
                            (center.y + (radius - 16.dp.toPx()) * sin(angleRad)).toFloat()
                        )
                        val p2 = Offset(
                            (center.x + radius * cos(angleRad)).toFloat(),
                            (center.y + radius * sin(angleRad)).toFloat()
                        )
                        drawLine(Color(0xFFCBD5E1), p1, p2, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                    }
                }

                // Modern Needle
                val needle = Path().apply {
                    moveTo(center.x, center.y - radius + 12.dp.toPx())
                    lineTo(center.x - 8.dp.toPx(), center.y)
                    lineTo(center.x + 8.dp.toPx(), center.y)
                    close()
                }
                drawPath(needle, color = Color(0xFFEF4444))
                drawCircle(Color(0xFF1E293B), radius = 8.dp.toPx(), center = center)
                drawCircle(Color.White, radius = 3.dp.toPx(), center = center)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${rawDegrees.toInt()}°", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                Text(currentZone?.id ?: "N", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Professional Data Card
        currentZone?.let { zone ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = 6.dp,
                backgroundColor = Color.White
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(zone.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Text("Pada: ${zone.pada}", fontSize = 13.sp, color = Color(0xFF64748B))
                        }
                        Surface(color = Color(0xFFEFF6FF), shape = RoundedCornerShape(8.dp)) {
                            Text(zone.element, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFF1F5F9))

                    // Premium Grid for Vastu Properties
                    VastuPropertyGrid(zone)

                    Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFF1F5F9))

                    Text("Key Attributes", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                    Text(zone.attributes, fontSize = 14.sp, color = Color(0xFF475569), modifier = Modifier.padding(top = 4.dp))
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("Effects", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                    Text(zone.effects, fontSize = 14.sp, color = Color(0xFF475569), modifier = Modifier.padding(top = 4.dp))

                    if (zone.remedies.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(color = Color(0xFFFEF2F2), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Remedies & Treatments", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFB91C1C))
                                Text(zone.remedies, fontSize = 13.sp, color = Color(0xFF991B1B), modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VastuPropertyGrid(zone: VastuZone) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PropertyItem("Good Color", zone.goodColor, Color(0xFF059669))
            PropertyItem("Bad Color", zone.badColor, Color(0xFFDC2626))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PropertyItem("Good Metal", zone.goodMetal, Color(0xFF059669))
            PropertyItem("Bad Metal", zone.badMetal, Color(0xFFDC2626))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PropertyItem("Ideal Shape", zone.shape, Color(0xFF475569))
        }
    }
}

@Composable
fun PropertyItem(label: String, value: String, valueColor: Color) {
    Column(modifier = Modifier.fillMaxWidth(0.48f)) {
        Text(label, fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
        Text(value.ifBlank { "N/A" }, fontSize = 14.sp, color = valueColor, fontWeight = FontWeight.Medium)
    }
}

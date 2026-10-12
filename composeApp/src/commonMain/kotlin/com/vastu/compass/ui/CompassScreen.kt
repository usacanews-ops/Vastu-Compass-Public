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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vastu.compass.BeigeBackground
import com.vastu.compass.VastuGreen
import com.vastu.compass.data.VastuRepository
import com.vastu.compass.model.VastuZone
import com.vastu.compass.sensor.getCompassSensor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Dynamically fetch the correct elemental icon
fun getElementIcon(element: String, zoneId: String): ImageVector {
    val lower = element.lowercase()
    return when {
        lower.contains("air") || zoneId == "NW" -> Icons.Default.Air // The 3-line Breeze icon
        lower.contains("water") -> Icons.Default.WaterDrop
        lower.contains("fire") -> Icons.Default.LocalFireDepartment
        lower.contains("earth") -> Icons.Default.Terrain
        lower.contains("space") -> Icons.Default.AllInclusive
        else -> Icons.Default.Info
    }
}

// Dynamically color the icon based on the element
fun getElementColor(element: String, zoneId: String): Color {
    val lower = element.lowercase()
    return when {
        lower.contains("air") || zoneId == "NW" -> Color(0xFF03A9F4) // Light Blue Wind
        lower.contains("water") -> Color(0xFF1976D2) // Deep Blue Water
        lower.contains("fire") -> Color(0xFFD32F2F) // Red Fire
        lower.contains("earth") -> Color(0xFFF57F17) // Yellow/Brown Earth
        lower.contains("space") -> Color(0xFF757575) // Grey Space
        else -> Color(0xFF64748B)
    }
}

@Composable
fun CompassScreen() {
    val sensor = remember { getCompassSensor() }
    val rawDegrees by sensor.azimuthDegrees.collectAsState(initial = 0f)
    val animatedDegrees by animateFloatAsState(targetValue = rawDegrees, animationSpec = tween(200))
    val pkg by VastuRepository.dataFlow.collectAsState()

    val currentZone = remember(rawDegrees, pkg) {
        val norm = (rawDegrees % 360f + 360f) % 360f
        pkg?.zones?.firstOrNull { z ->
            if (z.startDeg > z.endDeg) norm >= z.startDeg || norm < z.endDeg else norm >= z.startDeg && norm < z.endDeg
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(BeigeBackground).padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("VASTU COLOR CHAKRA", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
        Spacer(modifier = Modifier.height(24.dp))

        Box(modifier = Modifier.size(340.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension / 2 - 20.dp.toPx()

                rotate(-animatedDegrees, pivot = center) {
                    // 1. Draw Color Chakra Arcs (16 Zones)
                    val colorMap = listOf(
                        Color(0xFF2196F3), Color(0xFF64B5F6), Color(0xFFE0E0E0), Color(0xFFAED581),
                        Color(0xFF4CAF50), Color(0xFF81C784), Color(0xFFF44336), Color(0xFFE57373),
                        Color(0xFFFF9800), Color(0xFFFFB74D), Color(0xFFFFD54F), Color(0xFFFFF176),
                        Color(0xFF9E9E9E), Color(0xFFBDBDBD), Color(0xFF4FC3F7), Color(0xFF81D4FA)
                    )
                    
                    for (i in 0 until 16) {
                        val startAngle = i * 22.5f - 11.25f - 90f
                        drawArc(
                            color = colorMap[i].copy(alpha = 0.3f),
                            startAngle = startAngle,
                            sweepAngle = 22.5f,
                            useCenter = true,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2)
                        )
                    }

                    // 2. Draw 32 Padas (Ticks)
                    drawCircle(Color.White, radius = radius, style = Stroke(width = 2.dp.toPx()))
                    for (i in 0 until 32) {
                        val angleRad = (i * 11.25 - 90) * (PI / 180.0)
                        val isMajor = i % 2 == 0
                        val tickLength = if (isMajor) 16.dp.toPx() else 8.dp.toPx()
                        
                        val p1 = Offset((center.x + (radius - tickLength) * cos(angleRad)).toFloat(), (center.y + (radius - tickLength) * sin(angleRad)).toFloat())
                        val p2 = Offset((center.x + radius * cos(angleRad)).toFloat(), (center.y + radius * sin(angleRad)).toFloat())
                        drawLine(Color.DarkGray, p1, p2, strokeWidth = if(isMajor) 2.dp.toPx() else 1.dp.toPx())
                    }

                    // 3. Draw Wind Flow (Vayu) from NW
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                    val windPath1 = Path().apply {
                        moveTo(center.x - radius * 0.6f, center.y - radius * 0.6f)
                        quadraticBezierTo(center.x, center.y - radius * 0.2f, center.x + radius * 0.5f, center.y + radius * 0.3f)
                    }
                    val windPath2 = Path().apply {
                        moveTo(center.x - radius * 0.8f, center.y - radius * 0.4f)
                        quadraticBezierTo(center.x - radius * 0.2f, center.y, center.x + radius * 0.3f, center.y + radius * 0.5f)
                    }
                    drawPath(windPath1, color = Color(0xFF81D4FA), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, pathEffect = dashEffect))
                    drawPath(windPath2, color = Color(0xFF81D4FA), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, pathEffect = dashEffect))
                }

                // Fixed Red Needle
                val needle = Path().apply {
                    moveTo(center.x, center.y - radius + 10.dp.toPx())
                    lineTo(center.x - 6.dp.toPx(), center.y)
                    lineTo(center.x + 6.dp.toPx(), center.y)
                    close()
                }
                drawPath(needle, color = Color.Red)
                drawCircle(Color.DarkGray, radius = 6.dp.toPx(), center = center)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${rawDegrees.toInt()}°", fontSize = 32.sp, fontWeight = FontWeight.Black)
                Text(currentZone?.id ?: "N", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = VastuGreen)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Professional Insights Card
        currentZone?.let { zone ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), elevation = 6.dp, backgroundColor = Color.White) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Element Icon
                            Icon(
                                imageVector = getElementIcon(zone.element, zone.id),
                                contentDescription = zone.element,
                                tint = getElementColor(zone.element, zone.id),
                                modifier = Modifier.size(36.dp).padding(end = 12.dp)
                            )
                            Column {
                                Text(zone.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text("Pada: ${zone.pada}", fontSize = 13.sp, color = Color(0xFF64748B))
                            }
                        }
                        Surface(color = Color(0xFFEFF6FF), shape = RoundedCornerShape(8.dp)) {
                            Text(zone.element, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFF1F5F9))

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
            Spacer(modifier = Modifier.height(80.dp)) // Padding for bottom nav & consult button
        }
    }
}

@Composable
fun VastuPropertyGrid(zone: VastuZone) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PropertyItem("Good Color", zone.goodColor, Color(0xFF059669), Modifier.weight(1f))
            PropertyItem("Bad Color", zone.badColor, Color(0xFFDC2626), Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PropertyItem("Good Metal", zone.goodMetal, Color(0xFF059669), Modifier.weight(1f))
            PropertyItem("Bad Metal", zone.badMetal, Color(0xFFDC2626), Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PropertyItem("Ideal Shape", zone.shape, Color(0xFF475569), Modifier.weight(1f))
            PropertyItem("Strictly Avoid", zone.strictlyAvoid, Color(0xFFB91C1C), Modifier.weight(1f))
        }
        Divider(color = Color(0xFFF8FAFC))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PropertyItem("Good Business", zone.goodBusiness, Color(0xFF059669), Modifier.weight(1f))
            PropertyItem("Bad Business", zone.badBusiness, Color(0xFFDC2626), Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            PropertyItem("General Goods", zone.generalGoods, Color(0xFF059669), Modifier.weight(1f))
            PropertyItem("General Bads", zone.generalBads, Color(0xFFDC2626), Modifier.weight(1f))
        }
    }
}

@Composable
fun PropertyItem(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(end = 8.dp)) {
        Text(label, fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
        Text(value.ifBlank { "N/A" }, fontSize = 13.sp, color = valueColor, fontWeight = FontWeight.Medium)
    }
}

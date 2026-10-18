package com.vastu.compass.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vastu.compass.BeigeBackground
import com.vastu.compass.VastuGreen
import com.vastu.compass.data.VastuRepository
import com.vastu.compass.sensor.getCompassSensor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val PADA_LIST = listOf(
    "N1", "N2", "N3", "N4", "N5", "N6", "N7", "N8",
    "E1", "E2", "E3", "E4", "E5", "E6", "E7", "E8",
    "S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8",
    "W1", "W2", "W3", "W4", "W5", "W6", "W7", "W8"
)

private fun getPadaForDegrees(degrees: Float): String {
    val norm = (degrees % 360f + 360f) % 360f
    val offset = (norm - 315f + 360f) % 360f
    val index = (offset / 11.25f).toInt().coerceIn(0, 31)
    return PADA_LIST[index]
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
    
    val currentPada = remember(rawDegrees) { getPadaForDegrees(rawDegrees) }

    Column(
        modifier = Modifier.fillMaxSize().background(BeigeBackground).padding(16.dp),
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

                    // 3. Draw Wind Flow (VayuOverlay) from NW
                    val windPath = Path().apply {
                        moveTo(center.x - radius * 0.7f, center.y - radius * 0.7f) // NW
                        quadraticBezierTo(center.x, center.y - radius * 0.3f, center.x + radius * 0.6f, center.y + radius * 0.6f)
                    }
                    drawPath(windPath, color = Color(0x66B3E5FC), style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
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
                Text(currentPada, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Info Card
        currentZone?.let { zone ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), elevation = 4.dp) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("${zone.name} (${zone.id})", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Element:", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                            Text(zone.element, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Padas:", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                            Text(zone.pada, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Effects:", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                    Text(zone.effects, fontSize = 16.sp, lineHeight = 22.sp)
                    
                    if (zone.remedies.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Remedy:", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        Text(zone.remedies, fontSize = 16.sp, color = Color(0xFFD32F2F), lineHeight = 22.sp)
                    }
                }
            }
        }
    }
}

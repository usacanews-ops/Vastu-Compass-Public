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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vastu.compass.data.VastuRepository
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
        animationSpec = tween(durationMillis = 150)
    )

    val pkg by VastuRepository.dataFlow.collectAsState()

    val currentZone = remember(rawDegrees, pkg) {
        val zones = pkg?.zones ?: emptyList()
        val norm = (rawDegrees % 360f + 360f) % 360f
        zones.firstOrNull { z ->
            if (z.startDeg > z.endDeg) {
                norm >= z.startDeg || norm < z.endDeg
            } else {
                norm >= z.startDeg && norm < z.endDeg
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3EEEA))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("VASTU COMPASS", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723))
        Text("Data Version: ${pkg?.version ?: 1}", fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier.size(300.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension / 2 - 16.dp.toPx()

                // Dial rotates opposite to device azimuth
                rotate(-animatedDegrees, pivot = center) {
                    drawCircle(Color.White, radius = radius)
                    drawCircle(Color(0xFFBCAAA4), radius = radius, style = Stroke(width = 3.dp.toPx()))

                    for (i in 0 until 16) {
                        val angleDeg = i * 22.5 - 11.25
                        val angleRad = angleDeg * (PI / 180.0)

                        val p1 = Offset(
                            (center.x + (radius - 12.dp.toPx()) * cos(angleRad)).toFloat(),
                            (center.y + (radius - 12.dp.toPx()) * sin(angleRad)).toFloat()
                        )
                        val p2 = Offset(
                            (center.x + radius * cos(angleRad)).toFloat(),
                            (center.y + radius * sin(angleRad)).toFloat()
                        )
                        drawLine(Color(0xFF8D6E63), p1, p2, strokeWidth = 2.dp.toPx())
                    }
                }

                // Stationary Red Needle pointing North
                val needle = Path().apply {
                    moveTo(center.x, center.y - radius + 8.dp.toPx())
                    lineTo(center.x - 10.dp.toPx(), center.y)
                    lineTo(center.x + 10.dp.toPx(), center.y)
                    close()
                }
                drawPath(needle, color = Color(0xFFD32F2F))
                drawCircle(Color(0xFFD32F2F), radius = 6.dp.toPx(), center = center)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${rawDegrees.toInt()}°", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
                Text(currentZone?.id ?: "N", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF3E2723))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        currentZone?.let { zone ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = 4.dp,
                backgroundColor = Color.White
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(zone.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723))
                    Text("Deity / Pada: ${zone.pada}", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Attributes: ${zone.attributes}", fontSize = 14.sp, color = Color(0xFF424242))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Effects & Remedies:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2E7D32))
                    Text(zone.effects, fontSize = 13.sp, color = Color(0xFF616161))
                    if (zone.remedies.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(zone.remedies, fontSize = 13.sp, color = Color(0xFFC62828))
                    }
                }
            }
        }
    }
}

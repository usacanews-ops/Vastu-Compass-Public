package com.vastu.compass.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vastu.compass.BeigeBackground
import com.vastu.compass.Saffron
import com.vastu.compass.VastuGreen
import com.vastu.compass.sensor.getCompassSensor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CompassScreen() {
    val sensor = remember { getCompassSensor() }
    val rawDegrees by sensor.azimuthDegrees.collectAsState(initial = 0f)
    val animatedDegrees by animateFloatAsState(targetValue = rawDegrees, animationSpec = tween(180))

    val currentZoneId = remember(rawDegrees) { calculateZoneId(rawDegrees) }
    val currentPada = remember(rawDegrees) { calculatePada(rawDegrees) }

    Column(
        modifier = Modifier.fillMaxSize().background(BeigeBackground).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("VASTU COMPASS", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color.White,
            elevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${rawDegrees.toInt()}°", fontSize = 42.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                Text("Current Heading", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                
                Spacer(modifier = Modifier.height(24.dp))

                Box(modifier = Modifier.size(280.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.minDimension / 2 - 16.dp.toPx()

                        rotate(-animatedDegrees, pivot = center) {
                            val colorMap = listOf(
                                Color(0xFF2196F3), Color(0xFF64B5F6), Color(0xFFE0E0E0), Color(0xFFAED581),
                                Color(0xFF4CAF50), Color(0xFF81C784), Color(0xFFF44336), Color(0xFFE57373),
                                Color(0xFFFF9800), Color(0xFFFFB74D), Color(0xFFFFD54F), Color(0xFFFFF176),
                                Color(0xFF9E9E9E), Color(0xFFBDBDBD), Color(0xFF4FC3F7), Color(0xFF81D4FA)
                            )
                            for (i in 0 until 16) {
                                drawArc(
                                    color = colorMap[i].copy(alpha = 0.3f),
                                    startAngle = i * 22.5f - 11.25f - 90f, sweepAngle = 22.5f,
                                    useCenter = true, topLeft = Offset(center.x - radius, center.y - radius), size = Size(radius * 2, radius * 2)
                                )
                            }
                            drawCircle(color = Color(0xFFD4AF37), radius = radius, style = Stroke(width = 3.dp.toPx()))

                            for (i in 0 until 32) {
                                val angleRad = (i * 11.25 - 90) * (PI / 180.0)
                                val isMajor = i % 2 == 0
                                val length = if (isMajor) 14.dp.toPx() else 7.dp.toPx()
                                drawLine(
                                    color = if (isMajor) Color(0xFF1E293B) else Color.Gray,
                                    start = Offset((center.x + (radius - length) * cos(angleRad)).toFloat(), (center.y + (radius - length) * sin(angleRad)).toFloat()),
                                    end = Offset((center.x + radius * cos(angleRad)).toFloat(), (center.y + radius * sin(angleRad)).toFloat()),
                                    strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                                )
                            }
                        }

                        val needle = Path().apply {
                            moveTo(center.x, center.y - radius + 2.dp.toPx())
                            lineTo(center.x - 8.dp.toPx(), center.y - radius + 26.dp.toPx())
                            lineTo(center.x + 8.dp.toPx(), center.y - radius + 26.dp.toPx())
                            close()
                        }
                        drawPath(needle, color = Color(0xFFDC2626))
                        drawCircle(Color(0xFF1E293B), radius = 6.dp.toPx(), center = center)
                    }

                    // Inner Box - Now shows BOTH Zone ID and PADA
                    Surface(shape = CircleShape, color = Color.White, elevation = 4.dp, modifier = Modifier.size(90.dp)) {
                        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(currentZoneId, fontSize = 24.sp, fontWeight = FontWeight.Black, color = VastuGreen)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(currentPada, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Saffron)
                        }
                    }
                }
            }
        }
    }
}

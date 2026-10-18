package com.vastu.compass.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.WaterDrop
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
        modifier = Modifier
            .fillMaxSize()
            .background(BeigeBackground)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("VASTU COLOR CHAKRA", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
        Spacer(modifier = Modifier.height(24.dp))

        Box(modifier = Modifier.size(320.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension / 2 - 10.dp.toPx()

                rotate(-animatedDegrees, pivot = center) {
                    val colorMap = listOf(
                        Color(0xFF2196F3), Color(0xFF64B5F6), Color(0xFFE0E0E0), Color(0xFFAED581),
                        Color(0xFF4CAF50), Color(0xFF81C784), Color(0xFFF44336), Color(0xFFE57373),
                        Color(0xFFFF9800), Color(0xFFFFB74D), Color(0xFFFFD54F), Color(0xFFFFF176),
                        Color(0xFF9E9E9E), Color(0xFFBDBDBD), Color(0xFF4FC3F7), Color(0xFF81D4FA)
                    )
                    
                    for (i in 0 until 16) {
                        val startAngle = i * 22.5f - 11.25f - 90f
                        drawArc(
                            color = colorMap[i].copy(alpha = 0.4f),
                            startAngle = startAngle,
                            sweepAngle = 22.5f,
                            useCenter = true,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2)
                        )
                    }

                    drawCircle(Color.White, radius = radius, style = Stroke(width = 1.dp.toPx()))
                    for (i in 0 until 32) {
                        val angleRad = (i * 11.25 - 90) * (PI / 180.0)
                        val isMajor = i % 2 == 0
                        val tickLength = if (isMajor) 14.dp.toPx() else 7.dp.toPx()
                        
                        val p1 = Offset((center.x + (radius - tickLength) * cos(angleRad)).toFloat(), (center.y + (radius - tickLength) * sin(angleRad)).toFloat())
                        val p2 = Offset((center.x + radius * cos(angleRad)).toFloat(), (center.y + radius * sin(angleRad)).toFloat())
                        drawLine(Color(0xFF334155), p1, p2, strokeWidth = if(isMajor) 2.dp.toPx() else 1.dp.toPx())
                    }
                }
                
                drawCircle(Color(0xFFF3EEEA), radius = radius * 0.42f, center = center)

                val needle = Path().apply {
                    moveTo(center.x, center.y - radius + 10.dp.toPx())
                    lineTo(center.x - 5.dp.toPx(), center.y - 15.dp.toPx())
                    lineTo(center.x + 5.dp.toPx(), center.y - 15.dp.toPx())
                    close()
                }
                drawPath(needle, color = Color(0xFFDC2626))
                drawCircle(Color(0xFF1E293B), radius = 6.dp.toPx(), center = center)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = 16.dp)) {
                Text("${rawDegrees.toInt()}°", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                Text(currentZone?.id ?: "N", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = VastuGreen)
                Text(currentPada, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Saffron)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        currentZone?.let { zone ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), 
                shape = RoundedCornerShape(20.dp), 
                backgroundColor = Color.White,
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when {
                                    zone.element.contains("Air", ignoreCase = true) || zone.element.contains("Wood", ignoreCase = true) -> Icons.Default.Air
                                    zone.element.contains("Water", ignoreCase = true) -> Icons.Default.WaterDrop
                                    zone.element.contains("Fire", ignoreCase = true) -> Icons.Default.LocalFireDepartment
                                    zone.element.contains("Earth", ignoreCase = true) -> Icons.Default.Terrain
                                    else -> Icons.Default.Category
                                },
                                contentDescription = "Element",
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(zone.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
                                Text("Padas: ${zone.pada}", fontSize = 14.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            }
                        }
                        
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = zone.element,
                                color = Color(0xFF2563EB),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color(0xFFF1F5F9), thickness = 1.dp)
                    
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            GridItem("Good Color", zone.goodColor, isGood = true)
                            GridItem("Good Metal", zone.goodMetal, isGood = true)
                            GridItem("Ideal Shape", zone.shape, isNeutral = true) 
                            GridItem("Good Business", zone.goodBusiness, isGood = true)
                            GridItem("General Goods", zone.generalGoods, isGood = true)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            GridItem("Bad Color", zone.badColor, isGood = false)
                            GridItem("Bad Metal", zone.badMetal, isGood = false)
                            GridItem("Strictly Avoid", zone.strictlyAvoid, isGood = false)
                            GridItem("Bad Business", zone.badBusiness, isGood = false)
                            GridItem("General Bads", zone.generalBads, isGood = false)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Key Attributes", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(zone.attributes.ifBlank { "N/A" }, fontSize = 15.sp, color = Color(0xFF334155), lineHeight = 22.sp)

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("Effects", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(zone.effects.ifBlank { "N/A" }, fontSize = 15.sp, color = Color(0xFF334155), lineHeight = 22.sp)

                    if (zone.remedies.isNotBlank()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Remedies & Treatments", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(zone.remedies, fontSize = 15.sp, color = Color(0xFF991B1B), lineHeight = 22.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GridItem(title: String, value: String?, isGood: Boolean = true, isNeutral: Boolean = false) {
    val displayValue = if (value.isNullOrBlank()) "N/A" else value
    val valueColor = when {
        isNeutral || displayValue == "N/A" -> Color(0xFF64748B)
        isGood -> Color(0xFF16A34A)
        else -> Color(0xFFDC2626)
    }
    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF94A3B8))
        Spacer(modifier = Modifier.height(2.dp))
        Text(displayValue, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

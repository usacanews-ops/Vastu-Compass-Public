package com.vastu.compass.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vastu.compass.BeigeBackground
import com.vastu.compass.Saffron
import com.vastu.compass.VastuGreen
import com.vastu.compass.data.VastuRepository
import com.vastu.compass.sensor.getCompassSensor

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
    val degrees by sensor.azimuthDegrees.collectAsState(initial = 0f)
    val pkg by VastuRepository.dataFlow.collectAsState()

    val currentZoneId = remember(degrees) { calculateZoneId(degrees) }
    val currentPada = remember(degrees) { getPadaForDegrees(degrees) }
    val currentZone = pkg?.zones?.firstOrNull { it.id.equals(currentZoneId, ignoreCase = true) }

    Column(
        modifier = Modifier.fillMaxSize().background(BeigeBackground).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("VASTU COMPASS", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF3E2723))
        Spacer(modifier = Modifier.height(16.dp))

        // Compass Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = 4.dp,
            backgroundColor = Color.White
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentZone?.name ?: currentZoneId,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = VastuGreen
                )
                Text(
                    text = "Zone: $currentZoneId  •  Pada: $currentPada",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Saffron
                )
                Text("${degrees.toInt()}°", fontSize = 14.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(24.dp))

                Box(modifier = Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.minDimension / 2 - 10.dp.toPx()

                        rotate(-degrees, pivot = center) {
                            drawCircle(color = Color(0xFFD4AF37), radius = radius, style = Stroke(width = 2.dp.toPx()))
                            drawLine(Color.Red, start = center, end = Offset(center.x, center.y - radius), strokeWidth = 4.dp.toPx())
                            drawLine(Color.DarkGray, start = center, end = Offset(center.x, center.y + radius), strokeWidth = 4.dp.toPx())
                            drawLine(Color.DarkGray, start = center, end = Offset(center.x - radius, center.y), strokeWidth = 2.dp.toPx())
                            drawLine(Color.DarkGray, start = center, end = Offset(center.x + radius, center.y), strokeWidth = 2.dp.toPx())
                        }
                    }
                    Surface(shape = CircleShape, color = Color.White, elevation = 2.dp, modifier = Modifier.size(20.dp)) {}
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Zone Info Card
        currentZone?.let { zone ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                backgroundColor = Color.White,
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Zone Attributes", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF1F5F9))
                    
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Element", fontSize = 15.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                            Text(zone.element, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Padas", fontSize = 15.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                            Text(zone.pada, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text("Colors & Features", fontSize = 15.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                        Text(zone.attributes, fontSize = 16.sp, color = Color(0xFF334155), lineHeight = 22.sp)
                    }
                }
            }
        }
    }
}

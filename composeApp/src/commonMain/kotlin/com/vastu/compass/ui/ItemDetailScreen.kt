package com.vastu.compass.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vastu.compass.BeigeBackground
import com.vastu.compass.Saffron
import com.vastu.compass.VastuGreen
import com.vastu.compass.data.VastuRepository
import com.vastu.compass.model.VastuZone
import com.vastu.compass.model.ZoneEffect
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

private val ZONE_ORDER = listOf(
    "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
    "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"
)

private val ZONE_NAMES = mapOf(
    "N" to "North", "NNE" to "North-North-East", "NE" to "North-East", "ENE" to "East-North-East",
    "E" to "East", "ESE" to "East-South-East", "SE" to "South-East", "SSE" to "South-South-East",
    "S" to "South", "SSW" to "South-South-West", "SW" to "South-West", "WSW" to "West-South-West",
    "W" to "West", "WNW" to "West-North-West", "NW" to "North-West", "NNW" to "North-North-West"
)

fun calculatePada(degrees: Float): String {
    val norm = (degrees % 360f + 360f) % 360f
    val offset = (norm - 315f + 360f) % 360f
    val index = (offset / 11.25f).toInt().coerceIn(0, 31)
    return PADA_LIST[index]
}

fun calculateZoneId(degrees: Float): String {
    val norm = (degrees % 360f + 360f) % 360f
    val offset = (norm + 11.25f) % 360f
    val index = (offset / 22.5f).toInt().coerceIn(0, 15)
    return ZONE_ORDER[index]
}

@Composable
fun ItemDetailScreen(itemId: String, onBack: () -> Unit) {
    val sensor = remember { getCompassSensor() }
    val rawDegrees by sensor.azimuthDegrees.collectAsState(initial = 0f)
    val animatedDegrees by animateFloatAsState(targetValue = rawDegrees, animationSpec = tween(180))

    val pkg by VastuRepository.dataFlow.collectAsState()
    val item = pkg?.items?.firstOrNull { it.id == itemId }

    val currentPada = remember(rawDegrees) { calculatePada(rawDegrees) }
    val currentZoneId = remember(rawDegrees) { calculateZoneId(rawDegrees) }

    val currentZone = remember(currentZoneId, pkg) {
        pkg?.zones?.firstOrNull { it.id.equals(currentZoneId, ignoreCase = true) }
            ?: VastuZone(id = currentZoneId, name = ZONE_NAMES[currentZoneId] ?: currentZoneId)
    }

    var manualSelectedZoneId by remember { mutableStateOf<String?>(null) }
    val activeZoneId = manualSelectedZoneId ?: currentZoneId
    val activeZone = remember(activeZoneId, pkg) {
        pkg?.zones?.firstOrNull { it.id.equals(activeZoneId, ignoreCase = true) } ?: currentZone
    }

    val activeEffect: ZoneEffect? = item?.zoneEffects?.get(activeZoneId)
        ?: item?.zoneEffects?.entries?.firstOrNull { it.key.equals(activeZoneId, ignoreCase = true) }?.value

    Column(
        modifier = Modifier.fillMaxSize().background(BeigeBackground).padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(38.dp).background(Color.White, CircleShape)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF3E2723))
            }
            Spacer(modifier = Modifier.width(12.dp))
            item?.let {
                Text(text = it.title.uppercase(), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 72.dp)) {
            // COMPASS CARD
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), backgroundColor = Color.White, elevation = 4.dp) {
                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LIVE FACING DIRECTION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 1.sp)
                        Text("${ZONE_NAMES[currentZoneId] ?: currentZoneId} (${currentZoneId} · ${currentPada})", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = VastuGreen)
                        Text("${rawDegrees.toInt()}° Heading", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Box(modifier = Modifier.size(240.dp), contentAlignment = Alignment.Center) {
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
                            Surface(shape = CircleShape, color = Color.White, elevation = 3.dp, modifier = Modifier.size(76.dp)) {
                                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Text(currentZoneId, fontSize = 20.sp, fontWeight = FontWeight.Black, color = VastuGreen)
                                    Text(currentPada, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Saffron)
                                }
                            }
                        }
                    }
                }
            }

            // EFFECT STATUS BADGE & INFO
            item {
                val statusText = activeEffect?.status ?: "Neutral"
                val (badgeBg, badgeTextColor, statusIcon) = when {
                    statusText.equals("Best", ignoreCase = true) || statusText.equals("Good", ignoreCase = true) || statusText.equals("Favorable", ignoreCase = true) ->
                        Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), Icons.Default.CheckCircle)
                    statusText.equals("Bad", ignoreCase = true) || statusText.equals("Avoid", ignoreCase = true) || statusText.equals("Strictly Avoid", ignoreCase = true) ->
                        Triple(Color(0xFFFFEBEE), Color(0xFFC62828), Icons.Default.Cancel)
                    else ->
                        Triple(Color(0xFFFFF3E0), Color(0xFFEF6C00), Icons.Default.Info)
                }

                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), backgroundColor = Color.White, elevation = 3.dp) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${ZONE_NAMES[activeZoneId] ?: activeZoneId} (${activeZoneId})", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                                Text(if (activeZoneId == currentZoneId) "CURRENT FACING · PADA ${currentPada}" else "SELECTED ZONE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (activeZoneId == currentZoneId) Saffron else Color(0xFF64748B))
                            }
                            Surface(color = badgeBg, shape = RoundedCornerShape(10.dp)) {
                                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(statusIcon, contentDescription = null, tint = badgeTextColor, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(statusText.uppercase(), color = badgeTextColor, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                                }
                            }
                        }
                        
                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))
                        Text("Vastu Effects in this Zone", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(activeEffect?.note?.ifBlank { null } ?: activeZone.effects.ifBlank { "No specific effects recorded for this item in this zone." }, fontSize = 15.sp, color = Color(0xFF475569), lineHeight = 22.sp)
                        
                        val remedy = activeEffect?.remedy?.ifBlank { null }
                        if (remedy != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(color = Color(0xFFFEF2F2), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFFB91C1C), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Recommended Vastu Remedy", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFB91C1C))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(remedy, fontSize = 14.sp, color = Color(0xFF991B1B), lineHeight = 20.sp)
                                }
                            }
                        }
                    }
                }
            }

            // RESTORED: RICH ACTIVE ZONE PROPERTIES
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), backgroundColor = Color.White, elevation = 2.dp) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("${activeZone.name} Zone Properties", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B), modifier = Modifier.padding(bottom = 12.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Element", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(activeZone.element, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Ideal Shape", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(activeZone.shape.ifBlank { "N/A" }, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                            }
                        }
                        Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 8.dp))
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Good Colors", fontSize = 12.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                Text(activeZone.goodColor, fontSize = 15.sp, color = Color(0xFF0F172A))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Bad Colors", fontSize = 12.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                                Text(activeZone.badColor, fontSize = 15.sp, color = Color(0xFF0F172A))
                            }
                        }
                        Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 8.dp))
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Good Metals", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(activeZone.goodMetal, fontSize = 15.sp, color = Color(0xFF0F172A))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Bad Metals", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(activeZone.badMetal, fontSize = 15.sp, color = Color(0xFF0F172A))
                            }
                        }
                        Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 8.dp))
                        Text("Strictly Avoid", fontSize = 12.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                        Text(activeZone.strictlyAvoid.ifBlank { "N/A" }, fontSize = 15.sp, color = Color(0xFF0F172A), modifier = Modifier.padding(bottom = 8.dp))
                        
                        Text("Key Attributes", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(activeZone.attributes, fontSize = 15.sp, color = Color(0xFF0F172A), modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
            }

            item { Text("ALL 16 ZONES COMPATIBILITY", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF3E2723), modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)) }
            
            items(ZONE_ORDER) { zoneKey ->
                val isFacing = zoneKey == currentZoneId
                val isSelected = zoneKey == activeZoneId
                val effect = item?.zoneEffects?.get(zoneKey) ?: item?.zoneEffects?.entries?.firstOrNull { it.key.equals(zoneKey, ignoreCase = true) }?.value
                val status = effect?.status ?: "Neutral"

                val (pillBg, pillTextColor) = when {
                    status.equals("Best", ignoreCase = true) || status.equals("Good", ignoreCase = true) || status.equals("Favorable", ignoreCase = true) -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
                    status.equals("Bad", ignoreCase = true) || status.equals("Avoid", ignoreCase = true) || status.equals("Strictly Avoid", ignoreCase = true) -> Color(0xFFFFEBEE) to Color(0xFFC62828)
                    else -> Color(0xFFFFF3E0) to Color(0xFFEF6C00)
                }

                val borderColor by animateColorAsState(if (isSelected) VastuGreen else if (isFacing) Saffron else Color.Transparent)
                Card(modifier = Modifier.fillMaxWidth().border(if (isSelected || isFacing) 2.dp else 0.dp, borderColor, RoundedCornerShape(12.dp)).clickable { manualSelectedZoneId = zoneKey }, shape = RoundedCornerShape(12.dp), backgroundColor = Color.White, elevation = if (isSelected) 3.dp else 1.dp) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(zoneKey, fontWeight = FontWeight.Black, fontSize = 16.sp, color = if (isFacing) Saffron else Color(0xFF1E293B), modifier = Modifier.width(46.dp))
                            Column {
                                Text(ZONE_NAMES[zoneKey] ?: zoneKey, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                if (isFacing) Text("FACING NOW (${currentPada})", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Saffron)
                            }
                        }
                        Surface(color = pillBg, shape = RoundedCornerShape(6.dp)) {
                            Text(status, color = pillTextColor, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

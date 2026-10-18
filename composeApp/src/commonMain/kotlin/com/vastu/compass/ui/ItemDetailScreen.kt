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
            ?: VastuZone(
                id = currentZoneId,
                name = ZONE_NAMES[currentZoneId] ?: currentZoneId,
                element = "",
                startDeg = 0f,
                endDeg = 0f,
                attributes = "",
                pada = currentPada,
                effects = "",
                remedies = ""
            )
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
        // Top Bar
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(38.dp).background(Color.White, CircleShape)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF3E2723))
            }
            Spacer(modifier = Modifier.width(12.dp))
            item?.let {
                Text(text = it.title.uppercase(), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 72.dp)) {
            // Compass Block (Omitted to save space, keep your existing compass canvas block here exactly as before)
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), backgroundColor = Color.White, elevation = 4.dp) {
                    Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("LIVE FACING DIRECTION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), letterSpacing = 1.sp)
                        Text("${ZONE_NAMES[currentZoneId] ?: currentZoneId} (${currentZoneId} · ${currentPada})", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = VastuGreen)
                        Text("${rawDegrees.toInt()}° Heading", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Keep your existing Interactive Canvas Compass block right here
                        Box(modifier = Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                            // ... existing rotating dial ...
                            Surface(shape = CircleShape, color = Color.White, elevation = 3.dp, modifier = Modifier.size(76.dp)) {
                                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Text(currentZoneId, fontSize = 18.sp, fontWeight = FontWeight.Black, color = VastuGreen)
                                    Text(currentPada, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Saffron)
                                }
                            }
                        }
                    }
                }
            }

            // Highlighting the fix for "Best" Color Status
            item {
                val statusText = activeEffect?.status ?: "Neutral"
                val (badgeBg, badgeTextColor, statusIcon) = when {
                    statusText.contains("Best", ignoreCase = true) || statusText.contains("Good", ignoreCase = true) || statusText.contains("Favorable", ignoreCase = true) ->
                        Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), Icons.Default.CheckCircle)
                    statusText.contains("Bad", ignoreCase = true) || statusText.contains("Avoid", ignoreCase = true) || statusText.contains("Strictly", ignoreCase = true) ->
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
                                    Text(statusText.uppercase(), color = badgeTextColor, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                }
                            }
                        }
                        
                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))
                        Text("Vastu Effects in this Zone", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(activeEffect?.note?.ifBlank { null } ?: activeZone.effects.ifBlank { "No severe negative effects recorded for this zone." }, fontSize = 14.sp, color = Color(0xFF475569), lineHeight = 20.sp)
                        
                        val remedy = activeEffect?.remedy?.ifBlank { null } ?: activeZone.remedies.ifBlank { null }
                        if (remedy != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(color = Color(0xFFFEF2F2), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Build, contentDescription = null, tint = Color(0xFFB91C1C), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Recommended Vastu Remedy", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFB91C1C))
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(remedy, fontSize = 13.sp, color = Color(0xFF991B1B), lineHeight = 18.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Quick Zone Browser Fix for "Best"
            item { Text("ALL 16 ZONES COMPATIBILITY", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF3E2723), modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)) }
            
            items(ZONE_ORDER) { zoneKey ->
                val isFacing = zoneKey == currentZoneId
                val isSelected = zoneKey == activeZoneId
                val effect = item?.zoneEffects?.get(zoneKey) ?: item?.zoneEffects?.entries?.firstOrNull { it.key.equals(zoneKey, ignoreCase = true) }?.value
                val status = effect?.status ?: "Neutral"

                val (pillBg, pillTextColor) = when {
                    status.contains("Best", ignoreCase = true) || status.contains("Good", ignoreCase = true) || status.contains("Favorable", ignoreCase = true) -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
                    status.contains("Bad", ignoreCase = true) || status.contains("Avoid", ignoreCase = true) -> Color(0xFFFFEBEE) to Color(0xFFC62828)
                    else -> Color(0xFFFFF3E0) to Color(0xFFEF6C00)
                }

                val borderColor by animateColorAsState(if (isSelected) VastuGreen else if (isFacing) Saffron else Color.Transparent)
                Card(modifier = Modifier.fillMaxWidth().border(if (isSelected || isFacing) 2.dp else 0.dp, borderColor, RoundedCornerShape(12.dp)).clickable { manualSelectedZoneId = zoneKey }, shape = RoundedCornerShape(12.dp), backgroundColor = Color.White, elevation = if (isSelected) 3.dp else 1.dp) {
                    Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(zoneKey, fontWeight = FontWeight.Black, fontSize = 15.sp, color = if (isFacing) Saffron else Color(0xFF1E293B), modifier = Modifier.width(46.dp))
                            Column {
                                Text(ZONE_NAMES[zoneKey] ?: zoneKey, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF334155))
                                if (isFacing) Text("FACING NOW (${currentPada})", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Saffron)
                            }
                        }
                        Surface(color = pillBg, shape = RoundedCornerShape(6.dp)) {
                            Text(status, color = pillTextColor, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

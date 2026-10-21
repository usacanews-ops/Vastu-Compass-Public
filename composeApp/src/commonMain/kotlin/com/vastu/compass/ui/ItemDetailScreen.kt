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
import androidx.compose.ui.platform.LocalUriHandler
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
    val products by VastuRepository.productsFlow.collectAsState()
    val uriHandler = LocalUriHandler.current

    val item = pkg?.items?.firstOrNull { it.id == itemId }

    val currentPada = remember(rawDegrees) { calculatePada(rawDegrees) }
    val currentZoneId = remember(rawDegrees) { calculateZoneId(rawDegrees) }

    val currentZone = remember(currentZoneId, pkg) {
        pkg?.zones?.firstOrNull { it.id.equals(currentZoneId, ignoreCase = true) }
            ?: VastuZone(
                id = currentZoneId,
                name = ZONE_NAMES[currentZoneId] ?: currentZoneId,
                element = "Vastu",
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
        modifier = Modifier
            .fillMaxSize()
            .background(BeigeBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.White, CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF3E2723))
            }
            Spacer(modifier = Modifier.width(12.dp))
            item?.let {
                ItemIcon(
                    iconUrl = it.iconUrl.ifBlank { null },
                    fallbackKey = it.iconKey.ifBlank { it.title },
                    contentDescription = it.title,
                    modifier = Modifier.size(28.dp),
                    tint = VastuGreen
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = it.title.uppercase(),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1E293B)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 72.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    backgroundColor = Color.White,
                    elevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "LIVE FACING DIRECTION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${ZONE_NAMES[currentZoneId] ?: currentZoneId} (${currentZoneId} · ${currentPada})",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = VastuGreen
                        )
                        Text(
                            text = "${rawDegrees.toInt()}° Heading",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier.size(240.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val center = Offset(size.width / 2, size.height / 2)
                                val radius = size.minDimension / 2 - 14.dp.toPx()

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
                                            color = colorMap[i].copy(alpha = 0.25f),
                                            startAngle = startAngle,
                                            sweepAngle = 22.5f,
                                            useCenter = true,
                                            topLeft = Offset(center.x - radius, center.y - radius),
                                            size = Size(radius * 2, radius * 2)
                                        )
                                    }

                                    drawCircle(
                                        color = Color(0xFFD4AF37),
                                        radius = radius,
                                        style = Stroke(width = 2.dp.toPx())
                                    )

                                    for (i in 0 until 32) {
                                        val angleRad = (i * 11.25 - 90) * (PI / 180.0)
                                        val isMajor = i % 2 == 0
                                        val tickLength = if (isMajor) 12.dp.toPx() else 6.dp.toPx()

                                        val p1 = Offset(
                                            (center.x + (radius - tickLength) * cos(angleRad)).toFloat(),
                                            (center.y + (radius - tickLength) * sin(angleRad)).toFloat()
                                        )
                                        val p2 = Offset(
                                            (center.x + radius * cos(angleRad)).toFloat(),
                                            (center.y + radius * sin(angleRad)).toFloat()
                                        )
                                        drawLine(
                                            color = if (isMajor) Color(0xFF1E293B) else Color(0xFF94A3B8),
                                            start = p1,
                                            end = p2,
                                            strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                                        )
                                    }
                                }

                                val needle = Path().apply {
                                    moveTo(center.x, center.y - radius + 2.dp.toPx())
                                    lineTo(center.x - 7.dp.toPx(), center.y - radius + 22.dp.toPx())
                                    lineTo(center.x + 7.dp.toPx(), center.y - radius + 22.dp.toPx())
                                    close()
                                }
                                drawPath(needle, color = Color(0xFFDC2626))
                                drawCircle(Color(0xFF1E293B), radius = 5.dp.toPx(), center = center)
                            }

                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                elevation = 3.dp,
                                modifier = Modifier.size(76.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = currentZoneId,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = VastuGreen
                                    )
                                    Text(
                                        text = currentPada,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Saffron
                                    )
                                }
                            }
                        }

                        if (manualSelectedZoneId != null && manualSelectedZoneId != currentZoneId) {
                            TextButton(onClick = { manualSelectedZoneId = null }) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Saffron, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset to Live Sensor", color = Saffron, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                val statusText = activeEffect?.status ?: "Neutral"
                val (badgeBg, badgeTextColor, statusIcon) = when {
                    statusText.contains("Good", ignoreCase = true) || statusText.contains("Favorable", ignoreCase = true) || statusText.contains("Best", ignoreCase = true) ->
                        Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), Icons.Default.CheckCircle)
                    statusText.contains("Bad", ignoreCase = true) || statusText.contains("Avoid", ignoreCase = true) || statusText.contains("Strictly", ignoreCase = true) ->
                        Triple(Color(0xFFFFEBEE), Color(0xFFC62828), Icons.Default.Cancel)
                    else ->
                        Triple(Color(0xFFFFF3E0), Color(0xFFEF6C00), Icons.Default.Info)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    backgroundColor = Color.White,
                    elevation = 3.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${ZONE_NAMES[activeZoneId] ?: activeZoneId} (${activeZoneId})",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = if (activeZoneId == currentZoneId) "CURRENT FACING · PADA ${currentPada}" else "SELECTED ZONE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeZoneId == currentZoneId) Saffron else Color(0xFF64748B)
                                )
                            }
                            Surface(
                                color = badgeBg,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(statusIcon, contentDescription = null, tint = badgeTextColor, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = statusText.uppercase(),
                                        color = badgeTextColor,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))

                        Text(
                            text = "Vastu Effects in this Zone",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeEffect?.note?.ifBlank { null }
                                ?: activeZone.effects.ifBlank { "No severe negative effects recorded for this zone." },
                            fontSize = 14.sp,
                            color = Color(0xFF475569),
                            lineHeight = 20.sp
                        )

                        val remedy = activeEffect?.remedy?.ifBlank { null } ?: activeZone.remedies.ifBlank { null }
                        if (remedy != null) {
                            val linkedProduct = activeEffect?.productId?.let { pid ->
                                products.firstOrNull { it.id.equals(pid, ignoreCase = true) }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = Color(0xFFFFFBEB),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Build,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "RECOMMENDED REMEDY",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.5.sp,
                                            color = Color(0xFFB45309)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = remedy,
                                        fontSize = 13.sp,
                                        color = Color(0xFF78350F),
                                        lineHeight = 19.sp
                                    )

                                    if (!activeEffect?.productId.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = {
                                                val targetUrl = linkedProduct?.buyUrl?.ifBlank { null }
                                                    ?: "https://myfreetools.app/wp-content/plugins/apps/vastuconsultant/"
                                                try { uriHandler.openUri(targetUrl) } catch (_: Exception) {}
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                backgroundColor = Color(0xFFD97706),
                                                contentColor = Color.White
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (linkedProduct != null && linkedProduct.price.isNotBlank()) {
                                                    "Buy ${linkedProduct.title} (${linkedProduct.price})"
                                                } else if (linkedProduct != null) {
                                                    "Buy ${linkedProduct.title}"
                                                } else {
                                                    "View Remedy Product"
                                                },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "ALL 16 ZONES COMPATIBILITY",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF3E2723),
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }

            items(ZONE_ORDER) { zoneKey ->
                val isFacing = zoneKey == currentZoneId
                val isSelected = zoneKey == activeZoneId
                val effect = item?.zoneEffects?.get(zoneKey)
                    ?: item?.zoneEffects?.entries?.firstOrNull { it.key.equals(zoneKey, ignoreCase = true) }?.value
                val status = effect?.status ?: "Neutral"

                val (pillBg, pillTextColor) = when {
                    status.contains("Good", ignoreCase = true) || status.contains("Favorable", ignoreCase = true) || status.contains("Best", ignoreCase = true) ->
                        Color(0xFFE8F5E9) to Color(0xFF2E7D32)
                    status.contains("Bad", ignoreCase = true) || status.contains("Avoid", ignoreCase = true) || status.contains("Strictly", ignoreCase = true) ->
                        Color(0xFFFFEBEE) to Color(0xFFC62828)
                    else ->
                        Color(0xFFFFF3E0) to Color(0xFFEF6C00)
                }

                val borderColor by animateColorAsState(
                    when {
                        isSelected -> VastuGreen
                        isFacing -> Saffron
                        else -> Color.Transparent
                    }
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected || isFacing) 2.dp else 0.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { manualSelectedZoneId = zoneKey },
                    shape = RoundedCornerShape(12.dp),
                    backgroundColor = Color.White,
                    elevation = if (isSelected) 3.dp else 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = zoneKey,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = if (isFacing) Saffron else Color(0xFF1E293B),
                                modifier = Modifier.width(46.dp)
                            )
                            Column {
                                Text(
                                    text = ZONE_NAMES[zoneKey] ?: zoneKey,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF334155)
                                )
                                if (isFacing) {
                                    Text(
                                        text = "FACING NOW (${currentPada})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Saffron
                                    )
                                }
                            }
                        }

                        Surface(
                            color = pillBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = status,
                                color = pillTextColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

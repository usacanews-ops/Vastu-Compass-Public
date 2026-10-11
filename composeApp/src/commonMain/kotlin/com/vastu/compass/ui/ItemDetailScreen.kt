package com.vastu.compass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vastu.compass.data.VastuRepository

@Composable
fun ItemDetailScreen(itemId: String, onBack: () -> Unit) {
    val pkg by VastuRepository.dataFlow.collectAsState()
    val item = pkg?.items?.find { it.id == itemId }
    val zones = pkg?.zones ?: emptyList()

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF3EEEA))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Text(item?.title?.uppercase() ?: "DETAILS", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723))
        }

        if (item != null) {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(zones) { zone ->
                    val effect = item.zoneEffects[zone.id]
                    val statusColor = when (effect?.status) {
                        "GOOD" -> Color(0xFF2E7D32)
                        "BAD" -> Color(0xFFC62828)
                        else -> Color(0xFFF57F17)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        backgroundColor = Color.White,
                        elevation = 1.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("${zone.name} (${zone.id})", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF3E2723))
                                    Text(zone.element, fontSize = 12.sp, color = Color.Gray)
                                }
                                Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        effect?.status ?: "NEUTRAL",
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (effect != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Effects:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(effect.note, fontSize = 13.sp, color = Color(0xFF424242))

                                if (!effect.remedy.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Treatment:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                    Text(effect.remedy, fontSize = 13.sp, color = Color(0xFF424242))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

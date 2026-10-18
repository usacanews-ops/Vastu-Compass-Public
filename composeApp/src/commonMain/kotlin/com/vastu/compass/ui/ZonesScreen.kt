package com.vastu.compass.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vastu.compass.BeigeBackground
import com.vastu.compass.VastuGreen
import com.vastu.compass.data.VastuRepository

@Composable
fun ZonesScreen(onItemClick: (String) -> Unit) {
    val pkg by VastuRepository.dataFlow.collectAsState()
    val zones = pkg?.zones ?: emptyList()

    Column(
        modifier = Modifier.fillMaxSize().background(BeigeBackground).padding(16.dp)
    ) {
        Text("16 VASTU ZONES", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E293B))
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(zones) { zone ->
                var isExpanded by remember { mutableStateOf(false) }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    backgroundColor = Color.White,
                    elevation = 2.dp,
                    modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Weight(1f) ensures the title/pada takes available space without squeezing the Element Pill
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text("${zone.name} (${zone.id})", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Padas: ${zone.pada}", fontSize = 14.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                            }
                            
                            // Element Pill - wrapContentWidth prevents letter-by-letter squeezing
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = VastuGreen.copy(alpha = 0.1f),
                                modifier = Modifier.wrapContentWidth()
                            ) {
                                Text(zone.element, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = VastuGreen, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                            }
                            
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(top = 12.dp)) {
                                Divider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(bottom = 12.dp))

                                // Increased Text Sizes to 15.sp for readability
                                Text("Good Colors:", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF2E7D32))
                                Text(zone.goodColor, fontSize = 15.sp, color = Color(0xFF334155), modifier = Modifier.padding(bottom = 8.dp))

                                Text("Avoid Colors:", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFC62828))
                                Text(zone.badColor, fontSize = 15.sp, color = Color(0xFF334155), modifier = Modifier.padding(bottom = 8.dp))

                                Text("Key Attributes:", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1E293B))
                                Text(zone.attributes, fontSize = 15.sp, color = Color(0xFF475569), modifier = Modifier.padding(bottom = 8.dp), lineHeight = 20.sp)

                                Surface(
                                    color = Color(0xFFFFF8E1),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text("Remedies", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFFF57F17))
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(zone.remedies, fontSize = 15.sp, color = Color(0xFF3E2723), lineHeight = 22.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

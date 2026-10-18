package com.vastu.compass.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vastu.compass.data.VastuRepository

fun getZonesVectorIcon(iconKey: String): ImageVector {
    return when (iconKey) {
        "water_drop" -> Icons.Default.WaterDrop
        "opacity" -> Icons.Default.Opacity
        "delete" -> Icons.Default.Delete
        "directions_car" -> Icons.Default.DirectionsCar
        "electric_car" -> Icons.Default.ElectricCar
        "lock" -> Icons.Default.Lock
        "description" -> Icons.Default.Description
        "gavel" -> Icons.Default.Gavel
        "medication" -> Icons.Default.MedicalServices
        "countertops" -> Icons.Default.Countertops
        "wc" -> Icons.Default.Wc
        "park" -> Icons.Default.Park
        "menu_book" -> Icons.Default.MenuBook
        "face" -> Icons.Default.Face
        "brightness_7" -> Icons.Default.Brightness7
        else -> Icons.Default.Category
    }
}

@Composable
fun ZonesScreen(onItemClick: (String) -> Unit) {
    val pkg by VastuRepository.dataFlow.collectAsState()
    val items = pkg?.items ?: emptyList()

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF3EEEA)).padding(16.dp)) {
        Text("ZONES & UTILITIES", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3E2723))
        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                Card(
                    modifier = Modifier.aspectRatio(0.85f).clickable { onItemClick(item.id) },
                    shape = RoundedCornerShape(12.dp),
                    elevation = 2.dp,
                    backgroundColor = Color.White
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(getZonesVectorIcon(item.iconKey), contentDescription = item.title, tint = Color(0xFF8D6E63), modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            color = Color(0xFF424242)
                        )
                    }
                }
            }
        }
    }
}

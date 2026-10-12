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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vastu.compass.BeigeBackground
import com.vastu.compass.data.VastuRepository
import org.jetbrains.compose.resources.painterResource

// Import the auto-generated resources from your composeApp
import vastu_compass.composeapp.generated.resources.*

@Composable
fun getVectorPainter(iconKey: String): Painter {
    return when (iconKey) {
        "water_drop" -> painterResource(Res.drawable.ic_water_drop)
        "opacity" -> painterResource(Res.drawable.ic_opacity)
        "delete" -> painterResource(Res.drawable.ic_delete)
        "directions_car" -> painterResource(Res.drawable.ic_directions_car)
        "electric_car" -> painterResource(Res.drawable.ic_electric_car)
        "lock" -> painterResource(Res.drawable.ic_lock)
        "description" -> painterResource(Res.drawable.ic_description)
        "gavel" -> painterResource(Res.drawable.ic_gavel)
        "medication" -> painterResource(Res.drawable.ic_medication)
        "countertops" -> painterResource(Res.drawable.ic_countertops)
        "wc" -> painterResource(Res.drawable.ic_wc)
        "park" -> painterResource(Res.drawable.ic_park)
        "menu_book" -> painterResource(Res.drawable.ic_menu_book)
        "face" -> painterResource(Res.drawable.ic_mirror)
        "brightness_7" -> painterResource(Res.drawable.ic_mandir)
        else -> painterResource(Res.drawable.ic_description)
    }
}

@Composable
fun ZonesScreen(onItemClick: (String) -> Unit) {
    val pkg by VastuRepository.dataFlow.collectAsState()
    val items = pkg?.items ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BeigeBackground)
            .padding(16.dp)
    ) {
        Text(
            text = "ZONES & UTILITIES",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF3E2723)
        )
        Spacer(modifier = Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items) { item ->
                Card(
                    modifier = Modifier
                        .aspectRatio(0.85f)
                        .clickable { onItemClick(item.id) },
                    shape = RoundedCornerShape(12.dp),
                    elevation = 2.dp,
                    backgroundColor = Color.White
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Replaced Icons.Default.Info with your vector painter
                        Icon(
                            painter = getVectorPainter(item.iconKey),
                            contentDescription = item.title,
                            tint = Color(0xFF8D6E63),
                            modifier = Modifier.size(36.dp)
                        )
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

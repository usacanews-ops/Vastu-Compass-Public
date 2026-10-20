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
    val key = iconKey.lowercase()
    return when {
        key.contains("water") || key.contains("borewell") || key.contains("tubewell") -> Icons.Default.WaterDrop
        key.contains("filter") || key.contains("ro") || key.contains("drink") -> Icons.Default.LocalDrink
        key.contains("wc") || key.contains("toilet") || key.contains("bath") -> Icons.Default.Wc
        key.contains("bed") || key.contains("guest") || key.contains("room") -> Icons.Default.Home
        key.contains("gate") || key.contains("entrance") || key.contains("door") -> Icons.Default.MeetingRoom
        key.contains("stair") -> Icons.Default.Menu
        key.contains("balcony") || key.contains("terrace") -> Icons.Default.Home
        key.contains("tv") || key.contains("laptop") || key.contains("computer") -> Icons.Default.Devices
        key.contains("oven") || key.contains("microwave") || key.contains("fridge") -> Icons.Default.Kitchen
        key.contains("invertor") || key.contains("generator") || key.contains("power") -> Icons.Default.Power
        key.contains("heat") || key.contains("fire") -> Icons.Default.Whatshot
        key.contains("ac") || key.contains("conditioner") -> Icons.Default.Air
        key.contains("desk") || key.contains("study") || key.contains("bookshelf") || key.contains("book") -> Icons.Default.MenuBook
        key.contains("clock") || key.contains("time") -> Icons.Default.Schedule
        key.contains("shoe") || key.contains("iron") || key.contains("sew") -> Icons.Default.Layers
        key.contains("broom") || key.contains("mop") || key.contains("clean") -> Icons.Default.CleaningServices
        key.contains("mirror") || key.contains("face") -> Icons.Default.Face
        key.contains("ancestor") || key.contains("family") || key.contains("photo") -> Icons.Default.Image
        key.contains("medic") || key.contains("first aid") || key.contains("health") -> Icons.Default.MedicalServices
        key.contains("invest") || key.contains("share") || key.contains("file") -> Icons.Default.TrendingUp
        key.contains("storage") || key.contains("store") || key.contains("box") -> Icons.Default.AllInbox
        key.contains("plant") || key.contains("indoor") || key.contains("park") -> Icons.Default.Park
        key.contains("pet") || key.contains("cage") -> Icons.Default.Pets
        key.contains("pool") || key.contains("swim") -> Icons.Default.Pool
        key.contains("car") || key.contains("parking") -> Icons.Default.DirectionsCar
        key.contains("mandir") || key.contains("temple") || key.contains("pooja") -> Icons.Default.Brightness7
        key.contains("delete") || key.contains("dustbin") || key.contains("trash") -> Icons.Default.Delete
        key.contains("lock") || key.contains("safe") -> Icons.Default.Lock
        key.contains("kitchen") || key.contains("stove") || key.contains("counter") -> Icons.Default.Countertops
        key.contains("money") || key.contains("finance") -> Icons.Default.Payments
        key.contains("love") || key.contains("marriage") -> Icons.Default.Favorite
        key.contains("career") || key.contains("job") || key.contains("work") -> Icons.Default.Work
        key.contains("child") || key.contains("baby") -> Icons.Default.ChildCare
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
                        Icon(
                            getZonesVectorIcon(item.iconKey.ifBlank { item.title }),
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

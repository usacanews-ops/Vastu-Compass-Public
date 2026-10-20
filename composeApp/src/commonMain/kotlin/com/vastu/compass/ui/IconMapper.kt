package com.vastu.compass.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object IconMapper {
    fun get(key: String): ImageVector {
        val cleanKey = key.trim().lowercase()
        return when {
            cleanKey.contains("water") || cleanKey.contains("borewell") || cleanKey.contains("tubewell") -> Icons.Default.WaterDrop
            cleanKey.contains("filter") || cleanKey.contains("ro") || cleanKey.contains("drink") -> Icons.Default.LocalDrink
            cleanKey.contains("wc") || cleanKey.contains("toilet") || cleanKey.contains("bath") -> Icons.Default.Wc
            cleanKey.contains("bed") || cleanKey.contains("guest") || cleanKey.contains("room") -> Icons.Default.Home
            cleanKey.contains("gate") || cleanKey.contains("entrance") || cleanKey.contains("door") -> Icons.Default.MeetingRoom
            cleanKey.contains("stair") -> Icons.Default.Menu
            cleanKey.contains("balcony") || cleanKey.contains("terrace") -> Icons.Default.Home
            cleanKey.contains("tv") || cleanKey.contains("laptop") || cleanKey.contains("computer") -> Icons.Default.Devices
            cleanKey.contains("oven") || cleanKey.contains("microwave") || cleanKey.contains("fridge") -> Icons.Default.Kitchen
            cleanKey.contains("invertor") || cleanKey.contains("generator") || cleanKey.contains("power") -> Icons.Default.Power
            cleanKey.contains("heat") || cleanKey.contains("fire") -> Icons.Default.Whatshot
            cleanKey.contains("ac") || cleanKey.contains("conditioner") -> Icons.Default.Air
            cleanKey.contains("desk") || cleanKey.contains("study") || cleanKey.contains("bookshelf") || cleanKey.contains("book") -> Icons.Default.MenuBook
            cleanKey.contains("clock") || cleanKey.contains("time") -> Icons.Default.Schedule
            cleanKey.contains("shoe") || cleanKey.contains("iron") || cleanKey.contains("sew") -> Icons.Default.Layers
            cleanKey.contains("broom") || cleanKey.contains("mop") || cleanKey.contains("clean") -> Icons.Default.CleaningServices
            cleanKey.contains("mirror") || cleanKey.contains("face") -> Icons.Default.Face
            cleanKey.contains("ancestor") || cleanKey.contains("family") || cleanKey.contains("photo") -> Icons.Default.Image
            cleanKey.contains("medic") || cleanKey.contains("first aid") || cleanKey.contains("health") -> Icons.Default.MedicalServices
            cleanKey.contains("invest") || cleanKey.contains("share") || cleanKey.contains("file") -> Icons.Default.TrendingUp
            cleanKey.contains("storage") || cleanKey.contains("store") || cleanKey.contains("box") -> Icons.Default.AllInbox
            cleanKey.contains("plant") || cleanKey.contains("indoor") || cleanKey.contains("park") -> Icons.Default.Park
            cleanKey.contains("pet") || cleanKey.contains("cage") -> Icons.Default.Pets
            cleanKey.contains("pool") || cleanKey.contains("swim") -> Icons.Default.Pool
            cleanKey.contains("car") || cleanKey.contains("parking") -> Icons.Default.DirectionsCar
            cleanKey.contains("mandir") || cleanKey.contains("temple") || cleanKey.contains("pooja") -> Icons.Default.Brightness7
            cleanKey.contains("delete") || cleanKey.contains("dustbin") || cleanKey.contains("trash") -> Icons.Default.Delete
            cleanKey.contains("lock") || cleanKey.contains("safe") -> Icons.Default.Lock
            cleanKey.contains("kitchen") || cleanKey.contains("stove") || cleanKey.contains("counter") -> Icons.Default.Countertops
            cleanKey.contains("money") || cleanKey.contains("finance") -> Icons.Default.Payments
            cleanKey.contains("love") || cleanKey.contains("marriage") -> Icons.Default.Favorite
            cleanKey.contains("career") || cleanKey.contains("job") || cleanKey.contains("work") -> Icons.Default.Work
            cleanKey.contains("child") || cleanKey.contains("baby") -> Icons.Default.ChildCare
            else -> Icons.Default.Category
        }
    }
}

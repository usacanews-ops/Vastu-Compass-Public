package com.vastu.compass.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object IconMapper {
    private val iconMap: Map<String, ImageVector> = mapOf(
        // Rooms & Doors
        "bed" to Icons.Default.Home,
        "bedroom" to Icons.Default.Home,
        "guest" to Icons.Default.Home,
        "gate" to Icons.Default.MeetingRoom,
        "entrance" to Icons.Default.MeetingRoom,
        "staircase" to Icons.Default.Menu,
        "stairs" to Icons.Default.Menu,
        "balcony" to Icons.Default.Home,
        "terrace" to Icons.Default.Home,

        // Water & Sanitation
        "water" to Icons.Default.WaterDrop,
        "borewell" to Icons.Default.WaterDrop,
        "tubewell" to Icons.Default.WaterDrop,
        "water_filter" to Icons.Default.LocalDrink,
        "ro" to Icons.Default.LocalDrink,
        "toilet" to Icons.Default.Wc,
        "wc" to Icons.Default.Wc,
        "bathroom" to Icons.Default.Wc,
        "pool" to Icons.Default.Pool,

        // Kitchen & Heating
        "kitchen" to Icons.Default.Countertops,
        "stove" to Icons.Default.Countertops,
        "oven" to Icons.Default.Kitchen,
        "microwave" to Icons.Default.Kitchen,
        "heater" to Icons.Default.Whatshot,
        "fireplace" to Icons.Default.Whatshot,

        // Electronics & Power
        "power" to Icons.Default.Power,
        "invertor" to Icons.Default.Power,
        "generator" to Icons.Default.Power,
        "ac" to Icons.Default.Air,
        "tv" to Icons.Default.Devices,
        "laptop" to Icons.Default.Devices,
        "computer" to Icons.Default.Devices,

        // Office, Studies & Documents
        "study" to Icons.Default.MenuBook,
        "desk" to Icons.Default.MenuBook,
        "books" to Icons.Default.MenuBook,
        "bookshelf" to Icons.Default.MenuBook,
        "files" to Icons.Default.TrendingUp,
        "investment" to Icons.Default.TrendingUp,
        "shares" to Icons.Default.TrendingUp,

        // Photos & Health
        "family_photo" to Icons.Default.Image,
        "ancestor_photo" to Icons.Default.Portrait,
        "first_aid" to Icons.Default.MedicalServices,
        "medicine" to Icons.Default.MedicalServices,

        // Utilities, Storage & Outdoor
        "clock" to Icons.Default.Schedule,
        "shoe_rack" to Icons.Default.Layers,
        "storage" to Icons.Default.AllInbox,
        "clean" to Icons.Default.CleaningServices,
        "broom" to Icons.Default.CleaningServices,
        "iron" to Icons.Default.Layers,
        "sewing" to Icons.Default.Layers,
        "plants" to Icons.Default.Park,
        "pet" to Icons.Default.Pets,
        "mandir" to Icons.Default.Brightness7,
        "temple" to Icons.Default.Brightness7,
        "dustbin" to Icons.Default.Delete,
        "safe" to Icons.Default.Lock
    )

    fun get(key: String): ImageVector {
        val cleanKey = key.trim().lowercase()
        return iconMap[cleanKey]
            ?: iconMap.entries.firstOrNull { cleanKey.contains(it.key) }?.value
            ?: Icons.Default.Category
    }
}

package com.vastu.compass.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object IconMapper {
    fun get(key: String): ImageVector {
        val cleanKey = key.trim().lowercase()
        return when {
            // Living & Rooms
            cleanKey == "bed" || cleanKey.contains("master") -> Icons.Default.Bed
            cleanKey == "child_care" || cleanKey.contains("children") -> Icons.Default.ChildCare
            cleanKey == "hotel" || cleanKey.contains("guest") -> Icons.Default.Hotel
            cleanKey == "weekend" || cleanKey.contains("living") || cleanKey.contains("drawing") -> Icons.Default.Weekend
            cleanKey == "restaurant" || cleanKey.contains("dining") -> Icons.Default.Restaurant

            // Architecture & Layout
            cleanKey == "door_front" || cleanKey.contains("entrance") || cleanKey.contains("gate") -> Icons.Default.MeetingRoom
            cleanKey == "stairs" || cleanKey.contains("staircase") -> Icons.Default.Stairs
            cleanKey == "balcony" || cleanKey.contains("terrace") -> Icons.Default.Deck
            cleanKey == "inventory_2" || cleanKey.contains("heavy_storage") || cleanKey.contains("storeroom") -> Icons.Default.Inventory2
            cleanKey == "shelves" || cleanKey.contains("light_storage") -> Icons.Default.AllInbox

            // Sanitation & Drainage
            cleanKey == "wc" || cleanKey.contains("toilet") -> Icons.Default.Wc
            cleanKey == "shower" || cleanKey.contains("bathing") || cleanKey.contains("bathroom") -> Icons.Default.Shower
            cleanKey == "local_laundry_service" || cleanKey.contains("washing") -> Icons.Default.LocalLaundryService
            cleanKey == "delete" || cleanKey.contains("dustbin") || cleanKey.contains("trash") || cleanKey.contains("septic") -> Icons.Default.Delete
            cleanKey == "cleaning_services" || cleanKey.contains("broom") || cleanKey.contains("mop") -> Icons.Default.CleaningServices

            // Wealth & Assets
            cleanKey == "lock" || cleanKey.contains("safe") || cleanKey.contains("locker") -> Icons.Default.Lock
            cleanKey == "point_of_sale" || cleanKey.contains("cash") || cleanKey.contains("counter") -> Icons.Default.PointOfSale

            // Important Documents
            cleanKey == "description" || cleanKey.contains("property") || cleanKey.contains("papers") -> Icons.Default.Description
            cleanKey == "gavel" || cleanKey.contains("court") || cleanKey.contains("legal") -> Icons.Default.Gavel
            cleanKey == "trending_up" || cleanKey.contains("invest") || cleanKey.contains("share") -> Icons.Default.TrendingUp

            // Vehicles & Transport
            cleanKey == "directions_car" || cleanKey.contains("car") || cleanKey.contains("parking") -> Icons.Default.DirectionsCar
            cleanKey == "electric_car" || cleanKey.contains("ev") || cleanKey.contains("charging") -> Icons.Default.ElectricCar

            // Kitchen & Fire
            cleanKey == "countertops" || cleanKey.contains("kitchen") || cleanKey.contains("stove") -> Icons.Default.Countertops
            cleanKey == "microwave" || cleanKey.contains("oven") -> Icons.Default.Kitchen
            cleanKey == "battery_charging_full" || cleanKey.contains("inverter") || cleanKey.contains("generator") -> Icons.Default.BatteryChargingFull

            // Water & Utilities
            cleanKey == "water_drop" || cleanKey.contains("overhead") || cleanKey.contains("filter") -> Icons.Default.WaterDrop
            cleanKey == "opacity" || cleanKey.contains("underground") -> Icons.Default.Opacity
            cleanKey == "water" || cleanKey.contains("borewell") || cleanKey.contains("tubewell") -> Icons.Default.Water

            // Health & Healing
            cleanKey == "medication" || cleanKey.contains("medicine") -> Icons.Default.Medication
            cleanKey == "healing" || cleanKey.contains("first_aid") -> Icons.Default.Healing

            // Landscape & Open Spaces
            cleanKey == "park" || cleanKey.contains("lawn") || cleanKey.contains("yard") -> Icons.Default.Park
            cleanKey == "local_florist" || cleanKey.contains("plant") -> Icons.Default.LocalFlorist
            cleanKey == "pool" || cleanKey.contains("swim") -> Icons.Default.Pool

            // Education & Wisdom
            cleanKey == "menu_book" || cleanKey.contains("library") || cleanKey.contains("study") -> Icons.Default.MenuBook
            cleanKey == "desk" || cleanKey.contains("study_desk") -> Icons.Default.Desk
            cleanKey == "library_books" || cleanKey.contains("bookshelf") -> Icons.Default.LibraryBooks

            // Personal & Grooming
            cleanKey == "face" || cleanKey.contains("mirror") || cleanKey.contains("dressing") -> Icons.Default.Face

            // Spiritual & Temple
            cleanKey == "brightness_7" || cleanKey.contains("mandir") || cleanKey.contains("temple") -> Icons.Default.Brightness7
            cleanKey == "self_improvement" || cleanKey.contains("meditation") || cleanKey.contains("mat") -> Icons.Default.SelfImprovement

            // Electronics & Appliances
            cleanKey == "tv" || cleanKey.contains("television") -> Icons.Default.Tv
            cleanKey == "computer" || cleanKey.contains("laptop") -> Icons.Default.Computer
            cleanKey == "fireplace" || cleanKey.contains("heater") -> Icons.Default.Fireplace
            cleanKey == "ac_unit" || cleanKey.contains("ac") || cleanKey.contains("conditioner") -> Icons.Default.AcUnit
            cleanKey == "architecture" || cleanKey.contains("sewing") -> Icons.Default.Architecture
            cleanKey == "iron" || cleanKey.contains("ironing") -> Icons.Default.Iron

            // Decor & Furniture
            cleanKey == "schedule" || cleanKey.contains("clock") || cleanKey.contains("time") -> Icons.Default.Schedule
            cleanKey == "photo" || cleanKey.contains("family") -> Icons.Default.Photo
            cleanKey == "portrait" || cleanKey.contains("ancestor") -> Icons.Default.Portrait
            cleanKey == "ice_skating" || cleanKey.contains("shoe") -> Icons.Default.IceSkating
            cleanKey == "pets" || cleanKey.contains("pet") || cleanKey.contains("cage") -> Icons.Default.Pets

            else -> Icons.Default.Category
        }
    }
}

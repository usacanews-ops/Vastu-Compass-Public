package com.vastu.compass.model

import kotlinx.serialization.Serializable

@Serializable
data class VastuDataPackage(
    val version: Int = 1,
    val lastUpdated: String = "",
    val zones: List<VastuZone> = emptyList(),
    val padas: List<VastuPada> = emptyList(), // Mapscreen needs this
    val items: List<VastuItem> = emptyList(),
    val problemCategories: List<ProblemCategory> = emptyList(),
    val problems: List<Problem> = emptyList()
)

@Serializable
data class VastuZone(
    val id: String,
    val name: String,
    val sanskritName: String = "",
    val element: String,
    val shape: String = "",
    val goodColor: String = "",
    val badColor: String = "",
    val goodMetal: String = "",
    val badMetal: String = "",
    val startDeg: Float,
    val endDeg: Float,
    val strictlyAvoid: String = "",
    val goodBusiness: String = "",
    val badBusiness: String = "",
    val generalGoods: String = "",
    val generalBads: String = "",
    val attributes: String = "",
    val effects: String = "",
    val remedies: String = "",
    val pada: String = "" // Kept for backward compatibility
)

// MapScreen needs this entire data class to compile
@Serializable
data class VastuPada(
    val id: String,
    val zoneId: String,
    val deity: String,
    val startDeg: Float,
    val endDeg: Float,
    val entranceEffect: String,
    val isFavorableEntrance: Boolean,
    val entranceRemedy: String = ""
)

@Serializable
data class VastuItem(
    val id: String,
    val title: String,
    val category: String = "",
    val iconKey: String = "",
    val iconUrl: String = "",
    val isPadaSpecific: Boolean = false,
    val zoneEffects: Map<String, ZoneEffect> = emptyMap(),
    val padaEffects: Map<String, ZoneEffect> = emptyMap()
)

@Serializable
data class ZoneEffect(
    val id: String = "",
    val zoneId: String = "",
    val status: String = "Neutral",
    val note: String = "",
    val remedy: String? = null,
    val productId: String? = null
)

@Serializable
data class ProblemCategory(
    val id: String,
    val name: String,
    val iconKey: String = ""
)

@Serializable
data class Problem(
    val id: String,
    val categoryId: String,
    val title: String,
    val cause: String,
    val remedy: String,
    val solution: String
)

@Serializable
data class ProductPackage(
    val version: Int = 1,
    val products: List<VastuProduct> = emptyList()
)

@Serializable
data class VastuProduct(
    val id: String,
    val title: String,
    val description: String = "",
    val price: String = "",
    val imageUrl: String = "",
    val buyUrl: String = "",
    val category: String = ""
)

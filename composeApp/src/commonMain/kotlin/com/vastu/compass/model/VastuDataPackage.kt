package com.vastu.compass.model

import kotlinx.serialization.Serializable

@Serializable
data class VastuDataPackage(
    val version: Int,
    val lastUpdated: String,
    val zones: List<VastuZone> = emptyList(),
    val items: List<VastuItem> = emptyList(),
    val problemCategories: List<ProblemCategory> = emptyList(), // Added
    val problems: List<ProblemItem> = emptyList()               // Added
)

@Serializable
data class ProblemCategory(
    val id: String,
    val name: String,
    val iconKey: String
)

@Serializable
data class ProblemItem(
    val id: String,
    val categoryId: String,
    val title: String,
    val cause: String = "",
    val remedy: String = "",
    val solution: String = ""
)

@Serializable
data class VastuZone(
    val id: String,
    val name: String,
    val element: String,
    val startDeg: Float,
    val endDeg: Float,
    val attributes: String,
    val pada: String,
    val effects: String,
    val remedies: String,
    val goodColor: String = "",
    val badColor: String = "",
    val goodMetal: String = "",
    val badMetal: String = "",
    val shape: String = "",
    val generalGoods: String = "",
    val generalBads: String = "",
    val goodBusiness: String = "",
    val badBusiness: String = "",
    val strictlyAvoid: String = ""
)

@Serializable
data class VastuItem(
    val id: String,
    val title: String,
    val category: String,
    val iconKey: String,
    val zoneEffects: Map<String, ZoneEffect> = emptyMap()
)

@Serializable
data class ZoneEffect(
    val zoneId: String,
    val status: String,
    val note: String,
    val remedy: String? = null
)

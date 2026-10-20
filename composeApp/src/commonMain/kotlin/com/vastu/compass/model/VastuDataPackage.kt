package com.vastu.compass.model

import kotlinx.serialization.Serializable

@Serializable
data class VastuDataPackage(
    val version: Int = 1,
    val zones: List<VastuZone> = emptyList(),
    val items: List<VastuItem> = emptyList(),
    val problemCategories: List<ProblemCategory> = emptyList(),
    val problems: List<Problem> = emptyList()
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
    val category: String = "",
    val iconKey: String = "",
    val iconUrl: String = "",
    val zoneEffects: Map<String, ZoneEffect> = emptyMap()
)

@Serializable
data class ZoneEffect(
    val id: String = "",
    val zoneId: String = "",
    val status: String = "Neutral",
    val note: String = "",
    val remedy: String? = null
)

@Serializable
data class ProblemCategory(
    val id: String,
    val name: String
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

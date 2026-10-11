package com.vastu.compass.model

import kotlinx.serialization.Serializable

@Serializable
data class VastuDataPackage(
    val version: Int,
    val lastUpdated: String,
    val zones: List<VastuZoneDto>,
    val items: List<VastuItemDto>
)

@Serializable
data class VastuZoneDto(
    val id: String,
    val name: String,
    val element: String,
    val startDeg: Float,
    val endDeg: Float,
    val attributes: String,
    val pada: String,
    val effects: String,
    val remedies: String
)

@Serializable
data class ItemZoneEffectDto(
    val zoneId: String,
    val status: String,
    val note: String,
    val remedy: String? = null
)

@Serializable
data class VastuItemDto(
    val id: String,
    val title: String,
    val category: String,
    val iconKey: String,
    val zoneEffects: Map<String, ItemZoneEffectDto>
)

package com.vahitkeskin.targetping.domain.model

import java.util.UUID

data class TargetLocation(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
    val isActive: Boolean = true,
    val lastTriggered: Long = 0 // Timestamp
)
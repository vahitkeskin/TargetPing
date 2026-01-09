package com.vahitkeskin.targetping.domain.repository

import com.vahitkeskin.targetping.domain.model.TargetLocation
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun getAllTargets(): Flow<List<TargetLocation>>
    fun getActiveTargets(): Flow<List<TargetLocation>>
    suspend fun insertTarget(target: TargetLocation)
    suspend fun deleteTarget(id: String)
    suspend fun updateTarget(target: TargetLocation)
    suspend fun toggleActiveState(id: String, isActive: Boolean)
}
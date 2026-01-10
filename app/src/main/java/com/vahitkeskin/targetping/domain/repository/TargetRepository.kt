package com.vahitkeskin.targetping.domain.repository

import com.vahitkeskin.targetping.domain.model.TargetLocation
import kotlinx.coroutines.flow.Flow

interface TargetRepository {
    fun getTargets(): Flow<List<TargetLocation>>
    suspend fun getAllTargets(): List<TargetLocation>
    suspend fun getTargetById(id: String): TargetLocation?
    suspend fun insertTarget(target: TargetLocation)
    suspend fun deleteTarget(id: String)
    suspend fun updateTargetStatus(id: String, isActive: Boolean)
}
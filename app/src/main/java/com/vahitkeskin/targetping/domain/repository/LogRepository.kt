package com.vahitkeskin.targetping.domain.repository

import com.vahitkeskin.targetping.data.local.entity.LogEntity
import kotlinx.coroutines.flow.Flow

interface LogRepository {
    fun getAllLogs(): Flow<List<LogEntity>>
    suspend fun logEvent(targetName: String, type: com.vahitkeskin.targetping.data.local.entity.LogEventType, message: String)
    suspend fun clearAll()
}
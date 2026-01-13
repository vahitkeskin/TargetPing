package com.vahitkeskin.targetping.data.repository

import com.vahitkeskin.targetping.data.local.dao.LogDao
import com.vahitkeskin.targetping.data.local.entity.LogEntity
import com.vahitkeskin.targetping.data.local.entity.LogEventType
import com.vahitkeskin.targetping.domain.repository.LogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LogRepositoryImpl @Inject constructor(
    private val dao: LogDao
) : LogRepository {

    override fun getAllLogs(): Flow<List<LogEntity>> = dao.getAllLogs()

    override suspend fun logEvent(targetName: String, type: LogEventType, message: String) {
        val log = LogEntity(
            targetName = targetName,
            eventType = type,
            timestamp = System.currentTimeMillis(),
            message = message
        )
        dao.insertLog(log)
    }

    override suspend fun clearAll() = dao.clearLogs()
}
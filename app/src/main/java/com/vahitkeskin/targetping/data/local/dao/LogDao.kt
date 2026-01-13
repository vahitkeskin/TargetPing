package com.vahitkeskin.targetping.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.vahitkeskin.targetping.data.local.entity.LogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<LogEntity>>

    @Insert
    suspend fun insertLog(log: LogEntity)

    @Query("DELETE FROM activity_logs")
    suspend fun clearLogs()
}
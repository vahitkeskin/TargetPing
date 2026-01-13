package com.vahitkeskin.targetping.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_logs")
data class LogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val targetName: String,
    val eventType: LogEventType, // ENTRY veya EXIT
    val timestamp: Long,
    val message: String
)

enum class LogEventType {
    ENTRY, EXIT, SYSTEM
}
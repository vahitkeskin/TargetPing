package com.vahitkeskin.targetping.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import com.vahitkeskin.targetping.domain.model.TargetLocation
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "targets")
data class TargetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
    val isActive: Boolean,
    val lastTriggered: Long
) {
    fun toDomain() = TargetLocation(id, name, latitude, longitude, radiusMeters, isActive, lastTriggered)
}

fun TargetLocation.toEntity() = TargetEntity(id, name, latitude, longitude, radiusMeters, isActive, lastTriggered)

@Dao
interface TargetDao {
    @Query("SELECT * FROM targets")
    fun getAll(): Flow<List<TargetEntity>>

    @Query("SELECT * FROM targets WHERE isActive = 1")
    fun getActive(): Flow<List<TargetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(target: TargetEntity)

    @Query("DELETE FROM targets WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE targets SET isActive = :isActive WHERE id = :id")
    suspend fun updateState(id: String, isActive: Boolean)
}

@Database(entities = [TargetEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun targetDao(): TargetDao
}
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
    // Yeni eklenen alan (Varsayılan 0: Hiç tetiklenmedi)
    val lastTriggered: Long = 0
) {
    // Entity -> Domain dönüşümü
    fun toDomain() = TargetLocation(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        radiusMeters = radiusMeters,
        isActive = isActive,
        lastTriggered = lastTriggered
    )
}

fun TargetLocation.toEntity() =
    TargetEntity(id, name, latitude, longitude, radiusMeters, isActive, lastTriggered)

@Dao
interface TargetDao {
    // RepositoryImpl içinde "dao.getAllTargetsFlow()" dedik, o yüzden isim bu olmalı:
    @Query("SELECT * FROM targets")
    fun getAllTargetsFlow(): Flow<List<TargetEntity>>

    // RepositoryImpl içinde "dao.getAllTargetsOneShot()" dedik:
    @Query("SELECT * FROM targets")
    suspend fun getAllTargetsOneShot(): List<TargetEntity>

    @Query("SELECT * FROM targets WHERE id = :id")
    suspend fun getTargetById(id: String): TargetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTarget(target: TargetEntity)

    @Query("DELETE FROM targets WHERE id = :id")
    suspend fun deleteTarget(id: String)

    @Query("UPDATE targets SET isActive = :isActive WHERE id = :id")
    suspend fun updateTargetStatus(id: String, isActive: Boolean)
}

@Database(entities = [TargetEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun targetDao(): TargetDao // Bu fonksiyon ismi AppModule ile aynı olmalı
}
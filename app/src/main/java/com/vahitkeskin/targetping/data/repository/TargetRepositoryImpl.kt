package com.vahitkeskin.targetping.data.repository

import com.vahitkeskin.targetping.data.local.TargetDao
import com.vahitkeskin.targetping.data.local.TargetEntity
import com.vahitkeskin.targetping.domain.model.TargetLocation
import com.vahitkeskin.targetping.domain.repository.TargetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TargetRepositoryImpl @Inject constructor(
    private val dao: TargetDao
) : TargetRepository {

    // 1. UI (ViewModel) için Canlı Akış
    override fun getTargets(): Flow<List<TargetLocation>> {
        return dao.getAllTargetsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // 2. Servis (Background) için Tek Seferlik Liste
    override suspend fun getAllTargets(): List<TargetLocation> {
        return dao.getAllTargetsOneShot().map { it.toDomain() }
    }

    // 3. ID ile Hedef Bulma
    override suspend fun getTargetById(id: String): TargetLocation? {
        return dao.getTargetById(id)?.toDomain()
    }

    // 4. Hedef Ekleme (veya Güncelleme)
    override suspend fun insertTarget(target: TargetLocation) {
        dao.insertTarget(target.toEntity())
    }

    // 5. Hedef Silme
    override suspend fun deleteTarget(id: String) {
        dao.deleteTarget(id)
    }

    // 6. Aktiflik Durumunu Değiştirme
    override suspend fun updateTargetStatus(id: String, isActive: Boolean) {
        dao.updateTargetStatus(id, isActive)
    }

    // --- MAPPERS (Dönüştürücüler) ---
    // (Bu fonksiyonlar dışarıdan çağrılmaz, sadece burada kullanılır)

    private fun TargetEntity.toDomain(): TargetLocation {
        return TargetLocation(
            id = id,
            name = name,
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters,
            isActive = isActive,
            lastTriggered = lastTriggered
        )
    }

    private fun TargetLocation.toEntity(): TargetEntity {
        return TargetEntity(
            id = id,
            name = name,
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters,
            isActive = isActive,
            lastTriggered = lastTriggered
        )
    }
}
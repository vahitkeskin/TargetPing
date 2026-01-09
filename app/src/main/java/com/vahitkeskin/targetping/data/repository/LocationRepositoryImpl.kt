package com.vahitkeskin.targetping.data.repository

import com.vahitkeskin.targetping.data.local.TargetDao
import com.vahitkeskin.targetping.data.local.toEntity
import com.vahitkeskin.targetping.domain.model.TargetLocation
import com.vahitkeskin.targetping.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * LocationRepositoryImpl
 *
 * Bu sınıf, Domain katmanındaki 'LocationRepository' arayüzünü (interface) uygular.
 * Görevi: Domain katmanından gelen saf veriyi (TargetLocation), veritabanı formatına (TargetEntity)
 * çevirip DAO'ya iletmek veya tam tersini yapmaktır.
 *
 * @param dao Room veritabanı erişim nesnesi (Data Access Object).
 */
class LocationRepositoryImpl @Inject constructor(
    private val dao: TargetDao
) : LocationRepository {

    /**
     * Tüm hedefleri veritabanından çeker ve Domain modeline (TargetLocation) dönüştürür.
     * Flow kullanıldığı için veritabanında bir değişiklik olduğunda UI otomatik güncellenir.
     */
    override fun getAllTargets(): Flow<List<TargetLocation>> {
        return dao.getAll().map { entities ->
            entities.map { entity -> entity.toDomain() }
        }
    }

    /**
     * Sadece 'aktif' (takip edilecek) olan hedefleri getirir.
     * Servis (Background Service) genelde bunu dinler.
     */
    override fun getActiveTargets(): Flow<List<TargetLocation>> {
        return dao.getActive().map { entities ->
            entities.map { entity -> entity.toDomain() }
        }
    }

    /**
     * Yeni bir hedefi veritabanına kaydeder.
     * Domain modelini (TargetLocation) veritabanı entity'sine (TargetEntity) çevirir.
     */
    override suspend fun insertTarget(target: TargetLocation) {
        dao.insert(target.toEntity())
    }

    /**
     * Bir hedefi ID'sine göre siler.
     */
    override suspend fun deleteTarget(id: String) {
        dao.delete(id)
    }

    /**
     * Mevcut bir hedefi günceller (Örn: Son tetiklenme zamanı değiştiğinde).
     */
    override suspend fun updateTarget(target: TargetLocation) {
        dao.insert(target.toEntity()) // Room'da OnConflictStrategy.REPLACE olduğu için insert güncelleme de yapar.
    }

    /**
     * Bir hedefin aktiflik durumunu (açık/kapalı) değiştirir.
     */
    override suspend fun toggleActiveState(id: String, isActive: Boolean) {
        dao.updateState(id, isActive)
    }
}
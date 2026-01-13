package com.vahitkeskin.targetping.di

import android.app.Application
import android.content.Context
import android.content.Context.VIBRATOR_MANAGER_SERVICE
import android.content.Context.VIBRATOR_SERVICE
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import androidx.room.Room
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.vahitkeskin.targetping.data.local.AppDatabase
import com.vahitkeskin.targetping.data.local.TargetDao
import com.vahitkeskin.targetping.data.local.dao.LogDao
import com.vahitkeskin.targetping.data.repository.LogRepositoryImpl
import com.vahitkeskin.targetping.domain.repository.LogRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(app: Application): AppDatabase {
        return Room.databaseBuilder(
            app,
            AppDatabase::class.java,
            "target_ping_db" // Veritabanı ismini proje ile uyumlu yaptım
        ).build()
    }

    // EKSİK OLAN PARÇA BU:
    // Hilt'e DAO'yu nasıl bulacağını söylemeliyiz.
    @Provides
    @Singleton
    fun provideTargetDao(db: AppDatabase): TargetDao {
        return db.targetDao()
    }

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(context)
    }

    @Provides
    @Singleton
    fun provideVibrator(
        @ApplicationContext context: Context
    ): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(VIBRATOR_SERVICE) as Vibrator
        }
    }

    @Provides
    @Singleton
    fun provideLogDao(db: AppDatabase): LogDao = db.logDao()

    @Provides
    @Singleton
    fun provideLogRepository(dao: LogDao): LogRepository {
        return LogRepositoryImpl(dao)
    }
}
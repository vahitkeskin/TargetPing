package com.vahitkeskin.targetping.di

import android.app.Application
import androidx.room.Room
import com.vahitkeskin.targetping.data.local.AppDatabase
import com.vahitkeskin.targetping.data.repository.LocationRepositoryImpl
import com.vahitkeskin.targetping.domain.repository.LocationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
            "radius_alert_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideLocationRepository(db: AppDatabase): LocationRepository {
        return LocationRepositoryImpl(db.targetDao())
    }
}
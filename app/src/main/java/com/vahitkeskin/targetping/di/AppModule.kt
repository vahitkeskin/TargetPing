package com.vahitkeskin.targetping.di

import android.app.Application
import androidx.room.Room
import com.vahitkeskin.targetping.data.local.AppDatabase
import com.vahitkeskin.targetping.data.local.TargetDao // Bunu ekledik
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
}
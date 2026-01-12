package com.vahitkeskin.targetping.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.vahitkeskin.targetping.data.repository.AppSettingsRepositoryImpl
import com.vahitkeskin.targetping.data.repository.TargetRepositoryImpl
import com.vahitkeskin.targetping.domain.repository.AppSettingsRepository
import com.vahitkeskin.targetping.domain.repository.TargetRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // Target Repository Bağlaması (Daha önce yazmıştık, varsa buradadır)
    @Binds
    @Singleton
    abstract fun bindTargetRepository(
        impl: TargetRepositoryImpl
    ): TargetRepository

    // YENİ: AppSettings Repository Bağlaması
    @Binds
    @Singleton
    abstract fun bindAppSettingsRepository(
        impl: AppSettingsRepositoryImpl
    ): AppSettingsRepository
}

// DataStore Sağlayıcısı (Ayrı bir DataModule içinde de olabilir, buraya da ekleyebilirsin)
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("app_settings") }
        )
    }
}
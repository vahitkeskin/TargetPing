package com.vahitkeskin.targetping.di

import com.vahitkeskin.targetping.data.repository.TargetRepositoryImpl
import com.vahitkeskin.targetping.domain.repository.TargetRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton // Bunu eklemek iyi bir pratiktir (Tek bir instance olsun)
    abstract fun bindTargetRepository(
        targetRepositoryImpl: TargetRepositoryImpl
    ): TargetRepository
}
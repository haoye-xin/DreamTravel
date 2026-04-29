package com.dreamtravel.di

import com.dreamtravel.data.repository.DreamRepository
import com.dreamtravel.data.repository.DreamRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDreamRepository(impl: DreamRepositoryImpl): DreamRepository
}

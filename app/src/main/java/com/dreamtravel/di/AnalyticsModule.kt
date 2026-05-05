package com.dreamtravel.di

import com.dreamtravel.analytics.AnalyticsManager
import com.dreamtravel.analytics.LogAnalyticsManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {

    @Binds
    @Singleton
    abstract fun bindAnalyticsManager(impl: LogAnalyticsManager): AnalyticsManager
}

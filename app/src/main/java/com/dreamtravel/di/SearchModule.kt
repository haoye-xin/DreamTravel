package com.dreamtravel.di

import android.content.Context
import com.amap.api.services.geocoder.GeocodeSearch
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SearchModule {

    @Provides
    @Singleton
    fun provideGeocodeSearch(
        @ApplicationContext context: Context
    ): GeocodeSearch {
        return GeocodeSearch(context)
    }
}

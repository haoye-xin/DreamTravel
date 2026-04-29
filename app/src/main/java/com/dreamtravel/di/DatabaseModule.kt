package com.dreamtravel.di

import android.content.Context
import androidx.room.Room
import com.dreamtravel.data.local.AppDatabase
import com.dreamtravel.data.local.dao.DwellEventDao
import com.dreamtravel.data.local.dao.PlaceDao
import com.dreamtravel.data.local.dao.TodoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "dream_travel.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun providePlaceDao(db: AppDatabase): PlaceDao = db.placeDao()

    @Provides
    fun provideTodoDao(db: AppDatabase): TodoDao = db.todoDao()

    @Provides
    fun provideDwellEventDao(db: AppDatabase): DwellEventDao = db.dwellEventDao()
}

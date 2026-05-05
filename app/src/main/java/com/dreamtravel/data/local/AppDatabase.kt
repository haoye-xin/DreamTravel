package com.dreamtravel.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dreamtravel.data.local.dao.DwellEventDao
import com.dreamtravel.data.local.dao.PlaceDao
import com.dreamtravel.data.local.dao.TodoDao
import com.dreamtravel.data.local.entity.DwellEventEntity
import com.dreamtravel.data.local.entity.PlaceEntity
import com.dreamtravel.data.local.entity.TodoEntity

@Database(
    entities = [
        PlaceEntity::class,
        TodoEntity::class,
        DwellEventEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun placeDao(): PlaceDao
    abstract fun todoDao(): TodoDao
    abstract fun dwellEventDao(): DwellEventDao
}

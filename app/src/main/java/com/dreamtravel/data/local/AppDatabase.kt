package com.dreamtravel.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun placeDao(): PlaceDao
    abstract fun todoDao(): TodoDao
    abstract fun dwellEventDao(): DwellEventDao

    companion object {
        private fun getColumnNames(db: SupportSQLiteDatabase, table: String): Set<String> {
            val names = mutableSetOf<String>()
            try {
                db.query("PRAGMA table_info($table)").use { cursor ->
                    val nameIdx = cursor.getColumnIndex("name")
                    while (cursor.moveToNext()) {
                        names.add(cursor.getString(nameIdx))
                    }
                }
            } catch (_: Exception) {
                // Table doesn't exist
            }
            return names
        }

        private fun ensureColumns(db: SupportSQLiteDatabase, table: String, columns: Map<String, String>) {
            val existing = getColumnNames(db, table)
            for ((col, type) in columns) {
                if (col !in existing) {
                    db.execSQL("ALTER TABLE $table ADD COLUMN $col $type")
                }
            }
        }

        private fun ensureDwellEventsTable(db: SupportSQLiteDatabase) {
            db.execSQL("""CREATE TABLE IF NOT EXISTS `dwell_events` (
                `id` TEXT NOT NULL PRIMARY KEY,
                `placeId` TEXT NOT NULL,
                `enteredAt` INTEGER NOT NULL,
                `exitedAt` INTEGER,
                `triggeredAt` INTEGER,
                `status` TEXT NOT NULL DEFAULT 'ENTERED'
            )""")
        }

        private fun migrateAll(db: SupportSQLiteDatabase) {
            ensureColumns(db, "todos", mapOf(
                "provinceCode" to "TEXT",
                "provinceName" to "TEXT",
                "cityCode" to "TEXT",
                "cityName" to "TEXT",
                "districtCode" to "TEXT",
                "districtName" to "TEXT",
                "formattedAddress" to "TEXT",
                "color" to "TEXT"
            ))
            ensureColumns(db, "places", mapOf(
                "cityCode" to "TEXT"
            ))
            ensureDwellEventsTable(db)
        }

        val MIGRATION_1_4 = object : Migration(1, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateAll(db)
            }
        }

        val MIGRATION_2_4 = object : Migration(2, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                migrateAll(db)
            }
        }
    }
}
